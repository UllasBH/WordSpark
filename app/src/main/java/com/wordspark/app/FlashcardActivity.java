package com.wordspark.app;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.wordspark.app.db.FavoriteDao;
import com.wordspark.app.model.Definition;
import com.wordspark.app.model.Meaning;
import com.wordspark.app.model.WordResult;
import com.wordspark.app.network.ApiCallback;
import com.wordspark.app.network.DictionaryApiClient;
import com.wordspark.app.vocab.VocabCategory;
import com.wordspark.app.vocab.VocabularyBank;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.util.NetworkUtil;
import com.wordspark.app.util.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FlashcardActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";

    private TextView tvCardProgress, tvCardWord;
    private TextView tvRevealedPhonetic, tvCardError, btnReadCardAloud;
    private LinearLayout unrevealedLayout, cardContainer, meaningsContainer;
    private ScrollView revealedLayout;
    private ProgressBar cardProgress;
    private Button btnPrevious, btnNext, btnSaveWord;

    private List<String> words;
    private int currentIndex = 0;
    private boolean isRevealed = false;
    private boolean isLoading = false;
    private boolean isFavorite = false;
    private boolean isReadingCard = false;
    private String categoryId;

    private DictionaryApiClient apiClient;
    private FavoriteDao favoriteDao;
    private com.wordspark.app.db.SettingsDao settingsDao;
    private MediaPlayer mediaPlayer;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private WordResult currentResult;

    private static final java.util.Set<String> TECH_KEYWORDS = new java.util.HashSet<>(Arrays.asList(
            "computer", "software", "program", "data", "network", "digital", "system", "code",
            "cyber", "web", "internet", "electronic", "processor", "file", "device", "storage",
            "algorithm", "application", "server", "information", "online", "virtual", "user",
            "machine", "tech", "technology", "compute", "disk", "screen", "display", "input",
            "output", "memory", "cpu", "bytes", "link", "site", "connect", "transfer"
    ));

    private static final java.util.Set<String> BUSINESS_KEYWORDS = new java.util.HashSet<>(Arrays.asList(
            "business", "financial", "money", "company", "market", "trade", "commercial",
            "economy", "corporate", "profit", "revenue", "management", "asset", "capital",
            "investment", "sales", "work", "employment", "employer", "organization", "firm",
            "industry", "product", "service", "cost", "price", "value", "valuable", "bank",
            "funds", "wealth", "enterprise", "merchant", "customer", "client",
            "transaction", "contract", "agreement", "salary", "wage", "stock", "shares"
    ));

    private static final java.util.Set<String> ACADEMIC_KEYWORDS = new java.util.HashSet<>(Arrays.asList(
            "study", "research", "science", "education", "university", "scholarly", "analysis",
            "theory", "method", "formal", "systematic", "knowledge", "literature", "test",
            "evidence", "experiment", "discipline", "academic", "professor", "student", "learning",
            "publication", "data", "observe", "result", "hypothesis", "variable", "structure",
            "concept", "principle", "evaluate", "assessment", "investigate", "scholarly"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        VocabCategory category = VocabularyBank.getCategory(categoryId);
        if (category == null) {
            finish();
            return;
        }

        words = category.getWords(this);
        if (words == null || words.isEmpty()) {
            finish();
            return;
        }

        apiClient = new DictionaryApiClient();
        favoriteDao = new FavoriteDao(this);
        settingsDao = new com.wordspark.app.db.SettingsDao(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvCardProgress = findViewById(R.id.tvCardProgress);
        tvCardWord = findViewById(R.id.tvCardWord);
        tvRevealedPhonetic = findViewById(R.id.tvRevealedPhonetic);
        meaningsContainer = findViewById(R.id.meaningsContainer);
        tvCardError = findViewById(R.id.tvCardError);
        btnReadCardAloud = findViewById(R.id.btnReadCardAloud);
        unrevealedLayout = findViewById(R.id.unrevealedLayout);
        revealedLayout = findViewById(R.id.revealedLayout);
        cardContainer = findViewById(R.id.cardContainer);
        cardProgress = findViewById(R.id.cardProgress);
        View btnPlayAudio = findViewById(R.id.btnPlayAudio);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnSaveWord = findViewById(R.id.btnSaveWord);

        tvCategoryTitle.setText(category.getName());

        cardContainer.setOnClickListener(v -> toggleReveal());
        btnPrevious.setOnClickListener(v -> navigate(-1));
        btnNext.setOnClickListener(v -> navigate(1));
        btnSaveWord.setOnClickListener(v -> toggleFavorite());
        btnPlayAudio.setOnClickListener(v -> playCurrentAudio());
        btnReadCardAloud.setOnClickListener(v -> toggleReadCardAloud());

        initTextToSpeech();
        displayCard();
    }

    private void displayCard() {
        stopSpeech();
        isReadingCard = false;
        isRevealed = false;
        currentResult = null;
        unrevealedLayout.setVisibility(View.VISIBLE);
        revealedLayout.setVisibility(View.GONE);
        cardProgress.setVisibility(View.GONE);
        tvCardError.setVisibility(View.GONE);
        if (meaningsContainer != null) meaningsContainer.removeAllViews();
        if (btnReadCardAloud != null) {
            btnReadCardAloud.setVisibility(View.GONE);
            btnReadCardAloud.setText(R.string.read_definition_aloud);
        }

        String word = words.get(currentIndex);
        tvCardWord.setText(capitalize(word));
        tvCardProgress.setText(getString(R.string.card_progress_format, (currentIndex + 1), words.size()));

        btnPrevious.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < words.size() - 1);

        updateFavoriteButtonState(word);
    }

    private void toggleReveal() {
        if (isRevealed) {
            stopSpeech();
            isReadingCard = false;
            if (btnReadCardAloud != null) btnReadCardAloud.setText(R.string.read_definition_aloud);
            isRevealed = false;
            unrevealedLayout.setVisibility(View.VISIBLE);
            revealedLayout.setVisibility(View.GONE);
        } else {
            isRevealed = true;
            unrevealedLayout.setVisibility(View.GONE);
            revealedLayout.setVisibility(View.VISIBLE);

            String word = words.get(currentIndex);
            TextView tvRevealedWord = findViewById(R.id.tvRevealedWord);
            if (tvRevealedWord != null) {
                tvRevealedWord.setText(capitalize(word));
            }

            if (SessionManager.isLoggedIn(this)) {
                int uId = SessionManager.getCurrentUserId(this);
                new Thread(() -> settingsDao.incrementFlashcardsReviewed(uId)).start();
            }

            if (currentResult != null) {
                renderRevealedData(currentResult);
            } else {
                fetchDefinition(word);
            }
        }
    }

    private void fetchDefinition(String word) {
        if (isLoading) return;
        isLoading = true;
        cardProgress.setVisibility(View.VISIBLE);
        if (meaningsContainer != null) meaningsContainer.removeAllViews();
        tvRevealedPhonetic.setVisibility(View.GONE);
        tvCardError.setVisibility(View.GONE);
        if (btnReadCardAloud != null) btnReadCardAloud.setVisibility(View.GONE);

        apiClient.lookup(this, word, new ApiCallback() {
            @Override
            public void onSuccess(WordResult result) {
                isLoading = false;
                if (LifecycleUtil.isGone(FlashcardActivity.this)) return;
                currentResult = result;
                cardProgress.setVisibility(View.GONE);
                renderRevealedData(result);
            }

            @Override
            public void onWordNotFound() {
                isLoading = false;
                if (LifecycleUtil.isGone(FlashcardActivity.this)) return;

                if (NetworkUtil.isConnected(FlashcardActivity.this)) {
                    // API confirmed word is unsupported/invalid -> permanently exclude
                    VocabularyBank.excludeWord(FlashcardActivity.this, word);

                    // Find a replacement from the same category pool
                    String replacementWord = null;
                    VocabCategory cat = VocabularyBank.getCategory(categoryId);
                    if (cat != null) {
                        java.util.Set<String> excluded = VocabularyBank.getExcludedWords(FlashcardActivity.this);
                        for (String candidate : cat.getAllWords()) {
                            String lowerCand = candidate.trim().toLowerCase(Locale.ROOT);
                            if (!excluded.contains(lowerCand) && !words.contains(candidate)) {
                                replacementWord = candidate;
                                break;
                            }
                        }
                    }

                    if (replacementWord != null) {
                        words.set(currentIndex, replacementWord);
                        fetchDefinition(replacementWord);
                    } else {
                        words.remove(currentIndex);
                        if (words.isEmpty()) {
                            cardProgress.setVisibility(View.GONE);
                            tvCardError.setText("No definitions available in this category.");
                            tvCardError.setVisibility(View.VISIBLE);
                        } else {
                            if (currentIndex >= words.size()) {
                                currentIndex = words.size() - 1;
                            }
                            displayCard();
                        }
                    }
                } else {
                    cardProgress.setVisibility(View.GONE);
                    tvCardError.setText(R.string.definition_unavailable_offline);
                    tvCardError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNetworkError() {
                isLoading = false;
                if (LifecycleUtil.isGone(FlashcardActivity.this)) return;
                cardProgress.setVisibility(View.GONE);
                tvCardError.setText(R.string.definition_unavailable_offline);
                tvCardError.setVisibility(View.VISIBLE);
            }

            @Override
            public void onApiError() {
                isLoading = false;
                if (LifecycleUtil.isGone(FlashcardActivity.this)) return;
                cardProgress.setVisibility(View.GONE);
                tvCardError.setText(R.string.definition_unavailable_offline);
                tvCardError.setVisibility(View.VISIBLE);
            }
        });
    }

    private List<Meaning> getCategoryRelevantMeanings(WordResult result, String catId) {
        if (result == null || result.getMeanings() == null) return new ArrayList<>();
        List<Meaning> originalMeanings = result.getMeanings();
        if (catId == null || originalMeanings.size() <= 1) {
            return originalMeanings;
        }

        java.util.Set<String> keywords;
        String lowerCat = catId.toLowerCase(Locale.ROOT);
        if (lowerCat.contains("tech")) {
            keywords = TECH_KEYWORDS;
        } else if (lowerCat.contains("business")) {
            keywords = BUSINESS_KEYWORDS;
        } else if (lowerCat.contains("academic")) {
            keywords = ACADEMIC_KEYWORDS;
        } else {
            return originalMeanings;
        }

        class ScoredMeaning {
            Meaning meaning;
            int score;
            ScoredMeaning(Meaning m, int s) { this.meaning = m; this.score = s; }
        }

        List<ScoredMeaning> scoredList = new ArrayList<>();
        for (Meaning m : originalMeanings) {
            int score = 0;
            String pos = m.getPartOfSpeech() != null ? m.getPartOfSpeech().toLowerCase(Locale.ROOT) : "";
            for (String kw : keywords) {
                if (pos.contains(kw)) score += 2;
            }
            for (Definition d : m.getDefinitions()) {
                String defText = d.getDefinition() != null ? d.getDefinition().toLowerCase(Locale.ROOT) : "";
                String exText = d.hasExample() ? d.getExample().toLowerCase(Locale.ROOT) : "";
                for (String kw : keywords) {
                    if (defText.contains(kw)) score += 3;
                    if (exText.contains(kw)) score += 1;
                }
            }
            scoredList.add(new ScoredMeaning(m, score));
        }

        scoredList.sort((a, b) -> Integer.compare(b.score, a.score));

        List<Meaning> sortedMeanings = new ArrayList<>();
        for (ScoredMeaning sm : scoredList) {
            sortedMeanings.add(sm.meaning);
        }
        return sortedMeanings;
    }

    private void renderRevealedData(WordResult result) {
        revealedLayout.setVisibility(View.VISIBLE);
        if (btnReadCardAloud != null) btnReadCardAloud.setVisibility(View.VISIBLE);

        if (result.hasPhonetic()) {
            tvRevealedPhonetic.setVisibility(View.VISIBLE);
            tvRevealedPhonetic.setText(result.getPhonetic());
        } else {
            tvRevealedPhonetic.setVisibility(View.GONE);
        }

        if (meaningsContainer != null) {
            meaningsContainer.removeAllViews();
            List<Meaning> relevantMeanings = getCategoryRelevantMeanings(result, categoryId);
            for (Meaning meaning : relevantMeanings) {
                meaningsContainer.addView(buildMeaningBlock(meaning));
            }
        }
    }

    private View buildMeaningBlock(Meaning meaning) {
        View block = LayoutInflater.from(this).inflate(R.layout.item_meaning_block, meaningsContainer, false);

        TextView tvPartOfSpeech = block.findViewById(R.id.tvPartOfSpeech);
        LinearLayout definitionsContainer = block.findViewById(R.id.definitionsContainer);
        TextView tvSynonymsLabel = block.findViewById(R.id.tvSynonymsLabel);
        View svSynonyms = block.findViewById(R.id.svSynonyms);
        LinearLayout synonymsChipsContainer = block.findViewById(R.id.synonymsChipsContainer);
        TextView tvAntonymsLabel = block.findViewById(R.id.tvAntonymsLabel);
        View svAntonyms = block.findViewById(R.id.svAntonyms);
        LinearLayout antonymsChipsContainer = block.findViewById(R.id.antonymsChipsContainer);

        tvPartOfSpeech.setText(meaning.getPartOfSpeech());
        if (definitionsContainer != null) {
            definitionsContainer.removeAllViews();
            List<Definition> definitions = meaning.getDefinitions();
            for (int i = 0; i < definitions.size(); i++) {
                Definition def = definitions.get(i);
                LinearLayout defItemLayout = new LinearLayout(this);
                defItemLayout.setOrientation(LinearLayout.VERTICAL);
                if (i > 0) {
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = dp(8);
                    defItemLayout.setLayoutParams(lp);
                }

                TextView tvDef = new TextView(this);
                String prefix = definitions.size() > 1 ? (i + 1) + ". " : "";
                tvDef.setText(prefix + def.getDefinition());
                tvDef.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                tvDef.setTextSize(14);
                defItemLayout.addView(tvDef);

                if (def.hasExample()) {
                    TextView tvEx = new TextView(this);
                    tvEx.setText("\u201C" + def.getExample() + "\u201D");
                    tvEx.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    tvEx.setTextSize(13);
                    LinearLayout.LayoutParams exLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    exLp.topMargin = dp(3);
                    tvEx.setLayoutParams(exLp);
                    defItemLayout.addView(tvEx);
                }

                definitionsContainer.addView(defItemLayout);
            }
        }

        if (tvSynonymsLabel != null) tvSynonymsLabel.setVisibility(View.GONE);
        if (svSynonyms != null) svSynonyms.setVisibility(View.GONE);
        if (tvAntonymsLabel != null) tvAntonymsLabel.setVisibility(View.GONE);
        if (svAntonyms != null) svAntonyms.setVisibility(View.GONE);

        return block;
    }

    private void navigate(int delta) {
        stopSpeech();
        int nextIndex = currentIndex + delta;
        if (nextIndex >= 0 && nextIndex < words.size()) {
            currentIndex = nextIndex;
            displayCard();
        }
    }

    private void updateFavoriteButtonState(String word) {
        if (!SessionManager.isLoggedIn(this)) {
            isFavorite = false;
            btnSaveWord.setText(R.string.save_word);
            return;
        }
        int userId = SessionManager.getCurrentUserId(this);
        new Thread(() -> {
            boolean fav = favoriteDao.isFavorite(userId, word);
            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                isFavorite = fav;
                btnSaveWord.setText(isFavorite ? R.string.saved : R.string.save_word);
            });
        }).start();
    }

    private void toggleFavorite() {
        if (!SessionManager.isLoggedIn(this)) {
            Toast.makeText(this, R.string.err_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentResult == null) {
            Toast.makeText(this, "Reveal card definition first", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = SessionManager.getCurrentUserId(this);
        boolean addingFavorite = !isFavorite;

        new Thread(() -> {
            if (addingFavorite) {
                favoriteDao.addFavorite(userId, currentResult);
            } else {
                favoriteDao.removeFavorite(userId, currentResult.getWord());
            }
            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                isFavorite = addingFavorite;
                btnSaveWord.setText(isFavorite ? R.string.saved : R.string.save_word);
                Toast.makeText(this,
                        addingFavorite ? R.string.added_to_favorites : R.string.removed_from_favorites,
                        Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void playCurrentAudio() {
        String word = words.get(currentIndex);
        if (currentResult != null && currentResult.hasAudio()) {
            playAudio(currentResult.getAudioUrl(), word);
        } else {
            speakWord(word);
        }
    }

    private void playAudio(String url, String wordForFallback) {
        stopSpeech();
        try {
            releaseMediaPlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mp -> releaseMediaPlayer());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                releaseMediaPlayer();
                if (!LifecycleUtil.isGone(this)) speakWord(wordForFallback);
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            releaseMediaPlayer();
            speakWord(wordForFallback);
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    private void initTextToSpeech() {
        try {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    int lang = tts.setLanguage(Locale.getDefault());
                    if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        lang = tts.setLanguage(Locale.US);
                    }
                    if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        lang = tts.setLanguage(Locale.ENGLISH);
                    }
                    ttsReady = (lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED);

                    if (ttsReady) {
                        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                            @Override public void onStart(String utteranceId) {}
                            @Override public void onDone(String utteranceId) {
                                runOnUiThread(() -> {
                                    if (LifecycleUtil.isGone(FlashcardActivity.this)) return;
                                    isReadingCard = false;
                                    if (btnReadCardAloud != null) btnReadCardAloud.setText(R.string.read_definition_aloud);
                                });
                            }
                            @Override public void onError(String utteranceId) {
                                runOnUiThread(() -> {
                                    if (LifecycleUtil.isGone(FlashcardActivity.this)) return;
                                    isReadingCard = false;
                                    if (btnReadCardAloud != null) btnReadCardAloud.setText(R.string.read_definition_aloud);
                                });
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            tts = null;
            ttsReady = false;
        }
    }

    private void speakWord(String word) {
        if (word == null || word.isEmpty() || !ttsReady || tts == null) return;
        releaseMediaPlayer();
        try {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "flashcard_word");
        } catch (Exception ignored) {}
    }

    private void toggleReadCardAloud() {
        if (isReadingCard) {
            stopSpeech();
            isReadingCard = false;
            if (btnReadCardAloud != null) btnReadCardAloud.setText(R.string.read_definition_aloud);
            return;
        }

        if (currentResult == null || !ttsReady || tts == null) {
            Toast.makeText(this, R.string.err_tts_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        String text = buildCardReadText(currentResult);
        if (text.isEmpty()) return;

        stopSpeech();
        isReadingCard = true;
        if (btnReadCardAloud != null) btnReadCardAloud.setText(R.string.stop_reading);

        try {
            int maxLen = TextToSpeech.getMaxSpeechInputLength() - 1;
            if (maxLen <= 0) maxLen = 3900;

            if (text.length() <= maxLen) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "card_utterance");
            } else {
                int length = text.length();
                int start = 0;
                boolean first = true;
                while (start < length) {
                    int end = Math.min(start + maxLen, length);
                    String chunk = text.substring(start, end).trim();
                    int mode = first ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
                    tts.speak(chunk, mode, null, "card_utterance");
                    first = false;
                    start = end;
                }
            }
        } catch (Exception e) {
            isReadingCard = false;
            if (btnReadCardAloud != null) btnReadCardAloud.setText(R.string.read_definition_aloud);
            Toast.makeText(this, R.string.err_tts_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private String buildCardReadText(WordResult result) {
        StringBuilder sb = new StringBuilder(result.getWord()).append(". ");
        List<Meaning> relevantMeanings = getCategoryRelevantMeanings(result, categoryId);
        for (Meaning meaning : relevantMeanings) {
            if (meaning.getPartOfSpeech() != null && !meaning.getPartOfSpeech().isEmpty()) {
                sb.append(meaning.getPartOfSpeech()).append(". ");
            }
            List<Definition> defs = meaning.getDefinitions();
            for (int i = 0; i < defs.size(); i++) {
                Definition def = defs.get(i);
                sb.append("Definition ").append(i + 1).append(": ").append(def.getDefinition()).append(". ");
                if (def.hasExample()) {
                    sb.append("Example: ").append(def.getExample()).append(". ");
                }
            }
        }
        return sb.toString().trim();
    }

    private void stopSpeech() {
        isReadingCard = false;
        if (btnReadCardAloud != null) btnReadCardAloud.setText(R.string.read_definition_aloud);
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {}
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSpeech();
        releaseMediaPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
        }
    }
}
