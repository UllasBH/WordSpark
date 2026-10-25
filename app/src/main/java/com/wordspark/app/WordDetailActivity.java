package com.wordspark.app;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.wordspark.app.db.FavoriteDao;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.db.HistoryDao;
import com.wordspark.app.db.SettingsDao;
import com.wordspark.app.model.Definition;
import com.wordspark.app.model.Meaning;
import com.wordspark.app.model.WordResult;
import com.wordspark.app.network.ApiCallback;
import com.wordspark.app.network.DictionaryApiClient;
import com.wordspark.app.util.NetworkUtil;
import com.wordspark.app.util.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WordDetailActivity extends AppCompatActivity {

    public static final String EXTRA_WORD = "extra_word";

    private String word;
    private WordResult currentResult;
    private boolean isFavorite = false;
    private boolean isLoading = false;
    private boolean isTogglingFavorite = false;

    private View progressBar, contentScroll, errorContainer;
    private TextView tvWord, tvPhonetic, tvErrorMessage;
    private ImageView btnFavorite, btnPlayAudio, btnBack;
    private LinearLayout meaningsContainer;
    private View btnRetry;
    private TextView btnReadAloud;

    private DictionaryApiClient apiClient;
    private FavoriteDao favoriteDao;
    private HistoryDao historyDao;
    private SettingsDao settingsDao;
    private MediaPlayer mediaPlayer;

    // ----------------------------------------------------------
    // Text-to-Speech
    // ----------------------------------------------------------

    private static final String UTTERANCE_WORD = "wordspark_word";
    private static final String UTTERANCE_DEFINITION = "wordspark_definition";

    private TextToSpeech tts;

    private boolean ttsInitFinished = false;
    private boolean ttsReady = false;
    private boolean readingDefinition = false;

    // If the user taps a TTS button before initialization finishes,
    // remember the request and execute it when TTS becomes ready.
    private String pendingSpeechText = null;
    private String pendingUtteranceId = null;
    private boolean pendingIsDefinition = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_word_detail);

        word = getIntent().getStringExtra(EXTRA_WORD);

        apiClient = new DictionaryApiClient();
        favoriteDao = new FavoriteDao(this);
        historyDao = new HistoryDao(this);
        settingsDao = new SettingsDao(this);

        progressBar = findViewById(R.id.progressBar);
        contentScroll = findViewById(R.id.contentScroll);
        errorContainer = findViewById(R.id.errorContainer);

        tvWord = findViewById(R.id.tvWord);
        tvPhonetic = findViewById(R.id.tvPhonetic);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);

        btnFavorite = findViewById(R.id.btnFavorite);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        btnBack = findViewById(R.id.btnBack);

        meaningsContainer = findViewById(R.id.meaningsContainer);

        btnRetry = findViewById(R.id.btnRetry);
        btnReadAloud = findViewById(R.id.btnReadAloud);

        btnBack.setOnClickListener(v -> finish());

        btnRetry.setOnClickListener(v -> loadWord());

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        if (word == null || word.trim().isEmpty()) {
            finish();
            return;
        }

        initTextToSpeech();

        btnReadAloud.setOnClickListener(v -> toggleReadDefinition());

        loadWord();
    }


    // ----------------------------------------------------------
    // Load word
    // ----------------------------------------------------------

    private void loadWord() {

        if (isLoading) {
            return;
        }

        isLoading = true;

        showLoading();

        if (!NetworkUtil.isConnected(this)) {
            isLoading = false;
            showError(getString(R.string.err_no_internet), true);
            return;
        }

        apiClient.lookup(this, word, new ApiCallback() {

            @Override
            public void onSuccess(WordResult result) {

                isLoading = false;

                if (LifecycleUtil.isGone(WordDetailActivity.this)) {
                    return;
                }

                currentResult = result;

                renderWord(result);

                recordHistoryIfLoggedIn(result.getWord());

                refreshFavoriteState(result.getWord());
            }

            @Override
            public void onWordNotFound() {

                isLoading = false;

                if (LifecycleUtil.isGone(WordDetailActivity.this)) {
                    return;
                }

                showError(
                        getString(R.string.err_word_not_found),
                        false
                );

                loadSpellingSuggestions(word);
            }

            @Override
            public void onNetworkError() {

                isLoading = false;

                if (LifecycleUtil.isGone(WordDetailActivity.this)) {
                    return;
                }

                showError(
                        getString(R.string.err_no_internet),
                        true
                );
            }

            @Override
            public void onApiError() {

                isLoading = false;

                if (LifecycleUtil.isGone(WordDetailActivity.this)) {
                    return;
                }

                showError(
                        getString(R.string.err_api_failure),
                        true
                );
            }
        });
    }

    private void loadSpellingSuggestions(String searchWord) {
        View tvSuggestionsLabel = findViewById(R.id.tvSuggestionsLabel);
        View svSuggestions = findViewById(R.id.svSuggestions);
        LinearLayout suggestionsChipsContainer = findViewById(R.id.suggestionsChipsContainer);

        if (tvSuggestionsLabel == null || svSuggestions == null || suggestionsChipsContainer == null) return;

        apiClient.getSuggestions(this, searchWord, suggestions -> {
            if (LifecycleUtil.isGone(WordDetailActivity.this)) return;
            if (suggestions != null && !suggestions.isEmpty()) {
                suggestionsChipsContainer.removeAllViews();
                for (String sugg : suggestions) {
                    TextView chip = createChip(sugg);
                    chip.setOnClickListener(v -> {
                        word = sugg;
                        loadWord();
                    });
                    suggestionsChipsContainer.addView(chip);
                }
                tvSuggestionsLabel.setVisibility(View.VISIBLE);
                svSuggestions.setVisibility(View.VISIBLE);
            } else {
                tvSuggestionsLabel.setVisibility(View.GONE);
                svSuggestions.setVisibility(View.GONE);
            }
        });
    }


    // ----------------------------------------------------------
    // Loading / error
    // ----------------------------------------------------------

    private void showLoading() {

        progressBar.setVisibility(View.VISIBLE);
        contentScroll.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        View tvSuggestionsLabel = findViewById(R.id.tvSuggestionsLabel);
        View svSuggestions = findViewById(R.id.svSuggestions);
        if (tvSuggestionsLabel != null) tvSuggestionsLabel.setVisibility(View.GONE);
        if (svSuggestions != null) svSuggestions.setVisibility(View.GONE);
    }


    private void showError(String message, boolean showRetry) {

        progressBar.setVisibility(View.GONE);
        contentScroll.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);

        tvErrorMessage.setText(message);

        btnRetry.setVisibility(
                showRetry ? View.VISIBLE : View.GONE
        );

        View tvSuggestionsLabel = findViewById(R.id.tvSuggestionsLabel);
        View svSuggestions = findViewById(R.id.svSuggestions);
        if (tvSuggestionsLabel != null) tvSuggestionsLabel.setVisibility(View.GONE);
        if (svSuggestions != null) svSuggestions.setVisibility(View.GONE);
    }


    // ----------------------------------------------------------
    // Render Word
    // ----------------------------------------------------------

    private void renderWord(WordResult result) {

        progressBar.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        contentScroll.setVisibility(View.VISIBLE);

        tvWord.setText(capitalize(result.getWord()));

        if (result.hasPhonetic()) {

            tvPhonetic.setVisibility(View.VISIBLE);

            tvPhonetic.setText(result.getPhonetic());

        } else {

            tvPhonetic.setVisibility(View.GONE);
        }


        // ------------------------------------------------------
        // Pronunciation button
        // ------------------------------------------------------

        btnPlayAudio.setVisibility(View.VISIBLE);

        btnPlayAudio.setOnClickListener(v -> {

            if (result.hasAudio()) {

                // API recorded pronunciation first.
                playAudio(
                        result.getAudioUrl(),
                        result.getWord()
                );

            } else {

                // Android TTS fallback.
                speakWord(result.getWord());
            }
        });


        // ------------------------------------------------------
        // Read Definition
        // ------------------------------------------------------

        btnReadAloud.setVisibility(View.VISIBLE);

        setReadAloudLabel(false);


        // ------------------------------------------------------
        // Meanings
        // ------------------------------------------------------

        meaningsContainer.removeAllViews();

        for (Meaning meaning : result.getMeanings()) {

            meaningsContainer.addView(
                    buildMeaningBlock(meaning)
            );
        }
    }


    // ----------------------------------------------------------
    // Meaning block
    // ----------------------------------------------------------

    private View buildMeaningBlock(Meaning meaning) {

        View block = LayoutInflater.from(this)
                .inflate(
                        R.layout.item_meaning_block,
                        meaningsContainer,
                        false
                );

        TextView tvPartOfSpeech =
                block.findViewById(R.id.tvPartOfSpeech);

        TextView tvLabels =
                block.findViewById(R.id.tvLabels);

        TextView tvInflections =
                block.findViewById(R.id.tvInflections);

        TextView tvEtymology =
                block.findViewById(R.id.tvEtymology);

        LinearLayout definitionsContainer =
                block.findViewById(R.id.definitionsContainer);

        TextView tvSynonymsLabel =
                block.findViewById(R.id.tvSynonymsLabel);

        View svSynonyms =
                block.findViewById(R.id.svSynonyms);

        LinearLayout synonymsChipsContainer =
                block.findViewById(R.id.synonymsChipsContainer);

        TextView tvAntonymsLabel =
                block.findViewById(R.id.tvAntonymsLabel);

        View svAntonyms =
                block.findViewById(R.id.svAntonyms);

        LinearLayout antonymsChipsContainer =
                block.findViewById(R.id.antonymsChipsContainer);


        tvPartOfSpeech.setText(
                meaning.getPartOfSpeech()
        );

        if (meaning.hasLabels() && tvLabels != null) {
            tvLabels.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            for (int l = 0; l < meaning.getLabels().size(); l++) {
                if (l > 0) sb.append(", ");
                sb.append(meaning.getLabels().get(l));
            }
            tvLabels.setText("[" + sb.toString() + "]");
        } else if (tvLabels != null) {
            tvLabels.setVisibility(View.GONE);
        }

        if (meaning.hasInflections() && tvInflections != null) {
            tvInflections.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder("Forms: ");
            for (int f = 0; f < meaning.getInflections().size(); f++) {
                if (f > 0) sb.append(", ");
                sb.append(meaning.getInflections().get(f));
            }
            tvInflections.setText(sb.toString());
        } else if (tvInflections != null) {
            tvInflections.setVisibility(View.GONE);
        }

        if (meaning.hasEtymology() && tvEtymology != null) {
            tvEtymology.setVisibility(View.VISIBLE);
            tvEtymology.setText("Origin: " + meaning.getEtymology());
        } else if (tvEtymology != null) {
            tvEtymology.setVisibility(View.GONE);
        }


        // ------------------------------------------------------
        // Definitions
        // ------------------------------------------------------

        if (definitionsContainer != null) {

            definitionsContainer.removeAllViews();

            List<Definition> definitions =
                    meaning.getDefinitions();

            for (int i = 0; i < definitions.size(); i++) {

                Definition def = definitions.get(i);

                LinearLayout defItemLayout =
                        new LinearLayout(this);

                defItemLayout.setOrientation(
                        LinearLayout.VERTICAL
                );

                if (i > 0) {

                    LinearLayout.LayoutParams lp =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );

                    lp.topMargin = dp(10);

                    defItemLayout.setLayoutParams(lp);
                }


                TextView tvDef =
                        new TextView(this);

                String prefix =
                        definitions.size() > 1
                                ? (i + 1) + ". "
                                : "";

                tvDef.setText(
                        prefix + def.getDefinition()
                );

                tvDef.setTextColor(
                        ContextCompat.getColor(
                                this,
                                R.color.text_primary
                        )
                );

                tvDef.setTextSize(14);

                defItemLayout.addView(tvDef);


                if (def.hasExample()) {

                    TextView tvEx =
                            new TextView(this);

                    tvEx.setText(
                            "\u201C"
                                    + def.getExample()
                                    + "\u201D"
                    );

                    tvEx.setTextColor(
                            ContextCompat.getColor(
                                    this,
                                    R.color.text_secondary
                            )
                    );

                    tvEx.setTextSize(13);

                    LinearLayout.LayoutParams exLp =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );

                    exLp.topMargin = dp(4);

                    tvEx.setLayoutParams(exLp);

                    defItemLayout.addView(tvEx);
                }


                definitionsContainer.addView(
                        defItemLayout
                );
            }
        }


        // ------------------------------------------------------
        // Synonyms
        // ------------------------------------------------------

        if (meaning.hasSynonyms()
                && tvSynonymsLabel != null
                && svSynonyms != null
                && synonymsChipsContainer != null) {

            tvSynonymsLabel.setVisibility(View.VISIBLE);

            svSynonyms.setVisibility(View.VISIBLE);

            synonymsChipsContainer.removeAllViews();

            for (String syn : meaning.getSynonyms()) {

                synonymsChipsContainer.addView(
                        createChip(syn)
                );
            }

        } else if (
                tvSynonymsLabel != null
                        && svSynonyms != null) {

            tvSynonymsLabel.setVisibility(View.GONE);

            svSynonyms.setVisibility(View.GONE);
        }


        // ------------------------------------------------------
        // Antonyms
        // ------------------------------------------------------

        if (meaning.hasAntonyms()
                && tvAntonymsLabel != null
                && svAntonyms != null
                && antonymsChipsContainer != null) {

            tvAntonymsLabel.setVisibility(View.VISIBLE);

            svAntonyms.setVisibility(View.VISIBLE);

            antonymsChipsContainer.removeAllViews();

            for (String ant : meaning.getAntonyms()) {

                antonymsChipsContainer.addView(
                        createChip(ant)
                );
            }

        } else if (
                tvAntonymsLabel != null
                        && svAntonyms != null) {

            tvAntonymsLabel.setVisibility(View.GONE);

            svAntonyms.setVisibility(View.GONE);
        }


        return block;
    }


    private TextView createChip(String text) {

        TextView chip =
                new TextView(this);

        chip.setText(text);

        chip.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.primary
                )
        );

        chip.setTextSize(13);

        chip.setBackgroundResource(
                R.drawable.bg_chip
        );

        chip.setPadding(
                dp(12),
                dp(6),
                dp(12),
                dp(6)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMarginEnd(dp(8));

        chip.setLayoutParams(params);

        return chip;
    }


    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                value * density
        );
    }


    // ----------------------------------------------------------
    // API pronunciation audio
    // ----------------------------------------------------------

    private void playAudio(
            String url,
            String wordForFallback) {

        stopSpeech();

        try {

            releaseMediaPlayer();

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(
                                    AudioAttributes.USAGE_MEDIA
                            )
                            .setContentType(
                                    AudioAttributes.CONTENT_TYPE_SPEECH
                            )
                            .build()
            );

            mediaPlayer.setDataSource(url);

            mediaPlayer.setOnPreparedListener(
                    MediaPlayer::start
            );

            mediaPlayer.setOnCompletionListener(
                    mp -> releaseMediaPlayer()
            );

            mediaPlayer.setOnErrorListener(
                    (mp, what, extra) -> {

                        releaseMediaPlayer();

                        // If recorded audio fails,
                        // fall back to Android TTS.
                        if (!LifecycleUtil.isGone(this)) {

                            speakWord(wordForFallback);
                        }

                        return true;
                    }
            );

            mediaPlayer.prepareAsync();

        } catch (
                IOException
                | IllegalStateException
                | IllegalArgumentException
                | SecurityException e) {

            releaseMediaPlayer();

            speakWord(wordForFallback);
        }
    }


    private void releaseMediaPlayer() {

        if (mediaPlayer != null) {

            try {

                mediaPlayer.release();

            } catch (Exception ignored) {
                // Already released.
            }

            mediaPlayer = null;
        }
    }


    // ==========================================================
    // TEXT-TO-SPEECH
    // ==========================================================

    private void initTextToSpeech() {

        ttsInitFinished = false;
        ttsReady = false;
        Log.d("WordSpark-TTS", "TTS initialization started");

        try {

            tts = new TextToSpeech(
                    getApplicationContext(),
                    status -> {

                        if (isFinishing()
                                || isDestroyed()) {

                            return;
                        }

                        ttsInitFinished = true;
                        Log.d("WordSpark-TTS", "TTS initialization completed, status=" + status);


                        // Initialization failed.
                        if (status != TextToSpeech.SUCCESS
                                || tts == null) {

                            ttsReady = false;
                            Log.e("WordSpark-TTS", "TTS initialization failed");

                            return;
                        }


                        // --------------------------------------------------
                        // Try device/default language first
                        // --------------------------------------------------

                        Locale deviceLocale =
                                getResources()
                                        .getConfiguration()
                                        .getLocales()
                                        .get(0);

                        int langResult =
                                tts.setLanguage(deviceLocale);


                        // --------------------------------------------------
                        // Fallback to US English
                        // --------------------------------------------------

                        if (langResult
                                == TextToSpeech.LANG_MISSING_DATA
                                || langResult
                                == TextToSpeech.LANG_NOT_SUPPORTED) {

                            langResult =
                                    tts.setLanguage(Locale.US);
                        }


                        // --------------------------------------------------
                        // Final fallback to English
                        // --------------------------------------------------

                        if (langResult
                                == TextToSpeech.LANG_MISSING_DATA
                                || langResult
                                == TextToSpeech.LANG_NOT_SUPPORTED) {

                            langResult =
                                    tts.setLanguage(Locale.ENGLISH);
                        }


                        ttsReady =
                                langResult
                                        != TextToSpeech.LANG_MISSING_DATA
                                        && langResult
                                        != TextToSpeech.LANG_NOT_SUPPORTED;

                        Log.d("WordSpark-TTS", "selected language: " + deviceLocale + " result=" + langResult);


                        if (!ttsReady) {
                            Log.e("WordSpark-TTS", "Selected language not supported or missing data");
                            return;
                        }


                        // --------------------------------------------------
                        // Utterance listener
                        // --------------------------------------------------

                        tts.setOnUtteranceProgressListener(
                                new UtteranceProgressListener() {

                                    @Override
                                    public void onStart(
                                            String utteranceId) {
                                        Log.d("WordSpark-TTS", "speech started: " + utteranceId);
                                    }


                                    @Override
                                    public void onDone(
                                            String utteranceId) {
                                        Log.d("WordSpark-TTS", "speech completed: " + utteranceId);
                                        onUtteranceFinished(
                                                utteranceId
                                        );
                                    }


                                    @Override
                                    public void onError(
                                            String utteranceId) {
                                        Log.e("WordSpark-TTS", "speech error: " + utteranceId);
                                        onUtteranceFinished(
                                                utteranceId
                                        );
                                    }
                                }
                        );


                        // --------------------------------------------------
                        // Execute pending request
                        // --------------------------------------------------

                        if (pendingSpeechText != null
                                && !pendingSpeechText
                                .trim()
                                .isEmpty()) {

                            String text =
                                    pendingSpeechText;

                            String utteranceId =
                                    pendingUtteranceId;

                            boolean isDefinition =
                                    pendingIsDefinition;


                            pendingSpeechText = null;
                            pendingUtteranceId = null;
                            pendingIsDefinition = false;


                            if (isDefinition) {

                                speakDefinitionNow(
                                        text,
                                        utteranceId
                                );

                            } else {

                                speakWordNow(
                                        text,
                                        utteranceId
                                );
                            }
                        }
                    }
            );

        } catch (Exception e) {

            tts = null;

            ttsInitFinished = true;
            ttsReady = false;
            Log.e("WordSpark-TTS", "TTS initialization exception", e);
        }
    }


    // ----------------------------------------------------------
    // TTS utterance finished
    // ----------------------------------------------------------

    private void onUtteranceFinished(
            String utteranceId) {

        if (UTTERANCE_DEFINITION.equals(utteranceId)) {

            runOnUiThread(() -> {

                if (LifecycleUtil.isGone(this)) {
                    return;
                }

                readingDefinition = false;

                setReadAloudLabel(false);
            });
        }
    }


    // ----------------------------------------------------------
    // TTS readiness
    // ----------------------------------------------------------

    private boolean ensureTtsReady() {

        if (tts == null) {

            Toast.makeText(
                    this,
                    R.string.err_tts_unavailable,
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }


        if (!ttsInitFinished) {

            Toast.makeText(
                    this,
                    R.string.err_tts_not_ready,
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }


        if (!ttsReady) {

            Toast.makeText(
                    this,
                    R.string.err_tts_unavailable,
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }


        return true;
    }


    // ==========================================================
    // SPEAK WORD
    // ==========================================================

    private void speakWord(
            String spokenWord) {

        if (spokenWord == null
                || spokenWord.trim().isEmpty()) {

            return;
        }

        Log.d("WordSpark-TTS", "speech request received: " + spokenWord);


        // TTS is still initializing.
        // Queue the request instead of losing it.
        if (tts != null
                && !ttsInitFinished) {

            pendingSpeechText = spokenWord;
            pendingUtteranceId = UTTERANCE_WORD;
            pendingIsDefinition = false;

            return;
        }


        if (!ensureTtsReady()) {
            return;
        }


        speakWordNow(
                spokenWord,
                UTTERANCE_WORD
        );
    }


    private void speakWordNow(
            String spokenWord,
            String utteranceId) {

        if (spokenWord == null
                || spokenWord.trim().isEmpty()) {

            return;
        }


        releaseMediaPlayer();


        try {

            readingDefinition = false;

            setReadAloudLabel(false);


            int result =
                    tts.speak(
                            spokenWord,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            utteranceId
                    );

            Log.d("WordSpark-TTS", "speech started / speak return value: " + result);


            if (result == TextToSpeech.ERROR) {
                Log.e("WordSpark-TTS", "speech error return value from tts.speak()");

                Toast.makeText(
                        this,
                        R.string.err_tts_failed,
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {
            Log.e("WordSpark-TTS", "speech exception in tts.speak()", e);

            Toast.makeText(
                    this,
                    R.string.err_tts_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // ==========================================================
    // READ DEFINITION ALOUD
    // ==========================================================

    private void toggleReadDefinition() {

        if (currentResult == null) {
            return;
        }


        // Already speaking -> stop immediately.
        if (readingDefinition) {

            stopSpeech();

            return;
        }


        String text =
                buildReadAloudText(currentResult);


        if (text.isEmpty()) {

            Toast.makeText(
                    this,
                    R.string.err_tts_failed,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Log.d("WordSpark-TTS", "speech request received: " + text);


        // TTS is still initializing.
        if (tts != null
                && !ttsInitFinished) {

            pendingSpeechText = text;
            pendingUtteranceId = UTTERANCE_DEFINITION;
            pendingIsDefinition = true;

            readingDefinition = true;

            setReadAloudLabel(true);

            return;
        }


        if (!ensureTtsReady()) {
            return;
        }


        speakDefinitionNow(
                text,
                UTTERANCE_DEFINITION
        );
    }


    private void speakDefinitionNow(
            String text,
            String utteranceId) {

        if (text == null
                || text.trim().isEmpty()) {

            readingDefinition = false;

            setReadAloudLabel(false);

            return;
        }


        releaseMediaPlayer();


        try {

            readingDefinition = true;

            setReadAloudLabel(true);

            speakTextInChunks(text, utteranceId);

        } catch (Exception e) {
            Log.e("WordSpark-TTS", "speech exception in tts.speak()", e);

            readingDefinition = false;

            setReadAloudLabel(false);

            Toast.makeText(
                    this,
                    R.string.err_tts_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    private void speakTextInChunks(String text, String utteranceId) {
        int maxLen = TextToSpeech.getMaxSpeechInputLength() - 1;
        if (maxLen <= 0) maxLen = 3900;

        List<String> chunks = splitTextIntoChunks(text, maxLen);
        if (chunks.isEmpty()) {
            readingDefinition = false;
            setReadAloudLabel(false);
            return;
        }

        int result = TextToSpeech.ERROR;
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int queueMode = (i == 0) ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
            int res = tts.speak(chunk, queueMode, null, utteranceId);
            if (i == 0) {
                result = res;
            }
        }

        Log.d("WordSpark-TTS", "speech started / speak return value: " + result);

        if (result == TextToSpeech.ERROR) {
            Log.e("WordSpark-TTS", "speech error return value from tts.speak()");

            readingDefinition = false;

            setReadAloudLabel(false);

            Toast.makeText(
                    this,
                    R.string.err_tts_failed,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    private List<String> splitTextIntoChunks(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int length = text.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + maxLength, length);
            if (end < length) {
                int lastPeriod = text.lastIndexOf(". ", end);
                if (lastPeriod > start + 50) {
                    end = lastPeriod + 2;
                } else {
                    int lastSpace = text.lastIndexOf(" ", end);
                    if (lastSpace > start + 50) {
                        end = lastSpace + 1;
                    }
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }


    // ----------------------------------------------------------
    // Build definition speech text
    // ----------------------------------------------------------

    private String buildReadAloudText(
            WordResult result) {

        StringBuilder sb =
                new StringBuilder(
                        result.getWord()
                ).append(". ");


        for (Meaning meaning : result.getMeanings()) {

            if (meaning.getPartOfSpeech() != null
                    && !meaning.getPartOfSpeech().isEmpty()) {

                sb.append(
                        meaning.getPartOfSpeech()
                ).append(". ");
            }

            List<Definition> defs =
                    meaning.getDefinitions();

            for (int i = 0; i < defs.size(); i++) {
                Definition def = defs.get(i);
                sb.append("Definition ").append(i + 1).append(": ")
                  .append(def.getDefinition()).append(". ");

                if (def.hasExample()) {
                    sb.append("Example: ").append(def.getExample()).append(". ");
                }
            }
        }


        return sb.toString().trim();
    }


    // ----------------------------------------------------------
    // Read Aloud button label
    // ----------------------------------------------------------

    private void setReadAloudLabel(
            boolean reading) {

        if (btnReadAloud == null) {
            return;
        }

        btnReadAloud.setText(
                reading
                        ? R.string.stop_reading
                        : R.string.read_definition_aloud
        );
    }


    // ==========================================================
    // STOP SPEECH
    // ==========================================================

    private void stopSpeech() {

        readingDefinition = false;


        // Cancel anything waiting for TTS initialization.
        pendingSpeechText = null;
        pendingUtteranceId = null;
        pendingIsDefinition = false;


        if (btnReadAloud != null) {

            setReadAloudLabel(false);
        }


        if (tts != null) {

            try {

                // Immediately stop current speech.
                tts.stop();

            } catch (Exception ignored) {
                // TTS engine may already be unavailable.
            }
        }
    }


    // ==========================================================
    // HISTORY
    // ==========================================================

    private void recordHistoryIfLoggedIn(
            String resolvedWord) {

        if (!SessionManager.isLoggedIn(this)) {
            return;
        }

        int userId =
                SessionManager.getCurrentUserId(this);


        new Thread(() -> {

            int limit =
                    settingsDao
                            .getSettings(userId)
                            .getHistoryLimit();

            historyDao.recordSearch(
                    userId,
                    resolvedWord,
                    limit
            );

        }).start();
    }


    // ==========================================================
    // FAVORITES
    // ==========================================================

    private void refreshFavoriteState(
            String resolvedWord) {

        if (!SessionManager.isLoggedIn(this)) {

            isFavorite = false;

            updateFavoriteIcon();

            return;
        }


        int userId =
                SessionManager.getCurrentUserId(this);


        new Thread(() -> {

            boolean fav =
                    favoriteDao.isFavorite(
                            userId,
                            resolvedWord
                    );


            runOnUiThread(() -> {

                if (LifecycleUtil.isGone(this)) {
                    return;
                }

                isFavorite = fav;

                updateFavoriteIcon();
            });

        }).start();
    }


    private void toggleFavorite() {

        if (currentResult == null
                || isTogglingFavorite) {

            return;
        }


        if (!SessionManager.isLoggedIn(this)) {

            Toast.makeText(
                    this,
                    R.string.err_login_required,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        isTogglingFavorite = true;

        int userId =
                SessionManager.getCurrentUserId(this);

        boolean addingFavorite =
                !isFavorite;


        new Thread(() -> {

            if (addingFavorite) {

                favoriteDao.addFavorite(
                        userId,
                        currentResult
                );

            } else {

                favoriteDao.removeFavorite(
                        userId,
                        currentResult.getWord()
                );
            }


            runOnUiThread(() -> {

                if (LifecycleUtil.isGone(this)) {
                    return;
                }

                isFavorite = addingFavorite;

                isTogglingFavorite = false;

                updateFavoriteIcon();


                Toast.makeText(
                        this,
                        addingFavorite
                                ? R.string.added_to_favorites
                                : R.string.removed_from_favorites,
                        Toast.LENGTH_SHORT
                ).show();
            });

        }).start();
    }


    private void updateFavoriteIcon() {

        btnFavorite.setImageResource(
                isFavorite
                        ? R.drawable.ic_bookmark
                        : R.drawable.ic_bookmark_outline
        );
    }


    // ==========================================================
    // HELPERS
    // ==========================================================

    private String capitalize(String s) {

        if (s == null || s.isEmpty()) {
            return s;
        }

        return s.substring(0, 1).toUpperCase(
                Locale.getDefault()
        ) + s.substring(1);
    }


    // ==========================================================
    // ACTIVITY LIFECYCLE
    // ==========================================================

    @Override
    protected void onPause() {

        super.onPause();

        stopSpeech();
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

                tts.stop();

                tts.shutdown();

            } catch (Exception ignored) {
                // Nothing more to release.
            }

            tts = null;
        }
    }
}