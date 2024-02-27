package com.emwaver.ismwaver.ui.console;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.databinding.FragmentConsoleBinding;
import com.emwaver.ismwaver.CC1101;
import com.emwaver.ismwaver.Console;
import com.emwaver.ismwaver.Utils;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class ConsoleFragment extends Fragment {
    private FragmentConsoleBinding binding;
    private EditText terminalTextInput;
    private TextView windowText;
    private ConsoleViewModel consoleViewModel;
    private CC1101 cc;
    private Console console;
    private Utils utils;
    private USBService USBService;
    private boolean isServiceBound = false;
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
            cc = new CC1101(USBService);
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

        Utils.updateStatusBarFile(this);

        binding.consoleWindowText.setMovementMethod(new ScrollingMovementMethod()); // Set the TextView as scrollable

        // Observe the LiveData and update the UI accordingly
        consoleViewModel = new ViewModelProvider(this).get(ConsoleViewModel.class);

        consoleViewModel.getWindowData().observe(getViewLifecycleOwner(), data -> {
            binding.consoleWindowText.setText(data);
            binding.consoleWindowScrollView.post(() -> binding.consoleWindowScrollView.fullScroll(View.FOCUS_DOWN));
        });

        //loadScriptFromAssets();


        console = new Console();

        utils = new Utils();


        binding.saveFileAsButton.setOnClickListener(v -> {
            buttonCreateFile();
        });

        binding.saveFileButton.setOnClickListener(v -> {
            buttonSaveFile();
        });

        binding.openFileButton.setOnClickListener(v -> {
            buttonOpenFile();
        });

        binding.executeScriptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable the clear button
                binding.clearButton.setEnabled(false);
                // Optionally, change the button color to grey to indicate it's disabled
                binding.clearButton.setBackgroundColor(Color.GRAY); // Use an appropriate color
                new Thread(() -> {
                    try {
                        String jsCode = binding.jsCodeInput.getText().toString();
                        ScriptsEngine scriptsEngine = new ScriptsEngine(cc, console, utils);
                        //Utils.changeStatus("Running script...", getContext());
                        String result = scriptsEngine.executeJavaScript(jsCode);
                        Console.print("\n<Console>");
                        if(result != null){
                            Console.print(result);
                        }
                    } finally {
                        unbindServiceIfNeeded();
                        //Utils.changeStatus("", getContext());
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.clearButton.setEnabled(true);
                                // Optionally, reset the button color to indicate it's enabled
                                binding.clearButton.setBackgroundColor(Color.BLUE); // Reset to original color
                            }
                        });
                    }
                }).start();
            }
        });

        binding.consoleTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String userInput = terminalTextInput.getText().toString();
                consoleViewModel.appendData(userInput+"\n");
                terminalTextInput.setText("");
            }
            return false;
        });


        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                consoleViewModel.clearWindowData();
                consoleViewModel.appendData("<Console>");
            }
        });

        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                USBService.connectUSBSerial();
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

    @Override
    public void onStart() {
        super.onStart();
        // bind service
        if (!isServiceBound && getActivity() != null) {
            Intent intent = new Intent(getActivity(), USBService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }



    @Override
    public void onStop() {
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

    public void buttonSaveFile() {
        writeChangesToFile();
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

    private void writeChangesToFile() {
        if (currentFileUri == null) {
            Log.e("filesys", "No file is currently open");
            return;
        }

        try (OutputStream outstream = getActivity().getContentResolver().openOutputStream(currentFileUri)) {
            String fileContent = binding.jsCodeInput.getText().toString();
            outstream.write(fileContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e("filesys", "Error writing to file", e);
        }
    }




    public void showToastOnUiThread(final String message) {
        if (isAdded()) { // Check if Fragment is currently added to its activity
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
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
