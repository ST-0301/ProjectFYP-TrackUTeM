// Stop.java
package com.example.trackutem.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;

public class Stop {
    private String stopId;
    private String name;
    private GeoPoint location;

    public Stop() {}

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

    // Interfaces
    public interface StopLocationCallback {
        void onSuccess(LatLng location);
        void onError(Exception e);
    }
    public interface StopCallback {
        void onSuccess(String stopName);
        void onError(Exception e);
    }

    // Firebase Operations
    public void getStopLocationById(String stopId, StopLocationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("stops")
                .document(stopId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Stop stop = documentSnapshot.toObject(Stop.class);
                        if (stop != null && stop.getLocation() != null) {
                            GeoPoint geoPoint = stop.getLocation();
                            callback.onSuccess(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
                        } else {
                            callback.onError(new Exception("Stop location not found"));
                        }
                    } else {
                        callback.onError(new Exception("Stop not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
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