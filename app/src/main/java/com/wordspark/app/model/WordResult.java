package com.wordspark.app.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The fully parsed result of a Dictionary API lookup for one word.
 * Any field the API did not return is left null/empty rather than populated
 * with placeholder text, so the UI can simply hide that section.
 */
public class WordResult {
    private final String word;
    private String phonetic;      // may be null
    private String audioUrl;      // may be null
    private final List<Meaning> meanings = new ArrayList<>();

    public WordResult(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public List<Meaning> getMeanings() {
        return meanings;
    }

    public boolean hasPhonetic() {
        return phonetic != null && !phonetic.trim().isEmpty();
    }

    public boolean hasAudio() {
        return audioUrl != null && !audioUrl.trim().isEmpty();
    }

    /** Returns the first available definition text, used for Favorites summary + history preview. */
    public String getPrimaryMeaning() {
        for (Meaning m : meanings) {
            if (!m.getDefinitions().isEmpty()) {
                return m.getDefinitions().get(0).getDefinition();
            }
        }
        return "";
    }

    public String getPrimaryPartOfSpeech() {
        if (!meanings.isEmpty()) {
            return meanings.get(0).getPartOfSpeech();
        }
        return "";
    }
}
