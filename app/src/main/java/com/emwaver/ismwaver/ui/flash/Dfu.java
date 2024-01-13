/*
 * Copyright 2015 Umbrela Smart, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.emwaver.ismwaver.ui.flash;

import android.content.res.AssetManager;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import java.io.InputStream;

@SuppressWarnings("unused")
public class Dfu {
    private final static int USB_VENDOR_ID = 1155;   // VID while in DFU mode 0x0483
    private final static int USB_PRODUCT_ID = 57105; // PID while in DFU mode 0xDF11

    private static final String TAG = "Dfu";
    private final static int USB_DIR_OUT = 0;
    private final static int USB_DIR_IN = 128;       //0x80
    private final static int DFU_RequestType = 0x21;  // '2' => Class request ; '1' => to interface

    private final static int STATE_IDLE = 0x00;
    private final static int STATE_DETACH = 0x01;
    private final static int STATE_DFU_IDLE = 0x02;
    private final static int STATE_DFU_DOWNLOAD_SYNC = 0x03;
    private final static int STATE_DFU_DOWNLOAD_BUSY = 0x04;
    private final static int STATE_DFU_DOWNLOAD_IDLE = 0x05;
    private final static int STATE_DFU_MANIFEST_SYNC = 0x06;
    private final static int STATE_DFU_MANIFEST = 0x07;
    private final static int STATE_DFU_MANIFEST_WAIT_RESET = 0x08;
    private final static int STATE_DFU_UPLOAD_IDLE = 0x09;
    private final static int STATE_DFU_ERROR = 0x0A;
    private final static int STATE_DFU_UPLOAD_SYNC = 0x91;
    private final static int STATE_DFU_UPLOAD_BUSY = 0x92;

    // DFU Commands, request ID code when using controlTransfers
    private final static int DFU_DETACH = 0x00;
    private final static int DFU_DNLOAD = 0x01;
    private final static int DFU_UPLOAD = 0x02;
    private final static int DFU_GETSTATUS = 0x03;
    private final static int DFU_CLRSTATUS = 0x04;
    private final static int DFU_GETSTATE = 0x05;
    private final static int DFU_ABORT = 0x06;

    public final static int ELEMENT1_OFFSET = 293;  // constant offset in file array where image data starts
    public final static int TARGET_NAME_START = 22;
    public final static int TARGET_NAME_MAX_END = 276;
    public final static int TARGET_SIZE = 277;
    public final static int TARGET_NUM_ELEMENTS = 281;


    // Device specific parameters
    public static final String mInternalFlashString = "@Internal Flash  /0x08000000/04*016Kg,01*064Kg,07*128Kg"; // STM32F405RG, 1MB Flash, 192KB SRAM
    public static final int mInternalFlashSize = 1048575;
    public static final int mInternalFlashStartAddress = 0x08000000;
    public static final int mOptionByteStartAddress = 0x1FFFC000;
    private static final int OPT_BOR_1 = 0x08;
    private static final int OPT_BOR_2 = 0x04;
    private static final int OPT_BOR_3 = 0x00;
    private static final int OPT_BOR_OFF = 0x0C;
    private static final int OPT_WDG_SW = 0x20;
    private static final int OPT_nRST_STOP = 0x40;
    private static final int OPT_nRST_STDBY = 0x80;
    private static final int OPT_RDP_OFF = 0xAA00;
    private static final int OPT_RDP_1 = 0x3300;

    private final static String[] DEVICE_STATE = {
            "OK", "errTARGET", "errFILE",
            "errWRITE", "errERASE", "errCHECK_ERASED", "errPROG", "errVERIFY",
            "errADDRESS", "errNOTDONE", "errFIRMWARE", "errVENDOR", "errUSBR",
            "errPOR", "errUNKNOWN", "errSTALLEDPKT"
    };
    private final static String[] DEVICE_STATUS = {
            "appIDLE", "appDETACH", "dfuIDLE",
            "dfuDNLOAD -SYNC", "dfuDNBUSY", "dfuDNLOAD -IDLE", "dfuMANIFEST-SYNC", "dfuMANIFEST",
            "dfuMANIFEST-WAIT-RESET", "dfuUPLOAD -IDLE", "dfuERROR"
    };

    private final static int DFU_REQUEST_TYPE_IN =  0b10100001; // Adjust according to your needs
    private final static int STATE_OK = 0;
    private final static int DFU_REQUEST_TYPE_OUT = 0b00100001; // OUT Endpoint, Class Request, Interface Recipient

    private final static int BLOCK_SIZE = 2048; // wTransferSize

    private final int deviceVid;
    private final int devicePid;


    private UsbDeviceConnection finalConnection = null;
    private int deviceVersion;  //STM bootloader version

    private DfuListener listener;

    public interface DfuListener {
        void onStatusMsg(String msg);
    }

    private FlashFragment context;

    public Dfu(int usbVendorId, int usbProductId, FlashFragment context) {
        this.deviceVid = usbVendorId;
        this.devicePid = usbProductId;
        this.context = context;
    }

    public void setListener(final DfuListener listener) {
        this.listener = listener;
    }

    private void onStatusMsg(final String msg) {
        if (listener != null) {
            listener.onStatusMsg(msg);
        }
    }

    public void setUsbDeviceConnection(UsbDeviceConnection connection) {
        this.finalConnection = connection;
    }

    private void readUnprotect() throws Exception {
        byte[] buffer = new byte[1];
        buffer[0] = (byte) 0x92;
        //download(buffer);
    }

    private int getStatus(byte[] buffer) throws Exception {
        int length = finalConnection.controlTransfer(DFU_REQUEST_TYPE_IN, DFU_GETSTATUS, 0, 0, buffer, 6, 500);
        if (length < 0) {
            throw new Exception("USB Failed during getStatus");
        } else {
            byte state = buffer[1]; // Ensure unsigned byte
            byte status = buffer[4]; // Ensure unsigned byte
            if (state < DEVICE_STATE.length) {
                Log.i("Dfu", "state " + state + ": " + DEVICE_STATE[state]);
            } else {
                Log.i("Dfu", "state " + state + ": OUT OF RANGE");
            }
            Log.i("Dfu", "status " + status + ": " + DEVICE_STATUS[status]);
        }

        return length;
    }

    public int clearStatus() throws Exception  {
        int length = finalConnection.controlTransfer(DFU_REQUEST_TYPE_OUT, DFU_CLRSTATUS, 0, 0, null, 0, 5000);
        if (length < 0) {
            throw new Exception("error: clear_status() control transfer failed");
        }

        return length;
    }

    public void waitDownloadIdle() throws Exception {
        byte[] status = new byte[6];
        long startTime = System.currentTimeMillis();
        long timeout = 500;

        getStatus(status);
        while (!(status[4] == STATE_DFU_IDLE || status[4] == STATE_DFU_DOWNLOAD_IDLE)) {
            // Check if timeout has been reached
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new Exception("error: Timeout exceeded while waiting for download idle state");
            }
            clearStatus();
            getStatus(status);
        }
    }

    public void waitUploadIdle() throws Exception {
        byte[] status = new byte[6];
        long startTime = System.currentTimeMillis();
        long timeout = 500;

        getStatus(status);
        while (!(status[4] == STATE_DFU_IDLE || status[4] == STATE_DFU_UPLOAD_IDLE)) {
            // Check if timeout has been reached
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new Exception("error: Timeout exceeded while waiting for download idle state");
            }
            clearStatus();
            getStatus(status);
        }
    }

    public int massErase() throws Exception  {
        byte[] massEraseCommand = {0x41};
        byte[] status = new byte[6];
        int bwPollTimeout;

        // Assuming wait_idle() is implemented and called here
        waitDownloadIdle();

        int length = finalConnection.controlTransfer(DFU_REQUEST_TYPE_OUT, DFU_DNLOAD, 0, 0, massEraseCommand, 1, 50);
        if (length < 0) {
            throw new Exception("error: mass_erase() control transfer failed");
        }

        // Verify execution and success
        getStatus(status); // Assuming getStatus() updates and logs the status
        if ((status[4] == STATE_DFU_DOWNLOAD_BUSY || status[4] == STATE_DFU_DOWNLOAD_IDLE)) {
            Log.i("Dfu", "mass erasing...");
            onStatusMsg("mass erasing...\n");
        } else {
            Log.i("Dfu", "error while mass erasing (not dfuDNBUSY)");
            throw new Exception("error while mass erasing (not dfuDNBUSY)");
        }

        bwPollTimeout = (status[3] & 0xFF) << 16;
        bwPollTimeout |= (status[2] & 0xFF) << 8;
        bwPollTimeout |= (status[1] & 0xFF);
        Thread.sleep(bwPollTimeout); //Minimum time, in milliseconds, that the host should wait before sending a subsequent DFU_GETSTATUS request

        getStatus(status); // Get status again
        if ((status[4] == STATE_DFU_IDLE || status[4] == STATE_DFU_DOWNLOAD_IDLE)) {
            Log.i("Dfu", "mass erase complete.");
            onStatusMsg("mass erase complete.\n");
        } else {
            Log.i("Dfu", "mass erase failed");
            throw new Exception("error while mass erasing (not dfuDNBUSY)");
        }

        return length;
    }

    public int readBlock(byte[] buffer, int block, int num_bytes) {
        int length = finalConnection.controlTransfer(DFU_REQUEST_TYPE_IN, DFU_UPLOAD, block, 0, buffer, num_bytes, 500);
        if (length < 0) {
            Log.i("Dfu", "error: read_block() control transfer failed");
        }

        return length;
    }

    private void printBlock(byte[] buffer, int startAddress, int blockSize) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < blockSize; i += 16) {
            sb.append(String.format("0x%08X: ", startAddress + i));

            int remaining = Math.min(blockSize - i, 16);
            for (int j = 0; j < remaining; j++) {
                sb.append(String.format("%02x", buffer[i + j] & 0xFF));
                if ((j + 1) % 4 == 0 && j + 1 < remaining) {
                    sb.append("  ");
                }
            }
            sb.append("\n");
        }
        onStatusMsg(sb.toString());
    }

    public void readFlash(int flashSize) throws Exception{
        byte[] data = new byte[BLOCK_SIZE];
        int blockNum;
        int startAddress = 0x08000000;

        waitUploadIdle();

        for (blockNum = 0; blockNum < (flashSize / BLOCK_SIZE); blockNum++) {
            readBlock(data, 2 + blockNum, BLOCK_SIZE);
            printBlock(data, startAddress + (blockNum * BLOCK_SIZE), BLOCK_SIZE);
            onStatusMsg("\n");
        }

        // Read remaining data
        int remainingSize = flashSize % BLOCK_SIZE;
        if (remainingSize > 0) {
            readBlock(data, 2 + blockNum, remainingSize);
            printBlock(data, startAddress + (blockNum * BLOCK_SIZE), remainingSize);
        }
    }

    public int writeBlock(byte[] buffer, int block, int numBytes) throws Exception {
        int bwPollTimeout;

        waitDownloadIdle(); // Make sure we are in dfuIDLE or dfuDNLOAD-IDLE state

        // Write block control transfer
        int length = finalConnection.controlTransfer(DFU_REQUEST_TYPE_OUT, DFU_DNLOAD, block, 0, buffer, numBytes, 500);
        if (length < 0) {
            throw new Exception("error: write_block() control transfer failed");
        }

        // Verify execution and success
        byte[] status = new byte[6];
        getStatus(status);
        if (status[4] == STATE_DFU_DOWNLOAD_BUSY || status[4] == STATE_DFU_DOWNLOAD_IDLE) {
            onStatusMsg("writing block...");
        } else {
            throw new Exception("error while writing (not dfuDNBUSY)");
        }

        bwPollTimeout = (status[3] & 0xFF) << 16;
        bwPollTimeout |= (status[2] & 0xFF) << 8;
        bwPollTimeout |= (status[1] & 0xFF);
        Thread.sleep(bwPollTimeout); //Minimum time, in milliseconds, that the host should wait before sending a subsequent DFU_GETSTATUS request

        getStatus(status);
        if (status[4] == STATE_DFU_IDLE || status[4] == STATE_DFU_DOWNLOAD_IDLE) {
            onStatusMsg("block write complete.\n");
        } else {
            throw new Exception("block write failed");
        }

        return length;
    }

    public int setAddressPointer(int address) throws Exception {
        byte[] buffer = new byte[5];
        int bwPollTimeout;

        buffer[0] = 0x21; // Set address pointer command
        buffer[1] = (byte) (address & 0xFF);
        buffer[2] = (byte) ((address >> 8) & 0xFF);
        buffer[3] = (byte) ((address >> 16) & 0xFF);
        buffer[4] = (byte) ((address >> 24) & 0xFF);

        waitDownloadIdle(); // Make sure we are in dfuIDLE or dfuDNLOAD-IDLE state

        // Set address pointer control transfer
        int length = finalConnection.controlTransfer(DFU_REQUEST_TYPE_OUT, DFU_DNLOAD, 0, 0, buffer, buffer.length, 50);
        if (length < 0) {
            throw new Exception("error: set_address_pointer() control transfer failed");
        }

        // Verify execution and success
        byte[] status = new byte[6];
        getStatus(status);
        if (status[4] == STATE_DFU_DOWNLOAD_BUSY || status[4] == STATE_DFU_DOWNLOAD_IDLE) {
            onStatusMsg("setting address pointer...");
        } else {
            throw new Exception("error while setting pointer (not dfuDNBUSY)");
        }

        bwPollTimeout = (status[3] & 0xFF) << 16;
        bwPollTimeout |= (status[2] & 0xFF) << 8;
        bwPollTimeout |= (status[1] & 0xFF);
        Thread.sleep(bwPollTimeout); //Minimum time, in milliseconds, that the host should wait before sending a subsequent DFU_GETSTATUS request

        getStatus(status);
        if (status[4] == STATE_DFU_IDLE || status[4] == STATE_DFU_DOWNLOAD_IDLE) {
            onStatusMsg("setting address pointer complete.\n");
        } else {
            throw new Exception("setting pointer failed");
        }

        return length;
    }

    public void writeFlash() throws Exception {
        AssetManager assetManager = context.getContext().getAssets();
        InputStream inputStream = assetManager.open("dfu.dfu");

        byte[] writeBuffer = new byte[BLOCK_SIZE];
        byte[] readBuffer = new byte[BLOCK_SIZE];
        int blockNum = 2;
        int readBytes;

        while ((readBytes = inputStream.read(writeBuffer, 0, BLOCK_SIZE)) > 0) {
            writeBlock(writeBuffer, blockNum, readBytes);

            // Verify block write
            waitUploadIdle();
            readBlock(readBuffer, blockNum, readBytes);

            // You can log the read buffer using a method similar to print_block if needed
            if (equalArrays(writeBuffer, readBuffer, readBytes)) {
                onStatusMsg("Block " + blockNum + " verified successfully.\n");
            } else {
                throw new Exception("Error verifying block " + (blockNum-2) + ".");
            }

            blockNum++;
        }

        inputStream.close();
    }

    private boolean equalArrays(byte[] a, byte[] b, int length) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length < length || b.length < length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    // stores the result of a GetStatus DFU request
    public class DfuStatus {
        byte bStatus;       // state during request
        int bwPollTimeout;  // minimum time in ms before next getStatus call should be made
        byte bState;        // state after request
    }
}
