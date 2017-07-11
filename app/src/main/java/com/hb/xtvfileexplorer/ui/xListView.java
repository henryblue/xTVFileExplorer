package com.hb.xtvfileexplorer.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

import com.hb.xtvfileexplorer.R;

public class xListView extends ListView {


    public xListView(Context context) {
        super(context);
    }

    public xListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public xListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        int lastSelectItem = getSelectedItemPosition();
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        View other = getChildAt(lastSelectItem - getFirstVisiblePosition());
        if (gainFocus) {
            int top = 0;
            if (other != null) {
                other.setBackground(null);
                top = other.getTop();
            }
            setSelectionFromTop(lastSelectItem, top);
        } else {
            if (other != null) {
                other.setBackgroundResource(R.drawable.list_item_selected);
            }
        }
    }
}
