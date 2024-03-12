package com.emwaver.ismwaver.ui.packetmode;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.R;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.databinding.FragmentPacketModeBinding;
import com.emwaver.ismwaver.CC1101;
import com.emwaver.ismwaver.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;

public class PacketModeFragment extends Fragment {

    private FragmentPacketModeBinding binding;

    private PacketModeViewModel packetModeViewModel;

    private CC1101 cc;
    private USBService USBService;
    private boolean isServiceBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            USBService.LocalBinder binder = (USBService.LocalBinder) service;
            USBService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
            cc = new CC1101(USBService);
            if(USBService.checkConnection())
                updatePacketSettings();
            Utils.updateActionBarStatus(PacketModeFragment.this, Utils.getFileNameFromUri(getContext(), Utils.getUri(getContext(), Utils.KEY_PACKET_MODE_FRAGMENT)));
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            Log.i("service binding", "onServiceDisconnected");
        }
    };

    private ActivityResultLauncher<Intent> createFileLauncher;
    private ActivityResultLauncher<String[]> openFileLauncher;
    private Uri currentUri = null;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        packetModeViewModel = new ViewModelProvider(this).get(PacketModeViewModel.class);



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

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.packetmode_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.open) {
                    openFile();
                    return true;
                } else if (itemId == R.id.save) {
                    saveFile();
                    return true;
                } else if (itemId == R.id.save_as) {
                    saveAsFile();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        //Utils.updateStatusBarFile(this);

        //region onClickListeners
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
                        int dataRate = Integer.parseInt(dataRateStr);

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
                    byte[] syncWord = Utils.convertHexStringToByteArray(hexInput);
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
                    cc.initTx();
                    showToastOnUiThread("check console");
                }).start();
            }
        });

        binding.initReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.initRx();
                    showToastOnUiThread("check console");
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
                byte [] payload_bytes = Utils.convertHexStringToByteArray(payload);

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
                    Log.i("Received", Utils.toHexStringWithHexPrefix(receivedBytes));
                    String hexString = Utils.bytesToHexString(receivedBytes);
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
            String selectedItem = (String) parent.getItemAtPosition(position);
            new Thread(() -> {
                // Handle the selection
                if ("ASK".equals(selectedItem)) {
                    if(cc.setModulation(CC1101.MOD_ASK)){
                        showToastOnUiThread("modulation set successfully to ASK");
                    }
                    else
                        showToastOnUiThread("Failed to set modulation");
                } else if ("FSK".equals(selectedItem)) {
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
            new Thread(() -> {
                // Handle the selection based on index
                if(cc.setPreambleLength(position)) {

                    showToastOnUiThread("Preamble set successfully to index " + position);
                } else {
                    showToastOnUiThread("Failed to set preamble");
                }
            }).start();
        });

        //endregion

        openFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                //loadPacketFile(uri);
                Utils.saveUri(getContext(), Utils.KEY_PACKET_MODE_FRAGMENT, uri);
                Utils.updateActionBarStatus(this, Utils.getFileNameFromUri(getContext(), Utils.getUri(getContext(), Utils.KEY_PACKET_MODE_FRAGMENT)));
            }
        });

        createFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    saveFile();
                    Utils.saveUri(getContext(), Utils.KEY_PACKET_MODE_FRAGMENT, uri);
                    Utils.updateActionBarStatus(this, Utils.getFileNameFromUri(getContext(), Utils.getUri(getContext(), Utils.KEY_PACKET_MODE_FRAGMENT)));
                }
            }
        });


        return root;
    }


    public void updatePacketSettings() {
        // Modulation
        int modulation = cc.getModulation();
        binding.modulationSelector.setText(modulation == 0 ? "FSK" : "ASK");
        // Deviation
        double deviation = cc.getDeviation();
        binding.deviationTextInput.setText(String.valueOf(deviation));
        // Data Rate
        int dataRate = cc.getDataRate();
        binding.datarateTextInput.setText(String.valueOf(dataRate));
        // Packet Length
        int pktLength = cc.getPktLength();
        binding.packetLengthEditText.setText(String.valueOf(pktLength));
        // Preamble Length
        int preambleLength = cc.getPreambleLength();
        binding.preambleSelector.setText(String.valueOf(preambleLength));
        // Sync Word
        byte[] syncWord = cc.getSyncWord();
        binding.syncwordTextInput.setText(Utils.bytesToHexString(syncWord));
    }

    public void saveFile() {
        // Retrieve the URI for this fragment from the Utils.STATUS_BAR_URIS map
        //Uri uri = Utils.STATUS_BAR_URIS.get(this.getClass().getName());

        Uri uri = null;
        if (uri != null) {
            try (OutputStream outputStream = getActivity().getContentResolver().openOutputStream(uri)) {
                byte[] packetConfig = getPacketConfig(); // Method to gather current packet settings and payload
                outputStream.write(packetConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Handle the case where the URI is not set or not found in the map
            Log.e("SavePacketFile", "No URI found for this fragment.");
            saveAsFile();
        }
    }


    public void saveAsFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "newPacket.packet");
        createFileLauncher.launch(intent);
    }

    public void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        openFileLauncher.launch(new String[]{"*/*"});
    }

    private void loadPacketFile(Uri uri) {
        try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri)) {
            byte[] fileContent = readAllBytes(inputStream); // Utility method to read all bytes from InputStream
            setPacketSettings(fileContent); // Parses and applies settings to CC1101 and UI
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public byte[] getPacketConfig() {
        // Read settings from CC1101 registers
        byte[] settings = new byte[]{
                cc.readReg(CC1101.CC1101_SYNC1),
                cc.readReg(CC1101.CC1101_SYNC0),
                cc.readReg(CC1101.CC1101_PKTLEN),
                cc.readReg(CC1101.CC1101_PKTCTRL0),
                cc.readReg(CC1101.CC1101_MDMCFG4),
                cc.readReg(CC1101.CC1101_MDMCFG3),
                cc.readReg(CC1101.CC1101_MDMCFG2),
                cc.readReg(CC1101.CC1101_MDMCFG1),
                cc.readReg(CC1101.CC1101_DEVIATN),
        };

        // Obtain payload from UI
        String payloadHex = binding.transmitPayloadDataTextInput.getText().toString();
        byte[] payload = Utils.convertHexStringToByteArray(payloadHex);

        // Combine settings and payload
        byte[] packetConfig = new byte[settings.length + payload.length];
        System.arraycopy(settings, 0, packetConfig, 0, settings.length);
        System.arraycopy(payload, 0, packetConfig, settings.length, payload.length);
        Log.i("getPacketConfig", Utils.bytesToHexString(packetConfig));
        return packetConfig;
    }


    private void setPacketSettings(byte[] config) {
        if (config.length < 9) { // Check if the config array includes all the required settings
            Log.e("PacketModeFragment", "Config data too short");
            return;
        }

        // Write settings to CC1101 registers
        cc.writeReg(CC1101.CC1101_SYNC1, config[0]);
        cc.writeReg(CC1101.CC1101_SYNC0, config[1]);
        cc.writeReg(CC1101.CC1101_PKTLEN, config[2]);
        cc.writeReg(CC1101.CC1101_PKTCTRL0, config[3]);
        cc.writeReg(CC1101.CC1101_MDMCFG4, config[4]);
        cc.writeReg(CC1101.CC1101_MDMCFG3, config[5]);
        cc.writeReg(CC1101.CC1101_MDMCFG2, config[6]);
        cc.writeReg(CC1101.CC1101_MDMCFG1, config[7]);
        cc.writeReg(CC1101.CC1101_DEVIATN, config[8]);

        // Extract payload from the config array
        byte[] payload = Arrays.copyOfRange(config, 9, config.length);

        // Update payload EditText in UI
        final String payloadHex = Utils.bytesToHexString(payload);
        getActivity().runOnUiThread(() -> binding.transmitPayloadDataTextInput.setText(payloadHex));

        // Call updatePacketSettings to refresh UI with the new settings
        updatePacketSettings();
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
        binding = null;
    }




    public void showToastOnUiThread(final String message) {
        if (isAdded()) { // Check if Fragment is currently added to its activity
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }




}
