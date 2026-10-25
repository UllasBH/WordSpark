package com.wordspark.app.model;

/**
 * A single definition entry from the Dictionary API response, along with an
 * optional example sentence.
 */
public class Definition {
    private final String definition;
    private final String example; // may be null/empty if API did not provide one

    public Definition(String definition, String example) {
        this.definition = definition;
        this.example = example;
    }

    public String getDefinition() {
        return definition;
    }

    public String getExample() {
        return example;
    }

    public boolean hasExample() {
        return example != null && !example.trim().isEmpty();
    }
}
