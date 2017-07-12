package com.hb.xtvfileexplorer.misc;


import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;

import com.hb.xtvfileexplorer.utils.Utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class StorageUtils {

    public static final int PARTITION_SYSTEM = 1;
    public static final int PARTITION_DATA = 2;
    public static final int PARTITION_CACHE = 3;
    public static final int PARTITION_RAM = 4;
    public static final int PARTITION_EXTERNAL = 5;

    private ActivityManager mActivityManager;
    private StorageManager mStorageManager;
    private Context mContext;

    public StorageUtils(Context context) {
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        mContext = context;
    }

    public long getPartitionSize(int type, boolean isTotal){
        Long size = 0L;

        switch (type) {
            case PARTITION_SYSTEM:
                size = getPartitionSize(Environment.getRootDirectory().getPath(), isTotal);
                break;
            case PARTITION_DATA:
                size = getPartitionSize(Environment.getDataDirectory().getPath(), isTotal);
                break;
            case PARTITION_CACHE:
                size = getPartitionSize(Environment.getDownloadCacheDirectory().getPath(), isTotal);
                break;
            case PARTITION_EXTERNAL:
                size = getPartitionSize(Environment.getExternalStorageDirectory().getPath(), isTotal);
                break;
            case PARTITION_RAM:
                size = getSizeTotalRAM(isTotal);
                break;
        }
        return size;
    }

    /**
     * @param isTotal  The parameter for calculating total size
     * @return return Total Size when isTotal is true else return Free Size of Internal memory(data folder)
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
    private static long getPartitionSize(String path, boolean isTotal){
        StatFs stat = null;
        try {
            stat = new StatFs(path);
        } catch (Exception e) {
            //
        }
        if (null != stat) {
            final long blockSize = stat.getBlockSizeLong();
            final long availableBlocks = (isTotal ? stat.getBlockCountLong() : stat.getAvailableBlocksLong());
            return availableBlocks * blockSize;
        }
        else return 0L;
    }

    private long getSizeTotalRAM(boolean isTotal) {
        long sizeInBytes;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(mi);
        if(isTotal) {
            sizeInBytes = mi.totalMem;
        } else {
            sizeInBytes = mi.availMem;
        }
        return sizeInBytes;
    }

    public List<StorageVolume> getStorageMounts() {
        List<StorageVolume> mounts = new ArrayList<>();
        Object[] sv = null;
        try {
            Method getVolumeList = StorageManager.class.getDeclaredMethod("getVolumeList");
            sv = (Object[])getVolumeList.invoke(mStorageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null == sv) {
            return mounts;
        }
        for (Object object : sv) {
            int mStorageId = getInteger(object, "mStorageId");
            File mPath = getFile(object);
            String mDescription = getDescription(object);

            boolean mPrimary = getBoolean(object, "mPrimary");
            boolean mEmulated = getBoolean(object, "mEmulated");
            boolean mRemovable = getBoolean(object, "mRemovable");
            long mMtpReserveSize = getLong(object, "mMtpReserveSize");
            boolean mAllowMassStorage = getBoolean(object, "mAllowMassStorage");
            long mMaxFileSize = getLong(object, "mMaxFileSize");

            String mId = getString(object, "mId");
            String mFsUuid = getString(object, "mFsUuid");
            String mUuid = getString(object, "mUuid");
            String mUserLabel = getString(object, "mUserLabel");
            String mState = getString(object, "mState");

            StorageVolume storageVolume = new StorageVolume(mStorageId, mPath, mDescription, mPrimary,
                    mRemovable, mEmulated, mMtpReserveSize, mAllowMassStorage, mMaxFileSize);

            storageVolume.mId = mId;
            storageVolume.mFsUuid = mFsUuid;
            storageVolume.mUuid = mUuid;
            storageVolume.mUserLabel = mUserLabel;
            storageVolume.mState = mState;

            mounts.add(storageVolume);
        }
        return mounts;
    }

    private File getFile(Object object) {
        String path;
        File file = null;
        try {
            Field mPath = object.getClass().getDeclaredField("mPath");
            mPath.setAccessible(true);
            Object pathObj = mPath.get(object);
            if(Utils.hasJellyBeanMR1()){
                file = (File)pathObj;
            }
            else{
                path = (String)pathObj;
                file = new File(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private String getDescription(Object object) {
        String description;
        if(Utils.hasMarshmallow()){
            description = getDescription(object, false);
        }
        else if(Utils.hasJellyBean()){
            try {
                description = getDescription(object, true);
            }
            catch (Resources.NotFoundException e){
                description = getDescription(object, false);
            }
        }
        else{
            description = getDescription(object, false);
        }
        return description;
    }

    private String getDescription(Object object, boolean hasId) {
        String description;
        if (hasId) {
            int mDescriptionInt = getInteger(object, "mDescriptionId");
            description = mContext.getResources().getString(mDescriptionInt);
        } else {
            description = getString(object, "mDescription");
        }
        return description;
    }

    private String getString(Object object, String id) {
        String value = "";
        try {
            Field field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = (String) field.get(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private int getInteger(Object object, String id) {
        int value = 0;
        try {
            Field field;
            field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = field.getInt(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private boolean getBoolean(Object object, String id) {
        boolean value = false;
        try {
            Field field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = field.getBoolean(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private long getLong(Object object, String id) {
        long value = 0L;
        try {
            Field field = object.getClass().getDeclaredField(id);
            field.setAccessible(true);
            value = field.getLong(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
