package com.emwaver.ismwaver.ui.rawmode;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.databinding.FragmentRawModeBinding;
import com.emwaver.ismwaver.jsobjects.CC1101;
import com.emwaver.ismwaver.jsobjects.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RawModeFragment extends Fragment {

    private RawModeViewModel rawModeViewModel;
    private FragmentRawModeBinding binding;
    private USBService USBService;
    LineChart chart = null;
    private int chartMinX = 0;
    private int chartMaxX = 10000;
    private boolean isServiceBound = false;
    private float currentZoomLevel = 1.0f;
    private int prevRangeStart = 0;
    private int prevRangeEnd = 0;

    private CC1101 cc;
    public ScheduledExecutorService scheduler;
    private final int refreshRate = 100; // Refresh rate in milliseconds
    private int numberBins = 500;
    private int errorTolerance = 10;
    private int samplesPerSymbol = 40;
    private ActivityResultLauncher<Intent> createFileLauncher;
    private ActivityResultLauncher<String[]> openFileLauncher;
    private Uri currentFileUri;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            USBService.LocalBinder binder = (USBService.LocalBinder) service;
            USBService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
            //updateChart(compressDataAndGetDataSet(0, USBService.getBufferLength(), 1000));
            cc = new CC1101(USBService);
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            Log.i("service binding", "onServiceDisconnected");
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Initialize view binding
        binding = FragmentRawModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshChart, 0, refreshRate, TimeUnit.MILLISECONDS);
        // Todo: make executor function only execute when its recording

        chart = binding.chart;



        rawModeViewModel = new ViewModelProvider(this).get(RawModeViewModel.class);

        binding.connectButton.setOnClickListener(v -> {
            USBService.connectUSBSerial();
            //updateVisibleRange();
        });
        binding.initContinuousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInitRxContinuous();
                    //showToastOnUiThread("check console");
                }).start();
            }
        });

        binding.retransmitButton.setOnClickListener(v -> {
            int bufferLength = USBService.getDataBufferLength();
            USBService.setBuffer(Constants.COMMAND_BUFFER); //transmit
            Toast.makeText(getContext(), "Buffer Length: " + bufferLength, Toast.LENGTH_SHORT).show();

            transmitBuffer();

        });

        binding.clearBufferButton.setOnClickListener(v -> {
            USBService.clearDataBuffer();
            Toast.makeText(getContext(), "buffer cleared" , Toast.LENGTH_SHORT).show();
            updateChart(compressDataAndGetDataSet(rawModeViewModel.getVisibleRangeStart(), rawModeViewModel.getVisibleRangeEnd(), 1000));
        });

        binding.startRecordingButton.setOnClickListener(v -> {
            String contCommand = "raw";
            byte[] byteArray = contCommand.getBytes();
            USBService.setBuffer(Constants.DATA_BUFFER);
            USBService.write(byteArray);

            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::refreshChart, 0, refreshRate, TimeUnit.MILLISECONDS);

        });

        binding.stopRecordingButton.setOnClickListener(v -> {
            String contCommand = "ssss";
            byte[] byteArray = contCommand.getBytes();
            USBService.write(byteArray);
            new Thread(() -> {
                USBService.emptyReadBuffer(); //this function busy waits
                USBService.setBuffer(Constants.COMMAND_BUFFER); //wait before the buffer is empty
            }).start();

            chartMaxX = USBService.getDataBufferLength()*8;
            XAxis xAxis = chart.getXAxis();
            xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
            xAxis.setAxisMaximum(chartMaxX);

            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }

            updateChart(compressDataAndGetDataSet(rawModeViewModel.getVisibleRangeStart(), rawModeViewModel.getVisibleRangeEnd(), numberBins));
        });

        binding.reconstructButton.setOnClickListener(v -> {
            toggleVerticalLinesOnChart(USBService.findHighEdges(samplesPerSymbol, errorTolerance));
            byte [] reconstructedSignal = USBService.extractBitsFromEdges(USBService.findHighEdges(samplesPerSymbol, errorTolerance), samplesPerSymbol);
            logBuffer(reconstructedSignal);
            binding.reconstructedSinalEditText.setText(Utils.bytesToHexString(reconstructedSignal));
        });

        binding.fillTeslaButton.setOnClickListener(v -> {
            USBService.setBuffer(Constants.DATA_BUFFER);
            fillBufferWithTesla(0.001);
            USBService.setBuffer(Constants.COMMAND_BUFFER);
            refreshChart();
        });

        binding.errorToleranceEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                new Thread(() -> {
                    String deviationStr = binding.errorToleranceEditText.getText().toString().trim();
                    // Parse the string to an integer
                    try {
                        errorTolerance = Integer.parseInt(deviationStr);
                        showToastOnUiThread("error tolerance set to: " + errorTolerance);

                    } catch (NumberFormatException e) {
                        showToastOnUiThread("Invalid error tolerance entered");
                    }
                }).start();
                return true; // Consume the action
            }
            return false; // Pass the event on to other listeners
        });

        binding.samplesPerSymbolEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                new Thread(() -> {
                    String deviationStr = binding.samplesPerSymbolEditText.getText().toString().trim();
                    // Parse the string to an integer
                    try {
                        samplesPerSymbol = Integer.parseInt(deviationStr);
                        showToastOnUiThread("samples per symbol set to: " + samplesPerSymbol);

                    } catch (NumberFormatException e) {
                        showToastOnUiThread("Invalid samples per symbol entered");
                    }
                }).start();
                return true; // Consume the action
            }
            return false; // Pass the event on to other listeners
        });

        binding.numberPointsEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                new Thread(() -> {
                    String deviationStr = binding.numberPointsEditText.getText().toString().trim();
                    // Parse the string to an integer
                    try {
                        numberBins = Integer.parseInt(deviationStr);
                        showToastOnUiThread("numberBins set to: " + numberBins);

                    } catch (NumberFormatException e) {
                        showToastOnUiThread("Invalid numberBins entered");
                    }
                }).start();
                return true; // Consume the action
            }
            return false; // Pass the event on to other listeners
        });

        binding.saveFileAsButton.setOnClickListener(v -> {
            buttonCreateFile();
        });

        binding.saveFileButton.setOnClickListener(v -> {
            buttonSaveFile();
        });

        binding.openFileButton.setOnClickListener(v -> {
            buttonOpenFile();
        });

        initChart();

        chart.setOnChartGestureListener(new OnChartGestureListener() {
            //region useless chart listeners
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }
            //endregion
            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                float newZoomLevel = chart.getScaleX();

                if (Math.abs(newZoomLevel - currentZoomLevel) >= (newZoomLevel/10)) {
                    // Zoom level changed significantly
                    currentZoomLevel = newZoomLevel;

                    rawModeViewModel.setVisibleRangeStart((int) chart.getLowestVisibleX());
                    rawModeViewModel.setVisibleRangeEnd((int) chart.getHighestVisibleX());

                    updateChart(compressDataAndGetDataSet(rawModeViewModel.getVisibleRangeStart(), rawModeViewModel.getVisibleRangeEnd(), numberBins));
                }
            }
            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                int visibleRangeStart = (int) chart.getLowestVisibleX();
                int visibleRangeEnd = (int) chart.getHighestVisibleX();
                rawModeViewModel.setVisibleRangeStart(visibleRangeStart);
                rawModeViewModel.setVisibleRangeEnd(visibleRangeStart);

                int span = visibleRangeEnd - visibleRangeStart;
                float translationThreshold = (float)span / 100; // Define an appropriate threshold value


                // Check if the chart is at its boundaries
                if ((visibleRangeStart<= chartMinX && dX > 0) || (visibleRangeEnd >= chartMaxX && dX < 0)) {
                    // At the boundary, do not update
                    return;
                }

                // Check if the translation is significant
                if (Math.abs(visibleRangeStart - prevRangeStart) > translationThreshold ||
                        Math.abs(visibleRangeEnd - prevRangeEnd) > translationThreshold &&
                                span >= 10 ) {

                    prevRangeStart = visibleRangeStart;
                    prevRangeEnd = visibleRangeEnd;

                    updateChart(compressDataAndGetDataSet(visibleRangeStart, visibleRangeEnd, numberBins));
                }
            }
        });

        createFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    saveFileToUri(uri);
                }
            }
        });

        openFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                currentFileUri = uri; // Store the Uri
                loadFileToBuffer(uri);
            }
        });

        return root;
    }

    private void fillBufferWithTesla(double noise) {
        byte[] teslaSignal = {(byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0x8A, (byte) 0xCB, 50, -52, -52, -53, 77, 45, 74, -45, 76, -85, 75, 21, -106, 101, -103, -103, -106, -102, 90, -107, -90, -103, 86, -106, 43, 44, -53, 51, 51, 45, 52, -75, 43, 77, 50, -83, 40};
        int samplesPerBit = 40; // 40 samples per bit since 400us per bit and 10us sampling rate
        int signalSize = teslaSignal.length * 8 * samplesPerBit;
        int paddingSize = signalSize; // Same size as the signal
        int bufferSize = signalSize + 2 * paddingSize;

        byte[] dataBuffer = new byte[bufferSize / 8 + (bufferSize % 8 != 0 ? 1 : 0)]; // Each byte holds 8 bits
        int bitIndex = paddingSize;

        // Fill buffer with padding before the Tesla signal
        for (int i = 0; i < paddingSize; i++) {
            dataBuffer[i / 8] &= ~(1 << (7 - i % 8));
        }

        // Fill buffer with Tesla signal
        for (byte teslaByte : teslaSignal) {
            for (int i = 7; i >= 0; i--) {
                boolean isHigh = ((teslaByte >> i) & 1) == 1;
                for (int j = 0; j < samplesPerBit; j++) {
                    if (isHigh) {
                        dataBuffer[bitIndex / 8] |= (1 << (7 - bitIndex % 8));
                    } else {
                        dataBuffer[bitIndex / 8] &= ~(1 << (7 - bitIndex % 8));
                    }
                    bitIndex++;
                }
            }
        }

        // Fill buffer with padding after the Tesla signal
        for (int i = bitIndex; i < bufferSize; i++) {
            dataBuffer[i / 8] &= ~(1 << (7 - i % 8));
        }
        //logBuffer(dataBuffer);
        Log.i("dataBuffer", "size: "+dataBuffer.length);
        addNoiseToSignal(dataBuffer, noise);
        USBService.addToBuffer(dataBuffer);
        Log.i("data buflen", "size: "+ USBService.getDataBufferLength());

    }
    private void addNoiseToSignal(byte[] dataBuffer, double noiseProbability) {
        Random random = new Random();
        for (int i = 0; i < dataBuffer.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                if (random.nextDouble() < noiseProbability) {
                    dataBuffer[i] ^= (1 << bit); // Flip the bit
                }
            }
        }
    }
    private void transmitBuffer() {
        int nativeBufferSize = USBService.getDataBufferLength(); // Existing JNI method to get the native buffer size
        sendStartTransmissionCommand(nativeBufferSize);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int packetSize = 50; //12.5 bytes per frame, 10us sampling period
        long startTime = System.nanoTime();
        final long period = 4000 * 1000;
        final long flow_time_delta = 1000 * 1000;

        for (int i = 0; i < nativeBufferSize; i += packetSize) {
            int end = Math.min(i + packetSize, nativeBufferSize);
            byte[] packet = USBService.getBufferRange(i, end); // Native method to get a range of the buffer

            startTime += period;
            int bufferStatus = getLogStatus();
            if (bufferStatus > 200 && bufferStatus < 300) {
                USBService.write(packet);
            } else if(bufferStatus > 300){
                USBService.write(packet);
                startTime += flow_time_delta; //write slower than the processing speed
            }
            else if(bufferStatus < 300){
                USBService.write(packet);
                startTime -= flow_time_delta; //write faster than the processing speed
            }
            while (System.nanoTime() < startTime) {
                // Busy wait
            }
        }
        USBService.clearCommandBuffer();
    }
    private int getLogStatus(){
        int bufferStatus = USBService.getStatusNumber(); // Get buffer status from STM32
        if(bufferStatus == -1){
            Log.i("bufstatus", "not found" + "   buflen: " + USBService.getCommandBufferLength());
        }
        else{
            Log.i("bufstatus", "" + bufferStatus  + "   buflen: " + USBService.getCommandBufferLength());
        }
        return bufferStatus;
    }
    private void sendStartTransmissionCommand(int size) {
        // Convert "tran" string to bytes
        String contCommand = "tran";
        byte[] commandBytes = contCommand.getBytes();
        // Send the byte array over the serial connection
        USBService.write(commandBytes);
    }
    private void logBuffer(byte[] buffer) {
        StringBuilder sb = new StringBuilder();
        for (byte b : buffer) {
            for (int i = 7; i >= 0; i--) {
                sb.append((b >> i) & 1); // Append '1' for set bit, '0' for clear bit
            }
        }
        Log.i("BufferLog", sb.toString());
        sb = new StringBuilder();
        for (byte b : buffer) {
            sb.append(String.format("%02X ", b));
        }
        Log.i("BufferLog", sb.toString());
        Log.i("BufferLog", "length: " + buffer.length*8);
    }
    public void initChart() {
        // Configure the chart (optional, based on your needs)
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setScaleYEnabled(false); // Disable Y-axis scaling
        chart.setScaleXEnabled(true);  // Enable X-axis scaling

        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
        xAxis.setAxisMaximum(chartMaxX); // End at the maximum X value

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0); // Set minimum value for the left Y-axis
        leftAxis.setAxisMaximum(255); // Set maximum value for the left Y-axis

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMinimum(0); // Set minimum value for the right Y-axis
        rightAxis.setAxisMaximum(255); // Set maximum value for the right Y-axis


    }

    private void refreshChart() {
        // Get the current visible range
        int visibleRangeStart = (int) chart.getLowestVisibleX();
        int visibleRangeEnd = (int) chart.getHighestVisibleX();
        rawModeViewModel.setVisibleRangeStart((int) chart.getLowestVisibleX());
        rawModeViewModel.setVisibleRangeEnd((int) chart.getHighestVisibleX());

        chartMaxX = USBService.getDataBufferLength()*8;
        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(chartMinX);
        xAxis.setAxisMaximum(chartMaxX);
        // Update the chart
        getActivity().runOnUiThread(() -> updateChart(compressDataAndGetDataSet(visibleRangeStart, visibleRangeEnd, numberBins)));
    }

    private LineDataSet compressDataAndGetDataSet(int rangeStart, int rangeEnd, int numberBins) {
        // Call the native method
        Object[] result = (Object[]) USBService.compressDataBits(rangeStart, rangeEnd, numberBins);

        float[] timeValues = (float[]) result[0];
        float[] dataValues = (float[]) result[1];

        Log.i("compressed", dataValues.length + " values");

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < timeValues.length; i++) {
            entries.add(new Entry(timeValues[i], dataValues[i]));
        }
        LineDataSet lineDataSet = new LineDataSet(entries, "Demodulator");
        // Disable the drawing of values for each point
        lineDataSet.setDrawValues(false);
        // Increase the line thickness (example: 3f for a thicker line)
        lineDataSet.setLineWidth(3f);
        // Set a more pronounced line color (example: Color.BLUE)
        lineDataSet.setColor(Color.parseColor("#003d99"));

        return lineDataSet;
    }

    private void updateChart(LineDataSet lineDataSet) {
        LineData lineData = new LineData(lineDataSet);
        chart.setData(lineData);
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void toggleVerticalLinesOnChart(long[] lineTimestamps) {
        XAxis xAxis = chart.getXAxis();

        if (!xAxis.getLimitLines().isEmpty()) {
            xAxis.removeAllLimitLines();
            Toast.makeText(getContext(), "Removed vertical lines", Toast.LENGTH_SHORT).show();
        } else {
            for (long timestamp : lineTimestamps) {
                LimitLine line = new LimitLine(timestamp);
                line.setLineColor(Color.RED);
                line.setLineWidth(2f);
                xAxis.addLimitLine(line);
            }
            Toast.makeText(getContext(), "Added " + lineTimestamps.length + " vertical lines", Toast.LENGTH_SHORT).show();
        }
    }



    public void showToastOnUiThread(final String message) {
        if (isAdded()) { // Check if Fragment is currently added to its activity
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!isServiceBound && getActivity() != null) {
            Intent intent = new Intent(getActivity(), USBService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for avoiding memory leaks
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    public void buttonOpenFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // MIME type for .raw files or use "*/*" for any file type
        openFileLauncher.launch(new String[]{"*/*"}); // Pass the MIME type as an array
    }

    public void buttonCreateFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Set MIME Type as per your requirement
        intent.putExtra(Intent.EXTRA_TITLE, "mySignal.raw");

        createFileLauncher.launch(intent);
    }

    public void buttonSaveFile() {
        writeChangesToFile();
    }

    private void saveFileToUri(Uri uri) {
        try (OutputStream outstream = getActivity().getContentResolver().openOutputStream(uri)) {
            outstream.write(USBService.getDataBuffer());
        } catch (IOException e) {
            Log.e("filesys", "Error writing to file", e);
        }
    }

    private void loadFileToBuffer(Uri uri) {
        try (InputStream instream = getActivity().getContentResolver().openInputStream(uri)) {
            byte[] fileData = readBytes(instream);
            // Now send this data to your native code to populate dataBuffer
            USBService.loadDataBuffer(fileData);
            refreshChart();
        } catch (IOException e) {
            Log.e("filesys", "Error reading from file", e);
        }
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }

    private void writeChangesToFile() {
        if (currentFileUri == null) {
            Log.e("filesys", "No file is currently open");
            showToastOnUiThread("No file open");
            return;
        }

        try (OutputStream outstream = getActivity().getContentResolver().openOutputStream(currentFileUri)) {
            byte[] dataBuffer = USBService.getDataBuffer(); // Get the updated data
            outstream.write(dataBuffer);
        } catch (IOException e) {
            Log.e("filesys", "Error writing to file", e);
        }
    }
}