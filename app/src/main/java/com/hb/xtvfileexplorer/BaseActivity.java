package com.hb.xtvfileexplorer;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.hb.xtvfileexplorer.loader.RootsLoader;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.provider.AppsProvider;
import com.hb.xtvfileexplorer.provider.MediaProvider;

public abstract class BaseActivity extends AppCompatActivity {


    public abstract void onRootPicked(RootInfo root, boolean closeDrawer);

    public static BaseActivity get(Fragment fragment) {
        return (BaseActivity) fragment.getActivity();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RootsLoader.addAuthority(AppsProvider.AUTHORITY);
        RootsLoader.addAuthority(MediaProvider.AUTHORITY);
    }
}
