//package com.example.trackutem.controller;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.example.trackutem.model.DatabaseHelper;
//import com.example.trackutem.view.MainDrvActivity;
//import com.example.trackutem.view.MainStuActivity;
//import com.google.firebase.auth.AuthResult;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.auth.OAuthProvider;
//import com.google.android.gms.tasks.Task;
//
//public class AuthManager {
//    private static final String TAG = "AuthManager";
//    private final FirebaseAuth mAuth;
//    private final Activity mActivity;
//
//    public interface AuthCallback {
//        void onSuccess(AuthResult authResult);
//        void onFailure(Exception e);
//    }
//
//    public AuthManager(Activity activity) {
//        mActivity = activity;
//        mAuth = FirebaseAuth.getInstance();
//    }
//
//    public void signInWithMicrosoft(AuthCallback callback) {
//        OAuthProvider.Builder provider = OAuthProvider.newBuilder("microsoft.com");
//        provider.addCustomParameter("prompt", "select_account");
//
//        Task<AuthResult> pendingResultTask = mAuth.getPendingAuthResult();
//        if (pendingResultTask != null) {
//            pendingResultTask.addOnSuccessListener(callback::onSuccess)
//                    .addOnFailureListener(e -> {
//                        Log.e(TAG, "Sign-in failed: ", e);
//                        callback.onFailure(e);
//                    });
//        } else {
//            mAuth.startActivityForSignInWithProvider(mActivity, provider.build())
//                    .addOnSuccessListener(callback::onSuccess)
//                    .addOnFailureListener(e -> {
//                        Log.e(TAG, "Sign-in failed: ", e);
//                        callback.onFailure(e);
//                    });
//        }
//    }
//
//    public void handleMicrosoftAuthResult(AuthResult authResult, boolean rememberMe, SharedPreferences sharedPreferences) {
//        FirebaseUser user = authResult.getUser();
//        if (user != null) {
//           String email = user.getEmail().toLowerCase();
//
//           // Save login state
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putBoolean("remember_me", rememberMe);
//            editor.putString("email", email);
//            editor.apply();
//
//            // Check if new user
//            if (authResult.getAdditionalUserInfo().isNewUser()) {
//                if (email.endsWith("@student.utem.edu.my")) {
//                    // Save user data to database as student
//                    new DatabaseHelper().saveUserData(user.getUid(), user.getDisplayName(), email, "student");
//                    redirectUser(email);
//                } else if (email.endsWith("@utem.edu.my")) {
//                    // Driver cannot register themselves
//                    Toast.makeText(mActivity, "Driver account must be registered by Admin", Toast.LENGTH_SHORT).show();
//                    signOut();
//
//                    editor.putBoolean("remember_me", false);
//                    editor.remove("email");
//                    editor.apply();
//                    return;
//                } else {
//                    Toast.makeText(mActivity, "Invalid email domain", Toast.LENGTH_SHORT).show();
//                    signOut();
//                    return;
//                }
//            } else {
//                // Existing user
//                if (email.endsWith("@student.utem.edu.my") || email.endsWith("@utem.edu.my")) {
//                    redirectUser(email);
//                } else {
//                    Toast.makeText(mActivity, "Invalid email domain", Toast.LENGTH_SHORT).show();
//                    signOut();
//                }
//            }
//        }
//    }
//
////    public boolean isUserLoggedIn() {
////        return mAuth.getCurrentUser() != null;
////    }
//
//    private void redirectUser(String email) {
//        try {
//            Class<?> targetActivity;
//            if (email.endsWith("@student.utem.edu.my")) {
//                targetActivity = MainStuActivity.class;
//            } else if (email.endsWith("@utem.edu.my")) {
//                targetActivity = MainDrvActivity.class;
//            } else {
//                Toast.makeText(mActivity, "Invalid email domain", Toast.LENGTH_SHORT).show();
//                signOut();
//                return;
//            }
//
//            Intent intent = new Intent(mActivity, targetActivity);
//            mActivity.startActivity(intent);
//            mActivity.finish();
//        } catch (Exception e) {
//            Toast.makeText(mActivity, "Error redirecting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//        //    private void redirectUser(String email) {
////        if (email.endsWith("@student.utem.edu.my")) {
////            Intent intent = new Intent(mActivity, MainStuActivity.class);
////            mActivity.startActivity(intent);
////        } else if (email.endsWith("@utem.edu.my")) {
////            Intent intent = new Intent(mActivity, MainDrvActivity.class);
////            mActivity.startActivity(intent);
////        } else {
////            Toast.makeText(mActivity, "Invalid email domain", Toast.LENGTH_SHORT).show();
////            signOut();
////        }
////        mActivity.finish();
////    }
//    }
//
//
////    private void redirectBasedOnEmailDomain(String email) {
////        if (email.endsWith("@student.utem.edu.my")) {
////            startActivity(new Intent(this, MainStuActivity.class));
////        } else if (email.endsWith("@utem.edu.my")) {
////            startActivity(new Intent(this, MainDrvActivity.class));
////        } else {
////            Toast.makeText(this, "Invalid email domain", Toast.LENGTH_SHORT).show();
////            authManager.signOut();
////        }
////        finish();
////    }
//
//    public void signOut() {
//        mAuth.signOut();
//    }
//}