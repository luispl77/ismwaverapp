package com.emwaver.ismwaver.ui.console;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConsoleViewModel extends ViewModel {
    private final ConsoleRepository consoleRepository = ConsoleRepository.getInstance();

    public LiveData<String> getWindowData() {
        //Log.i("getWindowData", "console");
        return consoleRepository.getConsoleData();
    }
    public void appendData(String data) {
        //Log.i("appendData", "console");
        consoleRepository.appendMessage(data);

    }
    public void clearWindowData() {
        consoleRepository.clearData();
    }
}




