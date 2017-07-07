package com.hb.xtvfileexplorer.loader;


import android.content.AsyncTaskLoader;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.RemoteException;

import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.utils.Utils;


public class DirectoryLoader extends AsyncTaskLoader<DirectoryResult> {

    private final Uri mUri;
    private CancellationSignal mSignal;
    private DirectoryResult mResult;

    public DirectoryLoader(Context context, Uri uri) {
        super(context);
        mUri = uri;
    }

    @Override
    public DirectoryResult loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mSignal = new CancellationSignal();
        }
        final ContentResolver resolver = getContext().getContentResolver();
        final String authority = mUri.getAuthority();
        final DirectoryResult result = new DirectoryResult();

        ContentProviderClient client = null;
        Cursor cursor;
        try {
            client = Utils.acquireUnstableProviderOrThrow(resolver, authority);
            cursor = client.query(mUri, null, null, null, null);
            result.client = client;
            result.cursor = cursor;
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            Utils.releaseQuietly(client);
        }
        return result;
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mSignal != null) {
                mSignal.cancel();
            }
        }
    }

    @Override
    public void deliverResult(DirectoryResult result) {
        if (isReset()) {
            Utils.closeQuietly(result.cursor);
            return;
        }
        DirectoryResult oldResult = mResult;
        mResult = result;

        if (isStarted()) {
            super.deliverResult(result);
        }

        if (oldResult != null && oldResult != result) {
            Utils.closeQuietly(oldResult.cursor);
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
    public void onCanceled(DirectoryResult result) {
        Utils.closeQuietly(result.cursor);
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        Utils.closeQuietly(mResult.cursor);
        mResult = null;
    }
}
