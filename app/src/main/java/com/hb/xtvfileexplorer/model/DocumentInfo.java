package com.hb.xtvfileexplorer.model;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import com.hb.xtvfileexplorer.utils.Utils;

import java.io.FileNotFoundException;
import java.text.Collator;


public class DocumentInfo {

    public String authority;
    public String documentId;
    public String mimeType;
    public String displayName;
    public long lastModified;
    public int flags;
    public String summary;
    public long size;
    public int icon;
    public String path;

    public Uri derivedUri;

    public DocumentInfo() {
        reset();
    }

    private void reset() {
        authority = null;
        documentId = null;
        mimeType = null;
        displayName = null;
        lastModified = -1;
        flags = 0;
        summary = null;
        size = -1;
        icon = 0;
        path = null;

        derivedUri = null;
    }

    private void updateFromCursor(Cursor cursor, String authority) {
        this.authority = authority;
        this.documentId = getCursorString(cursor, DocumentsContract.Document.COLUMN_DOCUMENT_ID);
        this.mimeType = getCursorString(cursor, DocumentsContract.Document.COLUMN_MIME_TYPE);
        this.documentId = getCursorString(cursor, DocumentsContract.Document.COLUMN_DOCUMENT_ID);
        this.mimeType = getCursorString(cursor, DocumentsContract.Document.COLUMN_MIME_TYPE);
        this.displayName = getCursorString(cursor, DocumentsContract.Document.COLUMN_DISPLAY_NAME);
        this.lastModified = getCursorLong(cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED);
        this.flags = getCursorInt(cursor, DocumentsContract.Document.COLUMN_FLAGS);
        this.summary = getCursorString(cursor, DocumentsContract.Document.COLUMN_SUMMARY);
        this.size = getCursorLong(cursor, DocumentsContract.Document.COLUMN_SIZE);
        this.icon = getCursorInt(cursor, DocumentsContract.Document.COLUMN_ICON);
        this.path = getCursorString(cursor, "path");
        deriveFields();
    }

    private void deriveFields() {
        derivedUri = DocumentsContract.buildDocumentUri(authority, documentId);
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getString(index) : null;
    }

    /**
     * Missing or null values are returned as -1.
     */
    public static long getCursorLong(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        if (index == -1) return -1;
        final String value = cursor.getString(index);
        if (value == null) return -1;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Missing or null values are returned as 0.
     */
    public static int getCursorInt(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getInt(index) : 0;
    }


    /**
     * Missing or null values are returned as 0.
     */
    public static boolean getCursorBoolean(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) && cursor.getInt(index) == 1;
    }

    public static DocumentInfo fromUri(ContentResolver resolver, Uri uri)
            throws FileNotFoundException {
        final DocumentInfo info = new DocumentInfo();
        info.updateFromUri(resolver, uri);
        return info;
    }

    private void updateFromUri(ContentResolver resolver, Uri uri) throws FileNotFoundException {
        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            client = Utils.acquireUnstableProviderOrThrow(
                    resolver, uri.getAuthority());
            cursor = client.query(uri, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                throw new FileNotFoundException("Missing details for " + uri);
            }
            updateFromCursor(cursor, uri.getAuthority());
        } catch (Throwable t) {
            //throw asFileNotFoundException(t);
        } finally {
            Utils.closeQuietly(cursor);
            Utils.releaseQuietly(client);
        }
    }

    public boolean isDirectory() {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }

    public static DocumentInfo fromDirectoryCursor(Cursor cursor, String authority) {
        final DocumentInfo info = new DocumentInfo();
        info.updateFromCursor(cursor, authority);
        return info;
    }

    private static final Collator sCollator;

    static {
        sCollator = Collator.getInstance();
        sCollator.setStrength(Collator.SECONDARY);
    }

    /**
     * String prefix used to indicate the document is a directory.
     */
    public static final char DIR_PREFIX = '\001';

    /**
     * Compare two strings against each other using system default collator in a
     * case-insensitive mode. Clusters strings prefixed with {@link #DIR_PREFIX}
     * before other items.
     */
    public static int compareToIgnoreCaseNullable(String lhs, String rhs) {
        final boolean leftEmpty = TextUtils.isEmpty(lhs);
        final boolean rightEmpty = TextUtils.isEmpty(rhs);

        if (leftEmpty && rightEmpty) return 0;
        if (leftEmpty) return -1;
        if (rightEmpty) return 1;

        final boolean leftDir = (lhs.charAt(0) == DIR_PREFIX);
        final boolean rightDir = (rhs.charAt(0) == DIR_PREFIX);

        if (leftDir && !rightDir) return -1;
        if (rightDir && !leftDir) return 1;

        return sCollator.compare(lhs, rhs);
    }
}
