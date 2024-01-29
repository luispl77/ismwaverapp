package com.emwaver.ismwaver;

public interface CommandSender {
    byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis);
    //public void appendConsoleText(String source, String data);
}
