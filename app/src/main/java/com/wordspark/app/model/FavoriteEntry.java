package com.wordspark.app.model;

public class FavoriteEntry {
    private final int id;
    private final String word;
    private final String meaning;
    private final String partOfSpeech;
    private final String pronunciation;

    public FavoriteEntry(int id, String word, String meaning, String partOfSpeech, String pronunciation) {
        this.id = id;
        this.word = word;
        this.meaning = meaning;
        this.partOfSpeech = partOfSpeech;
        this.pronunciation = pronunciation;
    }

    public int getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public String getPronunciation() {
        return pronunciation;
    }
}
