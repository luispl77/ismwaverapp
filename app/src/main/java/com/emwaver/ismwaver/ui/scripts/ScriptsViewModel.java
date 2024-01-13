package com.emwaver.ismwaver.ui.scripts;

import androidx.lifecycle.ViewModel;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScriptsViewModel extends ViewModel {
    // TODO: Implement the ViewModel

    private Queue<Byte> responseQueue = new ConcurrentLinkedQueue<>();

    public void addResponseByte(Byte responseByte) {
        responseQueue.add(responseByte);
    }
    // Method to retrieve and clear data from the queue
    public byte[] getAndClearResponse(int expectedSize) {
        byte[] response = new byte[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            response[i] = responseQueue.poll(); // or handle nulls if necessary
        }
        return response;
    }

    public int getResponseQueueSize() {
        return responseQueue.size();
    }
}