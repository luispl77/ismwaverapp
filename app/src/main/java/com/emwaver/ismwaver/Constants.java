package com.emwaver.ismwaver;

public class Constants {
    // Action string for the broadcast when connecting USB
    public static final String ACTION_INITIATE_USB_CONNECTION = "com.example.myapp.ACTION_INITIATE_USB_CONNECTION";
    public static final String ACTION_CONNECT_USB = "com.example.ACTION_CONNECT_USB";
    public static final String ACTION_CONNECT_USB_BOOTLOADER = "com.example.emwaver10.GRANT_USB";



    // Action string for the broadcast when data is received from USB
    public static final String ACTION_USB_DATA_RECEIVED = "com.example.ACTION_USB_DATA";
    public static final String ACTION_UPDATE_STATUS = "com.example.UPDATE_STATUS";

    // Any other constant values that are used across multiple classes
    public static final int USB_BAUD_RATE = 115200;

    public static final int TRANSMIT = 0;

    public static final int RECEIVE = 1;

    public static final int TERMINAL = 2;

    // Private constructor to prevent instantiation
    private Constants() {
        // This utility class is not publicly instantiable
    }
}

