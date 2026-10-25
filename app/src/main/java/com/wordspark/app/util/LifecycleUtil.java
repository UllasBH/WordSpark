package com.wordspark.app.util;

import android.app.Activity;
import android.os.Build;

/**
 * Small guard used before touching Activity views from a background
 * callback (DB reads, Dictionary API results). Without it, a result that
 * arrives after the user has navigated away would try to update views on a
 * destroyed Activity.
 */
public class LifecycleUtil {

    /** True once the Activity can no longer safely have its views touched. */
    public static boolean isGone(Activity activity) {
        if (activity == null) return true;
        if (activity.isFinishing()) return true;
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed();
    }
}
