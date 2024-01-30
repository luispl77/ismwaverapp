package com.emwaver.ismwaver.jsobjects;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.emwaver.ismwaver.Constants;

public class Utils {

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

    public static String bytesToHexString(byte[] bytes) { //todo: verify change to static does not break anything
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    public static byte[] convertHexStringToByteArray(String hexString) {
        // Remove any non-hex characters (like spaces) if present
        hexString = hexString.replaceAll("[^0-9A-Fa-f]", "");
        Log.i("Hex Conversion", hexString);

        // Check if the string has an even number of characters
        if (hexString.length() % 2 != 0) {
            Log.e("Hex Conversion", "Invalid hex string");
            return null; // Return null or throw an exception as appropriate
        }

        byte[] bytes = new byte[hexString.length() / 2];

        StringBuilder hex_string = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hexString.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
            hex_string.append(String.format("%02X ", bytes[i]));
        }

        Log.i("Payload bytes", hex_string.toString());

        return bytes;
    }

    public static void changeStatus(String status, Context context) {
        Intent intent = new Intent(Constants.ACTION_UPDATE_STATUS);
        // Convert the message to bytes
        intent.putExtra("status", status);
        context.sendBroadcast(intent);
    }



}
