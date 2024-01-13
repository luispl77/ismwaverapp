package com.emwaver.ismwaver.ui.terminal;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class TerminalViewModel extends ViewModel {
    private final MutableLiveData<List<TextWithColor>> terminalData = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<TextWithColor>> getTerminalData() {
        return terminalData;
    }

    public void appendData(String data, @ColorInt int textColor) {
        List<TextWithColor> currentData = terminalData.getValue();
        if (currentData == null) {
            currentData = new ArrayList<>();
        }
        currentData.add(new TextWithColor(data, textColor));
        terminalData.setValue(new ArrayList<>(currentData)); // Set a new list to trigger LiveData update
    }

    public void setData(String data) {
        List<TextWithColor> newList = new ArrayList<>();
        newList.add(new TextWithColor(data, Color.BLACK)); // Default color
        terminalData.setValue(newList);
    }


    public static class TextWithColor {
        private final String text;
        private final int color;

        public TextWithColor(String text, @ColorInt int color) {
            this.text = text;
            this.color = color;
        }

        public String getText() {
            return text;
        }

        public int getColor() {
            return color;
        }
    }
}



