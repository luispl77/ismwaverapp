package com.emwaver.ismwaver.ui.console;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConsoleViewModel extends ViewModel {
    private ConsoleRepository repository = ConsoleRepository.getInstance();

    public LiveData<String> getConsoleData() {
        return repository.getConsoleData();
    }

    public void appendData(String data) {
        repository.appendMessage(data);
    }

    public void clearConsoleData() {
        repository.clearData();
    }

}



