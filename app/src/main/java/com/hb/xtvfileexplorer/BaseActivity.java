package com.hb.xtvfileexplorer;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.hb.xtvfileexplorer.loader.RootsLoader;
import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.model.DocumentInfo;
import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.provider.AppsProvider;
import com.hb.xtvfileexplorer.provider.MediaProvider;
import com.hb.xtvfileexplorer.provider.StorageProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    public static String[] mAcceptMimes;
    public static Map<Uri, DirectoryResult> mUriCache = new HashMap<>();

    public abstract void onRootPicked(RootInfo root, boolean closeDrawer);
    public abstract void onDocumentPicked(DocumentInfo doc);

    public static BaseActivity get(Fragment fragment) {
        return (BaseActivity) fragment.getActivity();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAcceptMimes = new String[]{"*/*"};
        RootsLoader.addAuthority(StorageProvider.AUTHORITY);
        RootsLoader.addAuthority(MediaProvider.AUTHORITY);
        RootsLoader.addAuthority(AppsProvider.AUTHORITY);
    }

    @Override
    protected void onDestroy() {
        Collection<DirectoryResult> values = mUriCache.values();
        for (DirectoryResult result : values) {
            result.close();
        }
        mUriCache = null;
        super.onDestroy();
    }

    public RootInfo getCurrentRoot() {
        return null;
    }
}
