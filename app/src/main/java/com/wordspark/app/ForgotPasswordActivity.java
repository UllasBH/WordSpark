package com.wordspark.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.wordspark.app.db.UserDao;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.util.ValidationUtil;

import java.util.Locale;

/**
 * Local (prototype) password reset.
 *
 * Step 1: the user enters their registered email, which is checked against the
 *         existing "users" table through {@link UserDao}.
 * Step 2: if the account exists, the user chooses a new password. It is
 *         validated with the same rules as registration, hashed through the
 *         existing PasswordUtil (via UserDao.updatePassword), and then read back
 *         through the normal login path to confirm it really works.
 *
 * NOTE: there is no email/OTP verification because the app has no backend.
 * Anyone who knows a registered email on this device can reset that account's
 * password. That is acceptable for an offline prototype but must be replaced
 * (e.g. emailed one-time link/code) before real use.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_PREFILL_EMAIL = "prefill_email"; // used when returning to Login

    private static final String STATE_VERIFIED_EMAIL = "state_verified_email";

    private View sectionEmail, sectionNewPassword;
    private EditText etEmail, etNewPassword, etConfirmNewPassword;
    private TextView tvSubtitle, tvEmailError, tvNewPasswordError, tvConfirmPasswordError;
    private Button btnContinue, btnResetPassword;
    private ProgressBar progressBar;

    private UserDao userDao;
    private String verifiedEmail;          // non-null once step 1 has succeeded
    private boolean busy = false;          // guards against double taps while a DB call is in flight
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        userDao = new UserDao(this);

        sectionEmail = findViewById(R.id.sectionEmail);
        sectionNewPassword = findViewById(R.id.sectionNewPassword);
        etEmail = findViewById(R.id.etEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvNewPasswordError = findViewById(R.id.tvNewPasswordError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        btnContinue = findViewById(R.id.btnContinue);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
        btnContinue.setOnClickListener(v -> verifyEmail());
        btnResetPassword.setOnClickListener(v -> resetPassword());

        findViewById(R.id.ivToggleNewPassword).setOnClickListener(v -> {
            newPasswordVisible = !newPasswordVisible;
            applyVisibility(etNewPassword, newPasswordVisible);
        });
        findViewById(R.id.ivToggleConfirmPassword).setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            applyVisibility(etConfirmNewPassword, confirmPasswordVisible);
        });

        if (savedInstanceState != null) {
            verifiedEmail = savedInstanceState.getString(STATE_VERIFIED_EMAIL);
        } else {
            String prefill = getIntent().getStringExtra(EXTRA_EMAIL);
            if (prefill != null) etEmail.setText(prefill);
        }
        if (verifiedEmail != null) showNewPasswordStep();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Only the verified email is kept; typed passwords are deliberately not
        // persisted across recreation.
        outState.putString(STATE_VERIFIED_EMAIL, verifiedEmail);
    }

    // ---------------------------------------------------------------- step 1

    private void verifyEmail() {
        if (busy) return;
        tvEmailError.setVisibility(View.GONE);

        String email = etEmail.getText().toString().trim();
        if (ValidationUtil.isEmpty(email)) {
            showError(tvEmailError, getString(R.string.err_required_field));
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            showError(tvEmailError, getString(R.string.err_invalid_email));
            return;
        }

        setBusy(true);
        new Thread(() -> {
            boolean exists;
            boolean failed = false;
            String errorDetail = null;
            try {
                exists = userDao.emailExists(email);
            } catch (Exception e) {
                exists = false;
                failed = true;
                android.util.Log.e("WordSparkReset", "Email lookup failed", e);
                errorDetail = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            final boolean found = exists;
            final boolean dbError = failed;
            final String detail = errorDetail;
            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                setBusy(false);
                if (dbError) {
                    showError(tvEmailError, getString(R.string.err_generic_try_again) + "\n[" + detail + "]");
                } else if (!found) {
                    showError(tvEmailError, getString(R.string.err_email_not_registered));
                } else {
                    verifiedEmail = email.toLowerCase(Locale.ROOT);
                    showNewPasswordStep();
                }
            });
        }).start();
    }

    private void showNewPasswordStep() {
        sectionEmail.setVisibility(View.GONE);
        sectionNewPassword.setVisibility(View.VISIBLE);
        tvSubtitle.setText(getString(R.string.reset_new_password_subtitle, verifiedEmail));
        etNewPassword.requestFocus();
    }

    // ---------------------------------------------------------------- step 2

    private void resetPassword() {
        if (busy || verifiedEmail == null) return;
        tvNewPasswordError.setVisibility(View.GONE);
        tvConfirmPasswordError.setVisibility(View.GONE);

        String newPassword = etNewPassword.getText().toString();
        String confirm = etConfirmNewPassword.getText().toString();

        boolean valid = true;
        String passwordError = ValidationUtil.getPasswordValidationError(newPassword);
        if (passwordError != null) {
            showError(tvNewPasswordError, passwordError);
            valid = false;
        }
        if (confirm.isEmpty()) {
            showError(tvConfirmPasswordError, getString(R.string.err_required_field));
            valid = false;
        } else if (!confirm.equals(newPassword)) {
            showError(tvConfirmPasswordError, getString(R.string.err_password_mismatch));
            valid = false;
        }
        if (!valid) return;

        final String email = verifiedEmail;
        setBusy(true);
        new Thread(() -> {
            boolean success = false;
            try {
                // Update through the existing hashing path, then prove it works by
                // logging in with the new password via the normal login query.
                success = userDao.updatePassword(email, newPassword)
                        && userDao.login(email, newPassword) != null;
            } catch (Exception ignored) {
                // falls through to the friendly failure message below
            }
            final boolean ok = success;
            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                setBusy(false);
                if (!ok) {
                    showError(tvConfirmPasswordError, getString(R.string.err_password_reset_failed));
                    return;
                }
                Toast.makeText(this, R.string.password_reset_success, Toast.LENGTH_LONG).show();
                Intent login = new Intent(this, LoginActivity.class);
                login.putExtra(EXTRA_PREFILL_EMAIL, email);
                login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(login);
                finish();
            });
        }).start();
    }

    // ---------------------------------------------------------------- helpers

    private void applyVisibility(EditText field, boolean visible) {
        field.setInputType(visible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setSelection(field.getText().length());
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    private void setBusy(boolean isBusy) {
        busy = isBusy;
        progressBar.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        btnContinue.setEnabled(!isBusy);
        btnResetPassword.setEnabled(!isBusy);
    }
}
