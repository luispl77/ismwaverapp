package com.emwaver.ismwaver.ui.packetmode;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.CommandSender;
import com.emwaver.ismwaver.OtherOptionActivity;
import com.emwaver.ismwaver.R;
import com.emwaver.ismwaver.SerialService;
import com.emwaver.ismwaver.SettingsActivity;
import com.emwaver.ismwaver.databinding.FragmentPacketModeBinding;
import com.emwaver.ismwaver.jsobjects.CC1101;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.renderer.LineChartRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PacketModeFragment extends Fragment implements CommandSender, OnSelectionChangedListener {

    private FragmentPacketModeBinding binding;

    private PacketModeViewModel packetModeViewModel;

    private CC1101 cc;
    private SerialService serialService;
    private boolean isServiceBound = false;

    private LineChart chart;
    List<Entry> entries;
    private String modulationSelected;
    private int dataRate;
    private String preambleSelected;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            Log.i("service binding", "onServiceDisconnected");
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        packetModeViewModel = new ViewModelProvider(this).get(PacketModeViewModel.class);

        setHasOptionsMenu(true);

        cc = new CC1101(this);

        binding = FragmentPacketModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        InputFilter hexFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.toString(source.charAt(i)).matches("[0-9a-fA-F]*")) {
                    return "";
                }
            }
            return null;
        };

        Button showGraph = binding.showGraph;

        chart = binding.chart;

        binding.sendTesla.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    byte [] teslaSignal = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xCB, (byte)0x8A, 50, -52, -52, -53, 77, 45, 74, -45, 76, -85, 75, 21, -106, 101, -103, -103, -106, -102, 90, -107, -90, -103, 86, -106, 43, 44, -53, 51, 51, 45, 52, -75, 43, 77, 50, -83, 40};
                    cc.sendData(teslaSignal, teslaSignal.length, 300);
                    showToastOnUiThread("tesla signal sent");
                }).start();
            }
        });

        binding.manchesterSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = binding.manchesterSwitch.isChecked();
                // isChecked will be true if the switch is currently to the right (Manchester)
                new Thread(() -> {
                    if(cc.setManchesterEncoding(isChecked)) {
                        showToastOnUiThread("Manchester encoding set successfully to " + isChecked);
                    } else {
                        showToastOnUiThread("Failed to set encoding");
                        // Revert the switch to its previous state on failure
                        // Must run on UI thread as it modifies the view
                        getActivity().runOnUiThread(() -> binding.manchesterSwitch.setChecked(!isChecked));
                    }
                }).start();
            }
        });

        binding.datarateTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                new Thread(() -> {
                    String dataRateStr = binding.datarateTextInput.getText().toString();
                    // Parse the string to an integer
                    try {
                        dataRate = Integer.parseInt(dataRateStr);

                        // Now use dataRate to set the data rate
                        if (cc.setDataRate(dataRate)) {
                            showToastOnUiThread("Data rate set to " + dataRate + " successfully");
                        } else {
                            showToastOnUiThread("Error setting data rate");
                        }
                    } catch (NumberFormatException e) {
                        showToastOnUiThread("Invalid data rate entered");
                    }
                }).start();
            }
            return false;
        });

        binding.deviationTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                new Thread(() -> {
                    String deviationStr = binding.deviationTextInput.getText().toString().trim();
                    // Parse the string to an integer
                    try {
                        int deviation = Integer.parseInt(deviationStr);

                        // Now use deviation to set the deviation
                        if (cc.setDeviation(deviation)) {
                            showToastOnUiThread("Deviation set to " + deviation + " successfully");
                        } else {
                            showToastOnUiThread("Error setting deviation");
                        }
                    } catch (NumberFormatException e) {
                        showToastOnUiThread("Invalid deviation entered");
                    }
                }).start();
                return true; // Consume the action
            }
            return false; // Pass the event on to other listeners
        });

        binding.syncwordTextInput.setFilters(new InputFilter[]{hexFilter});
        binding.syncwordTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                new Thread(() -> {
                    String hexInput = binding.syncwordTextInput.getText().toString().trim();
                    // Check if the input is a 4-character hex string
                    if (hexInput.length() != 4) {
                        showToastOnUiThread("Input must be a 4-character hex value");
                        return;
                    }
                    // Convert hex string to byte array
                    byte[] syncWord = cc.convertHexStringToByteArray(hexInput);
                    if (syncWord == null) {
                        showToastOnUiThread("Invalid hex input");
                        return;
                    }
                    // Set the sync word
                    if (cc.setSyncWord(syncWord)) {
                        showToastOnUiThread("Sync word set successfully to " + hexInput);
                    } else {
                        showToastOnUiThread("Error setting Sync word");
                    }
                }).start();
            }
            return false;
        });

        binding.initTransmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInit();
                    showToastOnUiThread("check terminal");
                }).start();
            }
        });

        binding.initReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInitRx();
                    showToastOnUiThread("check terminal");
                }).start();
            }
        });

        binding.receivePayloadDataTextInput.setFilters(new InputFilter[]{hexFilter});

        // Set an OnClickListener for the button
        binding.sendPayloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String payload = binding.transmitPayloadDataTextInput.getText().toString();
                Log.i("Payload", payload);
                byte [] payload_bytes = cc.convertHexStringToByteArray(payload);

                new Thread(() -> {
                    cc.sendData(payload_bytes, payload_bytes.length, 300);
                }).start();
            }
        });

        binding.receivePayloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    byte [] receivedBytes = cc.receiveData();
                    if(receivedBytes == null){
                        showToastOnUiThread("no data in fifo");
                        return;
                    }
                    Log.i("Received", cc.toHexStringWithHexPrefix(receivedBytes));
                    String hexString = cc.bytesToHexString(receivedBytes);
                    getActivity().runOnUiThread(() ->
                            binding.receivePayloadDataTextInput.setText(hexString));
                }).start();
            }
        });

        binding.transferPayloadTxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().runOnUiThread(() ->
                        binding.transmitPayloadDataTextInput.setText(binding.receivePayloadDataTextInput.getText().toString()));
            }
        });


        String[] modulations = getResources().getStringArray(R.array.modulations);
        ArrayAdapter<String> modulationsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, modulations);
        binding.modulationSelector.setAdapter(modulationsAdapter);
        binding.modulationSelector.setOnClickListener(v -> binding.modulationSelector.showDropDown());

        binding.modulationSelector.setOnItemClickListener((parent, view, position, id) -> {
            // Get the selected item
            modulationSelected = (String) parent.getItemAtPosition(position);
            new Thread(() -> {
                // Handle the selection
                if ("ASK".equals(modulationSelected)) {
                    if(cc.setModulation(CC1101.MOD_ASK)){
                        showToastOnUiThread("modulation set successfully to ASK");
                    }
                    else
                        showToastOnUiThread("Failed to set modulation");
                } else if ("FSK".equals(modulationSelected)) {
                    if(cc.setModulation(CC1101.MOD_2FSK)){
                        showToastOnUiThread("modulation set successfully to 2FSK");
                    }
                    else
                        showToastOnUiThread("Failed to set modulation");
                }
            }).start();
        });

        String[] preambles = getResources().getStringArray(R.array.preambles);
        ArrayAdapter<String> preamblesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, preambles);
        binding.preambleSelector.setAdapter(preamblesAdapter);
        binding.preambleSelector.setOnClickListener(v -> binding.preambleSelector.showDropDown());

        binding.preambleSelector.setOnItemClickListener((parent, view, position, id) -> {
            preambleSelected = (String) parent.getItemAtPosition(position);
            new Thread(() -> {
                // Handle the selection based on index
                if(cc.setNumPreambleBytes(position)) {
                    showToastOnUiThread("Preamble set successfully to index " + position);
                } else {
                    showToastOnUiThread("Failed to set preamble");
                }
            }).start();
        });

        String[] syncmodes = getResources().getStringArray(R.array.sync_modes);
        ArrayAdapter<String> syncmodeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, syncmodes);
        binding.syncModeSelector.setAdapter(syncmodeAdapter);
        binding.syncModeSelector.setOnClickListener(v -> binding.syncModeSelector.showDropDown());

        binding.syncModeSelector.setOnItemClickListener((parent, view, position, id) -> {
            new Thread(() -> {
                // Handle the selection based on index
                if(cc.setSyncMode((byte)position)) {
                    showToastOnUiThread("Sync mode set successfully to index " + position);
                } else {
                    showToastOnUiThread("Failed to set sync mode");
                }
            }).start();
        });

        binding.receivePayloadDataTextInput.addTextChangedListener(new TextWatcher() {

            //#region "Not Used/Implemented Functions"

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable editable) { }

            //#endregion

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() > 0)
                    showGraph.setVisibility(View.VISIBLE);
                else
                    showGraph.setVisibility(View.INVISIBLE);
            }
        });

        binding.receivePayloadDataTextInput.setOnSelectionChangedListener(this);

        showGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String hexString = preambleSelected + binding.syncwordTextInput.getText().toString() + binding.receivePayloadDataTextInput.getText().toString();
                String bitString = hexToBinary(hexString);

                createWaveModulation(bitString);

                configureChart(entries);

                //SpannableString spannableString = new SpannableString(binding.receivePayloadDataTextInput.getText().toString());
                //BackgroundColorSpan highlightSpan = new BackgroundColorSpan(Color.YELLOW);
                //spannableString.setSpan(highlightSpan, 0, binding.receivePayloadDataTextInput.getText().toString().length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                //binding.receivePayloadDataTextInput.setText(spannableString);
            }
        });

        chart.setOnChartGestureListener(new OnChartGestureListener() {

            //#region "Not Used/Implemented Functions"

            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) { }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) { }

            //#endregion

            @Override
            public void onChartLongPressed(MotionEvent me) {
                highlightText();
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
                highlightText();
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
                highlightText();
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
                highlightText();
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                highlightText();
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                highlightText();
            }
        });

        return root;
    }




    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_menu, menu); // Inflate your menu resource
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings){
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);

            return true;
        } else if (id == R.id.action_otherOption) {
            Intent intent = new Intent(getContext(), OtherOptionActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item); // Important line
    }

    //#region "Override Public Functions - View"

    @Override
    public void onStart() {
        super.onStart();
        if (!isServiceBound && getActivity() != null) {
            Intent intent = new Intent(getActivity(), SerialService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //#endregion

    //#region "Private Functions - Graph"

    private static String hexToBinary(String hexData) {
        StringBuilder binaryResult = new StringBuilder();

        for (int i = 0; i < hexData.length(); i++) {
            char hexChar = hexData.charAt(i);
            int decimalValue = Integer.parseInt(String.valueOf(hexChar), 16);
            StringBuilder binaryValue = new StringBuilder(Integer.toBinaryString(decimalValue));

            while (binaryValue.length() < 4) {
                binaryValue.insert(0, "0");
            }

            binaryResult.append(binaryValue);
        }

        return binaryResult.toString();
    }

    private void createWaveModulation(String bitString) {
        int bitrate = (int) (Math.pow(10, 6) / dataRate);

        entries = new ArrayList<>();

        float x = 0;
        float y;
        for (int i = 0; i < bitString.length(); i++) {
            char bit = bitString.charAt(i);

            for (int j = 0; j <= bitrate; j += 1) {
                if (bit == '1'){
                    y = (float) Math.sin(2 * Math.PI * x / (float) (bitrate / 5));
                }
                else {
                    y = (Objects.equals(modulationSelected, "ASK")) ? 0 : (float) Math.sin(2 * Math.PI * x / (float) (bitrate / 10));
                }

                entries.add(new Entry(x, y));
                x += 1;
            }

            x -= 1;
        }
    }

    private void configureChart(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Sinal Wave");
        dataSet.setColor(Color.CYAN);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setHorizontalScrollBarEnabled(true);

        // region "xAxis Setup"

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return String.valueOf((int) value);
            }
        });

        // endregion

        // region "yAxis Setup"

        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setAxisMinimum((float) -1.2);
        yAxisLeft.setAxisMaximum((float) 1.2);

        YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setAxisMinimum((float) -1.2);
        yAxisRight.setAxisMaximum((float) 1.2);

        // endregion

        chart.invalidate();
    }

    private void highlightText(){
        SpannableString spannableString = new SpannableString(binding.receivePayloadDataTextInput.getText().toString());
        BackgroundColorSpan highlightSpan = new BackgroundColorSpan(Color.YELLOW);

        int start = (int) Math.ceil(chart.getLowestVisibleX()/2000);
        int end = (int) Math.ceil(chart.getHighestVisibleX()/2000);

        if (start == 0 || start == 1)
            start = 2;

        spannableString.setSpan(highlightSpan, start - 2, end - 2, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        binding.receivePayloadDataTextInput.setText(spannableString);
    }

    //#endregion

    public void showToastOnUiThread(final String message) {
        if (isAdded()) { // Check if Fragment is currently added to its activity
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis) {
        // Send the command
        if(isServiceBound){
            serialService.write(command);
        }

        long startTime = System.currentTimeMillis(); // Start time for timeout

        // Wait for the response with timeout
        while (isServiceBound && serialService.getBufferLength() < expectedResponseSize) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                return null; // Timeout occurred
            }
            try {
                Thread.sleep(busyDelay); // Wait for it to arrive
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // Retrieve the response
        byte[] response = new byte[expectedResponseSize];
        response = serialService.pollData(expectedResponseSize);

        serialService.clearBuffer(); // Optionally clear the queue after processing (pollData() should already clear the response)
        return response;
    }

    @Override
    public void onTextSelected(int start, int end) {
        int bitrate = (int) (Math.pow(10, 6) / dataRate);

        if (end != 0 && end != start) {
            chart.setVisibleXRangeMaximum((end + 9) * bitrate);
            chart.moveViewToX((start + 4) * bitrate);
            chart.invalidate();
        } else if (start != 0 && end != 0) {
            chart.fitScreen();
        }
    }
}
