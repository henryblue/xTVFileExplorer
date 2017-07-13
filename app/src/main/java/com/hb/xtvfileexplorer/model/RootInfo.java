package com.hb.xtvfileexplorer.model;


import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.DocumentsContract.Root;

import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.provider.AppsProvider;
import com.hb.xtvfileexplorer.provider.MediaProvider;
import com.hb.xtvfileexplorer.provider.StorageProvider;
import com.hb.xtvfileexplorer.utils.IconUtils;


public class RootInfo {

    public boolean isManuGen = false;
    private String authority;
    private String rootId;
    private int flags;
    private int icon;
    public String title;
    private String summary;
    private String documentId;
    private long availableBytes;
    private long totalBytes;
    private String mimeTypes;
    private String path;

    public String derivedPackageName;
    public String[] derivedMimeTypes;
    public int derivedIcon;
    public int derivedColor;
    public String derivedTag;

    public RootInfo() {
        reset();
    }

    private void reset() {
        authority = null;
        rootId = null;
        flags = 0;
        icon = 0;
        title = null;
        summary = null;
        documentId = null;
        availableBytes = -1;
        totalBytes = -1;
        mimeTypes = null;
        path = null;

        derivedPackageName = null;
        derivedMimeTypes = null;
        derivedIcon = 0;
        derivedColor = 0;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public long getAvailableBytes() {
        return availableBytes;
    }

    public void setAvailableBytes(long availableBytes) {
        this.availableBytes = availableBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(String mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public static RootInfo fromRootsCursor(String authority, Cursor cursor) {
        final RootInfo root = new RootInfo();
        root.authority = authority;
        root.rootId = getCursorString(cursor, Root.COLUMN_ROOT_ID);
        root.flags = getCursorInt(cursor, Root.COLUMN_FLAGS);
        root.icon = getCursorInt(cursor, Root.COLUMN_ICON);
        root.title = getCursorString(cursor, Root.COLUMN_TITLE);
        root.summary = getCursorString(cursor, Root.COLUMN_SUMMARY);
        root.documentId = getCursorString(cursor, Root.COLUMN_DOCUMENT_ID);
        root.availableBytes = getCursorLong(cursor, Root.COLUMN_AVAILABLE_BYTES);
        root.mimeTypes = getCursorString(cursor, Root.COLUMN_MIME_TYPES);
        root.deriveFields();
        return root;
    }

    public void deriveFields() {
        derivedMimeTypes = (mimeTypes != null) ? mimeTypes.split("\n") : null;
        derivedColor = R.color.item_doc_doc;
        derivedTag = title;

        if (isInternalStorage()) {
            derivedIcon = R.drawable.ic_root_internal;
            derivedTag = "storage";
        } else if (isExternalStorage()) {
            derivedIcon = R.drawable.ic_root_sdcard;
            derivedTag = "external_storage";
        } else if (isPhoneStorage()) {
            derivedIcon = R.drawable.ic_root_device;
            derivedTag = "phone";
        } else if (isUserApp()) {
            derivedIcon = R.drawable.ic_root_apps;
            derivedColor = R.color.item_doc_apps;
            derivedTag = "user_apps";
        } else if (isSystemApp()) {
            derivedIcon = R.drawable.ic_root_system_apps;
            derivedColor = R.color.item_doc_apps;
            derivedTag = "system_apps";
        } else if (isAppProcess()) {
            derivedIcon = R.drawable.ic_root_process;
            derivedColor = R.color.item_doc_apps;
            derivedTag = "process";
        } else if (isImages()) {
            derivedIcon = R.drawable.ic_root_image;
            derivedColor = R.color.item_doc_image;
            derivedTag = "images";
        } else if (isVideos()) {
            derivedIcon = R.drawable.ic_root_video;
            derivedColor = R.color.item_doc_video;
            derivedTag = "videos";
        } else if (isAudio()) {
            derivedIcon = R.drawable.ic_root_audio;
            derivedColor = R.color.item_doc_audio;
            derivedTag = "audio";
        }
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
    public boolean isHome() {
        return authority == null && "home".equals(rootId);
    }

    public boolean isStorage() {
        return isInternalStorage() || isExternalStorage() || isSecondaryStorage();
    }

    public boolean isExternalStorage() {
        return StorageProvider.AUTHORITY.equals(authority)
                && StorageProvider.ROOT_ID_PRIMARY_EMULATED.equals(rootId);
    }

    public boolean isInternalStorage() {
        return StorageProvider.AUTHORITY.equals(authority)
                && (title.toLowerCase().contains("internal") || title.contains("内部"));
    }

    public boolean isPhoneStorage() {
        return StorageProvider.AUTHORITY.equals(authority)
                && StorageProvider.ROOT_ID_PHONE.equals(rootId);
    }

    public boolean isSecondaryStorage() {
        return StorageProvider.AUTHORITY.equals(authority)
                && rootId.startsWith(StorageProvider.ROOT_ID_SECONDARY);
    }

    public boolean isLibraryMedia(){
        return isImages() || isVideos() || isAudio();
    }

    public boolean isApp() {
        return AppsProvider.AUTHORITY.equals(authority);
    }

    public static void setTypeIndex(RootInfo info, String rootId) {
        info.setAuthority(MediaProvider.AUTHORITY);
        info.setRootId(rootId);
    }

    public static boolean isStorage(RootInfo root){
        return root.isHome() || root.isPhoneStorage() || root.isStorage();
    }

    public boolean isImages() {
        return MediaProvider.AUTHORITY.equals(authority)
                && MediaProvider.TYPE_IMAGES_ROOT.equals(rootId);
    }

    public boolean isVideos() {
        return MediaProvider.AUTHORITY.equals(authority)
                && MediaProvider.TYPE_VIDEOS_ROOT.equals(rootId);
    }

    public boolean isAudio() {
        return MediaProvider.AUTHORITY.equals(authority)
                && MediaProvider.TYPE_AUDIO_ROOT.equals(rootId);
    }

    public boolean isAppPackage() {
        return AppsProvider.AUTHORITY.equals(authority)
                && (AppsProvider.ROOT_ID_USER_APP.equals(rootId)
                || AppsProvider.ROOT_ID_SYSTEM_APP.equals(rootId));
    }

    public boolean isUserApp() {
        return AppsProvider.AUTHORITY.equals(authority)
                && AppsProvider.ROOT_ID_USER_APP.equals(rootId);
    }

    public boolean isSystemApp() {
        return AppsProvider.AUTHORITY.equals(authority)
                && AppsProvider.ROOT_ID_SYSTEM_APP.equals(rootId);
    }

    public boolean isAppProcess() {
        return AppsProvider.AUTHORITY.equals(authority)
                && AppsProvider.ROOT_ID_PROCESS.equals(rootId);
    }

    public Drawable loadDrawerIcon(Context context) {
        if (derivedIcon != 0) {
            return IconUtils.applyTintAttr(context, derivedIcon,
                    android.R.attr.textColorPrimary);
        } else {
            return IconUtils.loadPackageIcon(context, authority, icon);
        }
    }
}
