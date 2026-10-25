package com.wordspark.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.wordspark.app.model.User;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.util.PasswordUtil;

/**
 * Data access for the "users" table: registration and login.
 * Passwords are never stored in plain text - see {@link PasswordUtil}.
 * Implements transactional registration and unswallowed DB exception propagation.
 */
public class UserDao {

    private final DatabaseHelper dbHelper;

    public UserDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{normalizedEmail(email)}, null, null, null);
        boolean exists = false;
        if (cursor != null) {
            exists = cursor.moveToFirst();
            cursor.close();
        }
        return exists;
    }

    /**
     * Atomically registers a new user AND creates their default user_settings row
     * inside a single database transaction: create user -> create settings -> commit.
     * If anything fails, rolls back automatically and propagates the exception.
     */
    public long registerUserWithSettings(String name, String email, String plainPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_NAME, name.trim());
        values.put(DatabaseHelper.COL_USER_EMAIL, normalizedEmail(email));
        String hashedPassword = PasswordUtil.hash(plainPassword);
        values.put(DatabaseHelper.COL_USER_PASSWORD_HASH, hashedPassword);

        // Defensive check: if un-migrated legacy "password" column exists, satisfy NOT NULL constraint
        if (hasColumn(db, DatabaseHelper.TABLE_USERS, "password")) {
            values.put("password", hashedPassword);
        }

        db.beginTransaction();
        try {
            long newUserId = db.insertOrThrow(DatabaseHelper.TABLE_USERS, null, values);

            ContentValues settingsValues = new ContentValues();
            settingsValues.put(DatabaseHelper.COL_SET_USER_ID, (int) newUserId);
            settingsValues.put(DatabaseHelper.COL_SET_THEME, UserSettings.THEME_SYSTEM);
            settingsValues.put(DatabaseHelper.COL_SET_HISTORY_LIMIT, 50);
            settingsValues.put(DatabaseHelper.COL_SET_NOTIFICATIONS_ENABLED, 0);
            settingsValues.put(DatabaseHelper.COL_SET_NOTIFICATION_TIME, "09:00");
            settingsValues.put(DatabaseHelper.COL_SET_QUIZ_BEST_SCORE, 0);
            settingsValues.put(DatabaseHelper.COL_SET_QUIZ_BEST_TOTAL, 0);
            db.insertWithOnConflict(DatabaseHelper.TABLE_SETTINGS, null, settingsValues, SQLiteDatabase.CONFLICT_IGNORE);

            db.setTransactionSuccessful(); // COMMIT TRANSACTION
            return newUserId;
        } catch (Exception e) {
            Log.e("WordSparkUserDao", "registerUserWithSettings transaction failed, rolling back: " + e.getMessage(), e);
            throw e; // ROLLED BACK automatically because setTransactionSuccessful was not called!
        } finally {
            db.endTransaction(); // ROLLBACK if not successful
        }
    }

    public long registerUser(String name, String email, String plainPassword) {
        return registerUserWithSettings(name, email, plainPassword);
    }

    /** Returns the matching User if credentials are correct, otherwise null. */
    public User login(String email, String plainPassword) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID, DatabaseHelper.COL_USER_NAME,
                        DatabaseHelper.COL_USER_EMAIL, DatabaseHelper.COL_USER_PASSWORD_HASH},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{normalizedEmail(email)}, null, null, null);

        User result = null;
        if (cursor.moveToFirst()) {
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD_HASH));
            if (PasswordUtil.matches(plainPassword, storedHash)) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME));
                String storedEmail = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL));
                result = new User(id, name, storedEmail);
            }
        }
        cursor.close();
        return result;
    }

    /**
     * Replaces the stored password hash for the account with this email.
     * The new password goes through the same salted-hash path as registration
     * ({@link PasswordUtil#hash}); plain text is never stored.
     * Returns true only if exactly one account row was updated.
     */
    public boolean updatePassword(String email, String newPlainPassword) {
        if (email == null || newPlainPassword == null) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_USER_PASSWORD_HASH, PasswordUtil.hash(newPlainPassword));
            int rows = db.update(DatabaseHelper.TABLE_USERS, values,
                    DatabaseHelper.COL_USER_EMAIL + " = ?",
                    new String[]{normalizedEmail(email)});
            db.setTransactionSuccessful();
            return rows == 1;
        } catch (Exception e) {
            Log.e("WordSparkUserDao", "updatePassword failed", e);
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID, DatabaseHelper.COL_USER_NAME, DatabaseHelper.COL_USER_EMAIL},
                DatabaseHelper.COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, null);
        User result = null;
        if (cursor.moveToFirst()) {
            result = new User(userId,
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL)));
        }
        cursor.close();
        return result;
    }

    private boolean hasColumn(SQLiteDatabase db, String tableName, String colName) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    if (colName.equalsIgnoreCase(c.getString(1))) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String normalizedEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
