package com.wordspark.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.wordspark.app.db.SettingsDao;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.db.UserDao;
import com.wordspark.app.model.User;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.notification.NotificationHelper;
import com.wordspark.app.util.SessionManager;
import com.wordspark.app.util.ThemeUtil;
import com.wordspark.app.util.ValidationUtil;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_EMAIL = "extra_prefill_email";

    private EditText etEmail, etPassword;
    private TextView tvEmailError, tvPasswordError;
    private Button btnLogin;
    private ProgressBar progressBar;
    private ImageView ivTogglePassword;
    private UserDao userDao;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userDao = new UserDao(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        btnLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            String typedEmail = etEmail.getText().toString().trim();
            if (!typedEmail.isEmpty()) intent.putExtra(ForgotPasswordActivity.EXTRA_EMAIL, typedEmail);
            startActivity(intent);
        });

        applyPrefillEmail(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Returning here after a password reset or registration: show the email and focus password.
        applyPrefillEmail(intent);
    }

    private void applyPrefillEmail(Intent intent) {
        if (intent == null) return;
        String email = intent.getStringExtra(EXTRA_PREFILL_EMAIL);
        if (email == null || email.isEmpty()) {
            email = intent.getStringExtra(ForgotPasswordActivity.EXTRA_PREFILL_EMAIL);
        }
        if (email != null && !email.isEmpty()) {
            etEmail.setText(email);
            etPassword.setText("");
            tvEmailError.setVisibility(View.GONE);
            tvPasswordError.setVisibility(View.GONE);
            etPassword.requestFocus();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If a session is already active (app was reopened), skip straight to Home.
        if (SessionManager.isLoggedIn(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        etPassword.setInputType(passwordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setSelection(etPassword.getText().length());
    }

    private void attemptLogin() {
        tvEmailError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        boolean valid = true;
        if (ValidationUtil.isEmpty(email)) {
            showError(tvEmailError, getString(R.string.err_required_field));
            valid = false;
        }
        if (ValidationUtil.isEmpty(password)) {
            showError(tvPasswordError, getString(R.string.err_required_field));
            valid = false;
        }
        if (!valid) return;

        setLoading(true);

        new Thread(() -> {
            User loggedIn = null;
            UserSettings loadedSettings = null;
            boolean dbError = false;
            String errorDetail = null;
            try {
                loggedIn = userDao.login(email, password);

                // Load this user's own preferences on the background thread so the
                // correct theme/reminder can be applied the moment they land on Home.
                loadedSettings = (loggedIn == null)
                        ? null
                        : new SettingsDao(this).getSettings(loggedIn.getId());
            } catch (Exception e) {
                dbError = true; // a database problem must show a message, not crash
                android.util.Log.e("WordSparkLogin", "Login failed", e);
                errorDetail = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            final User user = loggedIn;
            final UserSettings userSettings = loadedSettings;
            final boolean failed = dbError;
            final String detail = errorDetail;

            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                setLoading(false);
                if (failed) {
                    showError(tvPasswordError, getString(R.string.err_generic_try_again) + "\n[" + detail + "]");
                    return;
                }
                if (user == null) {
                    showError(tvPasswordError, getString(R.string.err_invalid_login));
                    return;
                }

                SessionManager.login(this, user.getId(), user.getName());
                applyUserPreferences(userSettings);

                startActivity(new Intent(this, MainActivity.class));
                finishAffinity();
            });
        }).start();
    }

    /**
     * Applies the signed-in user's per-account preferences: their theme becomes
     * the active theme, and their daily reminder is re-armed if they had it on.
     * This is what makes Settings genuinely per-user rather than global.
     */
    private void applyUserPreferences(UserSettings userSettings) {
        if (userSettings == null) return;

        SessionManager.setCachedTheme(this, userSettings.getTheme());
        ThemeUtil.apply(userSettings.getTheme());

        if (userSettings.isNotificationsEnabled()) {
            try {
                String[] parts = userSettings.getNotificationTime().split(":");
                NotificationHelper.scheduleDaily(this,
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } catch (Exception ignored) {
                // Malformed stored time should never block login.
            }
        } else {
            NotificationHelper.cancel(this);
        }
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
