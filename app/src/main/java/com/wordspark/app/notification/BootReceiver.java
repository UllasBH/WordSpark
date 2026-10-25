package com.wordspark.app.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wordspark.app.db.SettingsDao;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.util.SessionManager;

/**
 * AlarmManager alarms are cleared on device reboot. If a user's session is
 * still active after restart and they had notifications enabled, this uses
 * {@link #goAsync()} to safely reschedule the daily Word of the Day reminder
 * off the main thread.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!SessionManager.isLoggedIn(context)) return;

        int userId = SessionManager.getCurrentUserId(context);
        final PendingResult pendingResult = goAsync();

        new Thread(() -> {
            try {
                UserSettings settings = new SettingsDao(context).getSettings(userId);
                if (settings != null && settings.isNotificationsEnabled()) {
                    NotificationHelper.createNotificationChannel(context);
                    NotificationHelper.scheduleDaily(context, settings.getNotificationHour(), settings.getNotificationMinute());
                }
            } catch (Exception e) {
                Log.e("BootReceiver", "Failed to reschedule daily notification on boot", e);
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
