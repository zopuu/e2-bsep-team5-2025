package com.besp.pki.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PasswordValidationService {
    
    // OWASP recommended patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    public PasswordStrengthResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrengthResult(false, "Password cannot be empty", 0);
        }
        
        // Check minimum length (OWASP: min 8 characters)
        if (password.length() < 8) {
            return new PasswordStrengthResult(false, "Password must be at least 8 characters long", 0);
        }
        
        // Check maximum length (OWASP: max 128 characters for password managers)
        if (password.length() > 128) {
            return new PasswordStrengthResult(false, "Password cannot exceed 128 characters", 0);
        }
        
        // Calculate strength score
        int score = calculateStrengthScore(password);
        String message = getStrengthMessage(score);
        
        // Password is valid if it meets minimum requirements
        boolean isValid = score >= 2; // At least 2 out of 4 criteria
        
        return new PasswordStrengthResult(isValid, message, score);
    }
    
    private int calculateStrengthScore(String password) {
        int score = 0;
        
        // Length bonus
        if (password.length() >= 12) score++;
        
        // Character type bonuses
        if (UPPERCASE_PATTERN.matcher(password).find()) score++;
        if (LOWERCASE_PATTERN.matcher(password).find()) score++;
        if (DIGIT_PATTERN.matcher(password).find()) score++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score++;
        
        return score;
    }
    
    private String getStrengthMessage(int score) {
        return switch (score) {
            case 0, 1 -> "Very Weak - Add uppercase, lowercase, numbers, and special characters";
            case 2 -> "Weak - Consider adding more character types";
            case 3 -> "Medium - Good password strength";
            case 4 -> "Strong - Excellent password strength";
            case 5 -> "Very Strong - Maximum password strength";
            default -> "Unknown strength";
        };
    }
    
    public static class PasswordStrengthResult {
        private final boolean valid;
        private final String message;
        private final int score;
        
        public PasswordStrengthResult(boolean valid, String message, int score) {
            this.valid = valid;
            this.message = message;
            this.score = score;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getScore() {
            return score;
        }
        
        public String getStrengthLevel() {
            return switch (score) {
                case 0, 1 -> "Very Weak";
                case 2 -> "Weak";
                case 3 -> "Medium";
                case 4 -> "Strong";
                case 5 -> "Very Strong";
                default -> "Unknown";
            };
        }
    }
}


