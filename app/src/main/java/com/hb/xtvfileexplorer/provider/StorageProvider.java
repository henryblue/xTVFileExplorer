package com.hb.xtvfileexplorer.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import com.hb.xtvfileexplorer.BuildConfig;
import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.archive.DocumentArchiveHelper;
import com.hb.xtvfileexplorer.misc.MimePredicate;
import com.hb.xtvfileexplorer.misc.StorageUtils;
import com.hb.xtvfileexplorer.misc.StorageVolume;
import com.hb.xtvfileexplorer.utils.FileUtils;
import com.hb.xtvfileexplorer.utils.Utils;

import net.jcip.annotations.GuardedBy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.hb.xtvfileexplorer.provider.AppsProvider.isTelevision;
import static com.hb.xtvfileexplorer.provider.MediaProvider.FLAG_EMPTY;


public class StorageProvider extends DocumentsProvider {

    private static final String TAG = "ExternalStorage";
    private static final String COLUMN_PATH = "path";
    public static final int FLAG_SUPER_ADVANCED = 1 << 91;
    public static final int FLAG_ADVANCED = 1 << 17;
    public static final int FLAG_SUPPORTS_EDIT = 1 << 18;

    private static final boolean LOG_INOTIFY = false;

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".storage.documents";


    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID, Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_CAPACITY_BYTES, COLUMN_PATH,
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, COLUMN_PATH, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Document.COLUMN_SUMMARY,
    };

    private static class RootInfo {
        public String rootId;
        public int flags;
        public String title;
        public String docId;
        public File path;
        public File visiblePath;
        public boolean reportAvailableBytes;
    }

    public static final String ROOT_ID_HOME = "home";
    public static final String ROOT_ID_PRIMARY_EMULATED = "primary";  // internal Storage
    public static final String ROOT_ID_SECONDARY = "secondary";  // external Storage
    public static final String ROOT_ID_PHONE = "phone";          // device Storage

    private static final String DIR_ROOT = "/";

    private Handler mHandler;
    private DocumentArchiveHelper mArchiveHelper;

    private final Object mRootsLock = new Object();

    @GuardedBy("mRootsLock")
    private ArrayMap<String, RootInfo> mRoots = new ArrayMap<>();

    @GuardedBy("mObservers")
    private ArrayMap<File, DirectoryObserver> mObservers = new ArrayMap<>();

    @Override
    public boolean onCreate() {
        mHandler = new Handler();
        mArchiveHelper = new DocumentArchiveHelper(this, (char) 0);
        updateRoots();
        return true;
    }

    private void updateRoots() {
        synchronized (mRootsLock) {
            updateVolumesLocked();
            includeOtherRoot();
            notifyRootsChanged(getContext());
        }
    }

    private void updateVolumesLocked() {
        mRoots.clear();
        int count = 0;
        StorageUtils storageUtils = new StorageUtils(getContext());
        for (StorageVolume storageVolume : storageUtils.getStorageMounts()) {
            final File path = storageVolume.getPathFile();
            String state = EnvironmentCompat.getStorageState(path);
            final boolean mounted = Environment.MEDIA_MOUNTED.equals(state)
                    || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
            if (!mounted)
                continue;
            final String rootId;
            String title = null;
            if (storageVolume.isPrimary()) {
                rootId = ROOT_ID_PRIMARY_EMULATED;
                if (getContext() != null) {
                    title = getContext().getString(R.string.root_internal_storage);
                }
            } else if (storageVolume.getUuid() != null) {
                rootId = ROOT_ID_SECONDARY + storageVolume.getUuid();
                String label = storageVolume.getUserLabel();
                title = !TextUtils.isEmpty(label) ? label
                        : getContext().getString(R.string.root_external_storage)
                        + (count > 0 ? " " + count : "");
                count++;
            } else {
                Log.d(TAG, "Missing UUID for " + storageVolume.getPath() + "; skipping");
                continue;
            }

            if (mRoots.containsKey(rootId)) {
                Log.w(TAG, "Duplicate UUID " + rootId + "; skipping");
                continue;
            }

            try {
                if (null == path.listFiles()) {
                    continue;
                }
                final RootInfo root = new RootInfo();
                mRoots.put(rootId, root);
                root.rootId = rootId;
                root.flags = Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_SEARCH
                        | Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_IS_CHILD;
                root.title = title;
                root.path = path;
                root.docId = getDocIdForFile(path);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void includeOtherRoot() {
        try {
            final String rootId = ROOT_ID_HOME;
            final File path = Utils.hasNougat() ? Environment.getRootDirectory() : new File(DIR_ROOT);
            final RootInfo root = new RootInfo();
            mRoots.put(rootId, root);
            root.rootId = rootId;
            root.flags = FLAG_SUPER_ADVANCED | Root.FLAG_SUPPORTS_SEARCH
                    | Root.FLAG_LOCAL_ONLY | FLAG_ADVANCED;
            if (isEmpty(path)) {
                root.flags |= FLAG_EMPTY;
            }
            root.title = getContext().getString(R.string.root_phone_storage);
            root.path = path;
            root.docId = getDocIdForFile(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getDocIdForFile(File file) throws FileNotFoundException {
        return getDocIdForFileMaybeCreate(file, false);
    }

    private String getDocIdForFileMaybeCreate(File file, boolean createNewDir)
            throws FileNotFoundException {
        String path = file.getAbsolutePath();
        // Find the most-specific root path
        String mostSpecificId = null;
        String mostSpecificPath = null;
        synchronized (mRootsLock) {
            for (int i = 0; i < mRoots.size(); i++) {
                final String rootId = mRoots.keyAt(i);
                final String rootPath = mRoots.valueAt(i).path.getAbsolutePath();
                if (path.startsWith(rootPath) && (mostSpecificPath == null
                        || rootPath.length() > mostSpecificPath.length())) {
                    mostSpecificId = rootId;
                    mostSpecificPath = rootPath;
                }
            }
        }

        if (mostSpecificPath == null) {
            throw new FileNotFoundException("Failed to find root that contains " + path);
        }

        // Start at first char of path under root
        final String rootPath = mostSpecificPath;
        if (rootPath.equals(path)) {
            path = "";
        } else if (rootPath.endsWith("/")) {
            path = path.substring(rootPath.length());
        } else {
            path = path.substring(rootPath.length() + 1);
        }

        if (!file.exists() && createNewDir) {
            Log.i(TAG, "Creating new directory " + file);
            if (!file.mkdir()) {
                Log.e(TAG, "Could not create directory " + file);
            }
        }

        return mostSpecificId + ':' + path;
    }

    private boolean isEmpty(File file) {
        return null != file  && (!file.isDirectory() || null == file.list() || file.list().length == 0);
    }

    public static void notifyRootsChanged(Context context) {
        context.getContentResolver()
                .notifyChange(DocumentsContract.buildRootsUri(AUTHORITY), null, false);
    }

    public static void notifyDocumentsChanged(Context context, String rootId) {
        Uri uri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, rootId);
        context.getContentResolver().notifyChange(uri, null, false);
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private File getFileForDocId(String docId) throws FileNotFoundException {
        return getFileForDocId(docId, false);
    }

    private File getFileForDocId(String docId, boolean visible) throws FileNotFoundException {
        final int splitIndex = docId.indexOf(':', 1);
        final String tag = docId.substring(0, splitIndex);
        final String path = docId.substring(splitIndex + 1);

        RootInfo root;
        synchronized (mRootsLock) {
            root = mRoots.get(tag);
        }
        if (root == null) {
            throw new FileNotFoundException("No root for " + tag);
        }

        File target = visible ? root.visiblePath : root.path;
        if (target == null) {
            return null;
        }
        if (!target.exists()) {
            target.mkdirs();
        }
        target = new File(target, path);
        if (!target.exists()) {
            throw new FileNotFoundException("Missing file for " + docId + " at " + target);
        }
        return target;
    }


    private void includeFile(MatrixCursor result, String docId, File file)
            throws FileNotFoundException {
        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }

        DocumentFile documentFile = FileUtils.getDocumentFile(getContext(), docId, file);

        int flags = 0;

        if (documentFile.canWrite()) {
            if (file.isDirectory()) {
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
            } else {
                flags |= Document.FLAG_SUPPORTS_WRITE;
            }
            flags |= Document.FLAG_SUPPORTS_DELETE;
            flags |= Document.FLAG_SUPPORTS_RENAME;
            flags |= Document.FLAG_SUPPORTS_MOVE;
            flags |= Document.FLAG_SUPPORTS_COPY;
            flags |= FLAG_SUPPORTS_EDIT;

            if(isTelevision()) {
                flags |= Document.FLAG_DIR_PREFERS_GRID;
            }
        }

        final String mimeType = FileUtils.getTypeForFile(file);

        final String displayName = file.getName();
        if (!TextUtils.isEmpty(displayName)) {
            if(displayName.charAt(0) == '.'){
                return;
            }
        }
        if(MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, mimeType)){
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(COLUMN_PATH, file.getAbsolutePath());
        row.add(Document.COLUMN_FLAGS, flags);
        if(file.isDirectory() && null != file.list()){
            row.add(Document.COLUMN_SUMMARY, FileUtils.formatFileCount(file.list().length));
        }

        // Only publish dates reasonably after epoch
        long lastModified = file.lastModified();
        if (lastModified > 31536000000L) {
            row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
        }
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        synchronized (mRootsLock) {
            for (RootInfo root : mRoots.values()) {
                final MatrixCursor.RowBuilder row = result.newRow();
                row.add(Root.COLUMN_ROOT_ID, root.rootId);
                row.add(Root.COLUMN_FLAGS, root.flags);
                row.add(Root.COLUMN_TITLE, root.title);
                row.add(Root.COLUMN_DOCUMENT_ID, root.docId);
                row.add(COLUMN_PATH, root.path);
                if(ROOT_ID_PRIMARY_EMULATED.equals(root.rootId)
                        || root.rootId.startsWith(ROOT_ID_SECONDARY)
                        || root.rootId.startsWith(ROOT_ID_PHONE)) {
                    final File file = root.rootId.startsWith(ROOT_ID_PHONE)
                            ? Environment.getRootDirectory() : root.path;
                    row.add(Root.COLUMN_AVAILABLE_BYTES, file.getFreeSpace());
                    row.add(Root.COLUMN_CAPACITY_BYTES, file.getTotalSpace());
                }
            }
        }
        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        final File parent;
        try {
            if (mArchiveHelper.isArchivedDocument(documentId)) {
                return mArchiveHelper.isChildDocument(parentDocumentId, documentId);
            }
            // Archives do not contain regular files.
            if (mArchiveHelper.isArchivedDocument(parentDocumentId)) {
                return false;
            }
            parent = getFileForDocId(parentDocumentId).getCanonicalFile();
            final File doc = getFileForDocId(documentId).getCanonicalFile();
            return FileUtils.contains(parent, doc);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to determine if " + documentId + " is child of "
                            + parentDocumentId + ": " + e);
        }
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.queryDocument(documentId, projection);
        }

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(parentDocumentId) ||
                DocumentArchiveHelper.isSupportedArchiveType(getDocumentType(parentDocumentId))) {
            return mArchiveHelper.queryChildDocuments(parentDocumentId, projection, sortOrder);
        }

        final File parent = getFileForDocId(parentDocumentId);
        final MatrixCursor result = new DirectoryCursor(
                resolveDocumentProjection(projection), parentDocumentId, parent);
        for (File file : parent.listFiles()) {
            includeFile(result, null, file);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        if (mArchiveHelper.isArchivedDocument(documentId)) {
            return mArchiveHelper.openDocument(documentId, mode, signal);
        }

        final File file = getFileForDocId(documentId);
        if(Utils.hasKitKat()){
            final int pfdMode = ParcelFileDescriptor.parseMode(mode);
            if (pfdMode == ParcelFileDescriptor.MODE_READ_ONLY) {
                return ParcelFileDescriptor.open(file, pfdMode);
            } else {
                try {
                    // When finished writing, kick off media scanner
                    return ParcelFileDescriptor.open(file, pfdMode, mHandler, new ParcelFileDescriptor.OnCloseListener() {
                        @Override
                        public void onClose(IOException e) {
                            FileUtils.updateMediaStore(getContext(), file.getPath());
                        }
                    });
                } catch (IOException e) {
                    throw new FileNotFoundException("Failed to open for writing: " + e);
                }
            }
        }
        else{
            return ParcelFileDescriptor.open(file, FileUtils.parseMode(mode));
        }
    }

    private void startObserving(File file, Uri notifyUri) {
        synchronized (mObservers) {
            DirectoryObserver observer = mObservers.get(file);
            if (observer == null) {
                observer = new DirectoryObserver(
                        file, getContext().getContentResolver(), notifyUri);
                observer.startWatching();
                mObservers.put(file, observer);
            }
            observer.mRefCount++;

            if (LOG_INOTIFY) Log.d(TAG, "after start: " + observer);
        }
    }

    private void stopObserving(File file) {
        synchronized (mObservers) {
            DirectoryObserver observer = mObservers.get(file);
            if (observer == null) return;

            observer.mRefCount--;
            if (observer.mRefCount == 0) {
                mObservers.remove(file);
                observer.stopWatching();
            }

            if (LOG_INOTIFY) Log.d(TAG, "after stop: " + observer);
        }
    }

    private class DirectoryCursor extends MatrixCursor {
        private final File mFile;

        public DirectoryCursor(String[] columnNames, String docId, File file) {
            super(columnNames);

            final Uri notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, docId);
            setNotificationUri(getContext().getContentResolver(), notifyUri);

            mFile = file;
            startObserving(mFile, notifyUri);
        }

        @Override
        public void close() {
            super.close();
            stopObserving(mFile);
        }
    }

    private class DirectoryObserver extends FileObserver {
        private static final int NOTIFY_EVENTS = ATTRIB | CLOSE_WRITE | MOVED_FROM | MOVED_TO
                | CREATE | DELETE | DELETE_SELF | MOVE_SELF;

        private final File mFile;
        private final ContentResolver mResolver;
        private final Uri mNotifyUri;

        private int mRefCount = 0;

        public DirectoryObserver(File file, ContentResolver resolver, Uri notifyUri) {
            super(file.getAbsolutePath(), NOTIFY_EVENTS);
            mFile = file;
            mResolver = resolver;
            mNotifyUri = notifyUri;
        }

        @Override
        public void onEvent(int event, String path) {
            if ((event & NOTIFY_EVENTS) != 0) {
                if (LOG_INOTIFY) Log.d(TAG, "onEvent() " + event + " at " + path);
                switch ((event & NOTIFY_EVENTS)){
                    case MOVED_FROM:
                    case MOVED_TO:
                    case CREATE:
                    case DELETE:
                        mResolver.notifyChange(mNotifyUri, null, false);
                        FileUtils.updateMediaStore(getContext(), FileUtils.makeFilePath(mFile, path));
                        break;
                }
            }
        }

        @Override
        public String toString() {
            return "DirectoryObserver{file=" + mFile.getAbsolutePath() + ", ref=" + mRefCount + "}";
        }
    }
}
