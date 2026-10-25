package com.wordspark.app.notification;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.wordspark.app.MainActivity;
import com.wordspark.app.R;
import com.wordspark.app.WordDetailActivity;
import com.wordspark.app.db.SettingsDao;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.util.SessionManager;
import com.wordspark.app.util.WordOfDay;

/**
 * Receives the daily AlarmManager broadcast and shows the Word of the Day
 * reminder. Tapping it opens today's word directly in WordDetailActivity.
 */
public class WotdReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "WordSparkReminder";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "WordSpark reminder receiver triggered");

        // Respect runtime notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // No one is signed in - there is no user whose reminder this could be.
        if (!SessionManager.isLoggedIn(context)) {
            NotificationHelper.cancel(context); // clean up the orphaned alarm
            return;
        }

        final int userId = SessionManager.getCurrentUserId(context);
        final Context appContext = context.getApplicationContext();
        final PendingResult pendingResult = goAsync(); // keep the receiver alive for DB read and rescheduling

        new Thread(() -> {
            try {
                UserSettings settings = new SettingsDao(appContext).getSettings(userId);

                // If user disabled notifications, cancel and return
                if (settings == null || !settings.isNotificationsEnabled()) {
                    NotificationHelper.cancel(appContext);
                    return;
                }

                showNotification(appContext);

                // Automatically schedule the next day's alarm
                NotificationHelper.scheduleDaily(
                        appContext,
                        settings.getNotificationHour(),
                        settings.getNotificationMinute()
                );
            } catch (Exception ignored) {
                // A reminder must never crash the app
            } finally {
                pendingResult.finish();
            }
        }).start();
    }

    private void showNotification(Context context) {
        String todayWord = WordOfDay.getTodayWord(context);
        String displayWord = capitalize(todayWord);

        Intent detailIntent = new Intent(context, WordDetailActivity.class);
        detailIntent.putExtra(WordDetailActivity.EXTRA_WORD, todayWord);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent contentIntent = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, MainActivity.class))
                .addNextIntent(detailIntent)
                .getPendingIntent(0, flags);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(context.getString(R.string.notification_wotd_title))
                        .setContentText(context.getString(R.string.notification_wotd_word, displayWord))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(contentIntent);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "WordSpark reminder notification posted");
        } catch (SecurityException ignored) {
            // fail-safe
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(java.util.Locale.getDefault()) + s.substring(1);
    }
}
