package com.example.trackutem.model;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private final FirebaseFirestore db;

    public DatabaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveStudentData(String studentId, String name, String email) {
        Student student = new Student(studentId, name, email);

        CollectionReference studentRef = db.collection("students");
        studentRef.document(studentId).set(student)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Student data saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving student data", e));
    }
    public void checkEmailExists(String email, final onEmailCheckedListener listener) {
        db.collection("students")
                .whereEqualTo("email", email.toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        boolean isEmailAvailable = (result == null || result.isEmpty());
                        listener.onEmailChecked(isEmailAvailable);
                    } else {
                        Log.e(TAG, "Error checking email availability", task.getException());
                        listener.onEmailChecked(false); // Treat failure as email exists (for safety)
                    }
                });
    }
    public void getStudentByEmail(String email, OnStudentFetchedListener listener) {
        db.collection("students")
                .whereEqualTo("email", email.toLowerCase())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        Student student = document.toObject(Student.class);
                        if (student != null) {
                            listener.onStudentFetched(student);
                        } else {
                            listener.onStudentFetchFailed("Student not found");
                        }
                    }
                })
                .addOnFailureListener(e -> listener.onStudentFetchFailed("Error fetching student: " + e.getMessage()));
    }
    public void getDriverByEmail(String email, onDriverFetchedListener listener) {
        db.collection("drivers")
                .whereEqualTo("email", email.toLowerCase())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        Driver driver = document.toObject(Driver.class);
                        if (driver != null) {
                            listener.onDriverFetched(driver);
                        } else {
                            listener.onDriverFetchFailed("Driver not found");
                        }
                    }
                })
                .addOnFailureListener(e -> listener.onDriverFetchFailed("Error fetching driver: " + e.getMessage()));
    }

    public interface onEmailCheckedListener {
        void onEmailChecked(boolean isAvailable);
    }
    public interface OnStudentFetchedListener {
        void onStudentFetched(Student student);
        void onStudentFetchFailed(String errorMessage);
    }
    public interface onDriverFetchedListener {
        void onDriverFetched(Driver driver);
        void onDriverFetchFailed(String errorMessage);
    }
}