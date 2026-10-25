package com.wordspark.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Caches the most recently successfully-fetched Word of the Day (for
 * today's date only) so Home can show it instantly on repeat visits
 * without another network call, and can still show *something* if a later
 * fetch fails (e.g. no internet) as long as today's word was already
 * fetched once.
 *
 * Stores the audio URL too, so the Home speaker button can play today's
 * pronunciation without re-hitting the API.
 */
public class WotdCache {

    private static final String PREFS_NAME = "wordspark_wotd_cache";
    private static final String KEY_DATE = "cached_date_key";
    private static final String KEY_WORD = "cached_word";
    private static final String KEY_MEANING = "cached_meaning";
    private static final String KEY_AUDIO = "cached_audio_url";

    public static class Cached {
        public final String word;
        public final String meaning;
        public final String audioUrl; // may be null/empty - the API often has no audio

        Cached(String word, String meaning, String audioUrl) {
            this.word = word;
            this.meaning = meaning;
            this.audioUrl = audioUrl;
        }

        public boolean hasAudio() {
            return audioUrl != null && !audioUrl.trim().isEmpty();
        }
    }

    /** Returns the cached entry only if it was cached for today's date key; otherwise null. */
    public static Cached getForToday(Context context) {
        SharedPreferences prefs = prefs(context);
        String cachedDateKey = prefs.getString(KEY_DATE, null);
        if (cachedDateKey == null || !cachedDateKey.equals(WordOfDay.getTodayKey())) {
            return null; // stale (yesterday's) cache - force a fresh fetch
        }
        String meaning = prefs.getString(KEY_MEANING, "");
        if (meaning.contains("Offline definition for") || meaning.contains("Stored securely in local")) {
            return null; // invalidate fake WOTD cache
        }
        return new Cached(
                prefs.getString(KEY_WORD, ""),
                meaning,
                prefs.getString(KEY_AUDIO, ""));
    }

    public static void save(Context context, String word, String meaning, String audioUrl) {
        if (meaning != null && (meaning.contains("Offline definition for") || meaning.contains("Stored securely in local"))) {
            return; // Never cache fake definitions
        }
        prefs(context).edit()
                .putString(KEY_DATE, WordOfDay.getTodayKey())
                .putString(KEY_WORD, word)
                .putString(KEY_MEANING, meaning)
                .putString(KEY_AUDIO, audioUrl == null ? "" : audioUrl)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
