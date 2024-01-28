function initTransmit() {
    CC1101.spiStrobe(0x30);
    CC1101.sendInit();
    Console.print("check terminal\n");
}

function sendTeslaSignal() {
    var teslaSignal = [50, -52, -52, -53, 77, 45, 74, -45, 76, -85, 75, 21, -106, 101, -103, -103, -106, -102, 90, -107, -90, -103, 86, -106, 43, 44, -53, 51, 51, 45, 52, -75, 43, 77, 50, -83, 40];
    for (var i = 0; i < 10; i++) {
        CC1101.sendData(teslaSignal, teslaSignal.length, 300);
        Console.print("tesla signal sent " + (i + 1) + "\n");
    }
}


initTransmit();
sendTeslaSignal();

