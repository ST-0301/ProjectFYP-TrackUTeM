package com.example.trackutem.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;

public class Stop {
    private String stopId;
    private String name;
    private GeoPoint location;  // Use Firestore's GeoPoint type

    // Required empty constructor for Firestore
    public Stop() {}

    // Firestore field mappings
    @PropertyName("stopId")
    public String getStopId() { return stopId; }

    @PropertyName("stopId")
    public void setStopId(String stopId) { this.stopId = stopId; }

    @PropertyName("name")
    public String getName() { return name; }

    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("location")
    public GeoPoint getLocation() { return location; }

    @PropertyName("location")
    public void setLocation(GeoPoint location) { this.location = location; }

    // Helper method to convert to LatLng
    @Exclude
    public LatLng getLatLng() {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }



    public interface StopCallback {
        void onSuccess(String stopName);
        void onError(Exception e);
    }
    public void getStopNameById(String stopId, StopCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("stops")
                .document(stopId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Stop stop = documentSnapshot.toObject(Stop.class);
                        if (stop != null) {
                            callback.onSuccess(stop.getName());
                        } else {
                            callback.onError(new Exception("Stop data corrupted"));
                        }
                    } else {
                        callback.onError(new Exception("Stop not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}