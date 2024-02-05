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

import com.emwaver.ismwaver.CC1101;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.databinding.FragmentOverviewBinding;

import java.util.List;

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

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AccordionAdapter();
        binding.recyclerView.setAdapter(adapter);

        binding.getRegsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerPacket = cc.getRegisterPacket();
            }
        });


        return root;
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