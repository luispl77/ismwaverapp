package com.emwaver.ismwaver.ui.rawmode;

import androidx.lifecycle.ViewModel;

public class RawModeViewModel extends ViewModel {
    public int visibleRangeStart = 0;
    public int visibleRangeEnd = 0;
    public int getVisibleRangeStart(){
        return visibleRangeStart;
    }
    public int getVisibleRangeEnd(){
        return visibleRangeEnd;
    }
    public void setVisibleRangeStart(int range){
        visibleRangeStart = range;
    }
    public void setVisibleRangeEnd(int range){
        visibleRangeEnd = range;
    }
}