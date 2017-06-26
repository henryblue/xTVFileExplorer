package com.hb.xtvfileexplorer.utils;


import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.RandomAccessFile;

public class StorageUtils {

    public static final int PARTITION_SYSTEM = 1;
    public static final int PARTITION_DATA = 2;
    public static final int PARTITION_CACHE = 3;
    public static final int PARTITION_RAM = 4;
    public static final int PARTITION_EXTERNAL = 5;

    private ActivityManager mActivityManager;

    public StorageUtils(Context context) {
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
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
            if(Utils.hasJellyBeanMR2()){
                final long blockSize = stat.getBlockSizeLong();
                final long availableBlocks = (isTotal ? stat.getBlockCountLong() : stat.getAvailableBlocksLong());
                return availableBlocks * blockSize;
            } else {
                final long blockSize = stat.getBlockSize();
                final long availableBlocks = (isTotal ? (long)stat.getBlockCount() : (long)stat.getAvailableBlocks());
                return availableBlocks * blockSize;
            }
        }
        else return 0L;
    }

    private long getSizeTotalRAM(boolean isTotal) {
        long sizeInBytes = 1000;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(mi);
        if(isTotal) {
            try {
                if(Utils.hasJellyBean()) {
                    sizeInBytes = mi.totalMem;
                } else {
                    RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
                    String load = reader.readLine();
                    String[] totrm = load.split(" kB");
                    String[] trm = totrm[0].split(" ");
                    sizeInBytes=Long.parseLong(trm[trm.length-1]);
                    sizeInBytes=sizeInBytes*1024;
                    reader.close();
                }
            } catch (Exception e) {
                //
            }
        } else {
            sizeInBytes = mi.availMem;
        }
        return sizeInBytes;
    }
}
