package com.hb.xtvfileexplorer.loader;

import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.text.format.DateUtils;

import com.hb.xtvfileexplorer.model.RootInfo;
import com.hb.xtvfileexplorer.provider.AppsProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



public class RootsLoader extends AsyncTaskLoader<Collection<RootInfo>> {
    private static final long PROVIDER_ANR_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;
    private Context mContext;
    private Collection<RootInfo> mResult;

    public RootsLoader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Collection<RootInfo> loadInBackground() {
        final ContentResolver resolver = mContext.getContentResolver();
        return loadRootsForAuthority(resolver, AppsProvider.AUTHORITY);
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

    private Collection<RootInfo> loadRootsForAuthority(ContentResolver resolver, String authority) {
        final List<RootInfo> roots = new ArrayList<>();
        final Uri rootsUri = DocumentsContract.buildRootsUri(authority);
        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            client = acquireUnstableProviderOrThrow(resolver, authority);
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
            closeQuietly(cursor);
            releaseQuietly(client);
        }

        return roots;
    }

    private ContentProviderClient acquireUnstableProviderOrThrow(
            ContentResolver resolver, String authority) throws RemoteException {
        final ContentProviderClient client = resolver.acquireUnstableContentProviderClient(authority);
        if (client == null) {
            throw new RemoteException("Failed to acquire provider for " + authority);
        }
        setDetectNotResponding(client, PROVIDER_ANR_TIMEOUT);
        return client;
    }

    private void setDetectNotResponding(ContentProviderClient client, long anrTimeout) {
        try {
            Method method = client.getClass().getMethod("setDetectNotResponding", long.class);
            if (method != null) {
                method.invoke(client, anrTimeout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  void closeQuietly(Cursor closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    private void releaseQuietly(ContentProviderClient client) {
        if (client != null) {
            try {
                client.release();
            } catch (Exception ignored) {
            }
        }
    }
}
