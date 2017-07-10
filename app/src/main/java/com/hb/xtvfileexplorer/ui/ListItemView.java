package com.hb.xtvfileexplorer.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hb.xtvfileexplorer.R;


public class ListItemView extends LinearLayout {

    private TextView mTitle;
    private ImageView mIcon;
    private TextView mDate;
    private TextView mSummary;
    private TextView mSize;

    public ListItemView(Context context) {
        super(context);
        init(context);
    }

    public ListItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ListItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Resources res = context.getResources();
        setMinimumHeight(res.getDimensionPixelOffset(R.dimen.app_list_item_height));
        int padding = res.getDimensionPixelOffset(R.dimen.app_list_item_padding);
        setPadding(padding, 0, padding, 0);
        setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setFocusable(true);

        View.inflate(context, R.layout.item_doc_app_list, this);
        mTitle = (TextView) findViewById(R.id.title);
        mIcon = (ImageView) findViewById(R.id.icon);
        mDate = (TextView) findViewById(R.id.date);
        mSummary = (TextView) findViewById(R.id.summary);
        mSize = (TextView) findViewById(R.id.size);
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
    }

    public void setIconResource(int resId) {
        mIcon.setImageResource(resId);
    }

    public void setDate(String date) {
        mDate.setText(date);
    }

    public void setSummary(String summary) {
        mSummary.setText(summary);
    }

    public void setSize(String size) {
        mSize.setText(size);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected) {
            mTitle.setTextColor(Color.WHITE);
            mDate.setTextColor(Color.WHITE);
            mSummary.setTextColor(Color.WHITE);
            mSize.setTextColor(Color.WHITE);
        } else {
            mTitle.setTextColor(Color.BLACK);
            mDate.setTextColor(Color.GRAY);
            mSummary.setTextColor(Color.GRAY);
            mSize.setTextColor(Color.GRAY);
        }
    }
}
