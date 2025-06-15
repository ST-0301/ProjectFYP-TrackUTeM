// RoutePoint.java
package com.example.trackutem.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;

public class RoutePoint {
    private String rpointId;
    private String name;
    private GeoPoint location;

    public RoutePoint() {}

    @PropertyName("rpointId")
    public String getRPointId() { return rpointId; }
    @PropertyName("rpointId")
    public void setRPointId(String rpointId) { this.rpointId = rpointId; }
    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }
    @PropertyName("location")
    public GeoPoint getLocation() { return location; }
    @PropertyName("location")
    public void setLocation(GeoPoint location) { this.location = location; }

    // Interfaces
    public interface RPointLocationCallback {
        void onSuccess(LatLng location);
        void onError(Exception e);
    }
    public interface RPointCallback {
        void onSuccess(String rpointName);
        void onError(Exception e);
    }

    // Firebase Operations
    public void getRPointLocationById(String rpointId, RPointLocationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("routePoints")
                .document(rpointId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        RoutePoint rpoint = documentSnapshot.toObject(RoutePoint.class);
                        if (rpoint != null && rpoint.getLocation() != null) {
                            GeoPoint geoPoint = rpoint.getLocation();
                            callback.onSuccess(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
                        } else {
                            callback.onError(new Exception("Route Point location not found"));
                        }
                    } else {
                        callback.onError(new Exception("Route Point not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    public void getRPointNameById(String rpointId, RPointCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("routePoints")
                .document(rpointId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        RoutePoint rpoint = documentSnapshot.toObject(RoutePoint.class);
                        if (rpoint != null) {
                            callback.onSuccess(rpoint.getName());
                        } else {
                            callback.onError(new Exception("Route Point data corrupted"));
                        }
                    } else {
                        callback.onError(new Exception("Route Point not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}