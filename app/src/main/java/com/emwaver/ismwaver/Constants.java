package com.emwaver.ismwaver;

public class Constants {
    // Action string for the broadcast when connecting USB
    public static final String ACTION_INITIATE_USB_CONNECTION = "com.emwaver.ACTION_INITIATE_USB_CONNECTION";
    public static final String ACTION_CONNECT_USB = "com.emwaver.ACTION_CONNECT_USB";
    public static final String ACTION_CONNECT_USB_BOOTLOADER = "com.emwaver.GRANT_USB";

    // Action string for the status bar message
    public static final String ACTION_UPDATE_STATUS = "com.emwaver.UPDATE_STATUS";

    // Any other constant values that are used across multiple classes
    public static final int USB_BAUD_RATE = 115200;

    public static final int TRANSMIT = 0;

    public static final int RECEIVE = 1;

    public static final int TERMINAL = 2;

    public static final boolean DATA_BUFFER = true;
    public static final boolean COMMAND_BUFFER = false;

    // Private constructor to prevent instantiation
    private Constants() {
        // This utility class is not publicly instantiable
    }
}

