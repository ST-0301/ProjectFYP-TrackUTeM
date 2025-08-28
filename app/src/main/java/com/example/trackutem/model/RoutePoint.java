// RoutePoint.java
package com.example.trackutem.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RoutePoint {
    private String rpointId;
    private String name;
    private GeoPoint coordinates;
    private String type;

    public RoutePoint() {}

    @PropertyName("rpointId")
    public String getRPointId() { return rpointId; }
    @PropertyName("rpointId")
    public void setRPointId(String rpointId) { this.rpointId = rpointId; }
    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }
    @PropertyName("coordinates")
    public GeoPoint getCoordinates() { return coordinates; }
    @PropertyName("coordinates")
    public void setCoordinates(GeoPoint coordinates) { this.coordinates = coordinates; }
    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    // Interfaces
    public interface RPointLocationCallback {
        void onSuccess(LatLng coordinates);
        void onError(Exception e);
    }
    public interface RPointCallback {
        void onSuccess(String rpointName);
        void onError(Exception e);
    }
    public interface AllRPointsCallback {
        void onSuccess(List<RoutePoint> rpoints);
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
                        if (rpoint != null && rpoint.getCoordinates() != null) {
                            GeoPoint geoPoint = rpoint.getCoordinates();
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
        if (rpointId == null) {
            callback.onError(new Exception("Route Point ID is null"));
            return;
        }
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
    public static void getAllRPoints(AllRPointsCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("routePoints")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RoutePoint> rpoints = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        RoutePoint rpoint = document.toObject(RoutePoint.class);
                        rpoint.setRPointId(document.getId());
                        rpoints.add(rpoint);
                    }
                    callback.onSuccess(rpoints);
                })
                .addOnFailureListener(callback::onError);
    }
}