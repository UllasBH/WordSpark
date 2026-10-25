package com.wordspark.app.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One "part of speech" block from the Dictionary API, e.g. all the noun
 * definitions of a word, plus any synonyms, antonyms, inflections, usage labels,
 * or etymology attached to it.
 */
public class Meaning {
    private final String partOfSpeech;
    private final List<Definition> definitions = new ArrayList<>();
    private final List<String> synonyms = new ArrayList<>();
    private final List<String> antonyms = new ArrayList<>();
    private final List<String> inflections = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private String etymology;

    public Meaning(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public List<String> getAntonyms() {
        return antonyms;
    }

    public List<String> getInflections() {
        return inflections;
    }

    public List<String> getLabels() {
        return labels;
    }

    public String getEtymology() {
        return etymology;
    }

    public void setEtymology(String etymology) {
        this.etymology = etymology;
    }

    public boolean hasSynonyms() {
        return !synonyms.isEmpty();
    }

    public boolean hasAntonyms() {
        return !antonyms.isEmpty();
    }

    public boolean hasInflections() {
        return !inflections.isEmpty();
    }

    public boolean hasLabels() {
        return !labels.isEmpty();
    }

    public boolean hasEtymology() {
        return etymology != null && !etymology.trim().isEmpty();
    }
}
