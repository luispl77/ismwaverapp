package com.emwaver.ismwaver.ui.flash;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.USBService;
import com.emwaver.ismwaver.databinding.FragmentFlashBinding;

import java.util.Arrays;

public class FlashFragment extends Fragment implements Dfu.DfuListener {
    private FragmentFlashBinding binding;
    private final static int USB_VENDOR_ID = 1155;   // VID while in DFU mode 0x0483
    private final static int USB_PRODUCT_ID = 57105; // PID while in DFU mode 0xDF11
    private FlashViewModel flashViewModel;

    private Dfu dfu;
    private static final int REQUEST_CODE_ATTACH = 1;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100; // A unique request code
    private TextView status;

    private USBService USBService;

    private boolean isServiceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            USBService.LocalBinder binder = (USBService.LocalBinder) service;
            USBService = binder.getService();
            isServiceBound = true;
            // Set the UsbDeviceConnection in Dfu
            //dfu.setUsbDeviceConnection(USBService.getUsbDeviceConnection());
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
        super.onCreate(savedInstanceState);

        flashViewModel = new ViewModelProvider(this).get(FlashViewModel.class);
        binding = FragmentFlashBinding.inflate(inflater, container, false);

        dfu = new Dfu(USB_VENDOR_ID, USB_PRODUCT_ID, this);
        dfu.setListener(this);

        status = binding.status;

        binding.connectButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                USBService.connectUSBFlash();
            }
        });

        binding.clearTxt.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                status.setText("");
            }
        });


        binding.writeBlockButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                int BLOCK_SIZE = 2048;
                int startAddress = 0x08000000;
                byte [] buffer = new byte[BLOCK_SIZE];
                // Initialize all elements to 0x69
                byte valueToSet = (byte) 0x69; // Casting is important as 0x69 is an int literal
                for (int i = 0; i < BLOCK_SIZE; i++)
                    buffer[i] = valueToSet;
                try {

                    dfu.massErase();
                    dfu.setAddressPointer(0x08000000);

                    byte[] block = new byte[BLOCK_SIZE];
                    Arrays.fill(block, (byte) 0x69); // Fill the block with 0x69
                    dfu.writeBlock(block, 2, BLOCK_SIZE); // Assuming writeBlock method is implemented in your Dfu class
                    //status.append("wrote flash\n");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });


        binding.massEraseButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dfu.massErase();
                } catch (Exception e) {
                    status.append(e.toString());
                }
            }
        });


        binding.readFlashButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dfu.readFlash(0x6BE0);
                } catch (Exception e) {
                    status.append(e.toString());
                }
            }
        });


        binding.writeFlashButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    dfu.massErase();
                    dfu.setAddressPointer(0x08000000);

                    dfu.writeFlash();
                } catch (Exception e) {
                    status.append(e.toString());
                }
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isServiceBound && getActivity() != null) {
            Intent intent = new Intent(getActivity(), USBService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        IntentFilter filter = new IntentFilter(Constants.ACTION_CONNECT_USB_BOOTLOADER);
        requireActivity().registerReceiver(connectReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    private final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_CONNECT_USB_BOOTLOADER.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbDeviceConnection connection =
                                    ((UsbManager) context.getSystemService(Context.USB_SERVICE)).openDevice(device);
                            dfu.setUsbDeviceConnection(connection);
                            Toast.makeText(context, "USB Permission Granted", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("service", "USB Permission Denied");
                        Toast.makeText(context, "USB Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };



    public void onStatusMsg(String msg) {
        // TODO since we are appending we should make the TextView scrollable like a log
        status.append(msg);
    }

}
