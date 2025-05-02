package com.example.trackutem.controller;

import android.content.Context;
import com.example.trackutem.model.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterStuController {
    private final Context context;
    private final FirebaseAuth mAuth;
    private final DatabaseHelper dbHelper;

    public RegisterStuController(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.dbHelper = new DatabaseHelper();
    }

    // Method to validate email format (using regex)
    public boolean isValidEmail(String email) {
        String emailRegex = "(?i)^[a-zA-Z0-9._%+-]+@(student\\.utem\\.edu\\.my|utem\\.edu\\.my)$";
        return email.matches(emailRegex);
    }
    public boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[A-Za-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?#^;:,./])[A-Za-z\\d@$!%*?#^;:,./]{8,}$";
        return password.matches(passwordRegex);
    }
    public void checkEmailAvailability(String email, DatabaseHelper.onEmailCheckedListener listener) {
        dbHelper.checkEmailExists(email, listener);
    }

    public void registerStudent(String name, String email, String password, RegistrationCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send verification email
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                       if (emailTask.isSuccessful()) {
                                           // Save student data
                                           dbHelper.saveStudentData(user.getUid(), name, email, "student");
                                           callback.onSuccess();
                                       }
                                    });
                        } else {
                            callback.onFailure("User creation failed");
                        }
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed. Please try again";
                        callback.onFailure(error);
                    }
                });
    }

    public interface RegistrationCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }
}