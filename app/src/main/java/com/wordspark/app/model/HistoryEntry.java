package com.wordspark.app.model;

public class HistoryEntry {
    private final int id;
    private final String word;
    private final long timestamp; // epoch millis of the search

    public HistoryEntry(int id, String word, long timestamp) {
        this.id = id;
        this.word = word;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
