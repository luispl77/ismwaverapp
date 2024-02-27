package com.emwaver.ismwaver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.ui.console.ConsoleFragment;
import com.emwaver.ismwaver.ui.overview.OverviewFragment;
import com.emwaver.ismwaver.ui.packetmode.PacketModeFragment;
import com.emwaver.ismwaver.ui.rawmode.RawModeFragment;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static final Map<String, Uri> STATUS_BAR_URIS = new HashMap<>();


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

    public static String toHexStringWithHexPrefix(byte[] array) {
        StringBuilder hexString = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            // Convert the byte to a hex string with a leading zero, then take the last two characters
            // (in case of negative bytes, which result in longer hex strings)
            String hex = "0x" + Integer.toHexString(array[i] & 0xFF).toUpperCase();

            hexString.append(hex);

            // Append comma and space if this is not the last byte
            if (i < array.length - 1) {
                hexString.append(", ");
            }
        }
        hexString.append("]");
        return hexString.toString();
    }

    public static void changeStatus(String status, Context context) {
        Intent intent = new Intent(Constants.ACTION_UPDATE_STATUS);
        // Convert the message to bytes
        intent.putExtra("status", status);
        context.sendBroadcast(intent);
    }


    public static void updateStatusBarFile(Fragment fragment, Uri uri) {
        // Store the URI against the fragment's class name
        STATUS_BAR_URIS.put(fragment.getClass().getName(), uri);
        // Update the action bar status with the file name
        updateActionBarStatus(fragment, getFileNameFromUri(fragment.getContext(), uri));
    }

    public static void updateStatusBarFile(Fragment fragment) {
        Uri uri = STATUS_BAR_URIS.get(fragment.getClass().getName());
        if (uri != null) {
            // If the URI exists, update the action bar status with the file name
            updateActionBarStatus(fragment, getFileNameFromUri(fragment.getContext(), uri));
        } else {
            // Handle the case where there's no URI associated with the fragment
            // For example, setting a default status or indicating that no file is selected
            updateActionBarStatus(fragment, "No File Selected");
        }
    }

    private static void updateActionBarStatus(Fragment fragment, String status) {
        if (fragment.getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) fragment.getActivity();
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle(status); // Or use setTitle() if you prefer
            }
        }
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        if (uri == null) return "Unknown File";
        String fileName = null;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileName != null ? fileName : "Unknown File";
    }

    public static void setStatusBarUri(Class<? extends Fragment> fragmentClass, Uri uri) {
        // Directly store the URI against the fragment class name
        STATUS_BAR_URIS.put(fragmentClass.getName(), uri);
    }



}
