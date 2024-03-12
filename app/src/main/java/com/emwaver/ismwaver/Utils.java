package com.emwaver.ismwaver;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    // SharedPreferences file name
    private static final String PREFS_FILE_NAME = "MyPrefs";

    // Define constants for SharedPreferences keys
    public static final String KEY_CONSOLE_FRAGMENT = "ConsoleFragmentUri";
    public static final String KEY_RAW_MODE_FRAGMENT = "RawModeFragmentUri";
    public static final String KEY_OVERVIEW_FRAGMENT = "OverviewFragmentUri";
    public static final String KEY_PACKET_MODE_FRAGMENT = "PacketModeFragmentUri";

    // Simplify saving URIs


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


    public static void updateActionBarStatus(Fragment fragment, String status) {
        if (fragment.getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) fragment.getActivity();
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle(status); // Or use setTitle() if you prefer
            }
        }
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        if (uri == null) return "No File Selected";
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

    public static void saveUri(Context context, String fragmentIdentifier, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(fragmentIdentifier, uri.toString());
        editor.apply();
    }

    public static Uri getUri(Context context, String fragmentIdentifier) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(fragmentIdentifier, null);
        return uriString != null ? Uri.parse(uriString) : null;
    }







}
