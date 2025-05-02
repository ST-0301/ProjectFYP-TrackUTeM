package com.example.trackutem.controller;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RestLogController {
    private static final String TAG = "RestLogController";

    private final FirebaseFirestore db;

    public RestLogController() {
        db = FirebaseFirestore.getInstance();
    }

    public void createTodayRestLog(String driverId, RestLogCallback callback) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DocumentReference restLogRef = db.collection("drivers")
                .document(driverId)
                .collection("restLogs")
                .document(todayDate);

        restLogRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Map<String,Object> restLogMap = new HashMap<>();
                restLogMap.put("restCount", 0);
                restLogMap.put("startWorkTime", new Timestamp(new Date()));
                restLogMap.put("endWorkTime", null);

                restLogRef.set(restLogMap)
                        .addOnSuccessListener(aVoid -> callback.onReady(restLogRef));
            } else {
                callback.onReady(restLogRef);
            }
        });
    }

    public void incrementRestCount(String driverId) {
        createTodayRestLog(driverId, restLogRef ->
                restLogRef.update("restCount", FieldValue.increment(1))
                        .addOnSuccessListener(aVoid -> Log.d(TAG,"Rest count updated"))
                        .addOnFailureListener(e -> Log.e(TAG, "Rest count update failed", e)));
    }

    public void updateEndWorkTime(String driverId) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DocumentReference restLogRef = db.collection("drivers")
                .document(driverId)
                .collection("restLogs")
                .document(todayDate);

        restLogRef.update("endWorkTime", new Timestamp(new Date()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update end time", e));

    }

    public interface RestLogCallback {
        void onReady(DocumentReference restLogRef);
    }
}