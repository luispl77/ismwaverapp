package com.emwaver.ismwaver.ui.rawmode;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.CommandSender;
import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.SerialService;
import com.emwaver.ismwaver.databinding.FragmentRawModeBinding;
import com.emwaver.ismwaver.jsobjects.CC1101;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RawModeFragment extends Fragment implements CommandSender {

    private RawModeViewModel continuousmodeViewModel;

    private FragmentRawModeBinding binding;

    private SerialService serialService;

    LineChart chart = null;

    private int chartMinX = 0;
    private int chartMaxX = 10000;

    private boolean isServiceBound = false;

    private float currentZoomLevel = 1.0f;

    private int prevRangeStart = 0;
    private int prevRangeEnd = 0;

    private CC1101 cc;

    public ScheduledExecutorService scheduler;

    private final int refreshRate = 100; // Refresh rate in milliseconds





    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SerialService.LocalBinder binder = (SerialService.LocalBinder) service;
            serialService = binder.getService();
            isServiceBound = true;
            Log.i("service binding", "onServiceConnected");
            //updateChart(compressDataAndGetDataSet(0, serialService.getBufferLength(), 1000));
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
        binding = FragmentRawModeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshChart, 0, refreshRate, TimeUnit.MILLISECONDS);
        // Todo: make executor function only execute when its recording

        chart = binding.chart;

        CC1101 cc = new CC1101(this);

        continuousmodeViewModel = new ViewModelProvider(this).get(RawModeViewModel.class);

        binding.connectButton.setOnClickListener(v -> {
            serialService.connectUSBSerial();
            //updateVisibleRange();
        });
        binding.initContinuousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(() -> {
                    cc.sendInitRxContinuous();
                    //showToastOnUiThread("check console");
                }).start();
            }
        });

        binding.retransmitButton.setOnClickListener(v -> {
            int bufferLength = serialService.getDataBufferLength();
            serialService.setMode(0); //transmit
            Toast.makeText(getContext(), "Buffer Length: " + bufferLength, Toast.LENGTH_SHORT).show();

            transmitBuffer();

        });

        binding.clearBufferButton.setOnClickListener(v -> {
            serialService.clearDataBuffer();
            Toast.makeText(getContext(), "buffer cleared" , Toast.LENGTH_SHORT).show();
            updateChart(compressDataAndGetDataSet(continuousmodeViewModel.getVisibleRangeStart(), continuousmodeViewModel.getVisibleRangeEnd(), 1000));
        });

        binding.startRecordingButton.setOnClickListener(v -> {
            String contCommand = "raw";
            byte[] byteArray = contCommand.getBytes();
            serialService.setMode(Constants.RECEIVE);
            serialService.write(byteArray);

        });

        binding.stopRecordingButton.setOnClickListener(v -> {
            String contCommand = "ssss";
            byte[] byteArray = contCommand.getBytes();
            serialService.write(byteArray);
            new Thread(() -> {
                serialService.emptyReadBuffer(); //this function busy waits
                serialService.setMode(Constants.TERMINAL); //wait before the buffer is empty
            }).start();

            chartMaxX = serialService.getDataBufferLength();
            XAxis xAxis = chart.getXAxis();
            xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
            xAxis.setAxisMaximum(chartMaxX);
            updateChart(compressDataAndGetDataSet(continuousmodeViewModel.getVisibleRangeStart(), continuousmodeViewModel.getVisibleRangeEnd(), 1000));
        });


        initChart();

        chart.setOnChartGestureListener(new OnChartGestureListener() {
            //region useless chart listeners
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }
            //endregion
            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                float newZoomLevel = chart.getScaleX();

                if (Math.abs(newZoomLevel - currentZoomLevel) >= (newZoomLevel/10)) {
                    // Zoom level changed significantly
                    currentZoomLevel = newZoomLevel;

                    continuousmodeViewModel.setVisibleRangeStart((int) chart.getLowestVisibleX());
                    continuousmodeViewModel.setVisibleRangeEnd((int) chart.getHighestVisibleX());

                    updateChart(compressDataAndGetDataSet(continuousmodeViewModel.getVisibleRangeStart(), continuousmodeViewModel.getVisibleRangeEnd(), 1000));
                }
            }
            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                int visibleRangeStart = (int) chart.getLowestVisibleX();
                int visibleRangeEnd = (int) chart.getHighestVisibleX();
                continuousmodeViewModel.setVisibleRangeStart(visibleRangeStart);
                continuousmodeViewModel.setVisibleRangeEnd(visibleRangeStart);

                int span = visibleRangeEnd - visibleRangeStart;
                float translationThreshold = (float)span / 100; // Define an appropriate threshold value


                // Check if the chart is at its boundaries
                if ((visibleRangeStart<= chartMinX && dX > 0) || (visibleRangeEnd >= chartMaxX && dX < 0)) {
                    // At the boundary, do not update
                    return;
                }

                // Check if the translation is significant
                if (Math.abs(visibleRangeStart - prevRangeStart) > translationThreshold ||
                        Math.abs(visibleRangeEnd - prevRangeEnd) > translationThreshold &&
                                span >= 10 ) {

                    prevRangeStart = visibleRangeStart;
                    prevRangeEnd = visibleRangeEnd;

                    updateChart(compressDataAndGetDataSet(visibleRangeStart, visibleRangeEnd, 1000));
                }
            }
        });

        return root;
    }



    private void transmitBuffer() {
        int nativeBufferSize = serialService.getDataBufferLength(); // Existing JNI method to get the native buffer size
        sendStartTransmissionCommand(nativeBufferSize);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int packetSize = 50; //12.5 bytes per frame, 10us sampling period
        long startTime = System.nanoTime();
        final long period = 4000 * 1000;
        final long flow_time_delta = 1000 * 1000;

        for (int i = 0; i < nativeBufferSize; i += packetSize) {
            int end = Math.min(i + packetSize, nativeBufferSize);
            byte[] packet = serialService.getBufferRange(i, end); // Native method to get a range of the buffer

            startTime += period;
            int bufferStatus = getLogStatus();
            if (bufferStatus > 200 && bufferStatus < 300) {
                serialService.write(packet);
            } else if(bufferStatus > 300){
                serialService.write(packet);
                startTime += flow_time_delta; //write slower than the processing speed
            }
            else if(bufferStatus < 300){
                serialService.write(packet);
                startTime -= flow_time_delta; //write faster than the processing speed
            }
            while (System.nanoTime() < startTime) {
                // Busy wait
            }
        }
        serialService.clearCommandBuffer();
    }

    private int getLogStatus(){
        int bufferStatus = serialService.getStatusNumber(); // Get buffer status from STM32
        if(bufferStatus == -1){
            Log.i("bufstatus", "not found" + "   buflen: " + serialService.getCommandBufferLength());
        }
        else{
            Log.i("bufstatus", "" + bufferStatus  + "   buflen: " + serialService.getCommandBufferLength());
        }
        return bufferStatus;
    }

    private void sendStartTransmissionCommand(int size) {
        // Convert "tran" string to bytes
        String contCommand = "tran";
        byte[] commandBytes = contCommand.getBytes();
        // Send the byte array over the serial connection
        serialService.write(commandBytes);
    }

    public void initChart() {
        // Configure the chart (optional, based on your needs)
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setScaleYEnabled(false); // Disable Y-axis scaling
        chart.setScaleXEnabled(true);  // Enable X-axis scaling

        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(chartMinX); // Start at 0 microseconds
        xAxis.setAxisMaximum(chartMaxX); // End at the maximum X value

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0); // Set minimum value for the left Y-axis
        leftAxis.setAxisMaximum(255); // Set maximum value for the left Y-axis

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMinimum(0); // Set minimum value for the right Y-axis
        rightAxis.setAxisMaximum(255); // Set maximum value for the right Y-axis


    }

    private void refreshChart() {
        if(serialService.getRecordingContinuous()){
            // Get the current visible range
            int visibleRangeStart = (int) chart.getLowestVisibleX();
            int visibleRangeEnd = (int) chart.getHighestVisibleX();
            continuousmodeViewModel.setVisibleRangeStart((int) chart.getLowestVisibleX());
            continuousmodeViewModel.setVisibleRangeEnd((int) chart.getHighestVisibleX());

            chartMaxX = serialService.getDataBufferLength()*8;
            XAxis xAxis = chart.getXAxis();
            xAxis.setAxisMinimum(chartMinX);
            xAxis.setAxisMaximum(chartMaxX);
            // Update the chart
            getActivity().runOnUiThread(() -> updateChart(compressDataAndGetDataSet(visibleRangeStart, visibleRangeEnd, 1000)));
        }
    }

    private LineDataSet compressDataAndGetDataSet(int rangeStart, int rangeEnd, int numberBins) {
        // Call the native method
        Object[] result = (Object[]) serialService.compressDataBits(rangeStart, rangeEnd, numberBins);

        float[] timeValues = (float[]) result[0];
        float[] dataValues = (float[]) result[1];

        Log.i("compressed", dataValues.length + " values");

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < timeValues.length; i++) {
            entries.add(new Entry(timeValues[i], dataValues[i]));
        }
        LineDataSet lineDataSet = new LineDataSet(entries, "Demodulator");
        // Disable the drawing of values for each point
        lineDataSet.setDrawValues(false);
        // Increase the line thickness (example: 3f for a thicker line)
        lineDataSet.setLineWidth(3f);
        // Set a more pronounced line color (example: Color.BLUE)
        lineDataSet.setColor(Color.parseColor("#003d99"));

        return lineDataSet;
    }

    private void updateChart(LineDataSet lineDataSet) {
        LineData lineData = new LineData(lineDataSet);
        chart.setData(lineData);
        chart.notifyDataSetChanged();
        chart.invalidate();
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
    public void onResume() {
        super.onResume();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshChart, 0, refreshRate, TimeUnit.MILLISECONDS);

    }
    @Override
    public void onPause() {
        super.onPause();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
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

        serialService.clearCommandBuffer(); // Optionally clear the queue after processing (pollData() should already clear the response)
        return response;
    }
}