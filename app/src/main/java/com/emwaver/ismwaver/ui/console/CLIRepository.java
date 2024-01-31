package com.emwaver.ismwaver.ui.console;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class CLIRepository {
    private static CLIRepository instance;
    private final MutableLiveData<String> cliData = new MutableLiveData<>("<ISMWaver>");

    private final StringBuilder dataBuilder = new StringBuilder();
    private final Object lock = new Object();

    public void appendMessage(String message) {
        synchronized (lock) { //the lock accumulates changes
            dataBuilder.append(message);
            cliData.postValue(dataBuilder.toString());
        }
    }

    private CLIRepository() {
        // Private constructor
    }

    public static synchronized CLIRepository getInstance() {
        if (instance == null) {
            instance = new CLIRepository();
        }
        return instance;
    }

    public LiveData<String> getCLIData() {
        return cliData;
    }

    public void clearData() {
        cliData.postValue(""); // Clear the data by posting an empty string
        dataBuilder.setLength(0); //clear the string builder
    }
}
