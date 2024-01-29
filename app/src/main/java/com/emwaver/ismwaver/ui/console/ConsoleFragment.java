package com.emwaver.ismwaver.ui.console;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.CommandSender;
import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.R;
import com.emwaver.ismwaver.SerialService;
import com.emwaver.ismwaver.databinding.FragmentConsoleBinding;
import com.emwaver.ismwaver.jsobjects.CC1101;
import com.emwaver.ismwaver.jsobjects.Console;
import com.emwaver.ismwaver.jsobjects.Utils;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class ConsoleFragment extends Fragment implements CommandSender {
    private FragmentConsoleBinding binding;
    private EditText terminalTextInput;
    private TextView terminalText;
    private ConsoleViewModel terminalViewModel;
    private boolean filterEnabled = true;
    private CC1101 cc1101;
    private Serial serial;
    private Console console;
    private Utils utils;
    private SerialService serialService;
    private boolean isServiceBound = false;
    private ActivityResultLauncher<Intent> createFileLauncher;
    private ActivityResultLauncher<String[]> openFileLauncher;
    private Uri currentFileUri;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
            cc1101 = new CC1101(serialService);
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            Log.i("service binding", "onServiceDisconnected");
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConsoleBinding.inflate(inflater, container, false);
        View root = binding.getRoot(); // inflate fragment_terminal.xml

        terminalText = binding.terminalText; //get bindings
        terminalTextInput = binding.terminalTextInput;
        binding.terminalText.setMovementMethod(new ScrollingMovementMethod()); // Set the TextView as scrollable

        // Observe the LiveData and update the UI accordingly
        terminalViewModel = new ViewModelProvider(this).get(ConsoleViewModel.class);
        terminalViewModel.getTerminalData().observe(getViewLifecycleOwner(), data -> {
            SpannableStringBuilder spannable = new SpannableStringBuilder();
            for (ConsoleViewModel.TextWithColor textWithColor : data) {
                int start = spannable.length();
                spannable.append(textWithColor.getText());
                int end = spannable.length();

                spannable.setSpan(new ForegroundColorSpan(textWithColor.getColor()),
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            terminalText.setText(spannable);
        });

        loadScriptFromAssets();



        serial = new Serial(getContext(), this);

        console = new Console(getContext());

        utils = new Utils(getContext());

        //initializeScripts();


        binding.saveFileAsButton.setOnClickListener(v -> {
            buttonCreateFile();
        });

        binding.saveFileButton.setOnClickListener(v -> {
            //buttonSaveFile();
        });

        binding.openFileButton.setOnClickListener(v -> {
            buttonOpenFile();
        });

        binding.executeScriptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    try {
                        String jsCode = binding.jsCodeInput.getText().toString();
                        ScriptsEngine scriptsEngine = new ScriptsEngine(cc1101, serial, console, utils);
                        serialService.changeStatus("Running script...");
                        String result = scriptsEngine.executeJavaScript(jsCode);
                        serialService.sendStringIntent("\n>", "user_input");
                        if(result != null){
                            serialService.sendStringIntent(result, "javascript");
                            serialService.sendStringIntent("\n>", "user_input");
                        }
                    } finally {
                        unbindServiceIfNeeded();
                        serialService.changeStatus("");
                    }
                }).start();
            }
        });

        terminalTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String userInput = terminalTextInput.getText().toString();
                serialService.write(userInput.getBytes()); // Send to SerialService for transmitting over USB
                terminalViewModel.appendData(userInput+"\n>", ContextCompat.getColor(getContext(), R.color.user_input));
                terminalTextInput.setText("");
            }
            return false;
        });

        binding.filterCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterEnabled = isChecked;
        });

        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Constants.ACTION_INITIATE_USB_CONNECTION);
                getContext().sendBroadcast(intent); // Send intent to SerialService to initiate USB connection
            }
        });

        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                terminalViewModel.setData("");
                terminalText.setText("");
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
                loadFileToEditText(uri);
            }
        });

        return root;
    }

    private void unbindServiceIfNeeded() {
        if (isServiceBound && !isFragmentActive() && getActivity() != null) {
            getActivity().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
    private boolean isFragmentActive() {
        return isAdded() && !isDetached() && !isRemoving();
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
        // bind service
        if (!isServiceBound && getActivity() != null) {
            Intent intent = new Intent(getActivity(), SerialService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers
    }
    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
        // Broadcast receiver for data coming from SerialService background USB service. Updates console live UI.
        private StringBuilder buffer = new StringBuilder();
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_USB_DATA_RECEIVED.equals(intent.getAction())) {
                byte[] data = intent.getByteArrayExtra("data");
                String source = intent.getStringExtra("source"); // Retrieve the source
                if(Objects.equals(source, "serial")){
                    if (filterEnabled) {
                        buffer.append(new String(data)); // Append new data to the buffer
                        processBufferForStrings();      // Process buffer for strings encapsulated within <STR> and </STR>
                    } else {
                        displayAllData(data);           // Display all data as it is
                    }
                }
                else if(Objects.equals(source, "javascript")){
                    terminalViewModel.appendData(new String(data), ContextCompat.getColor(getContext(), R.color.javascript_environment));
                }
                else if(Objects.equals(source, "system")){
                    terminalViewModel.appendData(new String(data), ContextCompat.getColor(getContext(), R.color.system_messages));
                }
                else if(Objects.equals(source, "user_input")){
                    terminalViewModel.appendData(new String(data), ContextCompat.getColor(getContext(), R.color.user_input));
                }
            }
        }
        private void processBufferForStrings() {
            int startIdx;
            int endIdx;

            while ((startIdx = buffer.indexOf("<STR>")) != -1 && (endIdx = buffer.indexOf("</STR>", startIdx)) != -1) {
                String message = buffer.substring(startIdx + "<STR>".length(), endIdx);
                terminalViewModel.appendData(message, ContextCompat.getColor(getContext(), R.color.serial_data)); // Append the complete message to the ViewModel
                buffer.delete(0, endIdx + "</STR>".length()); // Remove processed message
            }
        }
        private void displayAllData(byte[] data) {
            String dataString = new String(data);
            terminalViewModel.appendData(dataString, Color.GREEN);
        }
    };
    public void appendConsoleText(String source, String data){
        if(Objects.equals(source, "javascript")){
            terminalViewModel.appendData(data, ContextCompat.getColor(getContext(), R.color.javascript_environment));
        }
        else if(Objects.equals(source, "system")){
            terminalViewModel.appendData(data, ContextCompat.getColor(getContext(), R.color.system_messages));
        }
        else if(Objects.equals(source, "user_input")){
            terminalViewModel.appendData(data, ContextCompat.getColor(getContext(), R.color.user_input));
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
        while (isServiceBound && serialService.getCommandBufferLength() < expectedResponseSize) {
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

        serialService.clearCommandBuffer(); // Optionally clear the queue after processing
        return response;
    }
    @Override
    public void onStop() {
        //requireActivity().unregisterReceiver(usbDataReceiver); //don't call this to leave the broadcast of the USB data received active.
        super.onStop();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for avoiding memory leaks
    }

    public void buttonCreateFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Set MIME Type as per your requirement
        intent.putExtra(Intent.EXTRA_TITLE, "myScript.js");

        createFileLauncher.launch(intent);
    }

    public void buttonOpenFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // MIME type for .raw files or use "*/*" for any file type
        openFileLauncher.launch(new String[]{"*/*"}); // Pass the MIME type as an array
    }

    private void saveFileToUri(Uri uri) {
        try (OutputStream outstream = getActivity().getContentResolver().openOutputStream(uri)) {
            String fileContent = binding.jsCodeInput.getText().toString();
            outstream.write(fileContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e("filesys", "Error writing to file", e);
        }
    }

    private void loadFileToEditText(Uri uri) {
        try (InputStream instream = getActivity().getContentResolver().openInputStream(uri)) {
            byte[] fileData = readBytes(instream);
            String fileContent = new String(fileData, StandardCharsets.UTF_8);
            binding.jsCodeInput.setText(fileContent);
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

    private void loadScriptFromAssets() {
        try {
            // Open an input stream to read from the assets folder
            InputStream is = getActivity().getAssets().open(".js");
            int size = is.available();

            // Read the entire script into a byte array
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            // Convert the byte array to a String
            String scriptContent = new String(buffer, StandardCharsets.UTF_8);

            // Set the script content to the EditText
            binding.jsCodeInput.setText(scriptContent);
        } catch (IOException e) {
            Log.e("assets", "Error loading script from assets", e);
        }
    }


}
