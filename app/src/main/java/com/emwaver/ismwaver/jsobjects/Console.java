package com.emwaver.ismwaver.jsobjects;

import android.content.Context;
import android.content.Intent;

import com.emwaver.ismwaver.Constants;

public class Console {
    private Context context;

    /**
     * Constructor for Console utility.
     *
     * @param context The context to be used for sending broadcasts.
     */
    public Console(Context context) {
        this.context = context;
    }

    /**
     * Sends a broadcast intent with the specified data string.
     *
     * @param dataString The string data to include in the intent.
     */
    public void print(String dataString) {
        Intent intent = new Intent(Constants.ACTION_USB_DATA_RECEIVED);

        String encapsulated = dataString;
        // Convert the string back to a byte array
        byte[] dataBytes = encapsulated.getBytes();

        // Put the byte array into the intent
        intent.putExtra("data", dataBytes);
        intent.putExtra("source", "javascript");

        context.sendBroadcast(intent);
    }



}
