package com.wordspark.app.util;

import android.content.Context;
import com.wordspark.app.vocab.VocabCategory;
import com.wordspark.app.vocab.VocabularyBank;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Picks today's "Word of the Day" deterministically from today's vocabulary pools,
 * based on the calendar date. No storage is required: the same date always
 * maps to the same word, so re-creating the Activity (or reopening the app
 * later the same day) never changes it - and it automatically advances the
 * next calendar day.
 */
public class WordOfDay {

    public static String getTodayWord(Context context, int attemptIndex) {
        List<String> allTodayWords = new ArrayList<>();
        for (VocabCategory cat : VocabularyBank.getCategories()) {
            allTodayWords.addAll(VocabularyBank.getDailyWordsForCategory(context, cat));
        }
        if (allTodayWords.isEmpty()) {
            return "serendipity";
        }
        Calendar calendar = Calendar.getInstance();
        int seed = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR) + attemptIndex;
        int index = Math.abs(seed) % allTodayWords.size();
        return allTodayWords.get(index);
    }

    public static String getTodayWord(Context context) {
        return getTodayWord(context, 0);
    }

    /** A stable key for today's date (e.g. "2026-259"), used to cache today's fetched WOTD details. */
    public static String getTodayKey() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.DAY_OF_YEAR);
    }
}
