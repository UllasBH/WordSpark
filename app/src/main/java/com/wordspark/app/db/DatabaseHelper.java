package com.wordspark.app.db;

import android.content.Context;
import android.util.Log;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Central SQLite schema definition for WordSpark.
 *
 * Tables:
 *   users          - id, name, email (unique), password_hash
 *   favorites      - id, user_id (FK), word, meaning, part_of_speech, pronunciation, collection_id
 *   history        - id, user_id (FK), word, timestamp
 *   user_settings  - user_id (FK, unique), theme, history_limit,
 *                    notifications_enabled, notification_time,
 *                    quiz_best_score, quiz_best_total, quiz_attempts,
 *                    quiz_total_correct, flashcards_reviewed
 *   dictionary     - word (PK), json_data (local offline dictionary cache)
 *   difficult_words- id, user_id (FK), word
 *   collections    - id, user_id (FK), name
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "wordspark.db";
    private static final int DB_VERSION = 11;

    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NAME = "name";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD_HASH = "password_hash";

    public static final String TABLE_FAVORITES = "favorites";
    public static final String COL_FAV_ID = "id";
    public static final String COL_FAV_USER_ID = "user_id";
    public static final String COL_FAV_WORD = "word";
    public static final String COL_FAV_MEANING = "meaning";
    public static final String COL_FAV_POS = "part_of_speech";
    public static final String COL_FAV_PRONUNCIATION = "pronunciation";
    public static final String COL_FAV_COLLECTION_ID = "collection_id";

    public static final String TABLE_HISTORY = "history";
    public static final String COL_HIST_ID = "id";
    public static final String COL_HIST_USER_ID = "user_id";
    public static final String COL_HIST_WORD = "word";
    public static final String COL_HIST_TIMESTAMP = "timestamp";

    public static final String TABLE_SETTINGS = "user_settings";
    public static final String COL_SET_USER_ID = "user_id";
    public static final String COL_SET_THEME = "theme";
    public static final String COL_SET_HISTORY_LIMIT = "history_limit";
    public static final String COL_SET_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String COL_SET_NOTIFICATION_TIME = "notification_time";
    public static final String COL_SET_QUIZ_BEST_SCORE = "quiz_best_score";
    public static final String COL_SET_QUIZ_BEST_TOTAL = "quiz_best_total";
    public static final String COL_SET_QUIZ_ATTEMPTS = "quiz_attempts";
    public static final String COL_SET_QUIZ_TOTAL_CORRECT = "quiz_total_correct";
    public static final String COL_SET_FLASHCARDS_REVIEWED = "flashcards_reviewed";

    public static final String TABLE_DICTIONARY = "dictionary";
    public static final String COL_DICT_WORD = "word";
    public static final String COL_DICT_JSON = "json_data";

    public static final String TABLE_DIFFICULT = "difficult_words";
    public static final String COL_DIFF_ID = "id";
    public static final String COL_DIFF_USER_ID = "user_id";
    public static final String COL_DIFF_WORD = "word";

    public static final String TABLE_COLLECTIONS = "collections";
    public static final String COL_COLL_ID = "id";
    public static final String COL_COLL_USER_ID = "user_id";
    public static final String COL_COLL_NAME = "name";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUsersTable(db);
        createFavoritesTable(db);
        createHistoryTable(db);
        createSettingsTable(db);
        createDictionaryTable(db);
        createDifficultTable(db);
        createCollectionsTable(db);
        purgeFakeDictionaryCache(db);
    }

    private void createUsersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_NAME + " TEXT NOT NULL, " +
                COL_USER_EMAIL + " TEXT NOT NULL UNIQUE, " +
                COL_USER_PASSWORD_HASH + " TEXT NOT NULL DEFAULT ''" +
                ")");
    }

    private void createFavoritesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITES + " (" +
                COL_FAV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FAV_USER_ID + " INTEGER NOT NULL, " +
                COL_FAV_WORD + " TEXT NOT NULL, " +
                COL_FAV_MEANING + " TEXT, " +
                COL_FAV_POS + " TEXT, " +
                COL_FAV_PRONUNCIATION + " TEXT, " +
                COL_FAV_COLLECTION_ID + " INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE(" + COL_FAV_USER_ID + ", " + COL_FAV_WORD + "), " +
                "FOREIGN KEY(" + COL_FAV_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                ")");
    }

    private void createHistoryTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_HISTORY + " (" +
                COL_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HIST_USER_ID + " INTEGER NOT NULL, " +
                COL_HIST_WORD + " TEXT NOT NULL, " +
                COL_HIST_TIMESTAMP + " INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE(" + COL_HIST_USER_ID + ", " + COL_HIST_WORD + "), " +
                "FOREIGN KEY(" + COL_HIST_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                ")");
    }

    private void createSettingsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " (" +
                COL_SET_USER_ID + " INTEGER PRIMARY KEY, " +
                COL_SET_THEME + " TEXT NOT NULL DEFAULT 'light', " +
                COL_SET_HISTORY_LIMIT + " INTEGER NOT NULL DEFAULT 50, " +
                COL_SET_NOTIFICATIONS_ENABLED + " INTEGER NOT NULL DEFAULT 0, " +
                COL_SET_NOTIFICATION_TIME + " TEXT NOT NULL DEFAULT '09:00', " +
                COL_SET_QUIZ_BEST_SCORE + " INTEGER NOT NULL DEFAULT 0, " +
                COL_SET_QUIZ_BEST_TOTAL + " INTEGER NOT NULL DEFAULT 0, " +
                COL_SET_QUIZ_ATTEMPTS + " INTEGER NOT NULL DEFAULT 0, " +
                COL_SET_QUIZ_TOTAL_CORRECT + " INTEGER NOT NULL DEFAULT 0, " +
                COL_SET_FLASHCARDS_REVIEWED + " INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(" + COL_SET_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                ")");
    }

    private void createDictionaryTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DICTIONARY + " (" +
                COL_DICT_WORD + " TEXT PRIMARY KEY, " +
                COL_DICT_JSON + " TEXT NOT NULL" +
                ")");
    }

    private void createDifficultTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DIFFICULT + " (" +
                COL_DIFF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DIFF_USER_ID + " INTEGER NOT NULL, " +
                COL_DIFF_WORD + " TEXT NOT NULL, " +
                "UNIQUE(" + COL_DIFF_USER_ID + ", " + COL_DIFF_WORD + "), " +
                "FOREIGN KEY(" + COL_DIFF_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                ")");
    }

    private void createCollectionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COLLECTIONS + " (" +
                COL_COLL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_COLL_USER_ID + " INTEGER NOT NULL, " +
                COL_COLL_NAME + " TEXT NOT NULL, " +
                "UNIQUE(" + COL_COLL_USER_ID + ", " + COL_COLL_NAME + "), " +
                "FOREIGN KEY(" + COL_COLL_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                ")");
    }

    private void purgeFakeDictionaryCache(SQLiteDatabase db) {
        try {
            db.execSQL("DELETE FROM " + TABLE_DICTIONARY + " WHERE " + COL_DICT_JSON + " LIKE '%Offline definition for%' OR " + COL_DICT_JSON + " LIKE '%Stored securely in local%'");
        } catch (Exception e) {
            Log.e("WordSparkDB", "Failed to purge fake dictionary cache", e);
        }
    }

    private void migrateUsersTableIfNeeded(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_USERS + ")", null);
            boolean hasLegacyPassword = false;
            boolean hasPasswordHash = false;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String colName = cursor.getString(1);
                    if ("password".equalsIgnoreCase(colName)) {
                        hasLegacyPassword = true;
                    }
                    if ("password_hash".equalsIgnoreCase(colName)) {
                        hasPasswordHash = true;
                    }
                }
                cursor.close();
            }

            if (hasLegacyPassword || !hasPasswordHash) {
                db.execSQL("CREATE TABLE IF NOT EXISTS users_new (" +
                        COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USER_NAME + " TEXT NOT NULL, " +
                        COL_USER_EMAIL + " TEXT NOT NULL UNIQUE, " +
                        COL_USER_PASSWORD_HASH + " TEXT NOT NULL DEFAULT ''" +
                        ")");

                try {
                    if (hasPasswordHash && hasLegacyPassword) {
                        db.execSQL("INSERT OR IGNORE INTO users_new (id, name, email, password_hash) " +
                                "SELECT id, name, email, COALESCE(password_hash, password, '') FROM " + TABLE_USERS);
                    } else if (hasLegacyPassword) {
                        db.execSQL("INSERT OR IGNORE INTO users_new (id, name, email, password_hash) " +
                                "SELECT id, name, email, COALESCE(password, '') FROM " + TABLE_USERS);
                    } else {
                        db.execSQL("INSERT OR IGNORE INTO users_new (id, name, email, password_hash) " +
                                "SELECT id, name, email, '' FROM " + TABLE_USERS);
                    }
                } catch (Exception e) {
                    Log.e("WordSparkDB", "Error copying users during migration", e);
                }

                db.execSQL("DROP TABLE " + TABLE_USERS);
                db.execSQL("ALTER TABLE users_new RENAME TO " + TABLE_USERS);
            }
        } catch (Exception e) {
            Log.e("WordSparkDB", "Failed to migrate users table", e);
        }
    }

    private void migrateHistoryTableIfNeeded(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_HISTORY + ")", null);
            boolean hasDateColumn = false;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String colName = cursor.getString(1);
                    if ("date".equalsIgnoreCase(colName)) {
                        hasDateColumn = true;
                        break;
                    }
                }
                cursor.close();
            }

            if (hasDateColumn) {
                db.execSQL("CREATE TABLE IF NOT EXISTS history_new (" +
                        COL_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_HIST_USER_ID + " INTEGER NOT NULL, " +
                        COL_HIST_WORD + " TEXT NOT NULL, " +
                        COL_HIST_TIMESTAMP + " INTEGER NOT NULL DEFAULT 0, " +
                        "UNIQUE(" + COL_HIST_USER_ID + ", " + COL_HIST_WORD + "), " +
                        "FOREIGN KEY(" + COL_HIST_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                        ")");

                db.execSQL("INSERT OR IGNORE INTO history_new (" + COL_HIST_ID + ", " + COL_HIST_USER_ID + ", " + COL_HIST_WORD + ", " + COL_HIST_TIMESTAMP + ") " +
                        "SELECT " + COL_HIST_ID + ", " + COL_HIST_USER_ID + ", " + COL_HIST_WORD + ", " + COL_HIST_TIMESTAMP + " FROM " + TABLE_HISTORY);

                db.execSQL("DROP TABLE " + TABLE_HISTORY);
                db.execSQL("ALTER TABLE history_new RENAME TO " + TABLE_HISTORY);
            }
        } catch (Exception e) {
            Log.e("WordSparkDB", "Failed to migrate history table", e);
        }
    }

    private void ensureColumnsExist(SQLiteDatabase db) {
        createUsersTable(db);
        createFavoritesTable(db);
        createHistoryTable(db);
        createSettingsTable(db);
        createDictionaryTable(db);
        createDifficultTable(db);
        createCollectionsTable(db);
        purgeFakeDictionaryCache(db);
        migrateUsersTableIfNeeded(db);
        migrateHistoryTableIfNeeded(db);

        addColumnIfNeeded(db, TABLE_USERS, COL_USER_PASSWORD_HASH, "TEXT NOT NULL DEFAULT ''");

        addColumnIfNeeded(db, TABLE_FAVORITES, COL_FAV_USER_ID, "INTEGER NOT NULL DEFAULT 1");
        addColumnIfNeeded(db, TABLE_FAVORITES, COL_FAV_WORD, "TEXT NOT NULL DEFAULT ''");
        addColumnIfNeeded(db, TABLE_FAVORITES, COL_FAV_MEANING, "TEXT");
        addColumnIfNeeded(db, TABLE_FAVORITES, COL_FAV_POS, "TEXT");
        addColumnIfNeeded(db, TABLE_FAVORITES, COL_FAV_PRONUNCIATION, "TEXT");
        addColumnIfNeeded(db, TABLE_FAVORITES, COL_FAV_COLLECTION_ID, "INTEGER NOT NULL DEFAULT 0");

        addColumnIfNeeded(db, TABLE_HISTORY, COL_HIST_USER_ID, "INTEGER NOT NULL DEFAULT 1");
        addColumnIfNeeded(db, TABLE_HISTORY, COL_HIST_WORD, "TEXT NOT NULL DEFAULT ''");
        addColumnIfNeeded(db, TABLE_HISTORY, COL_HIST_TIMESTAMP, "INTEGER NOT NULL DEFAULT 0");

        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_THEME, "TEXT NOT NULL DEFAULT 'system'");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_HISTORY_LIMIT, "INTEGER NOT NULL DEFAULT 50");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_NOTIFICATIONS_ENABLED, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_NOTIFICATION_TIME, "TEXT NOT NULL DEFAULT '09:00'");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_QUIZ_BEST_SCORE, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_QUIZ_BEST_TOTAL, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_QUIZ_ATTEMPTS, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_QUIZ_TOTAL_CORRECT, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNeeded(db, TABLE_SETTINGS, COL_SET_FLASHCARDS_REVIEWED, "INTEGER NOT NULL DEFAULT 0");
    }

    private void addColumnIfNeeded(SQLiteDatabase db, String tableName, String columnName, String columnDefinition) {
        try {
            Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            boolean hasColumn = false;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String colName = cursor.getString(1);
                    if (columnName.equalsIgnoreCase(colName)) {
                        hasColumn = true;
                        break;
                    }
                }
                cursor.close();
            }
            if (!hasColumn) {
                db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
            }
        } catch (Exception e) {
            Log.e("WordSparkDB", "Failed to add column " + columnName + " to " + tableName, e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureColumnsExist(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("WordSparkDB", "Opening database with newer version " + oldVersion + " (app expects " + newVersion + ")");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (db.isReadOnly()) return;
        try {
            ensureColumnsExist(db);
        } catch (Exception e) {
            Log.e("WordSparkDB", "Table check failed", e);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
