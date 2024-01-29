package com.emwaver.ismwaver.ui.console;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ConsoleRepository {
    private static ConsoleRepository instance;
    private final MutableLiveData<String> consoleData = new MutableLiveData<>();

    private final StringBuilder dataBuilder = new StringBuilder();
    private final Object lock = new Object();

    public void appendMessage(String message) {
        synchronized (lock) { //the lock accumulates changes
            dataBuilder.append(message);
            consoleData.postValue(dataBuilder.toString());
        }
    }

    private ConsoleRepository() {
        // Private constructor
    }

    public static synchronized ConsoleRepository getInstance() {
        if (instance == null) {
            instance = new ConsoleRepository();
        }
        return instance;
    }

    public LiveData<String> getConsoleData() {
        return consoleData;
    }

    public void clearData() {
        consoleData.postValue(""); // Clear the data by posting an empty string
        dataBuilder.setLength(0); //clear the string builder
    }
}
