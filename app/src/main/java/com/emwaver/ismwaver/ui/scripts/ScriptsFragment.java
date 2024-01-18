package com.emwaver.ismwaver.ui.scripts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.CommandSender;
import com.emwaver.ismwaver.SerialService;
import com.emwaver.ismwaver.databinding.FragmentScriptsBinding;
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

public class ScriptsFragment extends Fragment implements CommandSender {

    private ScriptsViewModel scriptsViewModel;
    private CC1101 cc1101;
    private Serial serial;
    private Console console;
    private Utils utils;
    private FragmentScriptsBinding binding; // Binding class for the fragment_scripts.xml layout
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


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Initialize view binding
        binding = FragmentScriptsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        scriptsViewModel = new ViewModelProvider(this).get(ScriptsViewModel.class);

        cc1101 = new CC1101(this);

        serial = new Serial(getContext(), this);

        console = new Console(getContext());

        utils = new Utils(getContext());

        initializeScripts();


        // Populate spinner with filenames
        String[] fileNames = getJavaScriptFileNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fileNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFiles.setAdapter(adapter);
        // Listener for spinner selection
        binding.spinnerFiles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadFileContent(fileNames[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Button listeners for save and rename
        binding.buttonSave.setOnClickListener(v -> saveFile());
        binding.buttonRename.setOnClickListener(v -> renameFile());


        // Set up your button click listeners
        binding.executeScriptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    try {
                        String jsCode = binding.jsCodeInput.getText().toString();
                        ScriptsEngine scriptsEngine = new ScriptsEngine(cc1101, scriptsViewModel, serial, console, utils);
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
        binding = null; // Important for avoiding memory leaks
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
}
