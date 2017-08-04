package com.hb.xtvfileexplorer.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ExpandableListView;

import com.hb.xtvfileexplorer.R;


public class xExpandableListView extends ExpandableListView {

    private boolean mIsChangeCheckViewBg = false;
    private boolean mIsFocusChanged = false;

    public xExpandableListView(Context context) {
        super(context);
    }

    public xExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public xExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        int lastSelectItem = getCheckedItemPosition();
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        View other = getChildAt(lastSelectItem - getFirstVisiblePosition());
        mIsFocusChanged = gainFocus;
        if (gainFocus) {
            int top = 0;
            if (other != null) {
                other.setBackground(null);
                top = other.getTop();
            }
            setSelector(ContextCompat.getDrawable(getContext(), R.drawable.list_item_bg_focus));
            setSelectionFromTop(lastSelectItem, top);
        } else {
            if (getSelectedItemPosition() != lastSelectItem) {
                setSelector(ContextCompat.getDrawable(getContext(), R.drawable.list_item_bg_none));
            }
            if (other != null) {
                other.setBackgroundResource(R.drawable.list_item_selected);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int position = getSelectedItemPosition();
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            position += 1;
            changeCheckViewBg(position);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            position -= 1;
            changeCheckViewBg(position);
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onChildClick() {
        View checkView = getChildAt(getCheckedItemPosition());
        if (checkView != null) {
            checkView.setBackground(null);
        }
    }

    private void changeCheckViewBg(int position) {
        int checkPos = getCheckedItemPosition();
        View checkView = getChildAt(checkPos);
        if (mIsFocusChanged && checkView != null) {
            mIsChangeCheckViewBg = true;
            checkView.setBackgroundResource(R.drawable.list_item_selected);
            mIsFocusChanged = false;
            return;
        }

        if (position == checkPos) {
            if (checkView != null) {
                mIsChangeCheckViewBg = true;
                checkView.setBackgroundResource(R.drawable.list_item_bg_normal);
            }
        } else {
            if (checkView != null && mIsChangeCheckViewBg) {
                checkView.setBackgroundResource(R.drawable.list_item_selected);
                mIsChangeCheckViewBg = false;
            }
        }
    }
}
