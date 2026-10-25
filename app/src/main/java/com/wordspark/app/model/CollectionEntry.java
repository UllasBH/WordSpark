package com.wordspark.app.model;

public class CollectionEntry {
    private final int id;
    private final String name;

    public CollectionEntry(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
