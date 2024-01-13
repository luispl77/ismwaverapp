package com.emwaver.ismwaver.ui.flash;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

public class FlashViewModel extends ViewModel {

    private final MutableLiveData<StringBuilder> mTextBuilder;

    public FlashViewModel() {
        mTextBuilder = new MutableLiveData<>();
        mTextBuilder.setValue(new StringBuilder("This is dfu fragment"));
    }

    public LiveData<String> getText() {
        return Transformations.map(mTextBuilder, StringBuilder::toString);
    }

    public void appendText(String text) {
        StringBuilder currentBuilder = mTextBuilder.getValue();
        if (currentBuilder != null) {
            currentBuilder.append(text);
            mTextBuilder.setValue(currentBuilder);
        }
    }
}
