package com.example.trackutem.controller;

import android.os.Looper;
import com.example.trackutem.model.DatabaseHelper;
import com.example.trackutem.model.Driver;
import com.example.trackutem.model.Student;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import org.mindrot.jbcrypt.BCrypt;

public class LoginController {
    private final DatabaseHelper databaseHelper;
    private final FirebaseAuth mAuth;

    public LoginController() {
        this.databaseHelper = new DatabaseHelper();
        this.mAuth = FirebaseAuth.getInstance();
    }

    // Student login with email and password
    public void loginStudent(String email, String password, StudentLoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
//                            callback.onStudentLoginSuccess(user.getEmail());
                            databaseHelper.getStudentByEmail(email, new DatabaseHelper.OnStudentFetchedListener() {
                                @Override
                                public void onStudentFetched(Student student) {
                                    callback.onStudentLoginSuccess(student);
                                }
                                @Override
                                public void onStudentFetchFailed(String errorMessage) {
                                    mAuth.signOut();
                                    callback.onStudentLoginFailure(errorMessage);
                                }
                            });
                        } else {
                            mAuth.signOut();
                            callback.onStudentLoginFailure(user == null ? "Student not found" : "Email not verified");
                        }
                    } else {
                        handleStudentLoginError(task, callback);
                    }
                });
    }
    public void loginDriver(String email, String password, DriverLoginCallback callback) {
        databaseHelper.getDriverByEmail(email, new DatabaseHelper.onDriverFetchedListener() {
            @Override
            public void onDriverFetched(Driver driver) {
                new Thread(() -> {
                    try {
                        String storedHash = driver.getPassword();
                        if (storedHash == null || storedHash.isEmpty()) {
                            throw new IllegalArgumentException("Invalid password hash");
                        }

                        // Normalize hash format
                        if (storedHash.startsWith("$2b$")) {
                            storedHash = "$2a" + storedHash.substring(3);
                        }
                        boolean matched = BCrypt.checkpw(password, storedHash);
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            if (matched) {
                                callback.onDriverLoginSuccess(driver);
                            } else {
                                callback.onDriverLoginFailure("Incorrect password.");
                            }
                        });
                    } catch (Exception e) {
                        new android.os.Handler(Looper.getMainLooper()).post(() -> {
                            callback.onDriverLoginFailure("Authentication error: " + e.getMessage());
                        });
                    }
                }).start();
            }
            @Override
            public void onDriverFetchFailed(String errorMessage) {
                callback.onDriverLoginFailure(errorMessage);
            }
        });
    }
    private void handleStudentLoginError(Task<AuthResult> task, StudentLoginCallback callback) {
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
        callback.onStudentLoginFailure(errorMessage);
    }

    // Send password reset email to student
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

    public interface StudentLoginCallback {
        void onStudentLoginSuccess(Student student);
        void onStudentLoginFailure(String errorMessage);
    }
    public interface ResetPasswordCallback {
        void onResetPasswordSuccess();
        void onResetPasswordFailure(String errorMessage);
    }
    public interface DriverLoginCallback {
        void onDriverLoginSuccess(Driver driver);
        void onDriverLoginFailure(String errorMessage);
    }
}