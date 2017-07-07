package com.hb.xtvfileexplorer.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.hb.xtvfileexplorer.BuildConfig;
import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.utils.Utils;

import java.io.FileNotFoundException;


public class MediaProvider extends DocumentsProvider {

    private static final String COLUMN_PATH = "path";
    public static final int FLAG_EMPTY = 1 << 16;
    public static final int FLAG_DIR_HIDE_GRID_TITLES = 1 << 17;

    public static final String TYPE_IMAGES_ROOT = "images_root";
    public static final String TYPE_IMAGES_BUCKET = "images_bucket";
    public static final String TYPE_IMAGE = "image";

    public static final String TYPE_VIDEOS_ROOT = "videos_root";
    public static final String TYPE_VIDEOS_BUCKET = "videos_bucket";
    public static final String TYPE_VIDEO = "video";

    public static final String TYPE_AUDIO_ROOT = "audio_root";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_ARTIST = "artist";
    public static final String TYPE_ALBUM = "album";


    private static boolean sReturnedImagesEmpty = false;
    private static boolean sReturnedVideosEmpty = false;
    private static boolean sReturnedAudioEmpty = false;

    private static final String IMAGE_MIME_TYPES = joinNewline("image/*");

    private static final String VIDEO_MIME_TYPES = joinNewline("video/*");

    private static final String AUDIO_MIME_TYPES = joinNewline(
            "audio/*", "application/ogg", "application/x-flac");

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".media.documents";

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON,
            Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_MIME_TYPES
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE,
    };

    private static String joinNewline(String... args) {
        return TextUtils.join("\n", args);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private static String getDocIdForIdent(String type, long id) {
        return type + ":" + id;
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private void copyNotificationUri(MatrixCursor result, Uri uri) {
        result.setNotificationUri(getContext().getContentResolver(), uri);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        includeImagesRoot(result);
        includeVideosRoot(result);
        includeAudioRoot(result);
        return result;
    }

    private boolean isEmpty(Uri uri) {
        final ContentResolver resolver = getContext().getContentResolver();
        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, new String[] {
                    BaseColumns._ID }, null, null, null);
            return (cursor == null) || (cursor.getCount() == 0);
        } finally {
            Utils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
    }

    private void includeImagesRoot(MatrixCursor result) {
        int flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_RECENTS;
        if (isEmpty(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
            flags |= FLAG_EMPTY;
            sReturnedImagesEmpty = true;
        }
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, TYPE_IMAGES_ROOT);
        row.add(Root.COLUMN_FLAGS, flags);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_images));
        row.add(Root.COLUMN_DOCUMENT_ID, TYPE_IMAGES_ROOT);
        row.add(Root.COLUMN_MIME_TYPES, IMAGE_MIME_TYPES);
    }

    private void includeVideosRoot(MatrixCursor result) {
        int flags = Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_RECENTS;
        if (isEmpty(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) {
            flags |= FLAG_EMPTY;
            sReturnedVideosEmpty = true;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, TYPE_VIDEOS_ROOT);
        row.add(Root.COLUMN_FLAGS, flags);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_videos));
        row.add(Root.COLUMN_DOCUMENT_ID, TYPE_VIDEOS_ROOT);
        row.add(Root.COLUMN_MIME_TYPES, VIDEO_MIME_TYPES);
    }

    private void includeAudioRoot(MatrixCursor result) {
        int flags = Root.FLAG_LOCAL_ONLY;
        if (isEmpty(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
            flags |= FLAG_EMPTY;
            sReturnedAudioEmpty = true;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, TYPE_AUDIO_ROOT);
        row.add(Root.COLUMN_FLAGS, flags);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_audio));
        row.add(Root.COLUMN_DOCUMENT_ID, TYPE_AUDIO_ROOT);
        row.add(Root.COLUMN_MIME_TYPES, AUDIO_MIME_TYPES);
    }

    private static Identify getIdentifyForDocId(String docId) {
        final Identify ident = new Identify();
        final int split = docId.indexOf(':');
        if (split == -1) {
            ident.type = docId;
            ident.id = -1;
        } else {
            ident.type = docId.substring(0, split);
            ident.id = Long.parseLong(docId.substring(split + 1));
        }
        return ident;
    }

    private String cleanUpMediaDisplayName(String displayName) {
        if (!MediaStore.UNKNOWN_STRING.equals(displayName)) {
            return displayName;
        }
        return displayName;
    }

    private void includeImagesRootDocument(MatrixCursor result) {
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, TYPE_IMAGES_ROOT);
        row.add(Document.COLUMN_DISPLAY_NAME, getContext().getString(R.string.root_images));
        row.add(Document.COLUMN_FLAGS,
                Document.FLAG_DIR_PREFERS_GRID | Document.FLAG_DIR_PREFERS_LAST_MODIFIED | Document.FLAG_SUPPORTS_DELETE);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
    }

    private void includeVideosRootDocument(MatrixCursor result) {
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, TYPE_VIDEOS_ROOT);
        row.add(Document.COLUMN_DISPLAY_NAME, getContext().getString(R.string.root_videos));
        row.add(Document.COLUMN_FLAGS,
                Document.FLAG_DIR_PREFERS_GRID | Document.FLAG_DIR_PREFERS_LAST_MODIFIED | Document.FLAG_SUPPORTS_DELETE);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
    }

    private void includeAudioRootDocument(MatrixCursor result) {
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, TYPE_AUDIO_ROOT);
        row.add(Document.COLUMN_DISPLAY_NAME, getContext().getString(R.string.root_audio));
        row.add(Document.COLUMN_FLAGS,
                Document.FLAG_DIR_PREFERS_GRID | Document.FLAG_DIR_PREFERS_LAST_MODIFIED | Document.FLAG_SUPPORTS_DELETE);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
    }

    private interface ImagesBucketQuery {
        String[] PROJECTION = new String[] {
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_MODIFIED };
        String SORT_ORDER = MediaStore.Images.ImageColumns.BUCKET_ID + ", " + MediaStore.Images.ImageColumns.DATE_MODIFIED
                + " DESC";

        int BUCKET_ID = 0;
        int BUCKET_DISPLAY_NAME = 1;
        int DATE_MODIFIED = 2;
    }

    private void includeImagesBucket(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(ImagesBucketQuery.BUCKET_ID);
        final String docId = getDocIdForIdent(TYPE_IMAGES_BUCKET, id);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME,
                cursor.getString(ImagesBucketQuery.BUCKET_DISPLAY_NAME));
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        row.add(Document.COLUMN_LAST_MODIFIED,
                cursor.getLong(ImagesBucketQuery.DATE_MODIFIED) * DateUtils.SECOND_IN_MILLIS);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_PREFERS_GRID
                | Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_DIR_PREFERS_LAST_MODIFIED
                | FLAG_DIR_HIDE_GRID_TITLES | Document.FLAG_SUPPORTS_DELETE);
    }

    private interface ImageQuery {
        String[] PROJECTION = new String[] {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                MediaStore.Images.ImageColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.SIZE,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.DATE_MODIFIED };

        int _ID = 0;
        int DISPLAY_NAME = 1;
        int MIME_TYPE = 2;
        int SIZE = 3;
        int DATA = 4;
        int DATE_MODIFIED = 5;
    }

    private void includeImage(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(ImageQuery._ID);
        final String docId = getDocIdForIdent(TYPE_IMAGE, id);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, cursor.getString(ImageQuery.DISPLAY_NAME));
        row.add(Document.COLUMN_SIZE, cursor.getLong(ImageQuery.SIZE));
        row.add(Document.COLUMN_MIME_TYPE, cursor.getString(ImageQuery.MIME_TYPE));
        row.add(COLUMN_PATH, cursor.getString(ImageQuery.DATA));
        row.add(Document.COLUMN_LAST_MODIFIED,
                cursor.getLong(ImageQuery.DATE_MODIFIED) * DateUtils.SECOND_IN_MILLIS);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_SUPPORTS_DELETE);
    }

    private interface VideosBucketQuery {
        String[] PROJECTION = new String[] {
                MediaStore.Video.VideoColumns.BUCKET_ID,
                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Video.VideoColumns.DATE_MODIFIED };
        String SORT_ORDER = MediaStore.Video.VideoColumns.BUCKET_ID + ", " + MediaStore.Video.VideoColumns.DATE_MODIFIED
                + " DESC";

        int BUCKET_ID = 0;
        int BUCKET_DISPLAY_NAME = 1;
        int DATE_MODIFIED = 2;
    }

    private void includeVideosBucket(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(VideosBucketQuery.BUCKET_ID);
        final String docId = getDocIdForIdent(TYPE_VIDEOS_BUCKET, id);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME,
                cursor.getString(VideosBucketQuery.BUCKET_DISPLAY_NAME));
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        row.add(Document.COLUMN_LAST_MODIFIED,
                cursor.getLong(VideosBucketQuery.DATE_MODIFIED) * DateUtils.SECOND_IN_MILLIS);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_PREFERS_GRID
                | Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_DIR_PREFERS_LAST_MODIFIED
                | FLAG_DIR_HIDE_GRID_TITLES | Document.FLAG_SUPPORTS_DELETE);
    }

    private interface VideoQuery {
        String[] PROJECTION = new String[] {
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                MediaStore.Video.VideoColumns.MIME_TYPE,
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.VideoColumns.DATE_MODIFIED };

        int _ID = 0;
        int DISPLAY_NAME = 1;
        int MIME_TYPE = 2;
        int SIZE = 3;
        int DATA = 4;
        int DATE_MODIFIED = 5;
    }

    private void includeVideo(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(VideoQuery._ID);
        final String docId = getDocIdForIdent(TYPE_VIDEO, id);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, cursor.getString(VideoQuery.DISPLAY_NAME));
        row.add(Document.COLUMN_SIZE, cursor.getLong(VideoQuery.SIZE));
        row.add(Document.COLUMN_MIME_TYPE, cursor.getString(VideoQuery.MIME_TYPE));
        row.add(COLUMN_PATH, cursor.getString(VideoQuery.DATA));
        row.add(Document.COLUMN_LAST_MODIFIED,
                cursor.getLong(VideoQuery.DATE_MODIFIED) * DateUtils.SECOND_IN_MILLIS);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_SUPPORTS_DELETE);
    }

    private interface ArtistQuery {
        String[] PROJECTION = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.ArtistColumns.ARTIST };

        int _ID = 0;
        int ARTIST = 1;
    }

    private void includeArtist(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(ArtistQuery._ID);
        final String docId = getDocIdForIdent(TYPE_ARTIST, id);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, cleanUpMediaDisplayName(cursor.getString(ArtistQuery.ARTIST)));
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR );
    }

    private interface AlbumQuery {
        String[] PROJECTION = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.AlbumColumns.ALBUM };

        int _ID = 0;
        int ALBUM = 1;
    }

    private void includeAlbum(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(AlbumQuery._ID);
        final String docId = getDocIdForIdent(TYPE_ALBUM, id);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME,
                cleanUpMediaDisplayName(cursor.getString(AlbumQuery.ALBUM)));
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_PREFERS_GRID
                | Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_DIR_PREFERS_LAST_MODIFIED | Document.FLAG_SUPPORTS_DELETE);
    }

    private interface SongQuery {
        String[] PROJECTION = new String[] {
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.MIME_TYPE,
                MediaStore.Audio.AudioColumns.SIZE,
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.DATE_MODIFIED };

        int _ID = 0;
        int TITLE = 1;
        int MIME_TYPE = 2;
        int SIZE = 3;
        int DATA = 4;
        int DATE_MODIFIED = 5;
    }

    private void includeAudio(MatrixCursor result, Cursor cursor) {
        final long id = cursor.getLong(SongQuery._ID);
        final String docId = getDocIdForIdent(TYPE_AUDIO, id);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, cursor.getString(SongQuery.TITLE));
        row.add(Document.COLUMN_SIZE, cursor.getLong(SongQuery.SIZE));
        row.add(Document.COLUMN_MIME_TYPE, cursor.getString(SongQuery.MIME_TYPE));
        row.add(COLUMN_PATH, cursor.getString(SongQuery.DATA));
        row.add(Document.COLUMN_LAST_MODIFIED,
                cursor.getLong(SongQuery.DATE_MODIFIED) * DateUtils.SECOND_IN_MILLIS);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_SUPPORTS_DELETE);
    }


    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        final Identify ident = getIdentifyForDocId(documentId);
        Cursor cursor = null;
        try {
            if (TYPE_IMAGES_ROOT.equals(ident.type)) {
                // single root
                includeImagesRootDocument(result);
            } else if (TYPE_IMAGES_BUCKET.equals(ident.type)) {
                // single bucket
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ImagesBucketQuery.PROJECTION, MediaStore.Images.ImageColumns.BUCKET_ID + "=" + ident.id,
                        null, ImagesBucketQuery.SORT_ORDER);
                copyNotificationUri(result, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (cursor != null && cursor.moveToFirst()) {
                    includeImagesBucket(result, cursor);
                }
            } else if (TYPE_IMAGE.equals(ident.type)) {
                // single image
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ImageQuery.PROJECTION, BaseColumns._ID + "=" + ident.id, null,
                        null);
                copyNotificationUri(result, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (cursor != null && cursor.moveToFirst()) {
                    includeImage(result, cursor);
                }
            } else if (TYPE_VIDEOS_ROOT.equals(ident.type)) {
                // single root
                includeVideosRootDocument(result);
            } else if (TYPE_VIDEOS_BUCKET.equals(ident.type)) {
                // single bucket
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        VideosBucketQuery.PROJECTION, MediaStore.Video.VideoColumns.BUCKET_ID + "=" + ident.id,
                        null, VideosBucketQuery.SORT_ORDER);
                copyNotificationUri(result, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (cursor != null && cursor.moveToFirst()) {
                    includeVideosBucket(result, cursor);
                }
            } else if (TYPE_VIDEO.equals(ident.type)) {
                // single video
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        VideoQuery.PROJECTION, BaseColumns._ID + "=" + ident.id, null,
                        null);
                copyNotificationUri(result, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                if (cursor != null && cursor.moveToFirst()) {
                    includeVideo(result, cursor);
                }
            } else if (TYPE_AUDIO_ROOT.equals(ident.type)) {
                // single root
                includeAudioRootDocument(result);
            } else if (TYPE_ARTIST.equals(ident.type)) {
                // single artist
                cursor = resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        ArtistQuery.PROJECTION, BaseColumns._ID + "=" + ident.id, null,
                        null);
                copyNotificationUri(result, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI);
                if (cursor != null && cursor.moveToFirst()) {
                    includeArtist(result, cursor);
                }
            } else if (TYPE_ALBUM.equals(ident.type)) {
                // single album
                cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        AlbumQuery.PROJECTION, BaseColumns._ID + "=" + ident.id, null,
                        null);
                copyNotificationUri(result, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI);
                if (cursor != null && cursor.moveToFirst()) {
                    includeAlbum(result, cursor);
                }
            } else if (TYPE_AUDIO.equals(ident.type)) {
                // single song
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        SongQuery.PROJECTION, BaseColumns._ID + "=" + ident.id, null,
                        null);
                copyNotificationUri(result, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                if (cursor != null && cursor.moveToFirst()) {
                    includeAudio(result, cursor);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported document " + documentId);
            }
        } finally {
            Utils.closeQuietly(cursor);
        }
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        final ContentResolver resolver = getContext().getContentResolver();
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        final Identify ident = getIdentifyForDocId(parentDocumentId);

        final long token = Binder.clearCallingIdentity();
        Cursor cursor = null;
        try {
            if (TYPE_IMAGES_ROOT.equals(ident.type)) {
                // include all unique buckets
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ImagesBucketQuery.PROJECTION, null, null, ImagesBucketQuery.SORT_ORDER);
                // multiple orders
                copyNotificationUri(result, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                long lastId = Long.MIN_VALUE;
                while (cursor.moveToNext()) {
                    final long id = cursor.getLong(ImagesBucketQuery.BUCKET_ID);
                    if (lastId != id) {
                        includeImagesBucket(result, cursor);
                        lastId = id;
                    }
                }
            } else if (TYPE_IMAGES_BUCKET.equals(ident.type)) {
                // include images under bucket
                cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ImageQuery.PROJECTION, MediaStore.Images.ImageColumns.BUCKET_ID + "=" + ident.id,
                        null, null);
                copyNotificationUri(result, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                while (cursor.moveToNext()) {
                    includeImage(result, cursor);
                }
            } else if (TYPE_VIDEOS_ROOT.equals(ident.type)) {
                // include all unique buckets
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        VideosBucketQuery.PROJECTION, null, null, VideosBucketQuery.SORT_ORDER);
                copyNotificationUri(result, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                long lastId = Long.MIN_VALUE;
                while (cursor.moveToNext()) {
                    final long id = cursor.getLong(VideosBucketQuery.BUCKET_ID);
                    if (lastId != id) {
                        includeVideosBucket(result, cursor);
                        lastId = id;
                    }
                }
            } else if (TYPE_VIDEOS_BUCKET.equals(ident.type)) {
                // include videos under bucket
                cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        VideoQuery.PROJECTION, MediaStore.Video.VideoColumns.BUCKET_ID + "=" + ident.id,
                        null, null);
                copyNotificationUri(result, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                while (cursor.moveToNext()) {
                    includeVideo(result, cursor);
                }
            } else if (TYPE_AUDIO_ROOT.equals(ident.type)) {
                // include all albums under artist
                cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        AlbumQuery.PROJECTION, null, null, null);
                copyNotificationUri(result, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI);
                while (cursor.moveToNext()) {
                    includeAlbum(result, cursor);
                }
            } else if (TYPE_ARTIST.equals(ident.type)) {
                // include all albums under artist
                cursor = resolver.query(MediaStore.Audio.Artists.Albums.getContentUri("external", ident.id),
                        AlbumQuery.PROJECTION, null, null, null);
                copyNotificationUri(result, MediaStore.Audio.Artists.Albums.getContentUri("external", ident.id));
                while (cursor.moveToNext()) {
                    includeAlbum(result, cursor);
                }
            } else if (TYPE_ALBUM.equals(ident.type)) {
                // include all songs under album
                cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        SongQuery.PROJECTION, MediaStore.Audio.AudioColumns.ALBUM_ID + "=" + ident.id,
                        null, null);
                copyNotificationUri(result, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                while (cursor.moveToNext()) {
                    includeAudio(result, cursor);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported document " + parentDocumentId);
            }
        } finally {
            Utils.closeQuietly(cursor);
            Binder.restoreCallingIdentity(token);
        }
        return result;
    }

    private Uri getUriForDocumentId(String docId) {
        final Identify ident = getIdentifyForDocId(docId);
        if (TYPE_IMAGE.equals(ident.type) && ident.id != -1) {
            return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ident.id);
        } else if (TYPE_VIDEO.equals(ident.type) && ident.id != -1) {
            return ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ident.id);
        } else if (TYPE_AUDIO.equals(ident.type) && ident.id != -1) {
            return ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ident.id);
        } else {
            throw new UnsupportedOperationException("Unsupported document " + docId);
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new IllegalArgumentException("Media is read-only");
        }

        final Uri target = getUriForDocumentId(documentId);

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            return getContext().getContentResolver().openFileDescriptor(target, mode);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public void deleteDocument(String docId) throws FileNotFoundException {
        final Uri target = getUriForDocumentId(docId);

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            getContext().getContentResolver().delete(target, null, null);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static class Identify {
        public String type;
        public long id;
    }
}
