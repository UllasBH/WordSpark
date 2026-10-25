package com.wordspark.app;

import android.app.Application;

import com.wordspark.app.notification.NotificationHelper;
import com.wordspark.app.util.SessionManager;
import com.wordspark.app.util.ThemeUtil;

public class WordSparkApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply the last-known theme immediately, before any Activity is
        // created, so the very first frame (even the Login screen) already
        // shows the right theme instead of flashing light-then-dark.
        ThemeUtil.apply(SessionManager.getCachedTheme(this));

        // Creating the channel is idempotent and required before any
        // notification can be posted on Android 8+.
        NotificationHelper.createNotificationChannel(this);
    }
}
