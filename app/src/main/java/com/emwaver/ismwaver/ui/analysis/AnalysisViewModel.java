package com.emwaver.ismwaver.ui.analysis;

import androidx.lifecycle.ViewModel;

public class AnalysisViewModel extends ViewModel {
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