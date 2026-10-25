package com.wordspark.app.model;

public class UserSettings {

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    private String theme = THEME_LIGHT; // Default theme is light
    private int historyLimit = 50;
    private boolean notificationsEnabled = false;
    private String notificationTime = "09:00"; // 24h "HH:mm"
    private int quizBestScore = 0;
    private int quizBestTotal = 0; // 0 means "no quiz taken yet"
    private int quizAttempts = 0;
    private int quizTotalCorrect = 0;
    private int flashcardsReviewed = 0;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getHistoryLimit() {
        return historyLimit;
    }

    public void setHistoryLimit(int historyLimit) {
        this.historyLimit = historyLimit;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(String notificationTime) {
        this.notificationTime = notificationTime;
    }

    public int getQuizBestScore() {
        return quizBestScore;
    }

    public void setQuizBestScore(int quizBestScore) {
        this.quizBestScore = quizBestScore;
    }

    public int getQuizBestTotal() {
        return quizBestTotal;
    }

    public void setQuizBestTotal(int quizBestTotal) {
        this.quizBestTotal = quizBestTotal;
    }

    public boolean hasQuizBestScore() {
        return quizBestTotal > 0;
    }

    public int getQuizAttempts() {
        return quizAttempts;
    }

    public void setQuizAttempts(int quizAttempts) {
        this.quizAttempts = quizAttempts;
    }

    public int getQuizTotalCorrect() {
        return quizTotalCorrect;
    }

    public void setQuizTotalCorrect(int quizTotalCorrect) {
        this.quizTotalCorrect = quizTotalCorrect;
    }

    public int getFlashcardsReviewed() {
        return flashcardsReviewed;
    }

    public void setFlashcardsReviewed(int flashcardsReviewed) {
        this.flashcardsReviewed = flashcardsReviewed;
    }

    public int getNotificationHour() {
        return parseTimePart(0);
    }

    public int getNotificationMinute() {
        return parseTimePart(1);
    }

    private int parseTimePart(int index) {
        try {
            String[] parts = notificationTime.split(":");
            return Integer.parseInt(parts[index]);
        } catch (Exception e) {
            return index == 0 ? 9 : 0; // fall back to 09:00
        }
    }
}
