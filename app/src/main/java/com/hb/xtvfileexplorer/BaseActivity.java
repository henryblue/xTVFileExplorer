package com.hb.xtvfileexplorer;

import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.hb.xtvfileexplorer.model.RootInfo;

public abstract class BaseActivity extends AppCompatActivity {

    public static final int MODE_UNKNOWN = 0;
    public static final int MODE_LIST = 1;
    public static final int MODE_GRID = 2;

    public static final int SORT_ORDER_UNKNOWN = 0;
    public static final int SORT_ORDER_DISPLAY_NAME = 1;
    public static final int SORT_ORDER_LAST_MODIFIED = 2;
    public static final int SORT_ORDER_SIZE = 3;

    public abstract void onRootPicked(RootInfo root, boolean closeDrawer);

    public static BaseActivity get(Fragment fragment) {
        return (BaseActivity) fragment.getActivity();
    }

}
