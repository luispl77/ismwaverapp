package com.emwaver.ismwaver;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class RegisterFileManager {

    private static final String FILENAME = "cc1101_settings.bin";

    public static void saveRegisterSettings(Context context, byte[] registers) {
        try (FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE)) {
            fos.write(registers);
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception according to your application's policy
        }
    }

    public static byte[] getRegisterSettings(Context context) {

        try (FileInputStream fis = context.openFileInput(FILENAME)) {
            byte[] registers = new byte[0x2E]; // Size of the register settings array
            if (fis.read(registers) != registers.length) {
                throw new IOException("File size does not match expected register settings size.");
            }
            return registers;
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception, e.g., return default settings or notify the user
        }
        return null; // or default value
    }
}
