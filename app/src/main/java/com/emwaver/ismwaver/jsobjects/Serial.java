package com.emwaver.ismwaver.jsobjects;

import android.content.Context;
import android.content.Intent;

import com.emwaver.ismwaver.CommandSender;
import com.emwaver.ismwaver.Constants;

public class Serial {

    private Context context;

    private final CommandSender commandSender;

    /**
     * Constructor for Serial utility.
     *
     * @param context The context to be used for sending broadcasts.
     */
    public Serial(Context context, CommandSender commandSender) {
        this.context = context;
        this.commandSender = commandSender;
    }

    /**
     * Sends a broadcast intent with the specified data string.
     *
     * @param dataString The string data to include in the intent.
     */
    public void sendTerminalData(String dataString) {
        Intent intent = new Intent(Constants.ACTION_USB_DATA_RECEIVED);

        // Convert the string back to a byte array
        byte[] dataBytes = dataString.getBytes();

        // Put the byte array into the intent
        intent.putExtra("data", dataBytes);

        context.sendBroadcast(intent);
    }


    public byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis){
        return commandSender.sendCommandAndGetResponse(command, expectedResponseSize, busyDelay, timeoutMillis);
    }

    // Additional methods can be added here if needed
}


