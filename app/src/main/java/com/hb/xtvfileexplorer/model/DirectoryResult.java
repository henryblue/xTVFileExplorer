package com.hb.xtvfileexplorer.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import com.hb.xtvfileexplorer.utils.Utils;

import java.io.Closeable;

public class DirectoryResult implements Closeable {
	public ContentProviderClient client;
    public Cursor cursor;


    @Override
    public void close() {
        Utils.closeQuietly(cursor);
        Utils.releaseQuietly(client);
        cursor = null;
        client = null;
    }
}