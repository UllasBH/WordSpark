package com.wordspark.app;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.wordspark.app.db.HistoryDao;
import com.wordspark.app.db.SettingsDao;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.notification.NotificationHelper;
import com.wordspark.app.util.SessionManager;
import com.wordspark.app.util.ThemeUtil;

/**
 * Phase 2 Settings screen.
 *
 * Every preference here is stored per-user in the "user_settings" table
 * (see SettingsDao), so User A's theme / history limit / notification
 * preferences never leak into User B's account.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int[] HISTORY_LIMIT_OPTIONS = {10, 20, 50, 100};

    private SettingsDao settingsDao;
    private HistoryDao historyDao;
    private UserSettings settings;
    private int userId;

    private TextView tvThemeValue, tvHistoryLimitValue, tvNotificationTimeValue, tvNotificationTimeLabel;
    private SwitchCompat switchNotifications;
    private View rowNotificationTime, ivNotificationTimeChevron;

    /** Runtime POST_NOTIFICATIONS request (Android 13+). */
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enableNotifications();
                } else {
                    switchNotifications.setChecked(false);
                    persistNotificationsEnabled(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.voice_permission_title)
                                .setMessage(R.string.voice_permission_settings_hint)
                                .setPositiveButton(R.string.open_settings, (d, w) -> openAppSettings())
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    } else {
                        Toast.makeText(this, R.string.notif_permission_denied, Toast.LENGTH_LONG).show();
                    }
                }
            });

    private void openAppSettings() {
        try {
            Intent intent = new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)
            );
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.notif_permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsDao = new SettingsDao(this);
        historyDao = new HistoryDao(this);
        userId = SessionManager.getCurrentUserId(this);
        // Start from in-memory defaults so the rows can render immediately;
        // the user's real saved values are loaded off the UI thread below.
        settings = new UserSettings();

        tvThemeValue = findViewById(R.id.tvThemeValue);
        tvHistoryLimitValue = findViewById(R.id.tvHistoryLimitValue);
        tvNotificationTimeValue = findViewById(R.id.tvNotificationTimeValue);
        tvNotificationTimeLabel = findViewById(R.id.tvNotificationTimeLabel);
        switchNotifications = findViewById(R.id.switchNotifications);
        rowNotificationTime = findViewById(R.id.rowNotificationTime);
        ivNotificationTimeChevron = findViewById(R.id.ivNotificationTimeChevron);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.rowTheme).setOnClickListener(v -> showThemeDialog());
        findViewById(R.id.rowHistoryLimit).setOnClickListener(v -> showHistoryLimitDialog());
        findViewById(R.id.rowClearHistory).setOnClickListener(v -> confirmClearHistory());
        findViewById(R.id.rowAbout).setOnClickListener(v -> showAboutDialog());
        findViewById(R.id.rowLogout).setOnClickListener(v -> confirmLogout());
        rowNotificationTime.setOnClickListener(v -> showTimePicker());

        switchNotifications.setOnCheckedChangeListener((button, checked) -> {
            if (!button.isPressed()) return; // ignore programmatic state restoration
            if (checked) {
                requestNotificationsOrEnable();
            } else {
                disableNotifications();
            }
        });

        refreshUi();
        loadSettingsAsync();
    }

    /** Reads this user's saved preferences off the main thread, then repaints. */
    private void loadSettingsAsync() {
        new Thread(() -> {
            UserSettings loaded = settingsDao.getSettings(userId);
            runOnUiThread(() -> {
                if (isGone()) return;
                settings = loaded;
                refreshUi();
            });
        }).start();
    }

    /** True once this Activity can no longer safely have its views touched. */
    private boolean isGone() {
        return isFinishing()
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed());
    }

    // ---------------------------------------------------------------- UI state

    private void refreshUi() {
        tvThemeValue.setText(themeLabel(settings.getTheme()));
        tvHistoryLimitValue.setText(getString(R.string.entries_format, settings.getHistoryLimit()));
        tvNotificationTimeValue.setText(formatTimeForDisplay(settings.getNotificationTime()));
        switchNotifications.setChecked(settings.isNotificationsEnabled());
        setNotificationTimeRowEnabled(settings.isNotificationsEnabled());
    }

    /** The reminder-time row only makes sense while notifications are on. */
    private void setNotificationTimeRowEnabled(boolean enabled) {
        rowNotificationTime.setEnabled(enabled);
        rowNotificationTime.setClickable(enabled);
        float alpha = enabled ? 1f : 0.45f;
        tvNotificationTimeLabel.setAlpha(alpha);
        tvNotificationTimeValue.setAlpha(alpha);
        ivNotificationTimeChevron.setAlpha(alpha);
    }

    private String themeLabel(String theme) {
        switch (theme) {
            case UserSettings.THEME_LIGHT: return getString(R.string.theme_light);
            case UserSettings.THEME_DARK:  return getString(R.string.theme_dark);
            default:                       return getString(R.string.theme_system);
        }
    }

    // ---------------------------------------------------------------- Theme

    private void showThemeDialog() {
        final String[] values = {UserSettings.THEME_LIGHT, UserSettings.THEME_DARK, UserSettings.THEME_SYSTEM};
        String[] labels = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system)
        };

        int checked = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(settings.getTheme())) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    String chosen = values[which];
                    dialog.dismiss();
                    if (chosen.equals(settings.getTheme())) return;

                    settings.setTheme(chosen);
                    new Thread(() -> settingsDao.setTheme(userId, chosen)).start();
                    // Mirror to prefs so the choice survives restart and is applied
                    // before any user row is read at next launch.
                    SessionManager.setCachedTheme(this, chosen);
                    ThemeUtil.apply(chosen); // recreates activities with the new night mode
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------------------------------------------------------------- History limit

    private void showHistoryLimitDialog() {
        String[] labels = new String[HISTORY_LIMIT_OPTIONS.length];
        int checked = 0;
        for (int i = 0; i < HISTORY_LIMIT_OPTIONS.length; i++) {
            labels[i] = getString(R.string.entries_format, HISTORY_LIMIT_OPTIONS[i]);
            if (HISTORY_LIMIT_OPTIONS[i] == settings.getHistoryLimit()) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.history_limit)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    int chosen = HISTORY_LIMIT_OPTIONS[which];
                    dialog.dismiss();
                    if (chosen == settings.getHistoryLimit()) return;

                    settings.setHistoryLimit(chosen);
                    refreshUi();
                    new Thread(() -> {
                        settingsDao.setHistoryLimit(userId, chosen);
                        // Apply the new limit immediately rather than waiting for
                        // future searches to push old rows out.
                        historyDao.enforceRetentionLimit(userId, chosen);
                    }).start();
                    Toast.makeText(this, getString(R.string.history_limit_updated, chosen), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------------------------------------------------------------- Notifications

    private void requestNotificationsOrEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        enableNotifications();
    }

    private void enableNotifications() {
        settings.setNotificationsEnabled(true);
        persistNotificationsEnabled(true);
        switchNotifications.setChecked(true);
        setNotificationTimeRowEnabled(true);

        int[] hm = parseTime(settings.getNotificationTime());
        NotificationHelper.scheduleDaily(this, hm[0], hm[1]);
        Toast.makeText(this,
                getString(R.string.notif_enabled_toast, formatTimeForDisplay(settings.getNotificationTime())),
                Toast.LENGTH_SHORT).show();
    }

    private void disableNotifications() {
        settings.setNotificationsEnabled(false);
        persistNotificationsEnabled(false);
        setNotificationTimeRowEnabled(false);
        NotificationHelper.cancel(this); // no orphaned alarms left behind
        Toast.makeText(this, R.string.notif_disabled_toast, Toast.LENGTH_SHORT).show();
    }

    private void persistNotificationsEnabled(boolean enabled) {
        new Thread(() -> settingsDao.setNotificationsEnabled(userId, enabled)).start();
    }

    private void showTimePicker() {
        int[] hm = parseTime(settings.getNotificationTime());
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String stored = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute);
            settings.setNotificationTime(stored);
            refreshUi();
            new Thread(() -> settingsDao.setNotificationTime(userId, stored)).start();

            if (settings.isNotificationsEnabled()) {
                // Reschedule: scheduleDaily() cancels the previous alarm first,
                // so changing the time never stacks duplicate alarms.
                NotificationHelper.scheduleDaily(this, hourOfDay, minute);
                Toast.makeText(this,
                        getString(R.string.notif_enabled_toast, formatTimeForDisplay(stored)),
                        Toast.LENGTH_SHORT).show();
            }
        }, hm[0], hm[1], false).show();
    }

    /** "HH:mm" -> {hour, minute}, defaulting safely if the stored value is malformed. */
    private int[] parseTime(String time) {
        try {
            String[] parts = time.split(":");
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (Exception e) {
            return new int[]{9, 0};
        }
    }

    /** "09:00" -> "9:00 AM" using the device locale. */
    private String formatTimeForDisplay(String stored) {
        int[] hm = parseTime(stored);
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, hm[0]);
        c.set(java.util.Calendar.MINUTE, hm[1]);
        return new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(c.getTime());
    }

    // ---------------------------------------------------------------- History / account

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.ok, (dialog, which) -> new Thread(() -> {
                    boolean success = true;
                    try {
                        historyDao.clearHistory(userId);
                    } catch (Exception e) {
                        success = false;
                    }
                    final boolean cleared = success;
                    // Feedback only after the delete has actually completed.
                    runOnUiThread(() -> {
                        if (isGone()) return;
                        Toast.makeText(this,
                                cleared ? R.string.history_cleared : R.string.err_clear_history_failed,
                                Toast.LENGTH_SHORT).show();
                    });
                }).start())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_wordspark)
                .setMessage("WordSpark v2.0\n\nA dictionary and vocabulary-learning app built for our "
                        + "college Mobile Application Development mini project.\n\n"
                        + "Built with Android Studio, Java, XML, and SQLite.\n\n"
                        + "Dictionary Data:\nPowered by Merriam-Webster's Learner's Dictionary\n\n"
                        + "Features: dictionary search, word details, favorites, search history, "
                        + "vocabulary quiz, word of the day and daily reminders.")
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    // Cancel THIS user's alarm before clearing the session. The alarm
                    // is device-global, so leaving it armed would fire it for whoever
                    // logs in next. Their preference row in SQLite is deliberately
                    // left untouched, so it is restored when they log back in.
                    NotificationHelper.cancel(this);

                    // Logout clears the session only - the user's account, favorites,
                    // history, settings and quiz score all stay in SQLite.
                    SessionManager.logout(this);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
