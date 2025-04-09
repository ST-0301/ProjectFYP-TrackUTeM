package com.example.trackutem.controller;

import com.google.firebase.auth.FirebaseAuth;

public class LoginController {
    private final FirebaseAuth mAuth;

    public LoginController() {
        this.mAuth = FirebaseAuth.getInstance();
    }

    // Perform login with email and password
    public void loginUser(String email, String password, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onLoginSuccess();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        callback.onLoginFailure(errorMessage);
                    }
                });
    }

    // Send password reset email
    public void resetPassword(String email, ResetPasswordCallback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onResetPasswordSuccess();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        callback.onResetPasswordFailure(errorMessage);
                    }
                });
    }

    public interface LoginCallback {
        void onLoginSuccess();
        void onLoginFailure(String errorMessage);
    }

    public interface ResetPasswordCallback {
        void onResetPasswordSuccess();
        void onResetPasswordFailure(String errorMessage);
    }
}