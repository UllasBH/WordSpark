package com.wordspark.app.util;

import android.util.Patterns;

public class ValidationUtil {

    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return !isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return getPasswordValidationError(password) == null;
    }

    public static String getPasswordValidationError(String password) {
        if (password == null || password.isEmpty()) {
            return "Please enter your password.";
        }
        if (password.length() < 8) {
            return "Password must contain at least 8 characters.";
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(c)) {
                hasSpecial = true;
            }
        }
        if (!hasUpper) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!hasLower) {
            return "Password must contain at least one lowercase letter.";
        }
        if (!hasDigit) {
            return "Password must contain at least one number.";
        }
        if (!hasSpecial) {
            return "Password must contain at least one special character.";
        }
        return null;
    }
}
