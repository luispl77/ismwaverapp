package com.emwaver.ismwaver.ui.terminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.R;
import com.emwaver.ismwaver.databinding.FragmentTerminalBinding;

import java.util.Objects;


public class TerminalFragment extends Fragment{
    private FragmentTerminalBinding binding;
    private EditText terminalTextInput;
    private TextView terminalText;
    private TerminalViewModel terminalViewModel;

    private boolean filterEnabled = true;

    // In your onCreateView or onCreate method:



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTerminalBinding.inflate(inflater, container, false);
        View root = binding.getRoot(); // inflate fragment_terminal.xml

        terminalText = binding.terminalText; //get bindings
        terminalTextInput = binding.terminalTextInput;
        binding.terminalText.setMovementMethod(new ScrollingMovementMethod()); // Set the TextView as scrollable

        // Observe the LiveData and update the UI accordingly
        terminalViewModel = new ViewModelProvider(this).get(TerminalViewModel.class);
        terminalViewModel.getTerminalData().observe(getViewLifecycleOwner(), data -> {
            SpannableStringBuilder spannable = new SpannableStringBuilder();
            for (TerminalViewModel.TextWithColor textWithColor : data) {
                int start = spannable.length();
                spannable.append(textWithColor.getText());
                int end = spannable.length();

                spannable.setSpan(new ForegroundColorSpan(textWithColor.getColor()),
                        start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            terminalText.setText(spannable);
        });


        // Display input from EditText to TextView when the user hits 'Enter'
        terminalTextInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String userInput = terminalTextInput.getText().toString();
                sendUserInputToService(userInput); // Send to SerialService for transmitting over USB
                terminalViewModel.appendData(userInput+"\n>", ContextCompat.getColor(getContext(), R.color.user_input));
                terminalTextInput.setText("");
            }
            return false;
        });

        binding.filterCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            filterEnabled = isChecked;
        });

        return root;
    }



    @Override
    public void onStart() {
        super.onStart();
        // Register usbDataReceiver for listening to new data received on USB port
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_DATA_RECEIVED);
        requireActivity().registerReceiver(usbDataReceiver, filter); //todo: fix visibility of broadcast receivers
    }

    // Broadcast receiver for data coming from SerialService background USB service. Updates terminal live UI.
    private final BroadcastReceiver usbDataReceiver = new BroadcastReceiver() {
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //connect button
        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Constants.ACTION_INITIATE_USB_CONNECTION);
                getContext().sendBroadcast(intent); // Send intent to SerialService to initiate USB connection
            }
        });
        //clear-terminal-text button
        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                terminalViewModel.setData("");
                terminalText.setText("");
            }
        });
    }

    @Override
    public void onStop() {
        //requireActivity().unregisterReceiver(usbDataReceiver); //don't call this to leave the broadcast of the USB data received active.
        super.onStop();
    }

    //Broadcasts any data over to the SerialService. SerialService then transmits the data over USB.
    private void sendUserInputToService(String userInput) {
        Intent intent = new Intent(Constants.ACTION_SEND_DATA_TO_SERVICE);
        intent.putExtra("userInput", userInput);
        requireActivity().sendBroadcast(intent);
    }
}
