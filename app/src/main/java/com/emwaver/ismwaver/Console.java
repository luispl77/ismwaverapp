package com.emwaver.ismwaver;

import android.content.Context;
import android.content.Intent;

import com.emwaver.ismwaver.Constants;
import com.emwaver.ismwaver.ui.console.ConsoleRepository;

public class Console {
    private static final ConsoleRepository repository = ConsoleRepository.getInstance();

    public static void print(String dataString) {
        repository.appendMessage(dataString);
    }


}
