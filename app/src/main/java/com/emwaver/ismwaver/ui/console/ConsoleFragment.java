package com.emwaver.ismwaver.ui.console;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.emwaver.ismwaver.jsobjects.Serial;
import com.emwaver.ismwaver.jsobjects.Utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
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

        cc1101 = new CC1101(this);

        serial = new Serial(getContext(), this);

        console = new Console(getContext());

        utils = new Utils(getContext());

        initializeScripts();

        String[] fileNames = getJavaScriptFileNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFiles.setAdapter(adapter);
        binding.spinnerFiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadFileContent(fileNames[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.buttonSave.setOnClickListener(v -> saveFile());

        binding.buttonRename.setOnClickListener(v -> renameFile());

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
    private void loadFileContent(String fileName) {
        File file = new File(getContext().getFilesDir(), fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()];
                fis.read(buffer);
                fis.close();
                String content = new String(buffer, "UTF-8");
                binding.jsCodeInput.setText(content);
            } catch (IOException ex) {
                ex.printStackTrace();
                showToastOnUiThread("Error loading file");
            }
        }
    }
    private void saveFile() {
        String selectedFile = binding.spinnerFiles.getSelectedItem().toString();
        String fileContent = binding.jsCodeInput.getText().toString();

        try {
            FileOutputStream fos = getContext().openFileOutput(selectedFile, getContext().MODE_PRIVATE);
            fos.write(fileContent.getBytes());
            fos.close();
            showToastOnUiThread("File saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
            showToastOnUiThread("Error saving file");
        }
    }
    private void renameFile() {
        String oldFileName = binding.spinnerFiles.getSelectedItem().toString();
        String newFileName = binding.editTextNewFileName.getText().toString();

        File oldFile = new File(getContext().getFilesDir(), oldFileName);
        File newFile = new File(getContext().getFilesDir(), newFileName);

        if (oldFile.renameTo(newFile)) {
            showToastOnUiThread("File renamed successfully");
            // Update spinner and any other relevant UI components
            // Refresh file names in the spinner
        } else {
            showToastOnUiThread("Error renaming file");
        }
    }
    private String[] getJavaScriptFileNames() {
        File dir = getContext().getFilesDir();
        FilenameFilter filter = (dir1, name) -> name.endsWith(".js");
        File[] files = dir.listFiles(filter);

        if (files != null) {
            String[] fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                fileNames[i] = files[i].getName();
            }
            return fileNames;
        } else {
            // Fallback to default names if no files found
            return new String[]{"script_tesla.js", "script_mercedes.js", "script_receive_tesla.js"};
        }
    }
    private void initializeScripts() {
        String[] fileNames = {"script_tesla.js", "script_mercedes.js", "script_receive_tesla.js"}; // Predefined list of filenames
        for (String fileName : fileNames) {
            File file = new File(getContext().getFilesDir(), fileName);
            if (!file.exists()) {
                copyFileFromAssets(fileName);
            }
        }
    }
    private void copyFileFromAssets(String fileName) {
        try {
            InputStream is = getContext().getAssets().open(fileName);
            FileOutputStream fos = new FileOutputStream(new File(getContext().getFilesDir(), fileName));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
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


}
