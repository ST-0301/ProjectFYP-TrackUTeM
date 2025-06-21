package com.example.trackutem.controller;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class TrackingController {
    private static final String TAG = "TrackingController2";
    private final FirebaseFirestore db;
    private final String driverId;

    public TrackingController(String driverId) {
        this.db = FirebaseFirestore.getInstance();
        this.driverId = driverId;
    }

    public void updateLocationToFirestore(double lat, double lng) {
        GeoPoint location = new GeoPoint(lat, lng);

        db.collection("drivers").document(driverId)
                .update("currentLocation", location,"lastUpdate", Timestamp.now())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore location updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore update failed", e));
    }
}