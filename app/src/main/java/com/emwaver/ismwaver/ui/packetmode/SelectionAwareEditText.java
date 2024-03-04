package com.emwaver.ismwaver.ui.packetmode;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

public class SelectionAwareEditText extends androidx.appcompat.widget.AppCompatEditText{
    private int lastSelStart = -1;
    private int lastSelEnd = -1;
    private final Handler handler = new Handler();

    private OnSelectionChangedListener listener;

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    private final Runnable checkSelection = new Runnable() {
        @Override
        public void run() {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            if (selStart != lastSelStart || selEnd != lastSelEnd) {
                lastSelStart = selStart;
                lastSelEnd = selEnd;
                onSelectionChanged(selStart, selEnd);
            }

            handler.postDelayed(this, 100); // check every 100ms
        }
    };

    public SelectionAwareEditText(Context context) {
        super(context);
        init();
    }

    public SelectionAwareEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SelectionAwareEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        handler.post(checkSelection);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(checkSelection);
    }

    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        //Log.d("Selection", "Start: " + selStart + ", End: " + selEnd);
        if (listener != null) {
            //String selectedText = getText().subSequence(selStart, selEnd).toString();
            listener.onTextSelected(selStart, selEnd);
        }
    }
}

