package com.emwaver.ismwaver.ui.overview;

import androidx.lifecycle.ViewModelProvider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.emwaver.ismwaver.CC1101;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.databinding.FragmentOverviewBinding;

import java.util.List;
import java.util.Locale;

public class OverviewFragment extends Fragment {

    private OverviewViewModel overviewViewModel;
    private FragmentOverviewBinding binding;
    private USBService USBService;
    private boolean isServiceBound = false;

    private byte[] registerPacket;
    private CC1101 cc;
    private AccordionAdapter adapter;

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

    public OverviewFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        overviewViewModel = new ViewModelProvider(this).get(OverviewViewModel.class);

        binding = FragmentOverviewBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //adapter = new AccordionAdapter();
        //binding.recyclerView.setAdapter(adapter);

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






        binding.getRegsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerPacket = cc.getRegisterPacket();
                //adapter.updateAccordionSettings(registerPacket);
                updateAccordionSettings(registerPacket);
            }
        });


        return root;
    }

    public void updateAccordionSettings(byte [] registerPacket){
        int freq2 = registerPacket[7] & 0xFF;
        int freq1 = registerPacket[8] & 0xFF;
        int freq0 = registerPacket[9] & 0xFF;

        // Convert the frequency bytes to a single integer
        long frequency = ((freq2 << 16) | (freq1 << 8) | freq0);
        // Assuming the oscillator frequency is 26 MHz
        double fOsc = 26e6; // 26 MHz
        double frequencyMHz = frequency * (fOsc / Math.pow(2, 16)) / 1e6; // Convert to MHz
        Log.i("frequencyMHz", ""+frequencyMHz);
        binding.frequencyEditText.setText(String.format(Locale.getDefault(), "%.6f", frequencyMHz));

        // Extract modulation setting from MDMCFG2
        int mdmcfg2 = registerPacket[12] & 0xFF; // Replace 10 with the actual index of MDMCFG2 in registerPacket
        int modulationSetting = (mdmcfg2 >> 4) & 0x07; // Shift right by 4 bits and mask out everything but bits 6:4
        Log.i("modulationSetting", ""+modulationSetting);
        String modulation;
        switch (modulationSetting) {
            case 0:
                modulation = "FSK";
                break;
            case 3:
                modulation = "ASK";
                break;
            default:
                modulation = "Unknown"; // Handle other cases or set a default value
                break;
        }
        binding.modulationEditText.setText(modulation);

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

}