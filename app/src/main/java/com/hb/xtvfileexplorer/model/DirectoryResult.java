package com.hb.xtvfileexplorer.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import com.hb.xtvfileexplorer.utils.Utils;

import java.io.Closeable;

import static com.hb.xtvfileexplorer.BaseActivity.MODE_UNKNOWN;
import static com.hb.xtvfileexplorer.BaseActivity.SORT_ORDER_UNKNOWN;

public class DirectoryResult implements Closeable {
	public ContentProviderClient client;
    public Cursor cursor;
    public Exception exception;

    public int mode = MODE_UNKNOWN;
    public int sortOrder = SORT_ORDER_UNKNOWN;

    @Override
    public void close() {
        Utils.closeQuietly(cursor);
        Utils.releaseQuietly(client);
        cursor = null;
        client = null;
    }
}