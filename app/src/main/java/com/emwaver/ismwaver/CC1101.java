package com.emwaver.ismwaver;

import android.util.Log;

import java.util.Arrays;

public class CC1101 {


    private final USBService USBService;

    // CC1101 Configuration Registers
    public static final byte CC1101_IOCFG2 = 0x00;       // GDO2 output pin configuration
    public static final byte CC1101_IOCFG1 = 0x01;       // GDO1 output pin configuration
    public static final byte CC1101_IOCFG0 = 0x02;       // GDO0 output pin configuration
    public static final byte CC1101_FIFOTHR = 0x03;      // RX FIFO and TX FIFO thresholds
    public static final byte CC1101_SYNC1 = 0x04;        // Sync word, high INT8U
    public static final byte CC1101_SYNC0 = 0x05;        // Sync word, low INT8U
    public static final byte CC1101_PKTLEN = 0x06;       // Packet length
    public static final byte CC1101_PKTCTRL1 = 0x07;     // Packet automation control
    public static final byte CC1101_PKTCTRL0 = 0x08;     // Packet automation control
    public static final byte CC1101_ADDR = 0x09;         // Device address
    public static final byte CC1101_CHANNR = 0x0A;       // Channel number
    public static final byte CC1101_FSCTRL1 = 0x0B;      // Frequency synthesizer control
    public static final byte CC1101_FSCTRL0 = 0x0C;      // Frequency synthesizer control
    public static final byte CC1101_FREQ2 = 0x0D;        // Frequency control word, high INT8U
    public static final byte CC1101_FREQ1 = 0x0E;        // Frequency control word, middle INT8U
    public static final byte CC1101_FREQ0 = 0x0F;        // Frequency control word, low INT8U
    public static final byte CC1101_MDMCFG4 = 0x10;      // Modem configuration
    public static final byte CC1101_MDMCFG3 = 0x11;      // Modem configuration
    public static final byte CC1101_MDMCFG2 = 0x12;      // Modem configuration
    public static final byte CC1101_MDMCFG1 = 0x13;      // Modem configuration
    public static final byte CC1101_MDMCFG0 = 0x14;      // Modem configuration
    public static final byte CC1101_DEVIATN = 0x15;      // Modem deviation setting
    public static final byte CC1101_MCSM2 = 0x16;        // Main Radio Control State Machine configuration
    public static final byte CC1101_MCSM1 = 0x17;        // Main Radio Control State Machine configuration
    public static final byte CC1101_MCSM0 = 0x18;        // Main Radio Control State Machine configuration
    public static final byte CC1101_FOCCFG = 0x19;       // Frequency Offset Compensation configuration
    public static final byte CC1101_BSCFG = 0x1A;        // Bit Synchronization configuration
    public static final byte CC1101_AGCCTRL2 = 0x1B;     // AGC control
    public static final byte CC1101_AGCCTRL1 = 0x1C;     // AGC control
    public static final byte CC1101_AGCCTRL0 = 0x1D;     // AGC control
    public static final byte CC1101_WOREVT1 = 0x1E;      // High INT8U Event 0 timeout
    public static final byte CC1101_WOREVT0 = 0x1F;      // Low INT8U Event 0 timeout
    public static final byte CC1101_WORCTRL = 0x20;      // Wake On Radio control
    public static final byte CC1101_FREND1 = 0x21;       // Front end RX configuration
    public static final byte CC1101_FREND0 = 0x22;       // Front end TX configuration
    public static final byte CC1101_FSCAL3 = 0x23;       // Frequency synthesizer calibration
    public static final byte CC1101_FSCAL2 = 0x24;       // Frequency synthesizer calibration
    public static final byte CC1101_FSCAL1 = 0x25;       // Frequency synthesizer calibration
    public static final byte CC1101_FSCAL0 = 0x26;       // Frequency synthesizer calibration
    public static final byte CC1101_RCCTRL1 = 0x27;      // RC oscillator configuration
    public static final byte CC1101_RCCTRL0 = 0x28;      // RC oscillator configuration
    public static final byte CC1101_FSTEST = 0x29;       // Frequency synthesizer calibration control
    public static final byte CC1101_PTEST = 0x2A;        // Production test
    public static final byte CC1101_AGCTEST = 0x2B;      // AGC test
    public static final byte CC1101_TEST2 = 0x2C;        // Various test settings
    public static final byte CC1101_TEST1 = 0x2D;        // Various test settings
    public static final byte CC1101_TEST0 = 0x2E;        // Various test settings

    // CC1101 Strobe commands
    public static final byte CC1101_SRES = 0x30;         // Reset chip.
    public static final byte CC1101_SFSTXON = 0x31;      // Enable and calibrate frequency synthesizer (if MCSM0.FS_AUTOCAL=1).
    // If in RX/TX: Go to a wait state where only the synthesizer is
    // running (for quick RX / TX turnaround).
    public static final byte CC1101_SXOFF = 0x32;        // Turn off crystal oscillator.
    public static final byte CC1101_SCAL = 0x33;         // Calibrate frequency synthesizer and turn it off
    // (enables quick start).
    public static final byte CC1101_SRX = 0x34;          // Enable RX. Perform calibration first if coming from IDLE and
    // MCSM0.FS_AUTOCAL=1.
    public static final byte CC1101_STX = 0x35;          // In IDLE state: Enable TX. Perform calibration first if
    // MCSM0.FS_AUTOCAL=1. If in RX state and CCA is enabled:
    // Only go to TX if channel is clear.
    public static final byte CC1101_SIDLE = 0x36;        // Exit RX / TX, turn off frequency synthesizer and exit
    // Wake-On-Radio mode if applicable.
    public static final byte CC1101_SAFC = 0x37;         // Perform AFC adjustment of the frequency synthesizer
    public static final byte CC1101_SWOR = 0x38;         // Start automatic RX polling sequence (Wake-on-Radio)
    public static final byte CC1101_SPWD = 0x39;         // Enter power down mode when CSn goes high.
    public static final byte CC1101_SFRX = 0x3A;         // Flush the RX FIFO buffer.
    public static final byte CC1101_SFTX = 0x3B;         // Flush the TX FIFO buffer.
    public static final byte CC1101_SWORRST = 0x3C;      // Reset real time clock.
    public static final byte CC1101_SNOP = 0x3D;         // No operation. May be used to pad strobe commands to two
    // INT8Us for simpler software.

    // CC1101 Status Registers
    public static final byte CC1101_PARTNUM = 0x30;      // Part number
    public static final byte CC1101_VERSION = 0x31;      // Version number
    public static final byte CC1101_FREQEST = 0x32;      // Frequency estimate
    public static final byte CC1101_LQI = 0x33;          // Link quality indicator
    public static final byte CC1101_RSSI = 0x34;         // Received signal strength indicator
    public static final byte CC1101_MARCSTATE = 0x35;    // Main Radio Control State Machine state
    public static final byte CC1101_WORTIME1 = 0x36;     // High byte of WOR timer
    public static final byte CC1101_WORTIME0 = 0x37;     // Low byte of WOR timer
    public static final byte CC1101_PKTSTATUS = 0x38;    // Current GDOx status and packet status
    public static final byte CC1101_VCO_VC_DAC = 0x39;   // Current setting from PLL calibration module
    public static final byte CC1101_TXBYTES = 0x3A;      // Underflow and number of bytes in the TX FIFO
    public static final byte CC1101_RXBYTES = 0x3B;

    //CC1101 PATABLE,TXFIFO,RXFIFO
    public static final byte CC1101_PATABLE = 0x3E;
    public static final byte CC1101_TXFIFO = 0x3F;
    public static final byte CC1101_RXFIFO = 0x3F;

    //MODULATIONS
    public static final byte MOD_2FSK = 0;
    public static final byte MOD_GFSK = 1;
    public static final byte MOD_ASK = 3;
    public static final byte MOD_4FSK = 4;
    public static final byte MOD_MSK = 7;

    public static final byte WRITE_BURST = (byte)0x40;
    public static final byte READ_SINGLE = (byte)0x80;
    public static final byte READ_BURST = (byte)0xC0;
    public static final byte BYTES_IN_RXFIFO = 0x7F;            //byte number in RXfifo mask

    private static final int GDO_INPUT = 0;

    private static final int GDO_OUTPUT = 1;

    private static final int GDO_0 = 0;

    private static final int GDO_2 = 1;


    public CC1101(USBService USBService) {
        this.USBService = USBService;
    }
    public void spiStrobe(byte commandStrobe) {
        byte[] command = new byte[2];
        byte[] response;
        command[0] = '%'; // command strobe character
        command[1] = commandStrobe;
        //response = USBService.sendCommandAndGetResponse(command, 1, 1, 1000);
        response = USBService.sendCommand(command, 1000);
        //Log.i("spiStrobe", Arrays.toString(response));  //response is the status byte
    }
    public void writeBurstReg(byte addr, byte[] data, byte len){
        byte [] command = new byte[data.length+3];
        byte [] response = new byte[1];
        command[0] = '>'; //write burst reg character
        command[1] = addr; //burst write >[addr][len][data]
        command[2] = len;
        System.arraycopy(data, 0, command, 3, data.length); // Efficient array copy
        //response = USBService.sendCommandAndGetResponse(command, 1, 1, 1000);
        response = USBService.sendCommand(command, 1000);
        //Log.i("writeBurstReg", toHexStringWithHexPrefix(response)); //response is the status byte
    }
    public byte [] readBurstReg(byte addr, int len){
        byte [] command = new byte[3];
        byte [] response = new byte[len];
        command[0] = '<'; //read burst reg character
        command[1] = addr; ////burst read <[addr][len]
        command[2] = (byte)len;
        //response = USBService.sendCommandAndGetResponse(command, (byte)len, 1, 1000);
        response = USBService.sendCommand(command, 1000);
        Log.i("readBurstReg", toHexStringWithHexPrefix(response));
        return response;
    }
    public byte readReg(byte addr){
        byte [] command = new byte[2];
        byte [] response = new byte[1];
        command[0] = '?'; //read reg character
        command[1] = addr; //single read ?[addr]
        response = USBService.sendCommand(command, 1000);
        Log.i("readReg", toHexStringWithHexPrefix(response));
        return response[0];
    }
    public void writeReg(byte addr, byte data){
        byte [] command = new byte[3];
        byte [] response = new byte[1];
        command[0] = '!'; //write reg character
        command[1] = addr; //single write ![addr][data]
        command[2] = data;
        response = USBService.sendCommand(command, 1000);
        Log.i("writeReg", Arrays.toString(response));  //response is the reading at that register
    }
    public void sendData(byte [] txBuffer, int size, int t) {
        writeBurstReg(CC1101_TXFIFO, txBuffer, (byte) size);     //write data to send
        spiStrobe(CC1101_SIDLE);
        spiStrobe(CC1101_STX);                          //start send
        try {
            Thread.sleep(t);                                //wait for transmission to be done
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        spiStrobe(CC1101_SFTX);                         //flush TXfifo
    }
    public byte [] receiveData() {
        byte size_reading;
        byte [] rxBuffer;
        size_reading = readReg((byte)(CC1101_RXBYTES | READ_BURST));

        if((size_reading & BYTES_IN_RXFIFO) > 0) {
            rxBuffer = readBurstReg(CC1101_RXFIFO, size_reading);
            spiStrobe(CC1101_SFRX);
            spiStrobe(CC1101_SRX);
            return rxBuffer;
        }
        else {
            spiStrobe(CC1101_SFRX);
            spiStrobe(CC1101_SRX);
            return null;
        }
    }
    public void sendInit(){
        byte[] command = {'t', 'x', 'i', 'n', 'i', 't'}; // Replace with your actual command
        byte[] response = USBService.sendCommand(command, 1000);
        if (response != null) {
            Log.i("Command Response", Arrays.toString(response));
        }
    }
    public void sendInitRx(){
        byte[] command = {'r', 'x', 'i', 'n', 'i', 't'}; // Replace with your actual command
        byte[] response = USBService.sendCommand(command, 1000);
        if (response != null) {
            Log.i("Command Response", Arrays.toString(response));
        }
    }
    public void sendInitRxContinuous(){
        byte[] command = {'r', 'x', 'c', 'o', 'n', 't'}; // Replace with your actual command
        byte[] response = USBService.sendCommand(command, 1000);
        if (response != null) {
            Log.i("Command Response", Arrays.toString(response));
        }
    }
    public boolean setDataRate(int bitRate) {
        // Constants for the DRATE register calculation
        final double F_OSC = 26_000_000; // Oscillator frequency in Hz
        final int DRATE_M_MAX = 255; // 8-bit DRATE_M has max value 255
        final int DRATE_E_MAX = 15;  // 4-bit DRATE_E has max value 15
        double target = bitRate * Math.pow(2, 28) / F_OSC;
        double minDifference = Double.MAX_VALUE;
        int bestM = 0;
        int bestE = 0;

        // Find the closest DRATE_M and DRATE_E for the desired bit rate
        for (int e = 0; e <= DRATE_E_MAX; e++) {
            for (int m = 0; m <= DRATE_M_MAX; m++) {
                double currentValue = (256 + m) * Math.pow(2, e);
                double difference = Math.abs(currentValue - target);
                if (difference < minDifference) {
                    minDifference = difference;
                    bestM = m;
                    bestE = e;
                }
            }
        }

        byte [] values= {(byte)bestE, (byte)bestM};
        // Log the values found
        Log.i("DataRate", toHexStringWithHexPrefix(values));

        // Read the current value of the MDMCFG4 register to keep the first word
        byte readValue = readReg(CC1101_MDMCFG4);
        int bandwidthPart = readValue & 0xF0; // Ensure it is treated as unsigned

        // Combine the read first word with the calculated DRATE_M
        int combinedE = bandwidthPart | (bestE & 0x0F); // Assumes the first word is the high byte

        // Write the combined value and DRATE_E to the modem configuration registers
        byte[] mdmcfg = {(byte) combinedE, (byte) bestM};
        writeBurstReg((byte) CC1101_MDMCFG4, mdmcfg, (byte) 2);


        //confirm reading
        byte [] confirmValue = readBurstReg((byte)CC1101_MDMCFG4, 2);
        //Log.i("ModemConfig", "CC1101_MDMCFG4: " + (int)readValue[0] + ", CC1101_MDMCFG3: " + (int)readValue[1]);
        if(Arrays.equals(confirmValue, mdmcfg)){
            return true;
        }
        else{
            return false;
        }
    }
    public boolean setModulation(byte modulation) {
        // Read the current register value
        byte currentValue = readReg(CC1101_MDMCFG2);

        Log.i("MDMCFG2", "current value: " + currentValue);

        byte mask = 0b01110000; // Mask for the modulation bits (bit 4, 5, 6)
        currentValue &= ~mask; // Clear the modulation bits

        // Set the new modulation bits
        // Assuming that the 'modulation' argument is already just the 3 bits needed
        // If not, it would need to be shifted into place with something like (modulation << 4)
        currentValue |= (modulation << 4); // Combine the new modulation bits with the current value

        Log.i("MDMCFG2", "modified value: " + currentValue);
        // Write the new value back to the register
        writeReg(CC1101_MDMCFG2, currentValue);

        // Assuming writeReg method exists and returns a boolean indicating success
        return readReg(CC1101_MDMCFG2) == currentValue;
    }
    public String toHexStringWithHexPrefix(byte[] array) {
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
    public boolean setManchesterEncoding(boolean manchester){
        byte mdmcfg2 = readReg(CC1101_MDMCFG2);
        //bit 3 is the manchester encoding bit
        if(manchester){
            mdmcfg2 |= 0b00001000;
        }
        else{
            mdmcfg2 &= 0b11110111;
        }
        writeReg(CC1101_MDMCFG2, mdmcfg2);
        //verify
        return readReg(CC1101_MDMCFG2) == mdmcfg2;
    }
    public boolean setSyncMode(byte syncmode){
        // Read the current register value
        byte currentValue = readReg(CC1101_MDMCFG2);

        Log.i("MDMCFG2", "current value: " + currentValue);

        byte mask = 0b00000111; // Mask for the sync mode bits (bit 0, 1, 2)
        currentValue &= ~mask; // Clear the sync bits

        // Set the new sync bits
        // Assuming that the 'sync' argument is already just the 3 bits needed

        currentValue |= (syncmode); // Combine the new modulation bits with the current value

        Log.i("MDMCFG2", "modified value: " + currentValue);
        // Write the new value back to the register
        writeReg(CC1101_MDMCFG2, currentValue);

        // Assuming writeReg method exists and returns a boolean indicating success
        return readReg(CC1101_MDMCFG2) == currentValue;
    }
    public boolean setDeviation(int deviation) {
        // Constants for the DEVIATN register calculation
        final double F_OSC = 26_000_000; // Oscillator frequency in Hz
        final int DEVIATION_M_MAX = 7; // 3-bit DEVIATION_M has max value 7
        final int DEVIATION_E_MAX = 7; // 3-bit DEVIATION_E has max value 7

        // The target deviation formula as per the datasheet
        double target = deviation * Math.pow(2, 17) / F_OSC;
        double minDifference = Double.MAX_VALUE;
        int bestM = 0;
        int bestE = 0;

        // Find the closest DEVIATION_M and DEVIATION_E for the desired deviation
        for (int e = 0; e <= DEVIATION_E_MAX; e++) {
            for (int m = 0; m <= DEVIATION_M_MAX; m++) {
                double currentValue = (8 + m) * Math.pow(2, e);
                double difference = Math.abs(currentValue - target);
                if (difference < minDifference) {
                    minDifference = difference;
                    bestM = m;
                    bestE = e;
                }
            }
        }

        byte [] values = {(byte)bestE, (byte)bestM};
        // Log the values found
        Log.i("Deviation", toHexStringWithHexPrefix(values));

        // Combine the read TX and RX parts with the calculated DEVIATION_E and DEVIATION_M
        int combinedValue = ((bestE << 4) & 0x70) | (bestM & 0x07);

        // Write the combined value to the DEVIATN register
        writeReg((byte) CC1101_DEVIATN, (byte) combinedValue);

        // Confirm reading
        byte confirmValue = readReg((byte)CC1101_DEVIATN);
        if (confirmValue == (byte)combinedValue) {
            return true;
        } else {
            return false;
        }
    }
    public boolean setNumPreambleBytes(int num){
        byte mdmcfg1 = (byte)(num << 4);

        writeReg(CC1101_MDMCFG1, mdmcfg1);
        //verify
        return readReg(CC1101_MDMCFG1) == mdmcfg1;
    }
    public boolean setSyncWord(byte[] syncword) {
        if (syncword == null || syncword.length != 2) {
            // Invalid input: Sync word must be exactly 2 bytes long
            return false;
        }
        // Write the sync word to the CC1101_SYNC1 address
        writeBurstReg(CC1101_SYNC1, syncword, (byte) 2);

        // Read back the sync word from the same address
        byte[] readBack = readBurstReg(CC1101_SYNC1, 2);

        // Compare the written sync word with the read back value
        return Arrays.equals(syncword, readBack);
    }
    public boolean setPktLength(int length){
        byte pktlen = (byte)length;
        writeReg(CC1101_PKTLEN, pktlen);
        //verify
        return readReg(CC1101_PKTLEN) == pktlen;
    }
    public boolean getGDO() {
        byte[] command = {'g', 'd', 'o', '0'}; // Replace with your actual command
        byte[] response = USBService.sendCommand(command, 1000);

        if (response != null) {
            Log.i("Command Response", Arrays.toString(response));
            String responseString = new String(response).trim(); // Convert byte[] to String and trim any trailing whitespaces

            // Compare the response string
            return responseString.equals("HIGH");
        }

        return false;
    }

    public boolean getGDO0() {
        byte response = readReg((byte) (CC1101_PKTSTATUS | READ_BURST));
        return (response & 1) == 1;
    }

    public boolean getGDO2() {
        byte response = readReg((byte) (CC1101_PKTSTATUS | READ_BURST));
        return (response & 0x04) >> 2 == 1;
    }


    public void initRx() {
        byte[] PA_TABLE_OOK = {0x00, (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        setFrequency((byte) 0x10, (byte) 0xB0, (byte) 0x71); //433.919830
        setGDO((byte) 0x2E, (byte) 0x2E, (byte) 0x00); // high impedance, high impedance, GDO0 as fifo threshold assert.
        //writeReg(CC1101_FIFOTHR,  0x06);//smart rf ADC, fifo threshold 4 bytes in fifo (signal assert)

        writeReg(CC1101_MDMCFG2, (byte) 0x32); //MOD_FORMAT: ASK/OOK, SYNC_MODE: 16/16 bits, MANCHESTER: disable
        writeReg(CC1101_FREND0, (byte) 0x11); //selects index 1 for high state
        writeBurstReg(CC1101_PATABLE, PA_TABLE_OOK, (byte) 8); //0x60 for high state

        //given by smartrf studio:
        writeReg(CC1101_FSCAL3, (byte) 0xE9); //default values except bit 5 which is 1. should not matter as it is a enable that is already bigger than 1 without this bit
        writeReg(CC1101_FSCAL2, (byte) 0x2A); //frequency synth enable bit is ON (different from default) result and override value
        writeReg(CC1101_FSCAL1, (byte) 0x00); //calibration result
        writeReg(CC1101_FSCAL0, (byte) 0x1F); //different from default. this value is given by smartrf, the default is 0x0D (calibration control)
        //writeReg(CC1101_WORCTRL,   0xFB); //wake on radio maximum timeout 17 hours
        writeReg(CC1101_TEST2, (byte) 0x88); //deafult value (test register smartrf)
        writeReg(CC1101_TEST1, (byte) 0x31); //will change from default 0x31 to 0x35 when going from SLEEP
        writeReg(CC1101_TEST0, (byte) 0x09); //smartrf, different form default. contains bit to enable VCO selection calibration, which is by defualt 1, but here is set to 0.
        writeReg(CC1101_MCSM0, (byte) 0x18); //main control state machine. default value for oscilattor setting, FS_AUTOCAL=1 (calibrate when IDLE->RX or TX)
        writeReg(CC1101_FOCCFG, (byte) 0x16); //frequency offset regarding sync words. not available in ASK. the settings are not default, GATE is off, loop gain is 1 instead of 2
        //writeReg(CC1101_DEVIATN,  0x15); //value for deviation. this setting has not effect in ASK.
        writeReg(CC1101_FSCTRL1, (byte) 0x06); //IF frequency


        //number of preamble bytes
        writeReg(CC1101_MDMCFG1, (byte) 0x12); //3 preamble bytes for Tesla signal (aaaaaa), no FEC, default channel spacing exponent
        writeReg(CC1101_MDMCFG0, (byte) 0xF8); //default channel spacing value

        //bandwidth and symbol rate (bit rate)
        writeReg(CC1101_MDMCFG4, (byte) 0x56); //325 kHz 2.5kbit
        writeReg(CC1101_MDMCFG3, (byte) 0x93); //325 kHz 2.5kbitCC1101_FSCTRL1

        //packet format
        writeReg(CC1101_PKTCTRL0, (byte) 0x00); //use FIFO for Rx/Tx, CRC disabled, fixed packet length.
        writeReg(CC1101_PKTCTRL1, (byte) 0x00); //fifo flush when CRC fails, append to payload RSSI disabled, no address check
        writeReg(CC1101_PKTLEN, (byte) 39); //packet length for tesla signal: 42 bytes total, 3 are preamble, so 39 bytes

        writeReg(CC1101_SYNC1, (byte) 0x8A); //sync word tesla signal
        writeReg(CC1101_SYNC0, (byte) 0xCB); //sync word


        writeReg(CC1101_AGCCTRL2, (byte) 0x07);
        writeReg(CC1101_AGCCTRL1, (byte) 0x07);
        writeReg(CC1101_AGCCTRL0, (byte) 0x90);

        writeReg(CC1101_FIFOTHR, (byte) 0x00);

        //ConfigureGDO0Pin(GDO_INPUT);

    }

    void init433() {
        writeReg(CC1101_FSCAL3, (byte) 0xE9); //default values except bit 5 which is 1. should not matter as it is a enable that is already bigger than 1 without this bit
        writeReg(CC1101_FSCAL2, (byte) 0x2A); //frequency synth enable bit is ON (different from default) result and override value
        writeReg(CC1101_FSCAL1, (byte) 0x00); //calibration result
        writeReg(CC1101_FSCAL0, (byte) 0x1F); //different from default. this value is given by smartrf, the default is 0x0D (calibration control)

        writeReg(CC1101_TEST2, (byte) 0x81); //deafult value (test register smartrf)
        writeReg(CC1101_TEST1, (byte) 0x35); //will change from default 0x31 to 0x35 when going from SLEEP
        writeReg(CC1101_TEST0, (byte) 0x09); //smartrf, different form default. contains bit to enable VCO selection calibration, which is by defualt 1, but here is set to 0.
        writeReg(CC1101_FIFOTHR, (byte) 0x47);//smartrf is recommending this for 433.92 mhz

        setFrequency((byte) 0x10, (byte) 0xB0, (byte) 0x71); //433.919830
    }

    void initRxContinuous() {
        writeReg(CC1101_PKTCTRL0, (byte) 0x32); // async serial mode (packet engine is off)
        setGDO((byte) 0x0D, (byte) 0x2E, (byte) 0x0D);
        init433();
        configureGDO(GDO_0, GDO_INPUT);

        writeReg(CC1101_AGCCTRL2, (byte) 0x07);
        writeReg(CC1101_AGCCTRL1, (byte) 0x07);
        writeReg(CC1101_AGCCTRL0, (byte) 0x90);


        writeReg(CC1101_MDMCFG2, (byte) 0x30); //ASK/OOK modulation.
        writeReg(CC1101_FREND0, (byte) 0x11); //use entry 1 of PATABLE as logical '1' for transmission.


        writeReg(CC1101_MDMCFG0, (byte) 0xF8); //default channel spacing value

        writeReg(CC1101_MDMCFG4, (byte) 0xFB); //100Kbit
        writeReg(CC1101_MDMCFG3, (byte) 0xF8); //100Kbit


        // Perform a manual calibration just to check if it is working
        spiStrobe(CC1101_SIDLE);//going to idle turns off FS
        spiStrobe(CC1101_SCAL); //calibrate freq synthesizer and turn it off
        spiStrobe(CC1101_SFSTXON); //enable freq synthesizer
        spiStrobe(CC1101_SRX);        //start transmit

        // All future calibrations will be automatic
        writeReg(CC1101_MCSM0 , (byte) 0x18); //main control state machine. default value for oscilattor setting, FS_AUTOCAL=1 (calibrate when IDLE->RX or TX)

    }

    public void initTx(){
        byte[] PA_TABLE_OOK = {0x00, (byte) 0xC0,0x00,0x00,0x00,0x00,0x00,0x00};
        setFrequency((byte) 0x10, (byte) 0xB0, (byte) 0x71); //433.919830
        setGDO((byte) 0x2E, (byte) 0x2E, (byte) 0x00); // high impedance, high impedance, GDO0 as fifo threshold assert.
        //writeReg(CC1101_FIFOTHR,  0x06);//smart rf ADC, fifo threshold 4 bytes in fifo (signal assert)

        writeReg(CC1101_MDMCFG2, (byte) 0x32); //MOD_FORMAT: ASK/OOK, SYNC_MODE: 16/16 bits, MANCHESTER: disable
        writeReg(CC1101_FREND0, (byte) 0x11); //selects index 1 for high state
        writeBurstReg(CC1101_PATABLE, PA_TABLE_OOK, (byte) 8); //0x60 for high state

        //given by smartrf studio:
        writeReg(CC1101_FSCAL3, (byte) 0xE9); //default values except bit 5 which is 1. should not matter as it is a enable that is already bigger than 1 without this bit
        writeReg(CC1101_FSCAL2, (byte) 0x2A); //frequency synth enable bit is ON (different from default) result and override value
        writeReg(CC1101_FSCAL1, (byte) 0x00); //calibration result
        writeReg(CC1101_FSCAL0, (byte) 0x1F); //different from default. this value is given by smartrf, the default is 0x0D (calibration control)
        //writeReg(CC1101_WORCTRL,   0xFB); //wake on radio maximum timeout 17 hours
        writeReg(CC1101_TEST2, (byte) 0x88); //deafult value (test register smartrf)
        writeReg(CC1101_TEST1, (byte) 0x31); //will change from default 0x31 to 0x35 when going from SLEEP
        writeReg(CC1101_TEST0, (byte) 0x09); //smartrf, different form default. contains bit to enable VCO selection calibration, which is by defualt 1, but here is set to 0.
        writeReg(CC1101_MCSM0 , (byte) 0x18); //main control state machine. default value for oscilattor setting, FS_AUTOCAL=1 (calibrate when IDLE->RX or TX)
        writeReg(CC1101_FOCCFG, (byte) 0x16); //frequency offset regarding sync words. not available in ASK. the settings are not default, GATE is off, loop gain is 1 instead of 2
        //writeReg(CC1101_DEVIATN,  0x15); //value for deviation. this setting has not effect in ASK.
        writeReg(CC1101_FSCTRL1, (byte) 0x06); //IF frequency


        //number of preamble bytes
        writeReg(CC1101_MDMCFG1, (byte) 0x12); //3 preamble bytes for Tesla signal (aaaaaa), no FEC, default channel spacing exponent
        writeReg(CC1101_MDMCFG0, (byte) 0xF8); //default channel spacing value

        //bandwidth and symbol rate (bit rate)
        writeReg(CC1101_MDMCFG4, (byte) 0x56); //325 kHz 2.5kbit
        writeReg(CC1101_MDMCFG3, (byte) 0x93); //325 kHz 2.5kbitCC1101_FSCTRL1

        //packet format
        writeReg(CC1101_PKTCTRL0, (byte) 0x00); //use FIFO for Rx/Tx, CRC disabled, fixed packet length.
        writeReg(CC1101_PKTCTRL1, (byte) 0x00); //fifo flush when CRC fails, append to payload RSSI disabled, no address check
        writeReg(CC1101_PKTLEN, (byte) 37); //packet length for tesla signal: 42 bytes total, 3 are preamble, 2 sync cword, so 39 bytes

        writeReg(CC1101_SYNC1, (byte) 0x8A); //sync word tesla signal
        writeReg(CC1101_SYNC0, (byte) 0xCB); //sync word

        writeReg(CC1101_AGCCTRL2, (byte) 0x07);
        writeReg(CC1101_AGCCTRL1, (byte) 0x07);
        writeReg(CC1101_AGCCTRL0, (byte) 0x90);

    }

    public void initTxContinuous() {
        byte [] PA_TABLE_OOK = {0x00, (byte) 0xC0,0x00,0x00,0x00,0x00,0x00,0x00}; //10 dbm for OOK
        writeReg(CC1101_PKTCTRL0, (byte) 0x32); // async serial mode (packet engine is off)
        writeReg(CC1101_IOCFG2, (byte) 0x2B); // GDO2 synchronous serial clock. should not matter in Tx, but smartrf is recommending synchronous mode when in Tx
        writeReg(CC1101_IOCFG1, (byte) 0x2E); // high impedance
        writeReg(CC1101_IOCFG0, (byte) 0x0C); // GDO0 as synchronous serial output. should not matter in Tx, but smartrf is recommending synchronous mode when in Tx
        setGDO((byte) 0x2B, (byte) 0x2E, (byte) 0x0C);


        writeReg(CC1101_MDMCFG2, (byte) 0x30); //ASK/OOK modulation.
        writeReg(CC1101_FREND0, (byte) 0x11); //use entry 1 of PATABLE as logical '1' for transmission.
        writeBurstReg(CC1101_PATABLE, PA_TABLE_OOK, (byte) 8);

        writeReg(CC1101_MDMCFG4, (byte) 0xFB); //100Kbit
        writeReg(CC1101_MDMCFG3, (byte) 0xF8); //100Kbit

        // Perform a manual calibration just to check if it is working
        spiStrobe(CC1101_SIDLE);//going to idle turns off FS
        spiStrobe(CC1101_SCAL); //calibrate freq synthesizer and turn it off

        spiStrobe(CC1101_SFSTXON); //enable freq synthesizer
        spiStrobe(CC1101_STX);        //start transmit



        // All future calibrations will be automatic
        writeReg(CC1101_MCSM0 , (byte) 0x18); //main control state machine. default value for oscilattor setting, FS_AUTOCAL=1 (calibrate when IDLE->RX or TX)

    }
    
    public void init(){
        init433();
        initTxContinuous();
    }

    private void configureGDO(int gdo0, int gdoInput) {
        byte[] command = {'p', 'i', 'n', (byte) gdo0, (byte) gdoInput}; // Replace with your actual command
        byte[] response = USBService.sendCommand(command, 1000);
    }

    public void setFrequency(byte freq2, byte freq1, byte freq0){
        writeReg(CC1101_FREQ2, freq2);
        writeReg(CC1101_FREQ1, freq1);
        writeReg(CC1101_FREQ0, freq0);
    }

    public void setGDO(byte gdo2, byte gdo1, byte gdo0){
        writeReg(CC1101_IOCFG2, gdo2);
        writeReg(CC1101_IOCFG1, gdo1);
        writeReg(CC1101_IOCFG0, gdo0);
    }


}
