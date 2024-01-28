function initReceive() {
    CC1101.spiStrobe(0x30);
    CC1101.sendInitRx();
    CC1101.setNumPreambleBytes(1);
    CC1101.setModulation(3);
    var tesla_sync_word = [0x8A, 0xCB];
    var signedSyncWord = Utils.getSignedBytes(tesla_sync_word);
    CC1101.setSyncWord(signedSyncWord);
    CC1101.setDataRate(2500);
    CC1101.setPktLength(37);

    CC1101.receiveData(); /*init RX*/
}

function getTeslaSignal() {
    var i = 0;
    while(i < 5){
        if(CC1101.getGDO() == true){
         var receivedBytes = CC1101.receiveData();
                if(receivedBytes != null){
                    Console.print("signal: " + CC1101.bytesToHexString(receivedBytes) + "\n");
                    i++;
                }
        }
        Utils.delay(400);
    }
}


initReceive();
getTeslaSignal();

