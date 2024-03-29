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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.R;
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

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.console_menu, menu);
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
                } else if (itemId == R.id.execute) {
                    executeScript();
                    return true;
                } else if (itemId == R.id.clear) {
                    clearConsole();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        Utils.updateActionBarStatus(this, Utils.getFileNameFromUri(getContext(), Utils.getUri(getContext(), Utils.KEY_CONSOLE_FRAGMENT)));
        loadFileToEditText(Utils.getUri(getContext(), Utils.KEY_CONSOLE_FRAGMENT));

        binding.consoleWindowText.setMovementMethod(new ScrollingMovementMethod()); // Set the TextView as scrollable

        // Observe the LiveData and update the UI accordingly
        consoleViewModel = new ViewModelProvider(this).get(ConsoleViewModel.class);

        consoleViewModel.getWindowData().observe(getViewLifecycleOwner(), data -> {
            binding.consoleWindowText.setText(data);
            binding.consoleWindowScrollView.post(() -> binding.consoleWindowScrollView.fullScroll(View.FOCUS_DOWN));
        });



        console = new Console();

        utils = new Utils();


        createFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    saveFileToUri(uri);
                    Utils.saveUri(getContext(), Utils.KEY_CONSOLE_FRAGMENT, uri);
                    Utils.updateActionBarStatus(this, Utils.getFileNameFromUri(getContext(), Utils.getUri(getContext(), Utils.KEY_CONSOLE_FRAGMENT)));
                }
            }
        });

        openFileLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                loadFileToEditText(uri);
                Utils.saveUri(getContext(), Utils.KEY_CONSOLE_FRAGMENT, uri);
                Utils.updateActionBarStatus(this, Utils.getFileNameFromUri(getContext(), Utils.getUri(getContext(), Utils.KEY_CONSOLE_FRAGMENT)));
            }
        });

        return root;
    }

    private void executeScript(){
        new Thread(() -> {
            try {
                String jsCode = binding.jsCodeInput.getText().toString();
                ScriptsEngine scriptsEngine = new ScriptsEngine(cc, console, utils);
                String result = scriptsEngine.executeJavaScript(jsCode);
                Console.print("\n<Console>");
                if(result != null){
                    Console.print(result);
                }
            } finally {
                unbindServiceIfNeeded();
            }
        }).start();
    }

    private void clearConsole(){
        consoleViewModel.clearWindowData();
        consoleViewModel.appendData("<Console>");
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

    public void saveAsFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Set MIME Type as per your requirement
        intent.putExtra(Intent.EXTRA_TITLE, "myScript.js");

        createFileLauncher.launch(intent);
    }

    public void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // MIME type for .raw files or use "*/*" for any file type
        openFileLauncher.launch(new String[]{"*/*"}); // Pass the MIME type as an array
    }

    public void saveFile() {
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
        Uri currentFileUri = Utils.getUri(getContext(), Utils.KEY_CONSOLE_FRAGMENT);
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


}
