package com.emwaver.ismwaver.ui.overview;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.emwaver.ismwaver.CC1101;
import com.emwaver.ismwaver.R;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.Utils;
import com.emwaver.ismwaver.databinding.FragmentOverviewBinding;

import java.util.List;
import java.util.Locale;

public class OverviewFragment extends Fragment {

    private OverviewViewModel overviewViewModel;
    private FragmentOverviewBinding binding;
    private USBService USBService;
    private boolean isServiceBound = false;
    private CC1101 cc;
    private final Handler handler = new Handler(Looper.getMainLooper()); // Handler on the main UI thread
    private boolean wasPreviouslyConnected = false;

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




        return root;
    }

    public void updateAccordionSettings(){
        double frequencyMHz = cc.getFrequency();
        binding.frequencyEditText.setText(String.format(Locale.getDefault(), "%.6f", frequencyMHz));
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