package com.wordspark.app.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.wordspark.app.BuildConfig;
import com.wordspark.app.db.DatabaseHelper;
import com.wordspark.app.model.Definition;
import com.wordspark.app.model.Meaning;
import com.wordspark.app.model.WordResult;
import com.wordspark.app.util.NetworkUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryApiClient {

    // ============================================================
    // API URLS
    // ============================================================

    private static final String LEARNER_URL =
            "https://www.dictionaryapi.com/api/v3/references/learners/json/";

    private static final String THESAURUS_URL =
            "https://www.dictionaryapi.com/api/v3/references/thesaurus/json/";

    private static final int TIMEOUT_MS = 9000;


    // ============================================================
    // CACHE
    // ============================================================

    private static final Map<String, WordResult> memoryCache =
            new ConcurrentHashMap<>();


    // ============================================================
    // EXECUTOR
    // ============================================================

    private static final ExecutorService executor =
            Executors.newCachedThreadPool();


    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());


    // ============================================================
    // SUGGESTION CALLBACK
    // ============================================================

    public interface SuggestionCallback {

        void onSuggestions(List<String> suggestions);
    }


    // ============================================================
    // LOOKUP
    //
    // IMPORTANT:
    // ApiCallback is the EXISTING project-level
    // com.wordspark.app.network.ApiCallback.
    //
    // Do NOT declare another ApiCallback inside this class.
    // ============================================================

    public void lookup(
            String word,
            ApiCallback callback) {

        lookup(
                null,
                word,
                callback
        );
    }


    public void lookup(
            Context context,
            String word,
            ApiCallback callback) {

        if (word == null) {

            postToMain(
                    callback::onWordNotFound
            );

            return;
        }


        String trimmed =
                word.trim();


        if (trimmed.isEmpty()) {

            postToMain(
                    callback::onWordNotFound
            );

            return;
        }


        String cacheKey =
                trimmed.toLowerCase(
                        Locale.ROOT
                );


        // ========================================================
        // MEMORY CACHE
        // ========================================================

        WordResult cachedResult =
                memoryCache.get(cacheKey);


        if (cachedResult != null) {

            postToMain(() ->
                    callback.onSuccess(
                            cachedResult
                    )
            );

            return;
        }


        // ========================================================
        // NETWORK CHECK
        // ========================================================

        boolean isConnected =
                context == null ||
                        NetworkUtil.isConnected(
                                context
                        );


        // ========================================================
        // OFFLINE DATABASE FALLBACK
        // ========================================================

        if (!isConnected) {

            if (context != null) {

                WordResult localResult =
                        getFromLocalDb(
                                context,
                                trimmed
                        );


                if (localResult != null) {

                    memoryCache.put(
                            cacheKey,
                            localResult
                    );


                    postToMain(() ->
                            callback.onSuccess(
                                    localResult
                            )
                    );

                    return;
                }
            }


            postToMain(
                    callback::onNetworkError
            );

            return;
        }


        // ========================================================
        // ONLINE API REQUEST
        // ========================================================

        executor.execute(() -> {

            HttpURLConnection learnerConn =
                    null;


            try {

                String encodedWord =
                        java.net.URLEncoder
                                .encode(
                                        trimmed,
                                        "UTF-8"
                                )
                                .replace(
                                        "+",
                                        "%20"
                                );


                // =================================================
                // LEARNER'S DICTIONARY
                // =================================================

                URL learnerUrl =
                        new URL(
                                LEARNER_URL
                                        + encodedWord
                                        + "?key="
                                        + BuildConfig
                                        .MERRIAM_WEBSTER_LEARNER_API_KEY
                        );


                learnerConn =
                        (HttpURLConnection)
                                learnerUrl.openConnection();


                learnerConn.setRequestMethod(
                        "GET"
                );


                learnerConn.setConnectTimeout(
                        TIMEOUT_MS
                );


                learnerConn.setReadTimeout(
                        TIMEOUT_MS
                );


                int learnerStatus =
                        learnerConn.getResponseCode();


                if (learnerStatus ==
                        HttpURLConnection.HTTP_NOT_FOUND) {

                    learnerConn.disconnect();


                    postToMain(
                            callback::onWordNotFound
                    );


                    return;
                }


                if (learnerStatus !=
                        HttpURLConnection.HTTP_OK) {

                    learnerConn.disconnect();


                    WordResult localResult =
                            getFromLocalDb(
                                    context,
                                    trimmed
                            );


                    if (localResult != null) {

                        memoryCache.put(
                                cacheKey,
                                localResult
                        );


                        postToMain(() ->
                                callback.onSuccess(
                                        localResult
                                )
                        );

                    } else {

                        postToMain(
                                callback::onApiError
                        );
                    }


                    return;
                }


                String learnerBody =
                        readStream(
                                learnerConn
                                        .getInputStream()
                        );


                learnerConn.disconnect();

                String activeEncodedWord = encodedWord;

                try {
                    JSONArray jsonArray = new JSONArray(learnerBody);
                    if (jsonArray.length() > 0 && jsonArray.get(0) instanceof String) {
                        String suggestedWord = jsonArray.optString(0, "").trim();
                        if (!suggestedWord.isEmpty()) {
                            activeEncodedWord = java.net.URLEncoder.encode(suggestedWord, "UTF-8").replace("+", "%20");
                            URL suggUrl = new URL(
                                    LEARNER_URL
                                            + activeEncodedWord
                                            + "?key="
                                            + BuildConfig.MERRIAM_WEBSTER_LEARNER_API_KEY
                            );
                            HttpURLConnection suggConn = (HttpURLConnection) suggUrl.openConnection();
                            suggConn.setRequestMethod("GET");
                            suggConn.setConnectTimeout(TIMEOUT_MS);
                            suggConn.setReadTimeout(TIMEOUT_MS);
                            if (suggConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                learnerBody = readStream(suggConn.getInputStream());
                            }
                            suggConn.disconnect();
                        }
                    }
                } catch (Exception ignored) {}


                // =================================================
                // PARSE LEARNER RESPONSE
                // =================================================

                WordResult result =
                        parseLearner(
                                learnerBody,
                                trimmed
                        );


                if (result == null) {

                    postToMain(
                            callback::onWordNotFound
                    );

                    return;
                }


                // =================================================
                // THESAURUS API
                // =================================================

                HttpURLConnection thesaurusConn =
                        null;


                try {

                    URL thesaurusUrl =
                            new URL(
                                    THESAURUS_URL
                                            + activeEncodedWord
                                            + "?key="
                                            + BuildConfig
                                            .MERRIAM_WEBSTER_THESAURUS_API_KEY
                            );


                    thesaurusConn =
                            (HttpURLConnection)
                                    thesaurusUrl.openConnection();


                    thesaurusConn.setRequestMethod(
                            "GET"
                    );


                    thesaurusConn.setConnectTimeout(
                            TIMEOUT_MS
                    );


                    thesaurusConn.setReadTimeout(
                            TIMEOUT_MS
                    );


                    int thesaurusStatus =
                            thesaurusConn.getResponseCode();


                    if (thesaurusStatus ==
                            HttpURLConnection.HTTP_OK) {

                        String thesaurusBody =
                                readStream(
                                        thesaurusConn
                                                .getInputStream()
                                );


                        parseThesaurus(
                                thesaurusBody,
                                result
                        );
                    }


                } catch (Exception e) {

                    /*
                     * Thesaurus failure should never
                     * break the dictionary result.
                     */

                    Log.w(
                            "DictionaryApiClient",
                            "Thesaurus request failed",
                            e
                    );

                } finally {

                    if (thesaurusConn != null) {
                        thesaurusConn.disconnect();
                    }
                }


                // =================================================
                // SAVE DICTIONARY RESPONSE
                // =================================================

                if (context != null) {

                    saveToLocalDb(
                            context,
                            trimmed,
                            learnerBody
                    );
                }


                // =================================================
                // CACHE
                // =================================================

                memoryCache.put(
                        cacheKey,
                        result
                );


                // =================================================
                // RETURN
                // =================================================

                postToMain(() ->
                        callback.onSuccess(
                                result
                        )
                );


            } catch (Exception e) {

                if (learnerConn != null) {

                    try {
                        learnerConn.disconnect();
                    } catch (Exception ignored) {
                    }
                }


                // =================================================
                // SQLITE FALLBACK
                // =================================================

                if (context != null) {

                    WordResult localResult =
                            getFromLocalDb(
                                    context,
                                    trimmed
                            );


                    if (localResult != null) {

                        memoryCache.put(
                                cacheKey,
                                localResult
                        );


                        postToMain(() ->
                                callback.onSuccess(
                                        localResult
                                )
                        );


                        return;
                    }
                }


                postToMain(
                        callback::onNetworkError
                );
            }
        });
    }


    // ============================================================
    // LIVE SEARCH SUGGESTIONS
    // ============================================================

    public void getSuggestions(
            Context context,
            String query,
            SuggestionCallback callback) {

        if (callback == null) {
            return;
        }


        if (query == null ||
                query.trim().isEmpty()) {

            mainHandler.post(() ->
                    callback.onSuggestions(
                            new ArrayList<>()
                    )
            );

            return;
        }


        String trimmed =
                query.trim();


        executor.execute(() -> {

            HttpURLConnection connection =
                    null;


            List<String> suggestions =
                    new ArrayList<>();


            try {

                String encodedWord =
                        java.net.URLEncoder
                                .encode(
                                        trimmed,
                                        "UTF-8"
                                )
                                .replace(
                                        "+",
                                        "%20"
                                );


                URL url =
                        new URL(
                                LEARNER_URL
                                        + encodedWord
                                        + "?key="
                                        + BuildConfig
                                        .MERRIAM_WEBSTER_LEARNER_API_KEY
                        );


                connection =
                        (HttpURLConnection)
                                url.openConnection();


                connection.setRequestMethod(
                        "GET"
                );


                connection.setConnectTimeout(
                        TIMEOUT_MS
                );


                connection.setReadTimeout(
                        TIMEOUT_MS
                );


                int status =
                        connection.getResponseCode();


                if (status ==
                        HttpURLConnection.HTTP_OK) {


                    String response =
                            readStream(
                                    connection
                                            .getInputStream()
                            );


                    JSONArray array =
                            new JSONArray(
                                    response
                            );


                    /*
                     * When Merriam-Webster cannot find
                     * an exact entry, it can return:
                     *
                     * [
                     *     "read",
                     *     "real",
                     *     "ready",
                     *     ...
                     * ]
                     *
                     * Those strings become our
                     * live search suggestions.
                     */

                    for (int i = 0;
                         i < array.length();
                         i++) {


                        Object item =
                                array.get(i);


                        if (item instanceof String) {

                            String suggestion =
                                    ((String) item)
                                            .trim();


                            if (!suggestion.isEmpty()) {

                                addUnique(
                                        suggestions,
                                        suggestion
                                );
                            }
                        }
                    }
                }


            } catch (Exception e) {

                Log.w(
                        "DictionaryApiClient",
                        "Suggestion request failed",
                        e
                );


            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }


            // =====================================================
            // MAIN THREAD
            // =====================================================

            mainHandler.post(() ->
                    callback.onSuggestions(
                            suggestions
                    )
            );
        });
    }


    // ============================================================
    // GET FROM LOCAL DATABASE
    // ============================================================

    private WordResult getFromLocalDb(
            Context context,
            String word) {


        if (context == null) {
            return null;
        }


        try {

            DatabaseHelper dbHelper =
                    DatabaseHelper.getInstance(
                            context
                    );


            SQLiteDatabase db =
                    dbHelper.getReadableDatabase();


            Cursor cursor =
                    db.query(
                            DatabaseHelper.TABLE_DICTIONARY,

                            new String[]{
                                    DatabaseHelper.COL_DICT_JSON
                            },

                            DatabaseHelper.COL_DICT_WORD
                                    + " = ?",

                            new String[]{
                                    word.trim()
                                            .toLowerCase(
                                            Locale.ROOT
                                    )
                            },

                            null,
                            null,
                            null
                    );


            if (cursor != null &&
                    cursor.moveToFirst()) {

                String json =
                        cursor.getString(0);


                cursor.close();


                return parseLearner(
                        json,
                        word
                );
            }


            if (cursor != null) {
                cursor.close();
            }


        } catch (Exception e) {

            Log.e(
                    "DictionaryApiClient",
                    "Error reading local database",
                    e
            );
        }


        return null;
    }


    // ============================================================
    // SAVE TO LOCAL DATABASE
    // ============================================================

    private void saveToLocalDb(
            Context context,
            String word,
            String json) {


        if (context == null) {
            return;
        }


        try {

            DatabaseHelper dbHelper =
                    DatabaseHelper.getInstance(
                            context
                    );


            SQLiteDatabase db =
                    dbHelper.getWritableDatabase();


            ContentValues values =
                    new ContentValues();


            values.put(
                    DatabaseHelper.COL_DICT_WORD,
                    word.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            )
            );


            values.put(
                    DatabaseHelper.COL_DICT_JSON,
                    json
            );


            db.insertWithOnConflict(
                    DatabaseHelper.TABLE_DICTIONARY,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );


        } catch (Exception e) {

            Log.e(
                    "DictionaryApiClient",
                    "Error saving dictionary data",
                    e
            );
        }
    }


    // ============================================================
    // READ HTTP RESPONSE
    // ============================================================

    private String readStream(
            InputStream inputStream)
            throws IOException {


        StringBuilder result =
                new StringBuilder();


        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     inputStream,
                                     StandardCharsets.UTF_8
                             )
                     )) {


            String line;


            while ((line =
                    reader.readLine()) != null) {

                result.append(line);
            }
        }


        return result.toString();
    }


    // ============================================================
    // PARSE LEARNER API
    // ============================================================

    private WordResult parseLearner(
            String json,
            String originalWord) {


        try {

            if (json == null) {
                return null;
            }


            String trimmedJson =
                    json.trim();


            if (!trimmedJson.startsWith("[")) {
                return null;
            }


            JSONArray array =
                    new JSONArray(
                            trimmedJson
                    );


            if (array.length() == 0) {
                return null;
            }


            /*
             * If the first item is a String,
             * Merriam-Webster returned suggestions
             * rather than a dictionary entry.
             */
            if (array.get(0) instanceof String) {
                return null;
            }


            String searchLower =
                    originalWord.trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );


            String headword = null;
            String phonetic = null;
            String audioUrl = null;


            // =====================================================
            // FIRST PASS
            // =====================================================

            for (int i = 0;
                 i < array.length();
                 i++) {


                Object item =
                        array.get(i);


                if (!(item instanceof JSONObject)) {
                    continue;
                }


                JSONObject entry =
                        (JSONObject) item;


                JSONObject meta =
                        entry.optJSONObject(
                                "meta"
                        );


                if (meta != null) {

                    String metaId =
                            meta.optString(
                                    "id",
                                    ""
                            );


                    String baseId =
                            getBaseId(
                                    metaId
                            );


                    if (!searchLower.contains(" ")
                            &&
                            !baseId.equalsIgnoreCase(
                                    searchLower
                            )) {

                        continue;
                    }
                }


                JSONObject hwi =
                        entry.optJSONObject(
                                "hwi"
                        );


                if (hwi == null) {
                    continue;
                }


                if (headword == null) {

                    String hw =
                            hwi.optString(
                                    "hw",
                                    originalWord
                            );


                    headword =
                            cleanHeadword(hw);
                }


                JSONArray prs =
                        hwi.optJSONArray(
                                "prs"
                        );


                if (prs == null ||
                        prs.length() == 0) {

                    continue;
                }


                Object prObject =
                        prs.get(0);


                if (!(prObject instanceof JSONObject)) {
                    continue;
                }


                JSONObject pr =
                        (JSONObject) prObject;


                // =================================================
                // PHONETIC
                // =================================================

                if (phonetic == null) {

                    phonetic =
                            pr.optString(
                                    "mw",
                                    pr.optString(
                                            "ipa",
                                            ""
                                    )
                            );
                }


                // =================================================
                // AUDIO
                // =================================================

                if (audioUrl == null) {

                    JSONObject sound =
                            pr.optJSONObject(
                                    "sound"
                            );


                    if (sound != null) {

                        String audioFile =
                                sound.optString(
                                        "audio",
                                        ""
                                );


                        if (!audioFile.isEmpty()) {

                            audioUrl =
                                    buildMerriamAudioUrl(
                                            audioFile
                                    );
                        }
                    }
                }
            }


            // =====================================================
            // CREATE RESULT
            // =====================================================

            String finalWord =
                    headword != null &&
                            !headword.isEmpty()
                            ? headword
                            : originalWord;


            WordResult result =
                    new WordResult(
                            finalWord
                    );


            // =====================================================
            // SECOND PASS
            // =====================================================

            for (int i = 0;
                 i < array.length();
                 i++) {


                Object item =
                        array.get(i);


                if (!(item instanceof JSONObject)) {
                    continue;
                }


                JSONObject entry =
                        (JSONObject) item;


                JSONObject meta =
                        entry.optJSONObject(
                                "meta"
                        );


                if (meta != null) {

                    String metaId =
                            meta.optString(
                                    "id",
                                    ""
                            );


                    String baseId =
                            getBaseId(
                                    metaId
                            );


                    if (!searchLower.contains(" ")
                            &&
                            !baseId.equalsIgnoreCase(
                                    searchLower
                            )) {

                        continue;
                    }
                }


                String pos =
                        entry.optString(
                                "fl",
                                "definition"
                        );


                Meaning meaning =
                        new Meaning(pos);

                // =================================================
                // LABELS (lbs / sls)
                // =================================================
                JSONArray lbs = entry.optJSONArray("lbs");
                if (lbs != null) {
                    for (int l = 0; l < lbs.length(); l++) {
                        String label = lbs.optString(l, "").trim();
                        if (!label.isEmpty()) {
                            addUnique(meaning.getLabels(), label);
                        }
                    }
                }
                JSONArray sls = entry.optJSONArray("sls");
                if (sls != null) {
                    for (int s = 0; s < sls.length(); s++) {
                        String label = sls.optString(s, "").trim();
                        if (!label.isEmpty()) {
                            addUnique(meaning.getLabels(), label);
                        }
                    }
                }

                // =================================================
                // INFLECTIONS / WORD FORMS (ins)
                // =================================================
                JSONArray ins = entry.optJSONArray("ins");
                if (ins != null) {
                    for (int infIndex = 0; infIndex < ins.length(); infIndex++) {
                        JSONObject infObj = ins.optJSONObject(infIndex);
                        if (infObj != null) {
                            String form = infObj.optString("if", "").trim();
                            if (!form.isEmpty()) {
                                addUnique(meaning.getInflections(), cleanHeadword(form));
                            }
                        }
                    }
                }

                // =================================================
                // ETYMOLOGY (et)
                // =================================================
                JSONArray etArray = entry.optJSONArray("et");
                if (etArray != null && etArray.length() > 0) {
                    StringBuilder etSb = new StringBuilder();
                    for (int e = 0; e < etArray.length(); e++) {
                        JSONArray etPair = etArray.optJSONArray(e);
                        if (etPair != null && etPair.length() >= 2) {
                            String etType = etPair.optString(0, "");
                            String etVal = etPair.optString(1, "");
                            if ("text".equalsIgnoreCase(etType) && !etVal.isEmpty()) {
                                if (etSb.length() > 0) etSb.append(" ");
                                etSb.append(cleanMerriamTokens(etVal));
                            }
                        }
                    }
                    if (etSb.length() > 0) {
                        meaning.setEtymology(etSb.toString().trim());
                    }
                }


                // =================================================
                // SHORT DEFINITIONS
                // =================================================

                JSONArray shortDefs =
                        entry.optJSONArray(
                                "shortdef"
                        );


                if (shortDefs != null) {

                    for (int d = 0;
                         d < shortDefs.length();
                         d++) {


                        String definitionText =
                                shortDefs.optString(
                                        d,
                                        ""
                                );


                        if (definitionText.isEmpty()) {
                            continue;
                        }


                        String cleanedDefinition =
                                cleanMerriamTokens(
                                        definitionText
                                );


                        String example =
                                extractMerriamExample(
                                        entry,
                                        d
                                );


                        meaning.getDefinitions()
                                .add(
                                        new Definition(
                                                cleanedDefinition,
                                                example
                                        )
                                );
                    }
                }


                // =================================================
                // FALLBACK DEFINITION
                // =================================================

                if (meaning.getDefinitions()
                        .isEmpty()) {


                    JSONObject appShortDef =
                            entry.optJSONObject(
                                    "app-shortdef"
                            );


                    if (appShortDef != null) {

                        JSONArray defArray =
                                appShortDef.optJSONArray(
                                        "def"
                                );


                        if (defArray != null) {

                            for (int d = 0;
                                 d < defArray.length();
                                 d++) {


                                String definitionText =
                                        defArray.optString(
                                                d,
                                                ""
                                        );


                                if (!definitionText.isEmpty()) {

                                    meaning.getDefinitions()
                                            .add(
                                                    new Definition(
                                                            cleanMerriamTokens(
                                                                    definitionText
                                                            ),
                                                            null
                                                    )
                                            );
                                }
                            }
                        }
                    }
                }


                // =================================================
                // ADD MEANING
                // =================================================

                if (!meaning.getDefinitions()
                        .isEmpty()) {


                    result.getMeanings()
                            .add(
                                    meaning
                            );
                }
            }


            // =====================================================
            // PHONETIC
            // =====================================================

            if (phonetic != null &&
                    !phonetic.trim().isEmpty()) {


                String cleanPhonetic =
                        phonetic.trim();


                if (!cleanPhonetic.startsWith("/")) {

                    cleanPhonetic =
                            "/"
                                    + cleanPhonetic
                                    + "/";
                }


                result.setPhonetic(
                        cleanPhonetic
                );
            }


            // =====================================================
            // AUDIO
            // =====================================================

            if (audioUrl != null &&
                    !audioUrl.trim().isEmpty()) {


                result.setAudioUrl(
                        audioUrl
                );
            }


            if (result.getMeanings().isEmpty()) {
                return null;
            }


            return result;


        } catch (Exception e) {

            Log.e(
                    "DictionaryApiClient",
                    "Learner parse error",
                    e
            );


            return null;
        }
    }


    // ============================================================
    // PARSE THESAURUS
    // ============================================================

    private void parseThesaurus(
            String json,
            WordResult result) {


        try {

            if (json == null ||
                    result == null) {

                return;
            }


            String trimmed =
                    json.trim();


            if (!trimmed.startsWith("[")) {
                return;
            }


            JSONArray array =
                    new JSONArray(
                            trimmed
                    );


            if (array.length() == 0) {
                return;
            }


            if (array.get(0) instanceof String) {
                return;
            }


            String searchedWord =
                    cleanHeadword(
                            result.getWord()
                    ).toLowerCase(
                            Locale.ROOT
                    );


            // =====================================================
            // THESAURUS ENTRIES
            // =====================================================

            for (int i = 0;
                 i < array.length();
                 i++) {


                Object item =
                        array.get(i);


                if (!(item instanceof JSONObject)) {
                    continue;
                }


                JSONObject entry =
                        (JSONObject) item;


                // =================================================
                // HEADWORD
                // =================================================

                JSONObject meta =
                        entry.optJSONObject(
                                "meta"
                        );


                if (meta == null) {
                    continue;
                }


                String metaId =
                        meta.optString(
                                "id",
                                ""
                        );


                if (metaId.isEmpty()) {
                    continue;
                }


                String baseId =
                        cleanHeadword(
                                getBaseId(
                                        metaId
                                )
                        );


                if (!searchedWord.contains(" ")
                        &&
                        !baseId.equalsIgnoreCase(
                                searchedWord
                        )) {

                    continue;
                }


                // =================================================
                // POS
                // =================================================

                String thesaurusPos =
                        entry.optString(
                                "fl",
                                ""
                        ).trim();


                if (thesaurusPos.isEmpty()) {

                    thesaurusPos =
                            meta.optString(
                                    "fl",
                                    ""
                            ).trim();
                }


                if (thesaurusPos.isEmpty()) {
                    continue;
                }


                // =================================================
                // FIND MATCHING POS
                // =================================================

                Meaning matchingMeaning =
                        findMatchingMeaning(
                                result.getMeanings(),
                                thesaurusPos
                        );


                if (matchingMeaning == null) {
                    continue;
                }


                /*
                 * Prevent multiple senses from being
                 * dumped into the same POS block.
                 */

                boolean alreadyHasData =
                        !matchingMeaning
                                .getSynonyms()
                                .isEmpty()
                                ||
                                !matchingMeaning
                                        .getAntonyms()
                                        .isEmpty();


                if (alreadyHasData) {
                    continue;
                }


                JSONArray defArray =
                        entry.optJSONArray(
                                "def"
                        );


                if (defArray == null) {
                    continue;
                }


                boolean foundUsefulSense =
                        false;


                // =================================================
                // FIRST USEFUL SENSE
                // =================================================

                for (int d = 0;
                     d < defArray.length()
                             &&
                             !foundUsefulSense;
                     d++) {


                    JSONObject defObject =
                            defArray.optJSONObject(
                                    d
                            );


                    if (defObject == null) {
                        continue;
                    }


                    JSONArray sseq =
                            defObject.optJSONArray(
                                    "sseq"
                            );


                    if (sseq == null) {
                        continue;
                    }


                    for (int s = 0;
                         s < sseq.length()
                                 &&
                                 !foundUsefulSense;
                         s++) {


                        JSONArray senseGroup =
                                sseq.optJSONArray(
                                        s
                                );


                        if (senseGroup == null) {
                            continue;
                        }


                        for (int x = 0;
                             x < senseGroup.length();
                             x++) {


                            JSONArray sensePair =
                                    senseGroup.optJSONArray(
                                            x
                                    );


                            if (sensePair == null ||
                                    sensePair.length() < 2) {

                                continue;
                            }


                            String senseType =
                                    sensePair.optString(
                                            0,
                                            ""
                                    );


                            if (!"sense".equals(
                                    senseType
                            )) {

                                continue;
                            }


                            JSONObject sense =
                                    sensePair.optJSONObject(
                                            1
                                    );


                            if (sense == null) {
                                continue;
                            }


                            // =================================================
                            // SYNONYMS
                            // =================================================

                            JSONArray synList =
                                    sense.optJSONArray(
                                            "syn_list"
                                    );


                            List<String> synonyms =
                                    new ArrayList<>();


                            extractWordList(
                                    synList,
                                    synonyms
                            );


                            // =================================================
                            // ANTONYMS
                            // =================================================

                            JSONArray antList =
                                    sense.optJSONArray(
                                            "ant_list"
                                    );


                            List<String> antonyms =
                                    new ArrayList<>();


                            extractWordList(
                                    antList,
                                    antonyms
                            );


                            // =================================================
                            // SAVE FIRST USEFUL SENSE
                            // =================================================

                            if (!synonyms.isEmpty()
                                    ||
                                    !antonyms.isEmpty()) {


                                for (String synonym :
                                        synonyms) {

                                    addUnique(
                                            matchingMeaning
                                                    .getSynonyms(),
                                            synonym
                                    );
                                }


                                for (String antonym :
                                        antonyms) {

                                    addUnique(
                                            matchingMeaning
                                                    .getAntonyms(),
                                            antonym
                                    );
                                }


                                foundUsefulSense =
                                        true;


                                break;
                            }
                        }
                    }
                }
            }


        } catch (Exception e) {

            Log.e(
                    "DictionaryApiClient",
                    "Thesaurus parse error",
                    e
            );
        }
    }


    // ============================================================
    // EXTRACT WORD LIST
    // ============================================================

    private void extractWordList(
            JSONArray listArray,
            List<String> destination) {


        if (listArray == null ||
                destination == null) {

            return;
        }


        for (int i = 0;
             i < listArray.length();
             i++) {


            JSONArray group =
                    listArray.optJSONArray(i);


            if (group == null) {
                continue;
            }


            for (int j = 0;
                 j < group.length();
                 j++) {


                JSONObject wordObject =
                        group.optJSONObject(j);


                if (wordObject == null) {
                    continue;
                }


                String word =
                        wordObject.optString(
                                "wd",
                                ""
                        ).trim();


                if (!word.isEmpty()) {

                    addUnique(
                            destination,
                            word
                    );
                }


                // =================================================
                // WORD VARIANTS
                // =================================================

                JSONArray variants =
                        wordObject.optJSONArray(
                                "wvrs"
                        );


                if (variants != null) {

                    for (int v = 0;
                         v < variants.length();
                         v++) {


                        JSONObject variant =
                                variants.optJSONObject(
                                        v
                                );


                        if (variant == null) {
                            continue;
                        }


                        String variantWord =
                                variant.optString(
                                        "wva",
                                        ""
                                ).trim();


                        if (!variantWord.isEmpty()) {

                            addUnique(
                                    destination,
                                    variantWord
                            );
                        }
                    }
                }
            }
        }
    }


    // ============================================================
    // FIND MATCHING MEANING
    // ============================================================

    private Meaning findMatchingMeaning(
            List<Meaning> meanings,
            String thesaurusPos) {


        if (meanings == null ||
                meanings.isEmpty()) {

            return null;
        }


        if (thesaurusPos == null ||
                thesaurusPos.trim().isEmpty()) {

            return null;
        }


        String normalizedThesaurusPos =
                normalizePartOfSpeech(
                        thesaurusPos
                );


        for (Meaning meaning :
                meanings) {


            if (meaning == null) {
                continue;
            }


            String dictionaryPos =
                    meaning.getPartOfSpeech();


            if (dictionaryPos == null ||
                    dictionaryPos.trim().isEmpty()) {

                continue;
            }


            String normalizedDictionaryPos =
                    normalizePartOfSpeech(
                            dictionaryPos
                    );


            if (normalizedThesaurusPos.equals(
                    normalizedDictionaryPos
            )) {

                return meaning;
            }
        }


        return null;
    }


    // ============================================================
    // NORMALIZE PART OF SPEECH
    // ============================================================

    private String normalizePartOfSpeech(
            String pos) {


        if (pos == null) {
            return "";
        }


        String value =
                pos.trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace(
                                "-",
                                " "
                        )
                        .replace(
                                "_",
                                " "
                        );


        if (value.equals("noun")
                ||
                value.equals("n")
                ||
                value.equals(
                        "noun abbreviation"
                )) {

            return "noun";
        }


        if (value.equals("verb")
                ||
                value.equals("v")
                ||
                value.equals(
                        "transitive verb"
                )
                ||
                value.equals(
                        "intransitive verb"
                )) {

            return "verb";
        }


        if (value.equals("adjective")
                ||
                value.equals("adj")) {

            return "adjective";
        }


        if (value.equals("adverb")
                ||
                value.equals("adv")) {

            return "adverb";
        }


        if (value.equals("pronoun")
                ||
                value.equals("pron")) {

            return "pronoun";
        }


        if (value.equals("preposition")
                ||
                value.equals("prep")) {

            return "preposition";
        }


        if (value.equals("conjunction")
                ||
                value.equals("conj")) {

            return "conjunction";
        }


        if (value.equals("interjection")
                ||
                value.equals("interj")) {

            return "interjection";
        }


        return value;
    }


    // ============================================================
    // ADD UNIQUE
    // ============================================================

    private void addUnique(
            List<String> list,
            String value) {


        if (list == null ||
                value == null) {

            return;
        }


        String cleaned =
                value.trim();


        if (cleaned.isEmpty()) {
            return;
        }


        for (String existing :
                list) {


            if (existing != null &&
                    existing.equalsIgnoreCase(
                            cleaned
                    )) {

                return;
            }
        }


        list.add(cleaned);
    }


    // ============================================================
    // CLEAN HEADWORD
    // ============================================================

    private String cleanHeadword(
            String hw) {

        if (hw == null) {
            return "";
        }

        return hw.replace("*", "")
                .replace("·", "")
                .replace("•", "")
                .trim();
    }


    // ============================================================
    // GET BASE ID
    // ============================================================

    private String getBaseId(
            String metaId) {


        if (metaId == null ||
                metaId.trim().isEmpty()) {

            return "";
        }


        String baseId;


        if (metaId.contains(":")) {

            baseId =
                    metaId.substring(
                            0,
                            metaId.indexOf(':')
                    );

        } else {

            baseId = metaId;
        }


        return baseId
                .replaceAll(
                        "\\d+$",
                        ""
                )
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // ============================================================
    // EXTRACT EXAMPLE
    // ============================================================

    private String extractMerriamExample(
            JSONObject entry,
            int defIndex) {


        try {

            JSONArray defArray =
                    entry.optJSONArray(
                            "def"
                    );


            if (defArray == null ||
                    defArray.length() == 0) {

                return null;
            }


            JSONObject defObject =
                    defArray.optJSONObject(0);


            if (defObject == null) {
                return null;
            }


            JSONArray sseq =
                    defObject.optJSONArray(
                            "sseq"
                    );


            if (sseq == null) {
                return null;
            }


            for (int s = 0;
                 s < sseq.length();
                 s++) {


                JSONArray senseBlock =
                        sseq.optJSONArray(s);


                if (senseBlock == null) {
                    continue;
                }


                for (int sb = 0;
                     sb < senseBlock.length();
                     sb++) {


                    JSONArray sensePair =
                            senseBlock.optJSONArray(
                                    sb
                            );


                    if (sensePair == null ||
                            sensePair.length() < 2) {

                        continue;
                    }


                    JSONObject senseData =
                            sensePair.optJSONObject(
                                    1
                            );


                    if (senseData == null) {
                        continue;
                    }


                    JSONArray dt =
                            senseData.optJSONArray(
                                    "dt"
                            );


                    if (dt == null) {
                        continue;
                    }


                    for (int dtIndex = 0;
                         dtIndex < dt.length();
                         dtIndex++) {


                        JSONArray dtPair =
                                dt.optJSONArray(
                                        dtIndex
                                );


                        if (dtPair == null ||
                                dtPair.length() < 2) {

                            continue;
                        }


                        String type =
                                dtPair.optString(
                                        0,
                                        ""
                                );


                        if (!"vis".equals(type)) {
                            continue;
                        }


                        JSONArray visArray =
                                dtPair.optJSONArray(
                                        1
                                );


                        if (visArray == null ||
                                visArray.length() == 0) {

                            continue;
                        }


                        if (defIndex <
                                visArray.length()) {


                            JSONObject visObject =
                                    visArray.optJSONObject(
                                            defIndex
                                    );


                            if (visObject != null) {

                                String text =
                                        visObject.optString(
                                                "t",
                                                ""
                                        );


                                if (!text.isEmpty()) {

                                    return cleanMerriamTokens(
                                            text
                                    );
                                }
                            }
                        }


                        JSONObject firstExample =
                                visArray.optJSONObject(0);


                        if (firstExample != null) {

                            String text =
                                    firstExample.optString(
                                            "t",
                                            ""
                                    );


                            if (!text.isEmpty()) {

                                return cleanMerriamTokens(
                                        text
                                );
                            }
                        }
                    }
                }
            }


        } catch (Exception ignored) {
        }


        return null;
    }


    // ============================================================
    // BUILD MERRIAM AUDIO URL
    // ============================================================

    private String buildMerriamAudioUrl(
            String audioFilename) {


        if (audioFilename == null ||
                audioFilename.trim().isEmpty()) {

            return null;
        }


        String audio =
                audioFilename.trim();


        String subfolder;


        if (audio.startsWith("bix")) {

            subfolder = "bix";

        } else if (audio.startsWith("gg")) {

            subfolder = "gg";

        } else if (
                Character.isDigit(
                        audio.charAt(0)
                )
                        ||
                        !Character.isLetter(
                                audio.charAt(0)
                        )
        ) {

            subfolder = "number";

        } else {

            subfolder =
                    String.valueOf(
                                    audio.charAt(0)
                            )
                            .toLowerCase(
                                    Locale.ROOT
                            );
        }


        return
                "https://media.merriam-webster.com/audio/prons/en/us/mp3/"
                        + subfolder
                        + "/"
                        + audio
                        + ".mp3";
    }


    // ============================================================
    // CLEAN MERRIAM TOKENS
    // ============================================================

    private String cleanMerriamTokens(
            String text) {


        if (text == null) {
            return "";
        }


        return text
                .replaceAll(
                        "\\{[^}]+\\}",
                        ""
                )
                .trim();
    }


    // ============================================================
    // POST TO MAIN THREAD
    // ============================================================

    private void postToMain(
            Runnable runnable) {

        mainHandler.post(
                runnable
        );
    }
}