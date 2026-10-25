package com.wordspark.app.network;

import com.wordspark.app.model.WordResult;
/**
 * Callback for asynchronous Dictionary API lookups.
 * Exactly one of the four methods is invoked on the main thread
 * once the request finishes.
 */
public interface ApiCallback {
    void onSuccess(WordResult result);
    void onWordNotFound();
    void onNetworkError();
    void onApiError();
}
