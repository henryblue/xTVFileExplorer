package com.hb.xtvfileexplorer.ui;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hb.xtvfileexplorer.R;


public class GridItemView extends ItemView {

    private TextView mTitle;
    private ImageView mIcon;
    private TextView mDate;
    private TextView mSize;

    public GridItemView(Context context) {
        super(context);
        init(context);
    }

    public GridItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GridItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Resources res = context.getResources();
        setOrientation(VERTICAL);
        int height = res.getDimensionPixelOffset(R.dimen.grid_item_height);
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        setBackgroundColor(ContextCompat.getColor(context, R.color.item_doc_grid_background));
        View.inflate(context, R.layout.item_doc_grid, this);
        mTitle = (TextView) findViewById(R.id.title);
        mIcon = (ImageView) findViewById(R.id.icon);
        mDate = (TextView) findViewById(R.id.date);
        mSize = (TextView) findViewById(R.id.size);
    }

    @Override
    public void setTitle(String title) {
        mTitle.setText(title);
    }

    @Override
    public void setIcon(Drawable icon) {
    }

    @Override
    public void setIconResource(int resId) {
        mIcon.setBackgroundResource(resId);
    }

    @Override
    public void setBackgroundColor() {
        CircleImageView imageView = (CircleImageView) findViewById(R.id.icon_mime_background);
        imageView.setColor(ContextCompat.getColor(getContext(), R.color.list_item_bg_normal));
    }

    @Override
    public void setDate(String date) {
        mDate.setText(date);
    }

    @Override
    public void setSummary(String summary) {
    }

    @Override
    public void setSize(String size) {
        mSize.setText(size);
    }
}
