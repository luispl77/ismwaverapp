function initReceive() {
    CC1101.spiStrobe(0x30);
    CC1101.sendInitRx();
    CC1101.setNumPreambleBytes(5);
    CC1101.setModulation(0);
    var mercedes_sync_word = [0xAA, 0xA9];
    var signedSyncWord = Utils.getSignedBytes(mercedes_sync_word);
    CC1101.setSyncWord(signedSyncWord);
    CC1101.setDataRate(2000);
    CC1101.setDeviation(15000);
    CC1101.setPktLength(28);

    CC1101.receiveData(); //put in RX
}

function getMercedesSignal() {
    var i = 0;
    while(i < 5){
        if(CC1101.getGDO() == true){
         var receivedBytes = CC1101.receiveData();
                if(receivedBytes != null){
                    Console.print("signal: " + CC1101.bytesToHexString(receivedBytes) + "\n");
                    i++;
                }
        }
        Utils.delay(200);
    }
}


initReceive();
getMercedesSignal();

