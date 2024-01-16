package com.emwaver.ismwaver;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SerialService extends Service implements SerialInputOutputManager.Listener {

    static {
        System.loadLibrary("native-lib");
    }
    private SerialInputOutputManager ioManager;
    private UsbSerialPort finalPort = null;
    private final IBinder binder = new LocalBinder();
    private native void addToBuffer(byte[] data);
    public native int getBufferLength();
    public native byte[] pollData(int length);
    public native void clearBuffer();
    public native Object[] compressDataBits(int rangeStart, int rangeEnd, int numberBins);
    public native boolean getRecordingContinuous();
    public native void setRecordingContinuous(boolean recording);

    public native long [] findPulseEdges(int samplesPerSymbol, int errorTolerance, int maxLowPulseMultiplier);

    public native int getStatusNumber();



    public class LocalBinder extends Binder {
        public SerialService getService() {
            // Return this instance of SerialService so clients can call public methods
            return SerialService.this;
        }
    }


    public void write(byte[] bytes){
        if(bytes != null && finalPort != null) {
            try {
                finalPort.write(bytes, 2000);
            } catch (IOException e) {
                Log.e("SerialService", "Error writing to port: ", e);
            }
        }
        else{
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show();
        }
    }

    public void read(byte [] buf){

    }

    public void emptyReadBuffer() {
        // Assuming a reasonable buffer size for each read operation
        if(finalPort == null){
            Toast.makeText(this, "No USB device connected", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] buf = new byte[8192];
        try {
            int bytesRead = 1;
            // Continue reading as long as data is available
            while (bytesRead > 0) {
                bytesRead = finalPort.read(buf, 500);
                if(bytesRead > 0){
                    Log.i("SerialService", "emptied " + bytesRead + " bytes from read buffer");
                }
            }
        } catch (IOException e) {
            Log.e("SerialService", "Error reading while emptying buffer", e);
        }
    }




    private final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.ACTION_CONNECT_USB.equals(intent.getAction())) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // Permission granted, initiate connection
                            try {
                                finalPort = connectUSBSerialDevice(device);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(context, "USB Serial Permission Granted", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "USB Serial Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }else if (Constants.ACTION_INITIATE_USB_CONNECTION.equals(intent.getAction())) {
                connectUSBSerial(); // Method to start USB connection process

            }else if (Constants.ACTION_SEND_DATA_TO_SERVICE.equals(intent.getAction())) {
                String userInput = intent.getStringExtra("userInput");
                // Send the received data over USB.
                Log.i("service", userInput);
                assert userInput != null;
                byte[] byteArray = userInput.getBytes();
                try {
                    if(byteArray != null && finalPort != null)
                        finalPort.write(byteArray, 2000);
                    else{
                        Toast.makeText(context, "No devices found", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Log.e("SerialService", "Error writing to port: ", e);
                }
            }
        }
    };




    //Called when new data arrives on the USB port that is connected. Stores date in buffer in c++ environment
    @Override
    public void onNewData(byte[] data) {
        addToBuffer(data);
    }



    //Finds the port in which the USB device is connected to. Connects to the driver and returns the port.
    public void connectUSBSerial() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        // Check if permission is already granted
        if (!manager.hasPermission(device)) {
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    new Intent(Constants.ACTION_CONNECT_USB)
                            .putExtra(UsbManager.EXTRA_DEVICE, device),
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
            );
            manager.requestPermission(device, usbPermissionIntent);
        } else {
            // Permission is already granted, open the device here or handle as needed
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
            try {
                finalPort = connectUSBSerialDevice(device);
                Toast.makeText(this, "Connected!\nDriver: " + finalPort + "\n max pkt size: " + finalPort.getReadEndpoint().getMaxPacketSize(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // You might want to initiate connection here if permission is already granted
        }
    }

    private UsbSerialPort connectUSBSerialDevice(UsbDevice device) throws IOException {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection connection = manager.openDevice(device);
        if (connection == null) {
            Toast.makeText(this, "Connection returned null", Toast.LENGTH_SHORT).show();
            return null;
        }

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        UsbSerialPort port = driver.getPorts().get(0); // Assuming there's only one port
        port.open(connection);
        port.setParameters(Constants.USB_BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        ioManager = new SerialInputOutputManager(port, this);
        ioManager.start();

        return port;
    }


    public void connectUSBFlash() {
        final int USB_VENDOR_ID = 1155;   // VID while in DFU mode 0x0483
        final int USB_PRODUCT_ID = 57105; // PID while in DFU mode 0xDF11
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

        boolean deviceFound = false;
        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == USB_VENDOR_ID && device.getProductId() == USB_PRODUCT_ID) {
                deviceFound = true;
                // Check if permission is already granted
                if (!manager.hasPermission(device)) {
                    PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(
                            this,
                            0,
                            new Intent(Constants.ACTION_CONNECT_USB_BOOTLOADER)
                                    .putExtra(UsbManager.EXTRA_DEVICE, device),
                            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
                    );
                    manager.requestPermission(device, usbPermissionIntent);
                } else {
                    // Permission is already granted, handle as needed
                    Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }

        if (!deviceFound) {
            Toast.makeText(this, "No STM32 bootloader connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendIntentToTerminal(byte[] data) {
        Intent intent = new Intent(Constants.ACTION_USB_DATA_RECEIVED);
        intent.putExtra("data", data);
        intent.putExtra("source", "serial");
        sendBroadcast(intent);
    }

    public void sendStringIntent(String string, String source){
        Intent intent = new Intent(Constants.ACTION_USB_DATA_RECEIVED);
        // Convert the message to bytes
        byte[] messageBytes = string.getBytes();

        intent.putExtra("data", messageBytes);
        intent.putExtra("source", source); // Add this line; "system" is an example, replace with actual source
        this.sendBroadcast(intent);
    }

    public void changeStatus(String status) {
        Intent intent = new Intent(Constants.ACTION_UPDATE_STATUS);
        // Convert the message to bytes
        intent.putExtra("status", status);
        this.sendBroadcast(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // Register receivers for listening for broadcasts from Serial fragment.
        IntentFilter filterConnectButton = new IntentFilter(Constants.ACTION_CONNECT_USB);
        registerReceiver(connectReceiver, filterConnectButton); // Receiver for the connect button in terminal.
        IntentFilter filter = new IntentFilter(Constants.ACTION_CONNECT_USB_BOOTLOADER);
        registerReceiver(connectReceiver, filter);
        IntentFilter filterConnection = new IntentFilter(Constants.ACTION_INITIATE_USB_CONNECTION);
        registerReceiver(connectReceiver, filterConnection);

        IntentFilter filterData = new IntentFilter(Constants.ACTION_SEND_DATA_TO_SERVICE);
        registerReceiver(connectReceiver, filterData); // Receiver for the data inputted in terminal fragment and entered, to then be sent over USB.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectReceiver); //will this ever be destroyed? perhaps when app closes.
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public void onRunError(Exception e) {

    }

    // Callback that runs when the service is started. Not useful for now.
    // START_STICKY: If the service is killed by the system, recreate it, but do not redeliver the last intent. Instead, the system calls onStartCommand with a null intent, unless there are pending intents to start the service. This is suitable for services that are continually running in the background (like music playback) and that don't rely on the intent data.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


}
