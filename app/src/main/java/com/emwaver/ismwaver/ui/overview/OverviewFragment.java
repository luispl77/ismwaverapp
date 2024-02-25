package com.emwaver.ismwaver.ui.overview;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.emwaver.ismwaver.CC1101;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.Utils;
import com.emwaver.ismwaver.databinding.FragmentOverviewBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.function.Consumer;

public class OverviewFragment extends Fragment {

    private OverviewViewModel overviewViewModel;
    private FragmentOverviewBinding binding;
    private USBService USBService;
    private boolean isServiceBound = false;
    private CC1101 cc;
    private final Handler handler = new Handler(Looper.getMainLooper()); // Handler on the main UI thread
    private boolean wasPreviouslyConnected = false;
    private ActivityResultLauncher<Intent> createFileLauncher;
    private ActivityResultLauncher<String[]> openFileLauncher;

    private Uri currentUri = null;


    private final Runnable checkConnectionTask = new Runnable() {
        @Override
        public void run() {
            Log.i("checkConnectionTask", "checking connection..");
            updateConnection();
            handler.postDelayed(this, 3000); // Reschedule for another check in 3 seconds

        }
    };


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            USBService.LocalBinder binder = (USBService.LocalBinder) service;
            USBService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
            cc = new CC1101(USBService);
            handler.post(checkConnectionTask); // Start periodic updates when the fragment starts

        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            Log.i("service binding", "onServiceDisconnected");
        }
    };

    public OverviewFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        overviewViewModel = new ViewModelProvider(this).get(OverviewViewModel.class);

        binding = FragmentOverviewBinding.inflate(inflater, container, false);
        View root = binding.getRoot();



        binding.frequencyHeader.setOnClickListener(v -> {
            // Toggle visibility
            binding.frequencyContent.setVisibility( binding.frequencyContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        binding.modulationHeader.setOnClickListener(v -> {
            // Toggle visibility of the modulation content
            binding.modulationContent.setVisibility(binding.modulationContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        binding.powerHeader.setOnClickListener(v -> {
            // Toggle visibility of the power content
            binding.powerContent.setVisibility(binding.powerContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.bandwidthHeader.setOnClickListener(v -> {
            // Toggle visibility of the bandwidth content
            binding.bandwidthContent.setVisibility(binding.bandwidthContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.gainHeader.setOnClickListener(v -> {
            // Toggle visibility of the gain content
            binding.gainContent.setVisibility(binding.gainContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.packetSettingsHeader.setOnClickListener(v -> {
            // Toggle visibility of the packet settings content
            binding.packetSettingsContent.setVisibility(binding.packetSettingsContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.gpioHeader.setOnClickListener(v -> {
            // Toggle visibility of the GPIO content
            binding.gpioContent.setVisibility(binding.gpioContent.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });


        //region ACTION_DONE LISTENERS

        //region Frequency
        handleDoneAction(binding.frequencyEditText, (frequencyStr) -> {
            double frequencyMHz = parseDoubleInput(frequencyStr);
            cc.setFrequencyMHz(frequencyMHz);
        });
        //endregion

        //region Modulation
        handleDoneAction(binding.modulationEditText, (modulationInput) -> {
            modulationInput = modulationInput.toUpperCase();
            if (modulationInput.equals("ASK")) {
                cc.setModulation(CC1101.MOD_ASK);
            } else if (modulationInput.equals("FSK")) {
                cc.setModulation(CC1101.MOD_2FSK);
            } else {
                Toast.makeText(getContext(), "Invalid modulation", Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region Power
        handleDoneAction(binding.powerEditText, (powerStr) -> {
            int powerLevel = Integer.parseInt(powerStr);
            cc.setPowerLevel(powerLevel);
        });
        //endregion

        //region Bandwidth
        handleDoneAction(binding.bandwidthEditText, (bandwidthStr) -> {
            double bandwidthKHz = parseDoubleInput(bandwidthStr);
            boolean success = cc.setBandwidth(bandwidthKHz); // Call your setBandwidth function
            if (!success) {
                Toast.makeText(getContext(), "Invalid bandwidth or bandwidth too low", Toast.LENGTH_SHORT).show();
            }
        });

        handleDoneAction(binding.deviationEditText, (deviationStr) -> {
            int deviationKHz = Integer.parseInt(deviationStr);
            boolean success = cc.setDeviation(deviationKHz); // Call your setDeviation function
            if (!success) {
                Toast.makeText(getContext(), "Invalid deviation", Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region Gain
        handleDoneAction(binding.lnaGainEditText, (gainStr) -> {
            double gainDbm = parseDoubleInput(gainStr);
            boolean success = cc.setGainDbm(gainDbm);
            if (!success) {
                Toast.makeText(getContext(), "Invalid gain value", Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region Packet Settings
        handleDoneAction(binding.packetFormatEditText, (formatStr) -> {
            int format = Integer.parseInt(formatStr);
            boolean success = cc.setPacketFormat(format);
            if (!success) {
                Toast.makeText(getContext(), "Invalid packet format", Toast.LENGTH_SHORT).show();
            }
        });

        handleDoneAction(binding.packetLengthEditText, (lengthStr) -> {
            int length = Integer.parseInt(lengthStr);
            boolean success = cc.setPktLength(length);
            if (!success) {
                Toast.makeText(getContext(), "Invalid packet length", Toast.LENGTH_SHORT).show();
            }
        });

        handleDoneAction(binding.preambleLengthEditText, (preambleStr) -> {
            int preambleBytes = Integer.parseInt(preambleStr);
            boolean success = cc.setPreambleLength(preambleBytes);
            if (!success) {
                Toast.makeText(getContext(), "Invalid preamble length", Toast.LENGTH_SHORT).show();
            }
        });


        handleDoneAction(binding.dataRateEditText, (dataRateStr) -> {
            int dataRate = Integer.parseInt(dataRateStr);
            if (cc.setDataRate(dataRate)) {
                showToastOnUiThread("Data rate set to " + dataRate + " successfully");
            } else {
                showToastOnUiThread("Error setting data rate");
            }
        });


        handleDoneAction(binding.syncWordEditText, (hexInput) -> {
            hexInput = hexInput.trim();
            if (hexInput.length() != 4) {
                showToastOnUiThread("Input must be a 4-character hex value");
                return;
            }
            byte[] syncWord = Utils.convertHexStringToByteArray(hexInput);
            if (syncWord == null) {
                showToastOnUiThread("Invalid hex input");
                return;
            }
            if (cc.setSyncWord(syncWord)) {
                showToastOnUiThread("Sync word set successfully to " + hexInput);
            } else {
                showToastOnUiThread("Error setting Sync word");
            }
        });

        handleDoneAction(binding.syncModeEditText, (syncModeStr) -> {
            int syncMode = Integer.parseInt(syncModeStr);
            boolean success = cc.setSyncMode((byte)syncMode); // Assume `setSyncMode` expects a byte
            if (!success) {
                Toast.makeText(getContext(), "Failed to set sync mode", Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region GPIO
        handleDoneAction(binding.gpio0EditText, (gdo0ModeStr) -> {
            byte gdo0Mode = Byte.parseByte(gdo0ModeStr);
            boolean success = cc.setGDO0Mode(gdo0Mode);
            if (!success) {
                Toast.makeText(getContext(), "Failed to set GDO0 mode", Toast.LENGTH_SHORT).show();
            }
        });
        handleDoneAction(binding.gpio2EditText, (gdo2ModeStr) -> {
            byte gdo2Mode = Byte.parseByte(gdo2ModeStr);
            boolean success = cc.setGDO2Mode(gdo2Mode);
            if (!success) {
                Toast.makeText(getContext(), "Failed to set GDO2 mode", Toast.LENGTH_SHORT).show();
            }
        });
        handleDoneAction(binding.fifoThresholdEditText, (thresholdStr) -> {
            byte threshold = Byte.parseByte(thresholdStr);
            boolean success = cc.setFIFOThreshold(threshold);
            if (!success) {
                Toast.makeText(getContext(), "Failed to set FIFO threshold", Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //endregion


        openFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                currentUri = uri;
                // Update the TextView
                binding.configFileTextView.setText(getFileName(uri));
            }
        });

        createFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    currentUri = uri;
                    // Write the current configuration to the new file
                    saveFile();
                }
            }
        });

        binding.openButton.setOnClickListener(v -> openFile());
        binding.saveAsButton.setOnClickListener(v -> saveAsFile());
        binding.saveButton.setOnClickListener(v -> saveFile());
        binding.applyButton.setOnClickListener(v -> applyConfig());

        return root;
    }

    private void showToastOnUiThread(String message) {
        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }
    private void handleDoneAction(EditText editText, Consumer<String> settingAction) {
        editText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputStr = editText.getText().toString();
                try {
                    settingAction.accept(inputStr); // Execute the provided action
                    Toast.makeText(getContext(), "Setting updated", Toast.LENGTH_SHORT).show();
                } catch (IllegalArgumentException e) {
                    Toast.makeText(getContext(), "Invalid input format", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    // Helper for simple parsing if necessary (adjust as needed)
    private double parseDoubleInput(String inputStr) throws IllegalArgumentException {
        try {
            return Double.parseDouble(inputStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input format");
        }
    }


    public void updateAccordionSettings(){
        double frequencyMHz = cc.getFrequency();
        binding.frequencyEditText.setText(String.format(Locale.US, "%.6f", frequencyMHz));
        int modulation = cc.getModulation();
        binding.modulationEditText.setText(modulation == 0 ? "FSK" : "ASK");
        int powerLevel = cc.getPowerLevel();
        binding.powerEditText.setText(""+powerLevel);
        double bandwidth = cc.getBandwidth();
        binding.bandwidthEditText.setText(""+bandwidth);
        double deviation = cc.getDeviation();
        binding.deviationEditText.setText(""+deviation);

        int datarate = cc.getDataRate();
        binding.dataRateEditText.setText(""+datarate);
        int pktformat = cc.getPacketFormat();
        binding.packetFormatEditText.setText(""+pktformat);
        int pktlength = cc.getPktLength();
        binding.packetLengthEditText.setText(""+pktlength);
        int preamblelength = cc.getPreambleLength();
        binding.preambleLengthEditText.setText(""+preamblelength);
        byte[] syncword = cc.getSyncWord();
        binding.syncWordEditText.setText(Utils.bytesToHexString(syncword));
        int syncmode = cc.getSyncMode();
        binding.syncModeEditText.setText(""+syncmode);
        double gaindbm = cc.getGainDbm();
        binding.lnaGainEditText.setText(""+gaindbm);
        int gdo0 = cc.getGDO0Mode();
        binding.gpio0EditText.setText(""+gdo0);
        int gdo2 = cc.getGDO2Mode();
        binding.gpio2EditText.setText(""+gdo2);
        int fifothreshold = cc.getFIFOThreshold();
        binding.fifoThresholdEditText.setText(""+fifothreshold);
    }


    public void updateConnection(){
        boolean isConnected = USBService.checkConnection(); // Call your USBService method
        binding.connectionStatusTextView.setText(isConnected ? "Connected" : "Disconnected");
        if (isConnected != wasPreviouslyConnected) {
            if (isConnected) {
                binding.connectionStatusTextView.setTextColor(Color.GREEN);
                binding.deviceImageView.setVisibility(View.VISIBLE);
                updateAccordionSettings();
            } else {
                binding.connectionStatusTextView.setTextColor(Color.RED);
                binding.deviceImageView.setVisibility(View.INVISIBLE);
            }
            wasPreviouslyConnected = isConnected;
        }

    }



    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data,  0, data.length)) != -1) {
            buffer.write(data,  0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    public void saveAsFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Set MIME Type as per your requirement
        intent.putExtra(Intent.EXTRA_TITLE, "myConf.conf");
        createFileLauncher.launch(intent);
    }

    public void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // MIME type for .raw files or use "*/*" for any file type
        openFileLauncher.launch(new String[]{"*/*"}); // Pass the MIME type as an array
    }


    public void saveFile() {
        if (currentUri != null) {
            try (OutputStream outputStream = getActivity().getContentResolver().openOutputStream(currentUri)) {
                byte[] currentConfig = getConfigSettings(); // Placeholder for getting the current configuration
                Log.i("currentConfig", Utils.bytesToHexString(currentConfig));
                outputStream.write(currentConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void applyConfig(){
        if (currentUri != null) {
            try (InputStream inputStream = getActivity().getContentResolver().openInputStream(currentUri)) {
                byte[] fileContent = readAllBytes(inputStream);
                Log.i("applyConfig", Utils.bytesToHexString(fileContent));
                // Process the file content as needed
                setConfigSettings(fileContent);
                updateAccordionSettings();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            showToastOnUiThread("No file selected");
        }
    }


    public byte[] getConfigSettings() {
        // CC1101 configuration registers end at 0x2E, and PATABLE is 8 bytes long
        byte configRegistersEnd = 0x2E;
        int patableLength = 8;
        // Read the configuration registers
        byte[] configBytes = cc.readBurstReg((byte)0x00, configRegistersEnd + 1);
        // Read the PATABLE
        byte[] patableBytes = cc.readBurstReg(CC1101.CC1101_PATABLE, patableLength);
        // Combine the two arrays
        byte[] settings = new byte[configBytes.length + patableBytes.length];
        System.arraycopy(configBytes, 0, settings, 0, configBytes.length);
        System.arraycopy(patableBytes, 0, settings, configBytes.length, patableBytes.length);

        return settings;
    }

    public void setConfigSettings(byte[] settings) {
        byte configRegistersEnd = 0x2E;
        int patableLength = 8;
        // Ensure the settings array has the correct length
        if (settings.length != (configRegistersEnd + 1 + patableLength)) {
            // Handle error: the settings array does not have the expected length
            return;
        }
        // Write configuration registers
        byte[] configBytes = new byte[configRegistersEnd + 1];
        System.arraycopy(settings, 0, configBytes, 0, configRegistersEnd + 1);
        cc.writeBurstReg((byte)0x00, configBytes, (byte)(configRegistersEnd + 1));
        // Write PATABLE
        byte[] patableBytes = new byte[patableLength];
        System.arraycopy(settings, configRegistersEnd + 1, patableBytes, 0, patableLength);
        cc.writeBurstReg(CC1101.CC1101_PATABLE, patableBytes, (byte)patableLength);
    }



    private String getFileName(Uri uri) {
        String displayName = "No file selected";
        // Ensure getActivity() is not null
        if (getActivity() != null) {
            Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    // Check if the column index is valid
                    if (columnIndex != -1) {
                        displayName = cursor.getString(columnIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return displayName;
    }



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
    public void onStop() {
        super.onStop();
        if (isServiceBound) {
            getActivity().unbindService(serviceConnection);
            isServiceBound = false;
        }
        handler.removeCallbacks(checkConnectionTask); // Stop periodic updates when the fragment stops
        wasPreviouslyConnected = false;
    }

}