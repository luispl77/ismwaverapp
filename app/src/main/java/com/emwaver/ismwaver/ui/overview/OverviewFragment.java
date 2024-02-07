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
                updateAccordionSettings();
            }
        });


        return root;
    }

    public void updateAccordionSettings(){
        double frequencyMHz = cc.getFrequency();
        binding.frequencyEditText.setText(String.format(Locale.getDefault(), "%.6f", frequencyMHz));
        int modulation = cc.getModulation();
        binding.modulationEditText.setText(modulation == 0 ? "FSK" : "ASK");
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