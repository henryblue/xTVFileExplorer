package com.hb.xtvfileexplorer.provider;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.SparseArray;

import com.hb.xtvfileexplorer.BuildConfig;
import com.hb.xtvfileexplorer.R;
import com.hb.xtvfileexplorer.utils.StorageUtils;
import com.hb.xtvfileexplorer.utils.Utils;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class AppsProvider extends DocumentsProvider {

    public static final String ROOT_ID_USER_APP = "user_apps:";
    public static final String ROOT_ID_SYSTEM_APP = "system_apps:";
    public static final String ROOT_ID_PROCESS = "process:";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".apps.documents";

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON,
            Root.COLUMN_TITLE, Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES, Root.COLUMN_CAPACITY_BYTES,
    };

    private final static String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_SUMMARY,
            Document.COLUMN_FLAGS, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED};
    private static boolean mIsTelevision;


    private PackageManager mPackageManager;
    private ActivityManager mActivityManager;

    private static SparseArray<String> processTypeCache;

    static {
        processTypeCache = new SparseArray<>();
        processTypeCache.put(ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE, "Service");
        processTypeCache.put(ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND, "Background");
        processTypeCache.put(ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND, "Foreground");
        processTypeCache.put(ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE, "Visible");
        processTypeCache.put(ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY, "Empty");
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            mPackageManager = context.getPackageManager();
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            mIsTelevision = Utils.isTelevision(context);
        }
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        StorageUtils storageUtils = new StorageUtils(getContext());
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, ROOT_ID_USER_APP);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY  | Root.FLAG_SUPPORTS_SEARCH);
        row.add(Root.COLUMN_ICON, R.drawable.ic_root_apps);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_apps));
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_USER_APP);
        row.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartitionSize(StorageUtils.PARTITION_DATA, false));
        row.add(Root.COLUMN_CAPACITY_BYTES, storageUtils.getPartitionSize(StorageUtils.PARTITION_DATA, true));

        final MatrixCursor.RowBuilder row1 = result.newRow();
        row1.add(Root.COLUMN_ROOT_ID, ROOT_ID_SYSTEM_APP);
        row1.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY | Root.FLAG_SUPPORTS_SEARCH);
        row1.add(Root.COLUMN_ICON, R.drawable.ic_root_apps);
        row1.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_system_apps));
        row1.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_SYSTEM_APP);
        row1.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartitionSize(StorageUtils.PARTITION_DATA, false));
        row1.add(Root.COLUMN_CAPACITY_BYTES, storageUtils.getPartitionSize(StorageUtils.PARTITION_DATA, true));

        final MatrixCursor.RowBuilder row2 = result.newRow();
        row2.add(Root.COLUMN_ROOT_ID, ROOT_ID_PROCESS);
        row2.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY  | Root.FLAG_SUPPORTS_SEARCH);
        row2.add(Root.COLUMN_ICON, R.drawable.ic_root_process);
        row2.add(Root.COLUMN_TITLE, getContext().getString(R.string.root_processes));
        row2.add(Root.COLUMN_DOCUMENT_ID, ROOT_ID_PROCESS);
        row2.add(Root.COLUMN_AVAILABLE_BYTES, storageUtils.getPartitionSize(StorageUtils.PARTITION_RAM, false));
        row2.add(Root.COLUMN_CAPACITY_BYTES, storageUtils.getPartitionSize(StorageUtils.PARTITION_RAM, true));
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeDefaultDocument(result, documentId);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String docId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        final MatrixCursor result = new DocumentCursor(resolveDocumentProjection(projection), docId);

        // Delegate to real provider
        final long token = Binder.clearCallingIdentity();
        try {
            if (docId.startsWith(ROOT_ID_USER_APP)) {
                List<PackageInfo> allAppList = mPackageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
                for (PackageInfo packageInfo : allAppList) {
                    includeAppFromPackage(result, docId, packageInfo, false, null);
                }
            }
            else if (docId.startsWith(ROOT_ID_SYSTEM_APP)) {
                List<PackageInfo> allAppList = mPackageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
                for (PackageInfo packageInfo : allAppList) {
                    includeAppFromPackage(result, docId, packageInfo, true, null);
                }
            }
            else if(docId.startsWith(ROOT_ID_PROCESS)) {
                if(Utils.hasNougat()){
                    List<ActivityManager.RunningServiceInfo> runningServices = mActivityManager.getRunningServices(1000);
                    for (ActivityManager.RunningServiceInfo process : runningServices) {
                        includeAppFromService(result, docId, process, null);
                    }
                }
                else if (Utils.hasLollipopMR1()) {
                    List<AndroidAppProcess> runningAppProcesses = AndroidProcesses.getRunningAppProcesses();
                    for (AndroidAppProcess process : runningAppProcesses) {
                        includeAppFromProcess(result, docId, process, null);
                    }
                } else {
                    List<ActivityManager.RunningAppProcessInfo> runningProcessesList = mActivityManager.getRunningAppProcesses();
                    for (ActivityManager.RunningAppProcessInfo processInfo : runningProcessesList) {
                        includeAppFromProcess(result, docId, processInfo, null);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        return null;
    }

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private void includeDefaultDocument(MatrixCursor result, String docId) {
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_THUMBNAIL | Document.FLAG_SUPPORTS_DELETE);
    }

    private void includeAppFromPackage(MatrixCursor result, String docId, PackageInfo packageInfo,
                                       boolean showSystem, String query) {

        ApplicationInfo appInfo = packageInfo.applicationInfo;
        if(showSystem == isSystemApp(appInfo)){
            String displayName;
            final String packageName = packageInfo.packageName;
            displayName = packageName;

            if (null != query && !displayName.toLowerCase().contains(query)) {
                return;
            }
            final String path = appInfo.sourceDir;
            final String mimeType = Document.COLUMN_MIME_TYPE;

            int flags = Document.FLAG_SUPPORTS_COPY | Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
            if(isTelevision()) {
                flags |= Document.FLAG_DIR_PREFERS_GRID;
            }

            final long size = new File(appInfo.sourceDir).length();
            final long lastModified = packageInfo.lastUpdateTime;
            final MatrixCursor.RowBuilder row = result.newRow();
            row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
            row.add(Document.COLUMN_DISPLAY_NAME, getAppName(displayName) + getAppVersion(packageInfo.versionName));
            row.add(Document.COLUMN_SUMMARY, packageName);
            row.add(Document.COLUMN_SIZE, size);
            row.add(Document.COLUMN_MIME_TYPE, mimeType);
            row.add(Document.COLUMN_LAST_MODIFIED, lastModified);
            row.add("path", path);
            row.add(Document.COLUMN_FLAGS, flags);
        }
    }

    private void includeAppFromService(MatrixCursor result, String docId, ActivityManager.RunningServiceInfo processInfo, String query ) {

        String process = processInfo.process;
        final String packageName = processInfo.process;
        process = process.substring(process.lastIndexOf(".") + 1, process.length());
        String summary = "";
        String displayName = "";
        ApplicationInfo appInfo = null;
        try {
            appInfo = mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).applicationInfo;
            displayName = process ;
        } catch (Exception e) { }

        if (TextUtils.isEmpty(displayName)) {
            displayName = process;
        }

        if (null != query && !displayName.toLowerCase().contains(query)) {
            return;
        }
        final String path = null != appInfo ? appInfo.sourceDir : "";
        final String mimeType = Document.COLUMN_MIME_TYPE;

        int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
        if(isTelevision()) {
            flags |= Document.FLAG_DIR_PREFERS_GRID;
        }

        int importance = processInfo.foreground ? ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                : ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
        summary = processTypeCache.get(importance);
        final long size = getProcessSize(processInfo.pid);

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SUMMARY, summary);
        row.add(Document.COLUMN_SIZE, size);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add("path", path);
        row.add(Document.COLUMN_FLAGS, flags);
    }

    private void includeAppFromProcess(MatrixCursor result, String docId,
                                       ActivityManager.RunningAppProcessInfo processInfo, String query ) {

        if (processInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY
                && processInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
            String process = processInfo.processName;
            process = process.substring(process.lastIndexOf(".") + 1, process.length());
            String summary;
            String displayName = "";
            ApplicationInfo appInfo = null;
            try {
                appInfo = mPackageManager.getPackageInfo(processInfo.processName, PackageManager.GET_ACTIVITIES).applicationInfo;
                displayName = process ;
            } catch (Exception e) {
                //
            }

            if (TextUtils.isEmpty(displayName)) {
                displayName = process;
            }

            if (null != query && !displayName.toLowerCase().contains(query)) {
                return;
            }
            final String path = null != appInfo ? appInfo.sourceDir : "";
            final String mimeType = Document.COLUMN_MIME_TYPE;

            int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
            if(isTelevision()) {
                flags |= Document.FLAG_DIR_PREFERS_GRID;
            }
            summary = processTypeCache.get(processInfo.importance);
            final long size = getProcessSize(processInfo.pid);
            final String packageName = processInfo.processName;

            final MatrixCursor.RowBuilder row = result.newRow();
            row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
            row.add(Document.COLUMN_DISPLAY_NAME, displayName);
            row.add(Document.COLUMN_SUMMARY, summary);
            row.add(Document.COLUMN_SIZE, size);
            row.add(Document.COLUMN_MIME_TYPE, mimeType);
            row.add("path", path);
            row.add(Document.COLUMN_FLAGS, flags);
        }
    }

    private void includeAppFromProcess(MatrixCursor result, String docId, AndroidAppProcess processInfo, String query ) {

        String process = processInfo.name;
        final String packageName = processInfo.getPackageName();
        process = process.substring(process.lastIndexOf(".") + 1, process.length());
        String summary;
        String displayName = "";
        ApplicationInfo appInfo = null;
        try {
            appInfo = mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).applicationInfo;
            displayName = process ;
        } catch (Exception e) {
            //
        }

        if (TextUtils.isEmpty(displayName)) {
            displayName = process;
        }

        if (null != query && !displayName.toLowerCase().contains(query)) {
            return;
        }
        final String path = null != appInfo ? appInfo.sourceDir : "";
        final String mimeType = Document.COLUMN_MIME_TYPE;

        int flags = Document.FLAG_SUPPORTS_DELETE | Document.FLAG_SUPPORTS_THUMBNAIL;
        if(isTelevision()) {
            flags |= Document.FLAG_DIR_PREFERS_GRID;
        }

        int importance = processInfo.foreground ? ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                : ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
        summary = processTypeCache.get(importance);
        final long size = getProcessSize(processInfo.pid);


        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, getDocIdForApp(docId, packageName));
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SUMMARY, summary);
        row.add(Document.COLUMN_SIZE, size);
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add("path", path);
        row.add(Document.COLUMN_FLAGS, flags);
    }

    private static boolean isSystemApp(ApplicationInfo appInfo){
        return appInfo.flags != 0 && (appInfo.flags
                & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
    }

    public static boolean isTelevision() {
        return mIsTelevision;
    }

    private static String getAppName(String packageName){
        String name = packageName;
        try {
            int start = packageName.lastIndexOf('.');
            name = start != -1 ? packageName.substring(start+1) : packageName;
            if(name.equalsIgnoreCase("android")){
                start = packageName.substring(0, start).lastIndexOf('.');
                name = start != -1 ? packageName.substring(start+1) : packageName;
            }
        } catch (Exception e) {
            //
        }
        return capitalize(name);
    }

    private static String capitalize(String string){
        return TextUtils.isEmpty(string) ? string : Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    private static String getAppVersion(String packageVersion){
        return  TextUtils.isEmpty(packageVersion) ? "" : "-" + packageVersion;
    }

    public static String getDocIdForApp(String rootId, String packageName){
        return rootId + packageName;
    }

    private long getProcessSize(int pid) {
        android.os.Debug.MemoryInfo[] memInfos = mActivityManager.getProcessMemoryInfo(new int[] { pid });
        return memInfos[0].getTotalPss() * 1024;
    }

    private class DocumentCursor extends MatrixCursor {
        DocumentCursor(String[] columnNames, String docId) {
            super(columnNames);

            final Uri notifyUri = DocumentsContract.buildChildDocumentsUri(AUTHORITY, docId);
            setNotificationUri(getContext().getContentResolver(), notifyUri);
        }

        @Override
        public void close() {
            super.close();
        }
    }

}
