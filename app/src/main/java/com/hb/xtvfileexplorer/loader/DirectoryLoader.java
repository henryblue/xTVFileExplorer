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
import android.provider.DocumentsContract;

import com.hb.xtvfileexplorer.model.DirectoryResult;
import com.hb.xtvfileexplorer.utils.Utils;


public class DirectoryLoader extends AsyncTaskLoader<DirectoryResult> {

    public static final int SORT_ORDER_NONE = -1;
    public static final int SORT_ORDER_DISPLAY_NAME = 1;
    public static final int SORT_ORDER_LAST_MODIFIED = 2;
    public static final int SORT_ORDER_SIZE = 3;

    private final Uri mUri;
    private CancellationSignal mSignal;
    private DirectoryResult mResult;
    private int mSortOrder;

    public DirectoryLoader(Context context, Uri uri, int sortOrder) {
        super(context);
        mUri = uri;
        mSortOrder = sortOrder;
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
            cursor = client.query(mUri, null, null, null, getQuerySortOrder(mSortOrder));
            if (mSortOrder != SORT_ORDER_NONE) {
                cursor = new SortingCursorWrapper(cursor, mSortOrder);
            }
            result.client = client;
            result.cursor = cursor;
        } catch (RemoteException e) {
            result.exception = e;
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

    public static String getQuerySortOrder(int sortOrder) {
        switch (sortOrder) {
            case SORT_ORDER_DISPLAY_NAME:
                return DocumentsContract.Document.COLUMN_DISPLAY_NAME + " ASC";
            case SORT_ORDER_LAST_MODIFIED:
                return DocumentsContract.Document.COLUMN_LAST_MODIFIED + " DESC";
            case SORT_ORDER_SIZE:
                return DocumentsContract.Document.COLUMN_SIZE + " DESC";
            default:
                return null;
        }
    }
}
