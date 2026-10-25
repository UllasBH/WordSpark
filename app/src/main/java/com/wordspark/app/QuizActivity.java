package com.wordspark.app;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.wordspark.app.db.SettingsDao;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.model.QuizQuestion;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.quiz.QuizBank;
import com.wordspark.app.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final int QUIZ_LENGTH = 10;

    private TextView tvProgress, tvQuestion, tvFeedbackTitle, tvFeedbackDetail;
    private TextView tvScore, tvAccuracy, tvBestScore;
    private LinearLayout optionsContainer, feedbackContainer;
    private View quizContainer, resultContainer, btnNext, btnTryAgain, btnBackHome, btnBack;

    private final List<QuizQuestion> currentQuiz = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private boolean isAnswered = false;
    private boolean onResultsScreen = false;

    private SettingsDao settingsDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        settingsDao = new SettingsDao(this);

        tvProgress = findViewById(R.id.tvProgress);
        tvQuestion = findViewById(R.id.tvQuestion);
        optionsContainer = findViewById(R.id.optionsContainer);
        feedbackContainer = findViewById(R.id.feedbackContainer);
        tvFeedbackTitle = findViewById(R.id.tvFeedbackTitle);
        tvFeedbackDetail = findViewById(R.id.tvFeedbackDetail);
        quizContainer = findViewById(R.id.quizContainer);
        resultContainer = findViewById(R.id.resultContainer);
        tvScore = findViewById(R.id.tvScore);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvBestScore = findViewById(R.id.tvBestScore);
        btnNext = findViewById(R.id.btnNext);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        btnBackHome = findViewById(R.id.btnBackHome);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> handleBackPressed());
        btnNext.setOnClickListener(v -> goToNextQuestion());
        btnTryAgain.setOnClickListener(v -> startNewQuiz());
        btnBackHome.setOnClickListener(v -> finish());

        if (savedInstanceState != null && restoreQuizState(savedInstanceState)) {
            // Rotation/recreation: carry on with the same quiz rather than
            // silently throwing away the user's progress.
        } else {
            startNewQuiz();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    private void handleBackPressed() {
        if (onResultsScreen) {
            super.onBackPressed(); // no progress to lose on the results screen
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_quiz_title)
                .setMessage(R.string.exit_quiz_message)
                .setPositiveButton(R.string.exit, (dialog, which) -> QuizActivity.super.onBackPressed())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void startNewQuiz() {
        currentQuiz.clear();
        currentQuiz.addAll(QuizBank.selectQuizQuestions(this, QUIZ_LENGTH));

        currentIndex = 0;
        correctCount = 0;
        onResultsScreen = false;

        resultContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);

        showQuestion(currentIndex);
    }

    private void showQuestion(int index) {
        isAnswered = false;
        QuizQuestion question = currentQuiz.get(index);

        tvProgress.setText((index + 1) + " / " + currentQuiz.size());
        tvQuestion.setText(question.getQuestionText());

        feedbackContainer.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        optionsContainer.removeAllViews();
        for (String option : question.shuffledOptions()) {
            optionsContainer.addView(buildOptionView(option, question));
        }
    }

    private TextView buildOptionView(String optionText, QuizQuestion question) {
        TextView option = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(10);
        option.setLayoutParams(params);
        option.setText(optionText);
        option.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        option.setTextSize(15);
        option.setBackgroundResource(R.drawable.bg_card);
        option.setPadding(dp(16), dp(14), dp(16), dp(14));
        option.setOnClickListener(v -> onOptionSelected(optionText, question));
        return option;
    }

    private void onOptionSelected(String selected, QuizQuestion question) {
        if (isAnswered) return; // once answered, choices are locked
        isAnswered = true;

        boolean correct = selected.equals(question.getCorrectAnswer());
        if (correct) correctCount++;

        int correctColor = ContextCompat.getColor(this, R.color.primary);
        int errorColor = ContextCompat.getColor(this, R.color.error);

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            TextView optionView = (TextView) optionsContainer.getChildAt(i);
            String optionText = optionView.getText().toString();
            optionView.setClickable(false);

            if (optionText.equals(question.getCorrectAnswer())) {
                tintOption(optionView, correctColor);
            } else if (optionText.equals(selected)) {
                tintOption(optionView, errorColor);
            } else {
                optionView.setAlpha(0.5f);
            }
        }

        feedbackContainer.setVisibility(View.VISIBLE);
        if (correct) {
            tvFeedbackTitle.setText(R.string.correct_label);
            tvFeedbackTitle.setTextColor(correctColor);
            tvFeedbackDetail.setText(question.hasExplanation() ? question.getExplanation() : "");
            tvFeedbackDetail.setVisibility(question.hasExplanation() ? View.VISIBLE : View.GONE);
        } else {
            tvFeedbackTitle.setText(R.string.incorrect_label);
            tvFeedbackTitle.setTextColor(errorColor);
            String detail = getString(R.string.correct_answer_prefix) + " " + question.getCorrectAnswer() + ".";
            tvFeedbackDetail.setText(detail);
            tvFeedbackDetail.setVisibility(View.VISIBLE);
        }

        boolean isLastQuestion = currentIndex == currentQuiz.size() - 1;
        ((android.widget.Button) btnNext).setText(isLastQuestion ? R.string.see_results : R.string.next);
        btnNext.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- state

    private static final String STATE_QUESTION_IDS = "state_question_ids";
    private static final String STATE_INDEX = "state_index";
    private static final String STATE_CORRECT = "state_correct";
    private static final String STATE_ANSWERED = "state_answered";
    private static final String STATE_ON_RESULTS = "state_on_results";

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store the questions by their word key rather than serialising
        // QuizQuestion objects. QuizBank.all() returns freshly-built objects on
        // every call (and QuizQuestion has no equals()), so identity/index
        // lookups would not survive recreation - the word is the stable key.
        String[] ids = new String[currentQuiz.size()];
        for (int i = 0; i < currentQuiz.size(); i++) {
            ids[i] = currentQuiz.get(i).getWord();
        }
        outState.putStringArray(STATE_QUESTION_IDS, ids);
        outState.putInt(STATE_INDEX, currentIndex);
        outState.putInt(STATE_CORRECT, correctCount);
        outState.putBoolean(STATE_ANSWERED, isAnswered);
        outState.putBoolean(STATE_ON_RESULTS, onResultsScreen);
    }

    /** Rebuilds an in-progress quiz. Returns false if the state can't be trusted. */
    private boolean restoreQuizState(Bundle state) {
        String[] ids = state.getStringArray(STATE_QUESTION_IDS);
        if (ids == null || ids.length == 0) return false;

        List<QuizQuestion> bank = QuizBank.all();
        currentQuiz.clear();
        for (String word : ids) {
            QuizQuestion match = null;
            for (QuizQuestion q : bank) {
                if (q.getWord().equals(word)) {
                    match = q;
                    break;
                }
            }
            if (match == null) {
                currentQuiz.clear();
                return false; // bank changed underneath us - safer to start fresh
            }
            currentQuiz.add(match);
        }

        currentIndex = state.getInt(STATE_INDEX, 0);
        correctCount = state.getInt(STATE_CORRECT, 0);
        onResultsScreen = state.getBoolean(STATE_ON_RESULTS, false);
        boolean wasAnswered = state.getBoolean(STATE_ANSWERED, false);

        if (currentIndex < 0 || currentIndex >= currentQuiz.size()) {
            currentQuiz.clear();
            return false;
        }

        if (onResultsScreen) {
            showResults();
            return true;
        }

        resultContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        showQuestion(currentIndex);

        if (wasAnswered) {
            // The question was already answered before recreation. Re-lock it and
            // reveal the correct answer, without re-counting the score.
            lockAnsweredQuestion(currentQuiz.get(currentIndex));
        }
        return true;
    }

    /** Re-applies the post-answer state (options disabled, correct answer shown). */
    private void lockAnsweredQuestion(QuizQuestion question) {
        isAnswered = true;
        int correctColor = ContextCompat.getColor(this, R.color.primary);

        for (int i = 0; i < optionsContainer.getChildCount(); i++) {
            TextView optionView = (TextView) optionsContainer.getChildAt(i);
            optionView.setClickable(false);
            if (optionView.getText().toString().equals(question.getCorrectAnswer())) {
                tintOption(optionView, correctColor);
            } else {
                optionView.setAlpha(0.5f);
            }
        }

        feedbackContainer.setVisibility(View.VISIBLE);
        tvFeedbackTitle.setText(R.string.correct_answer_prefix);
        tvFeedbackTitle.setTextColor(correctColor);
        tvFeedbackDetail.setText(question.getCorrectAnswer());
        tvFeedbackDetail.setVisibility(View.VISIBLE);

        boolean isLastQuestion = currentIndex == currentQuiz.size() - 1;
        ((android.widget.Button) btnNext).setText(isLastQuestion ? R.string.see_results : R.string.next);
        btnNext.setVisibility(View.VISIBLE);
    }

    private void tintOption(TextView optionView, int color) {
        optionView.setBackgroundTintList(ColorStateList.valueOf(color));
        optionView.getBackground().setAlpha(40); // soft tint, keeps text readable
        optionView.setTextColor(color);
    }

    private void goToNextQuestion() {
        if (currentIndex < currentQuiz.size() - 1) {
            currentIndex++;
            showQuestion(currentIndex);
        } else {
            showResults();
        }
    }

    private void showResults() {
        onResultsScreen = true;
        quizContainer.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        resultContainer.setVisibility(View.VISIBLE);

        int total = currentQuiz.size();
        int percentage = total == 0 ? 0 : Math.round((correctCount * 100f) / total);

        tvScore.setText(correctCount + " / " + total);
        tvAccuracy.setText(percentage + "% Accuracy");

        if (SessionManager.isLoggedIn(this)) {
            int userId = SessionManager.getCurrentUserId(this);
            new Thread(() -> {
                settingsDao.updateQuizBestScoreIfHigher(userId, correctCount, total);
                UserSettings settings = settingsDao.getSettings(userId);
                runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                    if (settings.hasQuizBestScore()) {
                        tvBestScore.setVisibility(View.VISIBLE);
                        tvBestScore.setText(getString(R.string.best_score_format,
                                settings.getQuizBestScore(), settings.getQuizBestTotal()));
                    }
                });
            }).start();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
