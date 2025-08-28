package com.example.trackutem.controller;

import com.example.trackutem.model.UserRepository;
import com.example.trackutem.model.Driver;
import com.example.trackutem.model.Student;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginController {
    private final UserRepository userRepository;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    public LoginController() {
        this.userRepository = new UserRepository();
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void checkDriverStatusBeforeLogin(String email, String password, DriverLoginCallback callback) {
        db.collection("drivers")
                .whereEqualTo("email", email.toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String status = document.getString("status");
                        if ("disabled".equals(status)) {
                            callback.onDriverLoginFailure("Your account has been disabled. Please contact the administrator.");
                            return;
                        } else {
                            loginDriver(email, password, callback);
                        }
                    } else {
                        callback.onDriverLoginFailure("Driver account not found. Please contact administrator.");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onDriverLoginFailure("Error checking driver status: " + e.getMessage());
                });
    }

    // Student login with email and password
    public void loginStudent(String email, String password, StudentLoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            userRepository.getStudentByEmail(email, new UserRepository.OnStudentFetchedListener() {
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
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            userRepository.getDriverByEmail(email, new UserRepository.onDriverFetchedListener() {
                                @Override
                                public void onDriverFetched(Driver driver) {
                                    if ("pending".equals(driver.getStatus())) {
                                        updateDriverStatusOnLogin(driver.getDriverId(), new DriverStatusUpdateCallback() {
                                            @Override
                                            public void onDriverStatusUpdateSuccess() {
                                                callback.onDriverLoginSuccess(driver);
                                            }
                                            @Override
                                            public void onDriverStatusUpdateFailure(String errorMessage) {
                                                callback.onDriverLoginSuccess(driver);
                                            }
                                        });
                                    } else {
                                        callback.onDriverLoginSuccess(driver);
                                    }
                                }
                                @Override
                                public void onDriverFetchFailed(String errorMessage) {
                                    mAuth.signOut();
                                    callback.onDriverLoginFailure(errorMessage);
                                }
                            });
                        } else {
                            mAuth.signOut();
                            callback.onDriverLoginFailure(user == null ? "Driver not found" : "Email not verified");
                        }
                    } else {
                        handleDriverLoginError(task, callback);
                    }
                });
    }
    private void updateDriverStatusOnLogin(String driverId, DriverStatusUpdateCallback callback) {
        db.collection("drivers").document(driverId)
                .update(
                        "status", "available",
                        "link", null,
                        "linkGeneratedAt", null
                )
                .addOnSuccessListener(aVoid -> {
                    callback.onDriverStatusUpdateSuccess();
                })
                .addOnFailureListener(e -> {
                    callback.onDriverStatusUpdateFailure(e.getMessage());
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

    private void handleDriverLoginError(Task<AuthResult> task, DriverLoginCallback callback) {
        String errorMessage = "Login failed. Please try again";
        if (task.getException() instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();

            switch (errorCode) {
                case "ERROR_USER_NOT_FOUND":
                    errorMessage = "Email not found. Please contact administrator.";
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
        callback.onDriverLoginFailure(errorMessage);
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
    public interface DriverStatusUpdateCallback {
        void onDriverStatusUpdateSuccess();
        void onDriverStatusUpdateFailure(String errorMessage);
    }
}