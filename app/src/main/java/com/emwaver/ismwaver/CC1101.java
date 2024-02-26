package com.emwaver.ismwaver;

import android.util.Log;

import java.util.Arrays;

public class CC1101 {


    private final USBService USBService;

    //region CC1101 REGISTERS
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

    public static final int GDO_INPUT = 0;

    public static final int GDO_OUTPUT = 1;

    public static final int GDO_0 = 0;

    public static final int GDO_2 = 1;
    //endregion
    public CC1101(USBService USBService) {
        this.USBService = USBService;
    }

    //region SPI functions
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
        Log.i("readBurstReg", Utils.toHexStringWithHexPrefix(response));
        return response;
    }
    public byte readReg(byte addr){
        byte [] command = new byte[2];
        byte [] response = new byte[1];
        command[0] = '?'; //read reg character
        command[1] = addr; //single read ?[addr]
        response = USBService.sendCommand(command, 1000);
        Log.i("readReg", Utils.toHexStringWithHexPrefix(response));
        return response[0];
    }
    public void writeReg(byte addr, byte data){
        byte [] command = new byte[3];
        byte [] response = new byte[1];
        byte [] address = {addr};
        command[0] = '!'; //write reg character
        command[1] = addr; //single write ![addr][data]
        command[2] = data;
        response = USBService.sendCommand(command, 1000);
        Log.i("writeReg", Utils.bytesToHexString(address) + ", " + Utils.bytesToHexString(response));  //response is the reading at that register
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
        Log.i("DataRate", Utils.toHexStringWithHexPrefix(values));

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
    public int getDataRate() {
        // Constants for the DRATE register calculation
        final double F_OSC = 26_000_000; // Oscillator frequency in Hz

        // Read the DRATE_E from the MDMCFG4 register's lower nibble
        byte mdmcfg4Value = readReg(CC1101_MDMCFG4);
        int drateE = mdmcfg4Value & 0x0F;

        // Read the DRATE_M from the MDMCFG3 register
        byte mdmcfg3Value = readReg(CC1101_MDMCFG3);
        int drateM = mdmcfg3Value & 0xFF;

        // Calculate the bit rate using the formula
        double bitRate = ((256 + drateM) * Math.pow(2, drateE) * F_OSC) / Math.pow(2, 28);

        return (int) Math.round(bitRate);
    }
    //endregion


    //region Frequency
    public void setFrequency(byte freq2, byte freq1, byte freq0){
        writeReg(CC1101_FREQ2, freq2);
        writeReg(CC1101_FREQ1, freq1);
        writeReg(CC1101_FREQ0, freq0);
    }
    public void setFrequencyMHz(double frequencyMHz) {
        // Assuming the oscillator frequency is 26 MHz
        double fOsc = 26e6; // 26 MHz

        // Calculate the integer representation of the frequency for CC1101 registers
        long frequency = (long) Math.round(frequencyMHz * 1e6 * Math.pow(2, 16) / fOsc);

        // Extract the individual frequency bytes
        byte freq2 = (byte) ((frequency >> 16) & 0xFF);
        byte freq1 = (byte) ((frequency >> 8) & 0xFF);
        byte freq0 = (byte) (frequency & 0xFF);

        // Set the frequency using your existing function
        setFrequency(freq2, freq1, freq0);
    }
    public double getFrequency(){
        int freq2 = readReg(CC1101_FREQ2) & 0xFF;
        int freq1 = readReg(CC1101_FREQ1) & 0xFF;
        int freq0 = readReg(CC1101_FREQ0) & 0xFF;

        // Convert the frequency bytes to a single integer
        long frequency = ((freq2 << 16) | (freq1 << 8) | freq0);
        // Assuming the oscillator frequency is 26 MHz
        double fOsc = 26e6; // 26 MHz
        double frequencyMHz = frequency * (fOsc / Math.pow(2, 16)) / 1e6; // Convert to MHz
        Log.i("frequencyMHz", ""+frequencyMHz);
        return frequencyMHz;
    }
    //endregion

    //region Modulation
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
    public int getModulation(){
        int mdmcfg2 = readReg(CC1101_MDMCFG2) & 0xFF; // Replace 10 with the actual index of MDMCFG2 in registerPacket
        int modulationSetting = (mdmcfg2 >> 4) & 0x07; // Shift right by 4 bits and mask out everything but bits 6:4
        Log.i("modulationSetting", ""+modulationSetting);
        return modulationSetting;
    }
    //endregion

    //region Power
    public boolean setPowerLevel(int powerLevel) {
        byte powerSetting;
        switch (powerLevel) {
            case -30:
                powerSetting = 0x12;
                break;
            case -20:
                powerSetting = 0x0D; // Use 0x0E for 433 MHz
                break;
            case -15:
                powerSetting = 0x1C; // Use 0x1D for 433 MHz
                break;
            case -10:
                powerSetting = 0x34;
                break;
            case 0:
                powerSetting = 0x51; // Use 0x60 for 433 MHz
                break;
            case 5:
                powerSetting = (byte)0x85; // Use 0x84 for 433 MHz
                break;
            case 7:
                powerSetting = (byte)0xCB; // Use 0xC8 for 433 MHz
                break;
            case 10:
                powerSetting = (byte)0xC2; // Use 0xC0 for 433 MHz
                break;
            default:
                return false; // Invalid power level
        }
        // Write the power setting to the PA_TABLE
        writeReg(CC1101_PATABLE, powerSetting);
        // Verify that the write was successful
        byte readBack = readReg(CC1101_PATABLE);
        return powerSetting == readBack;
    }
    public int getPowerLevel() {
        // Read the current power setting from the PA_TABLE
        byte currentSetting = readReg(CC1101_PATABLE);

        // Match the read setting to the power level
        switch (currentSetting) {
            case 0x12:
                return -30;
            case 0x0D: // Use 0x0E for 433 MHz
                return -20;
            case 0x1C: // Use 0x1D for 433 MHz
                return -15;
            case 0x34:
                return -10;
            case 0x51: // Use 0x60 for 433 MHz
                return 0;
            case (byte)0x85: // Use 0x84 for 433 MHz
                return 5;
            case (byte)0xCB: // Use 0xC8 for 433 MHz
                return 7;
            case (byte)0xC2: // Use 0xC0 for 433 MHz
                return 10;
            default:
                return Integer.MIN_VALUE; // Indicates an unrecognized power level
        }
    }
    //endregion

    //region Bandwidth
    public boolean setBandwidth(double bandwidth) {
        // Constants for the register calculation
        final double F_XTAL = 26_000_000.0; // Crystal frequency in Hz
        final double F_IF = 100_000.0; // Intermediate frequency in Hz
        final double f_bw = bandwidth * 1e3; // Convert bandwidth to Hz

        // Calculate the bandwidth exponent (bw_exp)
        int bw_exp = 0;
        while (bw_exp <= 15 && (F_XTAL / (8 * (bw_exp + 2) * F_IF)) >= f_bw) {
            bw_exp++;
        }

        if (bw_exp > 15) {
            // Bandwidth is too low for this radio configuration
            return false;
        }

        // Calculate the bandwidth mantissa (bw_mant)
        double bw_mant = (F_XTAL / (8 * (bw_exp + 2) * F_IF)) / f_bw;
        int bw_mant_int = (int) bw_mant;
        if (bw_mant_int % 2 != 0) {
            // Round up to the nearest even number
            bw_mant_int++;
        }

        // Combine bw_exp and bw_mant to form the register value
        byte combinedValue = (byte) ((bw_exp << 4) | (bw_mant_int & 0x0F));

        // Write the combined value to the appropriate register
        writeReg((byte) CC1101_MDMCFG4, combinedValue);

        // Verify the write operation
        byte confirmValue = readReg((byte) CC1101_MDMCFG4);
        return confirmValue == combinedValue;
    }
    public double getBandwidth() {
        // Constants for the register calculation
        final double F_XTAL = 26_000_000.0; // Crystal frequency in Hz

        // Read the value from the MDMCFG4 register
        byte registerValue = readReg((byte) CC1101_MDMCFG4);

        // Extract the bandwidth exponent (CHANBW_E) and mantissa (CHANBW_M) from the register value
        int bw_exp = (registerValue >> 6) & 0x03; // CHANBW_E: bits 7-6
        int bw_mant = (registerValue >> 4) & 0x03; // CHANBW_M: bits 5-4

        // Calculate the bandwidth in Hz using the correct formula
        double bandwidthHz = F_XTAL / (8.0 * (4.0 + bw_mant) * Math.pow(2.0, bw_exp));

        // Convert the bandwidth to kHz
        double bandwidthkHz = bandwidthHz / 1000.0;

        return bandwidthkHz;
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
        Log.i("Deviation", Utils.toHexStringWithHexPrefix(values));

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
    public double getDeviation() {
        // Constants for the DEVIATN register calculation
        final double F_OSC = 26_000_000; // Oscillator frequency in Hz
        final int DEVIATION_M_MAX = 7; // 3-bit DEVIATION_M has max value 7
        final int DEVIATION_E_MAX = 7; // 3-bit DEVIATION_E has max value 7

        // Read the value from the DEVIATN register
        byte deviationValue = readReg((byte)CC1101_DEVIATN);

        // Extract DEVIATION_M and DEVIATION_E from the combined value
        int deviationM = deviationValue & 0x07;
        int deviationE = (deviationValue >> 4) & 0x07;

        // Calculate the deviation in kHz using the same formula as in setDeviation
        double deviation = ((8 + deviationM) * Math.pow(2, deviationE)) * (F_OSC / Math.pow(2, 17));

        return deviation;
    }
    //endregion

    //region Gain
    public boolean setMaxDvgaGain(byte maxDvgaGain) {
        byte MAX_DVGA_GAIN_MASK = (byte) 0xC0; // 1100 0000
        byte regValue = readReg(CC1101_AGCCTRL2);
        regValue = (byte) ((regValue & ~MAX_DVGA_GAIN_MASK) | ((maxDvgaGain << 6) & MAX_DVGA_GAIN_MASK));
        writeReg(CC1101_AGCCTRL2, regValue);
        return readReg(CC1101_AGCCTRL2) == regValue;
    }
    public byte getMaxDvgaGain() {
        byte MAX_DVGA_GAIN_MASK = (byte) 0xC0; // 1100 0000
        byte regValue = readReg(CC1101_AGCCTRL2);
        return (byte) ((regValue & MAX_DVGA_GAIN_MASK) >>> 6);
    }
    public boolean setMaxLnaGain(byte maxLnaGain) {
        byte MAX_LNA_GAIN_MASK = (byte) 0x38;  // 0011 1000
        byte regValue = readReg(CC1101_AGCCTRL2);
        regValue = (byte) ((regValue & ~MAX_LNA_GAIN_MASK) | ((maxLnaGain << 3) & MAX_LNA_GAIN_MASK));
        writeReg(CC1101_AGCCTRL2, regValue);
        return readReg(CC1101_AGCCTRL2) == regValue;
    }
    public byte getMaxLnaGain() {
        byte MAX_LNA_GAIN_MASK = (byte) 0x38;  // 0011 1000
        byte regValue = readReg(CC1101_AGCCTRL2);
        return (byte) ((regValue & MAX_LNA_GAIN_MASK) >>> 3);
    }
    public boolean setMagnTarget(byte magnTarget) {
        byte MAGN_TARGET_MASK = (byte) 0x07;   // 0000 0111
        byte regValue = readReg(CC1101_AGCCTRL2);
        regValue = (byte) ((regValue & ~MAGN_TARGET_MASK) | (magnTarget & MAGN_TARGET_MASK));
        writeReg(CC1101_AGCCTRL2, regValue);
        return readReg(CC1101_AGCCTRL2) == regValue;
    }
    public byte getMagnTarget() {
        byte MAGN_TARGET_MASK = (byte) 0x07;   // 0000 0111
        byte regValue = readReg(CC1101_AGCCTRL2);
        return (byte) (regValue & MAGN_TARGET_MASK);
    }
    public boolean setGainDbm(double gainDbm) {
        byte maxLnaGain = 0;
        byte maxDvgaGain = 0;
        boolean matchFound = false;
        int[][] gainSettings = {
                {-90, -84, -78, -72}, // MAX_LNA_GAIN 00
                {-88, -82, -76, -70}, // MAX_LNA_GAIN 01
                {-84, -78, -72, -66}, // MAX_LNA_GAIN 10
                {-82, -76, -70, -64}, // MAX_LNA_GAIN 11
                {-80, -74, -68, -62}, // MAX_LNA_GAIN 100
                {-78, -72, -66, -60}, // MAX_LNA_GAIN 101
                {-76, -70, -64, -58}, // MAX_LNA_GAIN 110
                {-74, -68, -62, -56}  // MAX_LNA_GAIN 111
        };

        // Find the matching setting for the gain in dBm
        for (int lna = 0; lna < gainSettings.length; lna++) {
            for (int dvga = 0; dvga < gainSettings[lna].length; dvga++) {
                if (gainSettings[lna][dvga] <= gainDbm) {
                    maxLnaGain = (byte)lna;
                    maxDvgaGain = (byte)dvga;
                    matchFound = true;
                    break;
                }
            }
            if (matchFound) {
                break;
            }
        }

        if (!matchFound) {
            // No match found, possibly log this or handle the error
            return false;
        }
        // Use previously defined setters to set the gain values
        if(!setMaxLnaGain(maxLnaGain)){
            return false;
        }
        if(!setMaxDvgaGain(maxDvgaGain)){
            return false;
        }
        return true;
    }
    public double getGainDbm() {
        // Use previously defined getters to get the gain values
        byte maxLnaGain = getMaxLnaGain();
        byte maxDvgaGain = getMaxDvgaGain();
        int[][] gainSettings = {
                {-90, -84, -78, -72}, // MAX_LNA_GAIN 00
                {-88, -82, -76, -70}, // MAX_LNA_GAIN 01
                {-84, -78, -72, -66}, // MAX_LNA_GAIN 10
                {-82, -76, -70, -64}, // MAX_LNA_GAIN 11
                {-80, -74, -68, -62}, // MAX_LNA_GAIN 100
                {-78, -72, -66, -60}, // MAX_LNA_GAIN 101
                {-76, -70, -64, -58}, // MAX_LNA_GAIN 110
                {-74, -68, -62, -56}  // MAX_LNA_GAIN 111
        };

        // Look up the dBm value from the table
        return gainSettings[maxLnaGain][maxDvgaGain];
    }
    public boolean setCarrierSenseRelThr(byte carrierSenseRelThr) {
        byte CARRIER_SENSE_REL_THR_MASK = (byte) 0x30; // 0011 0000
        byte regValue = readReg(CC1101_AGCCTRL1);
        // Clear the relative threshold bits and set the new value
        regValue = (byte) ((regValue & ~CARRIER_SENSE_REL_THR_MASK) | ((carrierSenseRelThr << 4) & CARRIER_SENSE_REL_THR_MASK));
        writeReg(CC1101_AGCCTRL1, regValue);
        return readReg(CC1101_AGCCTRL1) == regValue;
    }
    public byte getCarrierSenseRelThr() {
        byte CARRIER_SENSE_REL_THR_MASK = (byte) 0x30; // 0011 0000
        byte regValue = readReg(CC1101_AGCCTRL1);
        // Isolate the relative threshold bits
        return (byte) ((regValue & CARRIER_SENSE_REL_THR_MASK) >>> 4);
    }
    public boolean setCarrierSenseAbsThr(byte carrierSenseAbsThr) {
        byte CARRIER_SENSE_ABS_THR_MASK = (byte) 0x0F; // 0000 1111
        byte regValue = readReg(CC1101_AGCCTRL1);
        // Clear the absolute threshold bits and set the new value
        regValue = (byte) ((regValue & ~CARRIER_SENSE_ABS_THR_MASK) | (carrierSenseAbsThr & CARRIER_SENSE_ABS_THR_MASK));
        writeReg(CC1101_AGCCTRL1, regValue);
        return readReg(CC1101_AGCCTRL1) == regValue;
    }
    public byte getCarrierSenseAbsThr() {
        byte CARRIER_SENSE_ABS_THR_MASK = (byte) 0x0F; // 0000 1111
        byte regValue = readReg(CC1101_AGCCTRL1);
        // Isolate the absolute threshold bits
        return (byte) (regValue & CARRIER_SENSE_ABS_THR_MASK);
    }
    //endregion

    //region Packet Settings
    public boolean setPktLength(int length){
        byte pktlen = (byte)length;
        writeReg(CC1101_PKTLEN, pktlen);
        //verify
        return readReg(CC1101_PKTLEN) == pktlen;
    }
    public int getPktLength(){
        return readReg(CC1101_PKTLEN) & 0xFF;
    }

    public int getPacketFormat() {
        // Read the value of the PKTCTRL0 register
        byte pktctrl0Value = readReg(CC1101_PKTCTRL0);

        int packetFormat = (pktctrl0Value >> 4) & 0x03;

        // Return the PKT_FORMAT value
        return packetFormat;
    }
    public boolean setPacketFormat(int format) {
        byte PKT_FORMAT_MASK = (byte) 0xCF;
        if (format < 0 || format > 3) {
            return false; // Return false if the format is out of range
        }
        byte currentRegValue = readReg(CC1101_PKTCTRL0);
        currentRegValue &= PKT_FORMAT_MASK;
        byte newRegValue = (byte) (currentRegValue | (format << 4));
        writeReg(CC1101_PKTCTRL0, newRegValue);
        byte verifyRegValue = readReg(CC1101_PKTCTRL0);
        // Check if the written value matches the read value for the PKT_FORMAT bits
        return (verifyRegValue & ~PKT_FORMAT_MASK) == (newRegValue & ~PKT_FORMAT_MASK);
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
    public byte getSyncMode() {
        // Read the current register value
        byte currentValue = readReg(CC1101_MDMCFG2);

        // Log the current value if needed
        // Log.i("MDMCFG2", "current value: " + currentValue);

        byte mask = 0b00000111; // Mask for the sync mode bits (bit 0, 1, 2)
        byte syncMode = (byte) (currentValue & mask); // Isolate the sync mode bits

        return syncMode;
    }

    public boolean setPreambleLength(int numBytes) {
        // Map the number of preamble bytes to the corresponding setting value
        int setting;
        switch (numBytes) {
            case 2:
                setting = 0;
                break;
            case 3:
                setting = 1;
                break;
            case 4:
                setting = 2;
                break;
            case 6:
                setting = 3;
                break;
            case 8:
                setting = 4;
                break;
            case 12:
                setting = 5;
                break;
            case 16:
                setting = 6;
                break;
            case 24:
                setting = 7;
                break;
            default:
                return false; // Invalid number of preamble bytes
        }
        // Shift the setting into the correct position (bits 6:4)
        byte mdmcfg1Value = (byte) (setting << 4);
        // Write the value to the register
        writeReg(CC1101_MDMCFG1, mdmcfg1Value);
        // Verify that the register was set correctly
        return (readReg(CC1101_MDMCFG1) & 0x70) == mdmcfg1Value;
    }
    public int getPreambleLength() {
        // Read the register value
        byte mdmcfg1Value = readReg(CC1101_MDMCFG1);

        // Isolate the preamble setting bits (bits 6:4) and shift them to the LSB
        int setting = (mdmcfg1Value & 0x70) >> 4;

        // Map the setting value back to the number of preamble bytes
        switch (setting) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 4;
            case 3:
                return 6;
            case 4:
                return 8;
            case 5:
                return 12;
            case 6:
                return 16;
            case 7:
                return 24;
            default:
                return -1; // Indicate an error if the setting is out of range
        }
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
    public byte[] getSyncWord() {
        // Read the sync word from the CC1101_SYNC1 and CC1101_SYNC0 addresses
        return readBurstReg(CC1101_SYNC1, (byte) 2);
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
    public boolean getManchesterEncoding() {
        byte mdmcfg2 = readReg(CC1101_MDMCFG2);
        // Bit 3 is the Manchester encoding bit, mask it with 0b00001000
        return (mdmcfg2 & 0b00001000) != 0;
    }
    //endregion

    //region GPIO
    public boolean getGDO0() {
        byte response = readReg((byte) (CC1101_PKTSTATUS | READ_BURST));
        return (response & 1) == 1;
    }
    public boolean getGDO2() {
        byte response = readReg((byte) (CC1101_PKTSTATUS | READ_BURST));
        return (response & 0x04) >> 2 == 1;
    }
    public void configureGDO(int gdo0, int gdoInput) {
        byte[] command = {'p', 'i', 'n', (byte) gdo0, (byte) gdoInput}; // Replace with your actual command
        byte[] response = USBService.sendCommand(command, 1000);
        Log.i("configureGDO", gdo0 + ", " + Utils.bytesToHexString(response));  //response is the reading at that register
    }
    public void setGDOMode(byte gdo2, byte gdo1, byte gdo0){
        writeReg(CC1101_IOCFG2, gdo2);
        writeReg(CC1101_IOCFG1, gdo1);
        writeReg(CC1101_IOCFG0, gdo0);
    }
    public boolean setGDO0Mode(byte gdo0){
        writeReg(CC1101_IOCFG0, gdo0);
        return readReg(CC1101_IOCFG0) == gdo0;
    }
    public boolean setGDO2Mode(byte gdo2){
        writeReg(CC1101_IOCFG2, gdo2);
        return readReg(CC1101_IOCFG2) == gdo2;
    }
    public int getGDO0Mode(){
        return readReg(CC1101_IOCFG0);
    }
    public int getGDO2Mode(){
        return readReg(CC1101_IOCFG2);
    }

    public boolean setFIFOThreshold(byte threshold){
        writeReg(CC1101_FIFOTHR, threshold);
        return readReg(CC1101_FIFOTHR) == threshold;
    }
    public int getFIFOThreshold(){
        return readReg(CC1101_FIFOTHR);
    }
    //endregion


    //region Init Routines
    public void initRx() {
        byte[] PA_TABLE_OOK = {0x00, (byte) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        setFrequency((byte) 0x10, (byte) 0xB0, (byte) 0x71); //433.919830
        setGDOMode((byte) 0x2E, (byte) 0x2E, (byte) 0x00); // high impedance, high impedance, GDO0 as fifo threshold assert.
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

    public void init433() {
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

    public void initRxContinuous() {
        writeReg(CC1101_PKTCTRL0, (byte) 0x32); // async serial mode (packet engine is off)
        setGDOMode((byte) 0x0D, (byte) 0x2E, (byte) 0x0D);
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
        setGDOMode((byte) 0x2E, (byte) 0x2E, (byte) 0x00); // high impedance, high impedance, GDO0 as fifo threshold assert.
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
        setGDOMode((byte) 0x2B, (byte) 0x2E, (byte) 0x0C);


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
    //endregion

}
