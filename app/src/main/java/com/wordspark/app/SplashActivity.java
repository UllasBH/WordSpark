package com.wordspark.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1800; // 1.8 seconds for a smooth, visible splash experience

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (LifecycleUtil.isGone(this)) return;

            Intent intent;
            if (SessionManager.isLoggedIn(this)) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DURATION_MS);
    }
}
