package com.example.trackutem.controller;

import android.content.Context;
import com.example.trackutem.model.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterStudController {
    private final Context context;
    private final FirebaseAuth mAuth;
    private final DatabaseHelper dbHelper;

    public RegisterStudController(Context context) {
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

//    public boolean isValidPhone(String phone) {
//        // Phone number should start with +60, followed by 9-10 digits
//        String phoneRegex = "^\\+60\\d{9,10}$";
//        return phone.matches(phoneRegex);
//    }

    public void checkEmailAvailability(String email, DatabaseHelper.onEmailCheckedListener listener) {
        dbHelper.checkEmailExists(email, listener);
    }

//    public void checkPhoneAvailability(String phone, DatabaseHelper.onPhoneCheckedListener listener) {
//        dbHelper.checkPhoneExists(phone, listener);
//    }

    public void registerUser(String name, String email, String password, RegistrationCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save user data
                            dbHelper.saveUserData(user.getUid(), name, email, "student");
                            callback.onSuccess();
                        } else {
                            callback.onFailure("User creation failed");
                        }
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";
                        callback.onFailure(error);
                    }
                });
    }

//    public void sendOtp(String phone, OtpSentCallback callback) {
//        Activity activity = (Activity) context;
//        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
//                .setPhoneNumber(phone)
//                .setTimeout(60L, TimeUnit.SECONDS)
//                .setActivity(activity)
//                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//                    @Override
//                    public void onCodeSent(@NonNull String verificationId,
//                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
//                        callback.onOtpSent(verificationId);
//                    }
//                    @Override
//                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
//                        // Auto-verification handling
//                    }
//                    @Override
//                    public void onVerificationFailed(@NonNull FirebaseException e) {
//                        Toast.makeText(activity, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                    }
//                })
//                .build();
//        PhoneAuthProvider.verifyPhoneNumber(options);
//    }

    public interface RegistrationCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

//    public interface OtpSentCallback {
//        void onOtpSent(String verificationId);
//    }
}