package com.hb.xtvfileexplorer.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hb.xtvfileexplorer.R;


public class ListItemView extends ItemView {

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

        View.inflate(context, R.layout.item_doc_list, this);
        mTitle = (TextView) findViewById(R.id.title);
        mIcon = (ImageView) findViewById(R.id.icon);
        mDate = (TextView) findViewById(R.id.date);
        mSummary = (TextView) findViewById(R.id.summary);
        mSize = (TextView) findViewById(R.id.size);
    }

    @Override
    public void setTitle(String title) {
        mTitle.setText(title);
    }

    @Override
    public void setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
    }

    @Override
    public void setIconResource(int resId) {
        mIcon.setImageResource(resId);
    }

    @Override
    public void setDate(String date) {
        mDate.setText(date);
    }

    @Override
    public void setSummary(String summary) {
        mSummary.setText(summary);
    }

    @Override
    public void setSize(String size) {
        mSize.setText(size);
    }
}
