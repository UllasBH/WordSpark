package com.wordspark.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.wordspark.app.db.HistoryDao;
import com.wordspark.app.model.HistoryEntry;
import com.wordspark.app.network.DictionaryApiClient;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.util.SessionManager;
import com.wordspark.app.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SearchActivity extends AppCompatActivity {

    // Default suggestions shown when the search field is empty.
    private static final String[] SUGGESTED_WORDS = {
            "Ephemeral",
            "Luminous",
            "Resilient",
            "Ubiquitous",
            "Mellifluous"
    };


    private EditText etSearchWord;
    private TextView tvSearchError;

    private LinearLayout liveSuggestionsContainer;
    private LinearLayout recentSearchesContainer;
    private LinearLayout suggestedWordsContainer;

    private HistoryDao historyDao;

    private boolean isNavigating = false;
    private boolean isListening = false;

    /*
     * Every time a new suggestion request is made this number increases.
     *
     * Example:
     *
     * r  -> request 1
     * ru -> request 2
     * run -> request 3
     *
     * If request 1 finishes after request 3, its result is ignored.
     */
    private int suggestionRequestId = 0;


    // ============================================================
    // MICROPHONE PERMISSION
    // ============================================================

    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {

                        if (granted) {
                            launchSpeechRecognizer();
                        } else {
                            onMicPermissionDenied();
                        }
                    }
            );


    // ============================================================
    // SPEECH RECOGNIZER
    // ============================================================

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onSpeechResult
            );


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);


        historyDao =
                new HistoryDao(this);


        // --------------------------------------------------------
        // FIND VIEWS
        // --------------------------------------------------------

        etSearchWord =
                findViewById(R.id.etSearchWord);

        tvSearchError =
                findViewById(R.id.tvSearchError);

        liveSuggestionsContainer =
                findViewById(R.id.liveSuggestionsContainer);

        recentSearchesContainer =
                findViewById(R.id.recentSearchesContainer);

        suggestedWordsContainer =
                findViewById(R.id.suggestedWordsContainer);


        // --------------------------------------------------------
        // BUTTONS
        // --------------------------------------------------------

        findViewById(R.id.btnBack)
                .setOnClickListener(
                        v -> finish()
                );


        findViewById(R.id.btnSearchGo)
                .setOnClickListener(
                        v -> attemptSearch()
                );


        findViewById(R.id.btnVoiceSearch)
                .setOnClickListener(
                        v -> onMicClicked()
                );


        // ========================================================
        // LIVE SEARCH SUGGESTIONS
        // ========================================================

        etSearchWord.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        String query =
                                s.toString().trim();


                        tvSearchError
                                .setVisibility(
                                        View.GONE
                                );


                        // Empty search field:
                        // hide live suggestions.
                        if (query.isEmpty()) {

                            suggestionRequestId++;

                            if (liveSuggestionsContainer != null) {
                                liveSuggestionsContainer.removeAllViews();
                                liveSuggestionsContainer.setVisibility(View.GONE);
                            }

                            return;
                        }


                        // User typed something:
                        // request live dictionary suggestions.
                        loadApiSuggestions(query);
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );


        // ========================================================
        // ENTER / DONE KEY
        // ========================================================

        etSearchWord.setOnEditorActionListener(
                (v, actionId, event) -> {

                    attemptSearch();

                    return true;
                }
        );


        etSearchWord.setOnKeyListener(
                (v, keyCode, event) -> {

                    if (keyCode ==
                            KeyEvent.KEYCODE_ENTER) {

                        attemptSearch();

                        return true;
                    }

                    return false;
                }
        );


        // ========================================================
        // INITIAL SUGGESTIONS
        // ========================================================

        buildSuggestedWords();
    }


    // ============================================================
    // ON RESUME
    // ============================================================

    @Override
    protected void onResume() {

        super.onResume();

        isNavigating = false;

        loadRecentSearches();
    }


    // ============================================================
    // ATTEMPT SEARCH
    // ============================================================

    private void attemptSearch() {

        String word =
                etSearchWord
                        .getText()
                        .toString()
                        .trim();


        if (ValidationUtil.isEmpty(word)) {

            tvSearchError.setText(
                    R.string.err_empty_search
            );

            tvSearchError.setVisibility(
                    View.VISIBLE
            );

            return;
        }


        tvSearchError.setVisibility(
                View.GONE
        );


        hideKeyboard();


        openWordDetail(word);
    }


    // ============================================================
    // LOAD LIVE API SUGGESTIONS
    // ============================================================

    private void loadApiSuggestions(
            String query) {


        final int requestId =
                ++suggestionRequestId;


        if (query.length() < 1) {

            if (liveSuggestionsContainer != null) {
                liveSuggestionsContainer.removeAllViews();
                liveSuggestionsContainer.setVisibility(View.GONE);
            }

            return;
        }


        DictionaryApiClient apiClient =
                new DictionaryApiClient();


        apiClient.getSuggestions(
                this,
                query,
                suggestions -> {


                    // Activity is no longer usable.
                    if (LifecycleUtil.isGone(this)) {
                        return;
                    }


                    /*
                     * Ignore an older API response.
                     *
                     * Example:
                     *
                     * request 1 = "r"
                     * request 2 = "ru"
                     * request 3 = "run"
                     *
                     * If "r" returns after "run",
                     * don't show the old results.
                     */
                    if (requestId !=
                            suggestionRequestId) {

                        return;
                    }


                    liveSuggestionsContainer
                            .removeAllViews();


                    if (suggestions == null ||
                            suggestions.isEmpty()) {

                        liveSuggestionsContainer
                                .setVisibility(
                                        View.GONE
                                );

                        return;
                    }


                    for (String suggestion :
                            suggestions) {


                        if (suggestion == null ||
                                suggestion.trim().isEmpty()) {

                            continue;
                        }


                        addWordRow(
                                liveSuggestionsContainer,
                                suggestion.trim()
                        );
                    }


                    if (liveSuggestionsContainer.getChildCount() > 0) {

                        liveSuggestionsContainer
                                .setVisibility(
                                        View.VISIBLE
                                );

                    } else {

                        liveSuggestionsContainer
                                .setVisibility(
                                        View.GONE
                                );
                    }
                }
        );
    }


    // ============================================================
    // VOICE SEARCH
    // ============================================================

    private void onMicClicked() {

        if (isListening) {
            return;
        }


        tvSearchError.setVisibility(
                View.GONE
        );


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED) {


            launchSpeechRecognizer();


        } else if (
                ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.RECORD_AUDIO
                )
        ) {


            new AlertDialog.Builder(this)

                    .setTitle(
                            R.string.voice_permission_title
                    )

                    .setMessage(
                            R.string.voice_permission_rationale
                    )

                    .setPositiveButton(
                            R.string.allow,
                            (d, w) ->
                                    micPermissionLauncher.launch(
                                            Manifest.permission.RECORD_AUDIO
                                    )
                    )

                    .setNegativeButton(
                            R.string.not_now,
                            null
                    )

                    .show();


        } else {

            micPermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
            );
        }
    }


    // ============================================================
    // MICROPHONE PERMISSION DENIED
    // ============================================================

    private void onMicPermissionDenied() {

        if (LifecycleUtil.isGone(this)) {
            return;
        }


        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
        )) {


            new AlertDialog.Builder(this)

                    .setTitle(
                            R.string.voice_permission_title
                    )

                    .setMessage(
                            R.string.voice_permission_settings_hint
                    )

                    .setPositiveButton(
                            R.string.open_settings,
                            (d, w) ->
                                    openAppSettings()
                    )

                    .setNegativeButton(
                            R.string.cancel,
                            null
                    )

                    .show();


        } else {

            showSearchError(
                    R.string.voice_permission_denied
            );
        }
    }


    // ============================================================
    // OPEN APP SETTINGS
    // ============================================================

    private void openAppSettings() {

        try {

            Intent intent =
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts(
                                    "package",
                                    getPackageName(),
                                    null
                            )
                    );


            startActivity(intent);


        } catch (Exception e) {

            showSearchError(
                    R.string.voice_permission_denied
            );
        }
    }


    // ============================================================
    // LAUNCH SPEECH RECOGNIZER
    // ============================================================

    private void launchSpeechRecognizer() {

        Intent intent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );


        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );


        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-US"
        );


        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                getString(
                        R.string.voice_prompt
                )
        );


        intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
        );


        isListening = true;


        try {

            speechLauncher.launch(intent);


        } catch (ActivityNotFoundException e) {

            isListening = false;

            showSearchError(
                    R.string.err_voice_unavailable
            );


        } catch (Exception e) {

            isListening = false;

            showSearchError(
                    R.string.err_voice_failed
            );
        }
    }


    // ============================================================
    // SPEECH RESULT
    // ============================================================

    private void onSpeechResult(
            ActivityResult result) {


        isListening = false;


        if (LifecycleUtil.isGone(this)) {
            return;
        }


        switch (result.getResultCode()) {


            case RESULT_OK: {

                Intent data =
                        result.getData();


                ArrayList<String> matches =
                        (data == null)
                                ? null
                                : data.getStringArrayListExtra(
                                RecognizerIntent.EXTRA_RESULTS
                        );


                String spoken =
                        firstUsableWord(matches);


                if (spoken == null) {

                    showSearchError(
                            R.string.err_voice_no_speech
                    );

                    return;
                }


                /*
                 * Feed recognised text into the
                 * normal search field.
                 *
                 * This also triggers the TextWatcher,
                 * but we immediately perform the search.
                 */
                etSearchWord.setText(spoken);


                etSearchWord.setSelection(
                        spoken.length()
                );


                attemptSearch();

                break;
            }


            case RESULT_CANCELED:

                // User cancelled voice search.
                break;


            case RecognizerIntent.RESULT_NO_MATCH:

                showSearchError(
                        R.string.err_voice_no_speech
                );

                break;


            case RecognizerIntent.RESULT_NETWORK_ERROR:
            case RecognizerIntent.RESULT_SERVER_ERROR:

                showSearchError(
                        R.string.err_voice_network
                );

                break;


            case RecognizerIntent.RESULT_AUDIO_ERROR:

                showSearchError(
                        R.string.err_voice_audio
                );

                break;


            default:

                showSearchError(
                        R.string.err_voice_failed
                );

                break;
        }
    }


    // ============================================================
    // FIRST USABLE VOICE WORD
    // ============================================================

    private String firstUsableWord(
            List<String> candidates) {


        if (candidates == null) {
            return null;
        }


        for (String candidate :
                candidates) {


            if (candidate == null) {
                continue;
            }


            String cleaned =
                    candidate
                            .trim()
                            .replaceAll(
                                    "^[^\\p{L}]+|[^\\p{L}]+$",
                                    ""
                            );


            if (!cleaned.isEmpty()) {
                return cleaned;
            }
        }


        return null;
    }


    // ============================================================
    // SEARCH ERROR
    // ============================================================

    private void showSearchError(
            int messageRes) {


        tvSearchError.setText(
                messageRes
        );


        tvSearchError.setVisibility(
                View.VISIBLE
        );
    }


    // ============================================================
    // OPEN WORD DETAILS
    // ============================================================

    private void openWordDetail(
            String word) {


        if (isNavigating) {
            return;
        }


        isNavigating = true;


        Intent intent =
                new Intent(
                        this,
                        WordDetailActivity.class
                );


        intent.putExtra(
                WordDetailActivity.EXTRA_WORD,
                word
        );


        startActivity(intent);
    }


    // ============================================================
    // HIDE KEYBOARD
    // ============================================================

    private void hideKeyboard() {

        InputMethodManager imm =
                (InputMethodManager)
                        getSystemService(
                                Context.INPUT_METHOD_SERVICE
                        );


        if (imm != null &&
                getCurrentFocus() != null) {


            imm.hideSoftInputFromWindow(
                    getCurrentFocus()
                            .getWindowToken(),
                    0
            );
        }
    }


    // ============================================================
    // LOAD RECENT SEARCHES
    // ============================================================

    private void loadRecentSearches() {

        recentSearchesContainer
                .removeAllViews();


        if (!SessionManager.isLoggedIn(this)) {

            addPlaceholder(
                    recentSearchesContainer,
                    getString(
                            R.string.err_login_required
                    )
            );

            return;
        }


        int userId =
                SessionManager.getCurrentUserId(
                        this
                );


        new Thread(() -> {

            List<HistoryEntry> recent =
                    historyDao.getHistory(
                            userId,
                            5
                    );


            runOnUiThread(() -> {


                if (LifecycleUtil.isGone(this)) {
                    return;
                }


                recentSearchesContainer
                        .removeAllViews();


                if (recent.isEmpty()) {

                    addPlaceholder(
                            recentSearchesContainer,
                            "No recent searches yet."
                    );


                } else {


                    for (HistoryEntry entry :
                            recent) {


                        addWordRow(
                                recentSearchesContainer,
                                entry.getWord()
                        );
                    }
                }
            });

        }).start();
    }


    // ============================================================
    // DEFAULT SUGGESTED WORDS
    // ============================================================

    private void buildSuggestedWords() {

        suggestedWordsContainer
                .removeAllViews();


        for (String word :
                SUGGESTED_WORDS) {


            addWordRow(
                    suggestedWordsContainer,
                    word
            );
        }
    }


    // ============================================================
    // ADD WORD ROW
    // ============================================================

    private void addWordRow(
            LinearLayout container,
            String word) {


        TextView row =
                new TextView(this);


        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        row.setLayoutParams(params);


        row.setText(
                capitalize(word)
        );


        row.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_primary
                )
        );


        row.setTextSize(15);


        row.setBackgroundResource(
                R.drawable.bg_card
        );


        row.setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
        );


        LinearLayout.LayoutParams marginParams =
                (LinearLayout.LayoutParams)
                        row.getLayoutParams();


        marginParams.bottomMargin =
                dp(8);


        row.setLayoutParams(
                marginParams
        );


        /*
         * Tapping a suggestion/recent word
         * puts that word in the search box and performs the search.
         */
        row.setOnClickListener(v -> {
            etSearchWord.setText(word);
            etSearchWord.setSelection(word.length());
            attemptSearch();
        });


        container.addView(row);
    }


    // ============================================================
    // PLACEHOLDER
    // ============================================================

    private void addPlaceholder(
            LinearLayout container,
            String text) {


        TextView placeholder =
                new TextView(this);


        placeholder.setText(text);


        placeholder.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_secondary
                )
        );


        placeholder.setTextSize(14);


        container.addView(
                placeholder
        );
    }


    // ============================================================
    // CAPITALIZE
    // ============================================================

    private String capitalize(
            String s) {


        if (s == null ||
                s.isEmpty()) {

            return s;
        }


        return s.substring(
                0,
                1
        ).toUpperCase(
                Locale.getDefault()
        )
                +
                s.substring(1);
    }


    // ============================================================
    // DP
    // ============================================================

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;


        return Math.round(
                value * density
        );
    }
}