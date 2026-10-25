package com.wordspark.app.model;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestion {
    private final String id;
    private final String category;
    private final String word;
    private final String questionText;
    private final String correctAnswer;
    private final List<String> wrongAnswers;
    private final String explanation; // optional, may be null

    public QuizQuestion(String id, String category, String word, String questionText,
                         String correctAnswer, List<String> wrongAnswers, String explanation) {
        this.id = id;
        this.category = category;
        this.word = word;
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        this.wrongAnswers = wrongAnswers;
        this.explanation = explanation;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getWord() {
        return word;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getWrongAnswers() {
        return wrongAnswers;
    }

    /** Returns a NEW list containing all 4 options in random order (correct answer's position is not fixed). */
    public List<String> shuffledOptions() {
        List<String> options = new ArrayList<>(wrongAnswers);
        options.add(correctAnswer);
        java.util.Collections.shuffle(options);
        return options;
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean hasExplanation() {
        return explanation != null && !explanation.trim().isEmpty();
    }
}
