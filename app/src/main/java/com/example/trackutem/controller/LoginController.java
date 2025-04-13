package com.example.trackutem.controller;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

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
                        handleSuccessfulLogin(callback);
                    } else {
                        handleLoginError(task, callback);
                    }
                });
    }
    private void handleSuccessfulLogin(LoginCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isEmailVerified()) {
            callback.onLoginSuccess();
        } else {
            mAuth.signOut();
            callback.onLoginFailure("Email not verified");
        }
    }

    private void handleLoginError(Task<AuthResult> task, LoginCallback callback) {
        String errorMessage = "Login failed. Please try again";
        if (task.getException() instanceof FirebaseAuthException) {
            String errorCode;
            errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();

            switch (errorCode) {
                case "ERROR_USER_NOT_FOUND":
                    errorMessage = "Email not found. Please register first.";
                    break;
                case "ERROR_WRONG_PASSWORD":
                    errorMessage = "Incorrect password. Please try again.";
                    break;
                case "ERROR_INVALID_EMAIL":
                    errorMessage = "Invalid email format.";
                    break;
                default:
                    errorMessage = "Incorrect email or password.";
            }
        }
        callback.onLoginFailure(errorMessage);
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