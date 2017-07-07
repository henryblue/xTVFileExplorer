package com.hb.xtvfileexplorer.loader;

import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.utils.Utils;

import net.jcip.annotations.GuardedBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class RootsLoader extends AsyncTaskLoader<Collection<RootInfo>> {
    private Context mContext;
    private Collection<RootInfo> mResult;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private static List<String> mStoppedAuthorities = new ArrayList<>();

    public static void addAuthority(String authority) {
        mStoppedAuthorities.add(authority);
    }

    public RootsLoader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Collection<RootInfo> loadInBackground() {
        mResult = loadStoppedAuthorities();
        return mResult;
    }

    @Override
    public void deliverResult(Collection<RootInfo> result) {
        if (isReset()) {
            return;
        }
        mResult = result;

        if (isStarted()) {
            super.deliverResult(result);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mResult != null) {
            deliverResult(mResult);
        }
        if (takeContentChanged() || mResult == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        mResult = null;
    }

    private Collection<RootInfo> loadStoppedAuthorities() {
        List<RootInfo> rootInfos = new ArrayList<>();
        final ContentResolver resolver = mContext.getContentResolver();
        synchronized (mLock) {
            for (String authority : mStoppedAuthorities) {
                rootInfos.addAll(loadRootsForAuthority(resolver, authority));
            }
            mStoppedAuthorities.clear();
        }
        return rootInfos;
    }

    private Collection<RootInfo> loadRootsForAuthority(ContentResolver resolver, String authority) {
        final List<RootInfo> roots = new ArrayList<>();
        final Uri rootsUri = DocumentsContract.buildRootsUri(authority);
        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            client = Utils.acquireUnstableProviderOrThrow(resolver, authority);
            cursor = client.query(rootsUri, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    RootInfo root = RootInfo.fromRootsCursor(authority, cursor);
                    roots.add(root);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Utils.closeQuietly(cursor);
            Utils.releaseQuietly(client);
        }

        return roots;
    }


}
