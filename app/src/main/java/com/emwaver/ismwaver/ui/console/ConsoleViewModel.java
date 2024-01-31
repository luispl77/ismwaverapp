package com.emwaver.ismwaver.ui.console;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConsoleViewModel extends ViewModel {
    private final ConsoleRepository consoleRepository = ConsoleRepository.getInstance();
    private final CLIRepository cliRepository = CLIRepository.getInstance();

    public LiveData<String> getWindowData(boolean isCLIMode) {
        if (isCLIMode) {
            Log.i("getWindowData", "cli");
            return cliRepository.getCLIData();
        } else {
            Log.i("getWindowData", "console");
            return consoleRepository.getConsoleData();
        }
    }

    public void appendData(String data, boolean isCLIMode) {
        if (isCLIMode) {
            Log.i("appendData", "cli");
            cliRepository.appendMessage(data);
        } else {
            Log.i("appendData", "console");
            consoleRepository.appendMessage(data);
        }
    }

    public void clearWindowData(boolean isCLIMode) {
        if (isCLIMode) {
            cliRepository.clearData();
        } else {
            consoleRepository.clearData();
        }
    }
}




