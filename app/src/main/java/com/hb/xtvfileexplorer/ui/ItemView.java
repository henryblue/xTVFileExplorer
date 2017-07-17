package com.hb.xtvfileexplorer.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;


public abstract class ItemView extends LinearLayout {
    public ItemView(Context context) {
        super(context);
    }

    public ItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract void setTitle(String title);

    public abstract void setIcon(Drawable icon);

    public abstract void setIconResource(int resId);

    public abstract void setDate(String date);

    public abstract void setSummary(String summary);

    public abstract void setSize(String size);

    public void setBackgroundColor() {}
}
