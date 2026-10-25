package com.wordspark.app;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.wordspark.app.db.FavoriteDao;
import com.wordspark.app.db.HistoryDao;
import com.wordspark.app.db.SettingsDao;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.model.WordResult;
import com.wordspark.app.network.ApiCallback;
import com.wordspark.app.network.DictionaryApiClient;
import com.wordspark.app.util.NetworkUtil;
import com.wordspark.app.util.SessionManager;
import com.wordspark.app.util.WordOfDay;
import com.wordspark.app.util.WotdCache;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String[] EXPLORE_WORDS = {"serendipity", "luminous", "resilient"};

    private LinearLayout exploreWordsContainer, statsContainer;
    private TextView tvGreeting, tvWotdWord, tvWotdMeaning;
    private View tvWotdViewDetails, btnPlayWotd, wotdProgress, wotdErrorContainer, btnWotdRetry;

    private DictionaryApiClient apiClient;
    private FavoriteDao favoriteDao;
    private HistoryDao historyDao;
    private SettingsDao settingsDao;
    private String todayWord;
    private int wotdAttempt = 0;
    private boolean isLoadingWotd = false;      // prevents duplicate WOTD API calls
    private String todayAudioUrl;               // null/empty when the API has no audio
    private MediaPlayer wotdPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = new DictionaryApiClient();
        favoriteDao = new FavoriteDao(this);
        historyDao = new HistoryDao(this);
        settingsDao = new SettingsDao(this);
        todayWord = WordOfDay.getTodayWord(this, wotdAttempt);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvWotdWord = findViewById(R.id.tvWotdWord);
        tvWotdMeaning = findViewById(R.id.tvWotdMeaning);
        tvWotdViewDetails = findViewById(R.id.tvWotdViewDetails);
        btnPlayWotd = findViewById(R.id.btnPlayWotd);
        wotdProgress = findViewById(R.id.wotdProgress);
        wotdErrorContainer = findViewById(R.id.wotdErrorContainer);
        btnWotdRetry = findViewById(R.id.btnWotdRetry);
        exploreWordsContainer = findViewById(R.id.exploreWordsContainer);
        statsContainer = findViewById(R.id.statsContainer);

        findViewById(R.id.tvSearchBar).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        View.OnClickListener openWotdDetail = v -> openWordDetail(todayWord);
        findViewById(R.id.wotdCard).setOnClickListener(openWotdDetail);
        tvWotdViewDetails.setOnClickListener(openWotdDetail);
        // The speaker plays today's pronunciation rather than opening Details,
        // so the icon matches what it actually does.
        btnPlayWotd.setOnClickListener(v -> playWotdAudio());
        btnWotdRetry.setOnClickListener(v -> {
            wotdAttempt = 0;
            todayWord = WordOfDay.getTodayWord(this, wotdAttempt);
            loadWordOfDay();
        });

        findViewById(R.id.btnSaved).setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btnQuiz).setOnClickListener(v ->
                startActivity(new Intent(this, QuizActivity.class)));
        findViewById(R.id.btnVocabLists).setOnClickListener(v ->
                startActivity(new Intent(this, VocabularyListsActivity.class)));

        buildExploreWordChips();
        setupBottomNav();
        showGreeting();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!SessionManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupBottomNav(); // refresh active tab highlighting each time Home is shown

        // Recompute here rather than only in onCreate(): if the app is left open
        // across midnight, Home rolls over to the new day's word on return.
        // WordOfDay is deterministic, so this stays stable within a single day.
        todayWord = WordOfDay.getTodayWord(this);

        loadWordOfDay();
        loadStats();
    }

    private void openWordDetail(String word) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_WORD, word);
        startActivity(intent);
    }

    private void showGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String rawName = SessionManager.getCurrentUserName(this);
        String formattedName = "User";
        if (rawName != null && !rawName.trim().isEmpty()) {
            String[] parts = rawName.trim().split("\\s+");
            formattedName = capitalize(parts[0]);
        }
        int templateRes = hour < 12 ? R.string.greeting_morning
                : hour < 18 ? R.string.greeting_afternoon
                : R.string.greeting_evening;
        tvGreeting.setText(getString(templateRes, formattedName));
    }

    /**
     * Loads today's Word of the Day. Uses today's cached copy if we already
     * fetched it once (no repeated network calls per screen view); falls
     * back to a friendly error+retry state if the API/network fails and
     * nothing is cached for today yet. Never touches Search History - only
     * explicitly opening Word Details does that.
     */
    private void loadWordOfDay() {
        WotdCache.Cached cached = WotdCache.getForToday(this);
        if (cached != null) {
            todayAudioUrl = cached.audioUrl;
            todayWord = cached.word;
            showWotdContent(cached.word, cached.meaning);
            return;
        }

        if (isLoadingWotd) return; // a request is already in flight - don't stack another

        showWotdLoading();

        if (!NetworkUtil.isConnected(this)) {
            showWotdError();
            return;
        }

        if (todayWord == null || todayWord.trim().isEmpty()) {
            todayWord = WordOfDay.getTodayWord(this, wotdAttempt);
        }

        isLoadingWotd = true;
        apiClient.lookup(this, todayWord, new ApiCallback() {
            @Override
            public void onSuccess(WordResult result) {
                isLoadingWotd = false;
                wotdAttempt = 0;
                if (isGone()) return;

                String meaning = result.getPrimaryMeaning();
                String word = capitalize(result.getWord());
                todayAudioUrl = result.getAudioUrl();
                todayWord = word;
                WotdCache.save(MainActivity.this, word, meaning, todayAudioUrl);
                showWotdContent(word, meaning);
            }

            @Override
            public void onWordNotFound() {
                isLoadingWotd = false;
                if (isGone()) return;

                com.wordspark.app.vocab.VocabularyBank.excludeWord(MainActivity.this, todayWord);

                if (wotdAttempt < 3 && NetworkUtil.isConnected(MainActivity.this)) {
                    wotdAttempt++;
                    todayWord = WordOfDay.getTodayWord(MainActivity.this, wotdAttempt);
                    loadWordOfDay();
                } else {
                    showWotdError();
                }
            }

            @Override
            public void onNetworkError() {
                isLoadingWotd = false;
                if (!isGone()) showWotdError();
            }

            @Override
            public void onApiError() {
                isLoadingWotd = false;
                if (!isGone()) showWotdError();
            }
        });
    }

    /** True once this Activity can no longer safely have its views touched. */
    private boolean isGone() {
        return isFinishing()
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed());
    }

    /**
     * Plays today's pronunciation if the API supplied an audio URL.
     * The speaker button is hidden entirely when no audio exists, so this
     * is only reachable when todayAudioUrl is usable.
     */
    private void playWotdAudio() {
        if (todayAudioUrl == null || todayAudioUrl.trim().isEmpty()) return;

        releaseWotdPlayer(); // never stack players on rapid taps
        try {
            wotdPlayer = new MediaPlayer();
            wotdPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            wotdPlayer.setDataSource(todayAudioUrl);
            wotdPlayer.setOnPreparedListener(MediaPlayer::start);
            wotdPlayer.setOnCompletionListener(mp -> releaseWotdPlayer());
            wotdPlayer.setOnErrorListener((mp, what, extra) -> {
                releaseWotdPlayer();
                if (!isGone()) {
                    Toast.makeText(MainActivity.this, R.string.err_audio_playback, Toast.LENGTH_SHORT).show();
                }
                return true; // handled - prevents the default crash path
            });
            wotdPlayer.prepareAsync();
        } catch (Exception e) {
            releaseWotdPlayer();
            Toast.makeText(this, R.string.err_audio_playback, Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseWotdPlayer() {
        if (wotdPlayer != null) {
            try {
                wotdPlayer.release();
            } catch (Exception ignored) {
                // Already released / never prepared - nothing to do.
            }
            wotdPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseWotdPlayer(); // don't keep audio or the player alive off-screen
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseWotdPlayer();
    }

    private void showWotdLoading() {
        wotdProgress.setVisibility(View.VISIBLE);
        wotdErrorContainer.setVisibility(View.GONE);
        tvWotdWord.setVisibility(View.GONE);
        tvWotdMeaning.setVisibility(View.GONE);
        tvWotdViewDetails.setVisibility(View.GONE);
        btnPlayWotd.setVisibility(View.GONE);
    }

    private void showWotdContent(String word, String meaning) {
        wotdProgress.setVisibility(View.GONE);
        wotdErrorContainer.setVisibility(View.GONE);
        tvWotdWord.setVisibility(View.VISIBLE);
        tvWotdMeaning.setVisibility(View.VISIBLE);
        tvWotdViewDetails.setVisibility(View.VISIBLE);
        // Hidden when the API gave us no audio for this word, so the speaker
        // is never shown as a control that does nothing.
        boolean hasAudio = todayAudioUrl != null && !todayAudioUrl.trim().isEmpty();
        btnPlayWotd.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
        tvWotdWord.setText(word);
        tvWotdMeaning.setText(meaning);
    }

    private void showWotdError() {
        wotdProgress.setVisibility(View.GONE);
        tvWotdWord.setVisibility(View.GONE);
        tvWotdMeaning.setVisibility(View.GONE);
        tvWotdViewDetails.setVisibility(View.GONE);
        btnPlayWotd.setVisibility(View.GONE);
        wotdErrorContainer.setVisibility(View.VISIBLE);
    }

    /** Simple local stats built from data that already exists (Favorites count, unique words searched, quiz best score). */
    private void loadStats() {
        int userId = SessionManager.getCurrentUserId(this);
        new Thread(() -> {
            int favoritesCount = favoriteDao.getFavorites(userId).size();
            int searchedCount = historyDao.getHistory(userId).size(); // history is de-duplicated per word already
            UserSettings settings = settingsDao.getSettings(userId);

            runOnUiThread(() -> {
                if (isGone()) return;
                statsContainer.removeAllViews();
                addStat(String.valueOf(favoritesCount), getString(R.string.stats_favorites));
                addStatDivider();
                addStat(String.valueOf(searchedCount), getString(R.string.stats_words_searched));
                if (settings.hasQuizBestScore()) {
                    addStatDivider();
                    addStat(settings.getQuizBestScore() + "/" + settings.getQuizBestTotal(), getString(R.string.stats_quiz_best));
                }
            });
        }).start();
    }

    private void addStat(String value, String label) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        column.setLayoutParams(params);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(this, R.color.primary));
        valueView.setTextSize(20);
        valueView.setGravity(android.view.Gravity.CENTER);
        valueView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        labelView.setTextSize(12);
        labelView.setGravity(android.view.Gravity.CENTER);

        column.addView(valueView);
        column.addView(labelView);
        statsContainer.addView(column);
    }

    private void addStatDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
        statsContainer.addView(divider);
    }

    private void buildExploreWordChips() {
        exploreWordsContainer.removeAllViews();
        for (String word : EXPLORE_WORDS) {
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(10));
            chip.setLayoutParams(params);
            chip.setText(capitalize(word));
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
            chip.setTextSize(14);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setOnClickListener(v -> openWordDetail(word));
            exploreWordsContainer.addView(chip);
        }
    }

    private void setupBottomNav() {
        View navHistory = findViewById(R.id.navHistory);
        View navSaved = findViewById(R.id.navSaved);
        View navQuiz = findViewById(R.id.navQuiz);

        navHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        navSaved.setOnClickListener(v -> startActivity(new Intent(this, FavoritesActivity.class)));
        navQuiz.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));

        highlightNavItem(R.id.ivNavHome, R.id.tvNavHome, true);
        highlightNavItem(R.id.ivNavHistory, R.id.tvNavHistory, false);
        highlightNavItem(R.id.ivNavSaved, R.id.tvNavSaved, false);
        highlightNavItem(R.id.ivNavQuiz, R.id.tvNavQuiz, false);
    }

    private void highlightNavItem(int iconId, int labelId, boolean active) {
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);
        int color = ContextCompat.getColor(this, active ? R.color.primary : R.color.text_secondary);
        icon.setColorFilter(color);
        label.setTextColor(color);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
