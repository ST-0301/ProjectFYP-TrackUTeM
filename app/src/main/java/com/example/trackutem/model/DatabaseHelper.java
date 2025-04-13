package com.example.trackutem.model;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class DatabaseHelper {
    private final FirebaseFirestore db;

    public DatabaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveUserData(String userId, String name, String email, String role) {
        User user = new User(userId, name, email, role);

        CollectionReference usersRef = db.collection("users");
        usersRef.document(userId).set(user)
                .addOnSuccessListener(aVoid -> Log.d("DatabaseHelper", "User data saved successfully"))
                .addOnFailureListener(e -> Log.e("DatabaseHelper", "Error saving user data", e));
    }

    public void getUserData(String userId, onUserDataFetchedListener listener) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        listener.onDataFetched(user);
                    } else {
                        Log.e("DatabaseHelper", "No such user found!");
                    }
                })
                .addOnFailureListener(e -> Log.e("DatabaseHelper", "Error fetching user data", e));
    }

    public void getAllUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for(DocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);

                        if (user != null) {
                            Log.d("DatabaseHelper", "User: " + user.getName());
                        } else {
                            Log.e("DatabaseHelper", "User object is null for document: " + document.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DatabaseHelper", "Error fetching all users", e));
    }

    public void checkEmailExists(String email, final onEmailCheckedListener listener) {
        db.collection("users")
                .whereEqualTo("email", email.toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        boolean isEmailAvailable = (result == null || result.isEmpty());
                        listener.onEmailChecked(isEmailAvailable);
                    } else {
                        Log.e("DatabaseHelper", "Error checking email availability", task.getException());
                        listener.onEmailChecked(false); // Treat failure as email exists (for safety)
                    }
                });
    }

    public interface onUserDataFetchedListener {
        void onDataFetched(User user);
    }

    public interface onEmailCheckedListener {
        void onEmailChecked(boolean isAvailable);
    }

}
