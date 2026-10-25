package com.wordspark.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wordspark.app.model.FavoriteEntry;
import com.wordspark.app.model.WordResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Data access for the "favorites" table.
 * Every method requires a userId so that a user can only ever read or write
 * their own favorites - this is enforced at the SQL WHERE-clause level.
 */
public class FavoriteDao {

    private final DatabaseHelper dbHelper;

    public FavoriteDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public boolean isFavorite(int userId, String word) {
        if (word == null) return false;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_FAVORITES,
                new String[]{DatabaseHelper.COL_FAV_ID},
                DatabaseHelper.COL_FAV_USER_ID + " = ? AND " + DatabaseHelper.COL_FAV_WORD + " = ?",
                new String[]{String.valueOf(userId), word.toLowerCase(Locale.ROOT)}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /** Adds a word to favorites. Silently ignores if it is already a favorite (no duplicates). */
    public void addFavorite(int userId, WordResult word) {
        if (isFavorite(userId, word.getWord())) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_FAV_USER_ID, userId);
        values.put(DatabaseHelper.COL_FAV_WORD, word.getWord().toLowerCase(Locale.ROOT));
        values.put(DatabaseHelper.COL_FAV_MEANING, word.getPrimaryMeaning());
        values.put(DatabaseHelper.COL_FAV_POS, word.getPrimaryPartOfSpeech());
        values.put(DatabaseHelper.COL_FAV_PRONUNCIATION, word.getPhonetic());
        db.insertWithOnConflict(DatabaseHelper.TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removeFavorite(int userId, String word) {
        if (word == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_FAVORITES,
                DatabaseHelper.COL_FAV_USER_ID + " = ? AND " + DatabaseHelper.COL_FAV_WORD + " = ?",
                new String[]{String.valueOf(userId), word.toLowerCase(Locale.ROOT)});
    }

    /**
     * Deletes a favorite by its row id, but ONLY if it belongs to userId.
     */
    public void removeFavoriteById(int userId, int favoriteId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_FAVORITES,
                DatabaseHelper.COL_FAV_ID + " = ? AND " + DatabaseHelper.COL_FAV_USER_ID + " = ?",
                new String[]{String.valueOf(favoriteId), String.valueOf(userId)});
    }

    public List<FavoriteEntry> getFavorites(int userId) {
        List<FavoriteEntry> results = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_FAVORITES,
                null,
                DatabaseHelper.COL_FAV_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                DatabaseHelper.COL_FAV_ID + " DESC");

        while (cursor.moveToNext()) {
            results.add(new FavoriteEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FAV_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FAV_WORD)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FAV_MEANING)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FAV_POS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FAV_PRONUNCIATION))
            ));
        }
        cursor.close();
        return results;
    }
}
