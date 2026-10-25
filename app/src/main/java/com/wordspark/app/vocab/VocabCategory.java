package com.wordspark.app.vocab;

import android.content.Context;
import java.util.List;

public class VocabCategory {
    private final String id;
    private final String name;
    private final List<String> allWords;

    public VocabCategory(String id, String name, List<String> allWords) {
        this.id = id;
        this.name = name;
        this.allWords = allWords;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getAllWords() {
        return allWords;
    }

    public List<String> getWords(Context context) {
        return VocabularyBank.getDailyWordsForCategory(context, this);
    }

    public int getWordCount(Context context) {
        return getWords(context).size();
    }
}
