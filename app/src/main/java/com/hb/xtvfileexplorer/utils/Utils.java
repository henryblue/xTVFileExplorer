package com.hb.xtvfileexplorer.utils;

import android.app.UiModeManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.text.TextUtilsCompat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;

import com.hb.xtvfileexplorer.ExplorerActivity;

import java.lang.reflect.Method;
import java.util.Locale;


public class Utils {

    private static final long PROVIDER_ANR_TIMEOUT = 20 * DateUtils.SECOND_IN_MILLIS;

    public static boolean hasNougat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean hasLollipopMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    public static boolean isTelevision(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    public static int dpToPx(int dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_MEDIUM);
        return Math.round(px);
    }

    public static boolean isRTL() {
        return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == android.support.v4.view.ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(
            ContentResolver resolver, String authority) throws RemoteException {
        final ContentProviderClient client = resolver.acquireUnstableContentProviderClient(authority);
        if (client == null) {
            throw new RemoteException("Failed to acquire provider for " + authority);
        }
        setDetectNotResponding(client, PROVIDER_ANR_TIMEOUT);
        return client;
    }

    private static void setDetectNotResponding(ContentProviderClient client, long anrTimeout) {
        try {
            Method method = client.getClass().getMethod("setDetectNotResponding", long.class);
            if (method != null) {
                method.invoke(client, anrTimeout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeQuietly(Cursor closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static void releaseQuietly(ContentProviderClient client) {
        if (client != null) {
            try {
                client.release();
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean isActivityAlive(ExplorerActivity activity) {
        return !(null == activity
                || activity.isDestroyed());
    }

    public static String formatTime(Context context, long when) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        int flags = DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT | DateUtils.FORMAT_ABBREV_ALL;

        if (then.year != now.year) {
            flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            flags |= DateUtils.FORMAT_SHOW_TIME;
        }

        return DateUtils.formatDateTime(context, when, flags);
    }
}
