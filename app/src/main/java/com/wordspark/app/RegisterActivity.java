package com.wordspark.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wordspark.app.db.UserDao;
import com.wordspark.app.model.UserSettings;
import com.wordspark.app.util.LifecycleUtil;
import com.wordspark.app.util.SessionManager;
import com.wordspark.app.util.ThemeUtil;
import com.wordspark.app.util.ValidationUtil;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private TextView tvNameError, tvEmailError, tvPasswordError, tvConfirmError;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private UserDao userDao;

    private boolean isPasswordVisible = false;
    private boolean isConfirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userDao = new UserDao(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvNameError = findViewById(R.id.tvNameError);
        tvEmailError = findViewById(R.id.tvEmailError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        tvConfirmError = findViewById(R.id.tvConfirmError);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> attemptRegister());
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        ivToggleConfirmPassword.setOnClickListener(v -> toggleConfirmVisibility());

        findViewById(R.id.tvGoLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivTogglePassword.setContentDescription(getString(R.string.hide_password));
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivTogglePassword.setContentDescription(getString(R.string.show_password));
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void toggleConfirmVisibility() {
        isConfirmVisible = !isConfirmVisible;
        if (isConfirmVisible) {
            etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivToggleConfirmPassword.setContentDescription(getString(R.string.hide_confirm_password));
        } else {
            etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivToggleConfirmPassword.setContentDescription(getString(R.string.show_confirm_password));
        }
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    private void attemptRegister() {
        hideErrors();

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        boolean valid = true;

        if (ValidationUtil.isEmpty(name)) {
            showError(tvNameError, getString(R.string.err_required_field));
            valid = false;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            showError(tvEmailError, getString(R.string.err_invalid_email));
            valid = false;
        }
        String passwordError = ValidationUtil.getPasswordValidationError(password);
        if (passwordError != null) {
            showError(tvPasswordError, passwordError);
            valid = false;
        }
        if (!password.equals(confirmPassword)) {
            showError(tvConfirmError, getString(R.string.err_password_mismatch));
            valid = false;
        }

        if (!valid) return;

        setLoading(true);

        new Thread(() -> {
            boolean isDuplicate = false;
            long createdId = -1;
            boolean dbError = false;
            String errorDetail = null;
            try {
                isDuplicate = userDao.emailExists(email);
                if (!isDuplicate) {
                    createdId = userDao.registerUserWithSettings(name, email, password);
                }
            } catch (Exception e) {
                dbError = true;
                android.util.Log.e("WordSparkRegister", "Registration failed", e);
                errorDetail = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            final boolean duplicate = isDuplicate;
            final long newUserId = createdId;
            final boolean failed = dbError;
            final String detail = errorDetail;

            runOnUiThread(() -> {
                if (LifecycleUtil.isGone(this)) return;
                setLoading(false);
                if (duplicate) {
                    showError(tvEmailError, getString(R.string.err_account_exists));
                    return;
                }
                if (failed || newUserId == -1) {
                    showError(tvEmailError, getString(R.string.err_generic_try_again) + (detail != null ? "\n[" + detail + "]" : ""));
                    return;
                }

                Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra(LoginActivity.EXTRA_PREFILL_EMAIL, email);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }

    private void hideErrors() {
        tvNameError.setVisibility(View.GONE);
        tvEmailError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);
        tvConfirmError.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
