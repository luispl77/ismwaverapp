package com.emwaver.ismwaver.jsobjects;

import android.content.Context;

public class Utils {
    private Context context;

    /**
     * Constructor for Console utility.
     *
     * @param context The context to be used for sending broadcasts.
     */
    public Utils(Context context) {
        this.context = context;
    }

    /**
     * Sends a broadcast intent with the specified data string.
     *
     * @param delay_ms The string data to include in the intent.
     */
    public void delay(int delay_ms) {
        try {
            Thread.sleep(delay_ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getSignedBytes(int[] unsignedBytes) {
        byte[] signedBytes = new byte[unsignedBytes.length];
        for (int i = 0; i < unsignedBytes.length; i++) {
            signedBytes[i] = (byte) (unsignedBytes[i] & 0xFF);
        }
        return signedBytes;
    }

}
