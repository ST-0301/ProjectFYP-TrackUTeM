// Route.java
package com.example.trackutem.model;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Route {
    private String routeId;
    private String name;
    private List<String> rpoints;
    private String created;

    public Route() {}

    @PropertyName("routeId")
    public String getRouteId() { return routeId; }
    @PropertyName("routeId")
    public void setRouteId(String routeId) { this.routeId = routeId; }
    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }
    @PropertyName("rpoints")
    public List<String> getRPoints() { return rpoints; }
    @PropertyName("rpoints")
    public void setRPoints(List<String> rpoints) { this.rpoints = rpoints; }
    @PropertyName("created")
    public String getCreated() { return created; }
    @PropertyName("created")
    public void setCreated(String created) { this.created = created; }

    // Interfaces
    public interface RouteNameCallback {
        void onSuccess(String routeName);
        void onError(Exception e);
    }
    public interface RPointNamesCallback {
        void onSuccess(List<String> rpointNames);
        void onError(Exception e);
    }
    interface RPointLocationsCallback {
        void onSuccess(List<LatLng> locations);
        void onError(Exception e);
    }
    public interface OnRPointsResolvedListener {
        void onRPointsResolved(List<String> rpointNames);
        void onError(Exception e);
    }
    public interface OnRPointsLocationResolvedListener {
        void onRPointsLocationResolved(List<LatLng> locations);
        void onError(Exception e);
    }

    // Firebase Operations
    public static void resolveRouteName(String routeId, Route.RouteNameCallback callback) {
        FirebaseFirestore.getInstance().collection("routes").document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Route route = documentSnapshot.toObject(Route.class);
                        if (route != null && route.getName() != null) {
                            callback.onSuccess(route.getName());
                        } else {
                            callback.onError(new Exception("Route data is incomplete"));
                        }
                    } else {
                        callback.onError(new Exception("Route not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    public static void getRouteRPoints(String routeId, OnRPointsResolvedListener listener) {
        FirebaseFirestore.getInstance().collection("routes").document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Route route = documentSnapshot.toObject(Route.class);
                        if (route != null && route.getRPoints() != null) {
                            RoutePoint rpointResolver = new RoutePoint();
                            resolveRPointNames(route.getRPoints(), rpointResolver, new RPointNamesCallback() {
                                @Override
                                public void onSuccess(List<String> rpointNames) {
                                    listener.onRPointsResolved(rpointNames);
                                }
                                @Override
                                public void onError(Exception e) {
                                    listener.onError(e);
                                }
                            });
                        } else {
                            listener.onError(new Exception("Route or route points not found"));
                        }
                    } else {
                        listener.onError(new Exception("Route document not found"));
                    }
                }).addOnFailureListener(listener::onError);
    }
    public static void getRPointLocations(String routeId, OnRPointsLocationResolvedListener listener) {
        FirebaseFirestore.getInstance().collection("routes").document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Route route = documentSnapshot.toObject(Route.class);
                        if (route != null && route.getRPoints() != null) {
                            List<String> rpointIds = route.getRPoints();
                            resolveRPointLocations(rpointIds, new RPointLocationsCallback() {
                                @Override
                                public void onSuccess(List<LatLng> locations) {
                                    listener.onRPointsLocationResolved(locations);
                                }
                                @Override
                                public void onError(Exception e) {
                                    listener.onError(e);
                                }
                            });
                        } else {
                            listener.onError(new Exception("Route or route points not found"));
                        }
                    } else {
                        listener.onError(new Exception("Route document not found"));
                    }
                }).addOnFailureListener(listener::onError);
    }

    // Helper Methods
    private static void resolveRPointLocations(List<String> rpointIds, RPointLocationsCallback callback) {
        if (rpointIds == null || rpointIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        final List<LatLng> locations = new ArrayList<>();
        final AtomicInteger count = new AtomicInteger(0);
        final RoutePoint rpointResolver = new RoutePoint();

        for (String rpointId : rpointIds) {
            rpointResolver.getRPointLocationById(rpointId, new RoutePoint.RPointLocationCallback() {
                @Override
                public void onSuccess(LatLng location) {
                    locations.add(location);
                    if (count.incrementAndGet() == rpointIds.size()) {
                        callback.onSuccess(locations);
                    }
                }
                @Override
                public void onError(Exception e) {
                    Log.e("Route", "Error resolving location for rpointId: " + rpointId + ", Error: " + e.getMessage());

                    if (count.incrementAndGet() == rpointIds.size()) {
                        callback.onSuccess(locations); // Return partial results
                    }
                }
            });
        }
    }
    public static void resolveRPointNames(List<String> rpointIds, RoutePoint rpoint, RPointNamesCallback callback) {
        if (rpointIds == null || rpointIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        final String[] orderedRPointNames = new String[rpointIds.size()];
        final AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < rpointIds.size(); i++) {
            final int currentIndex = i;
            String rpointId = rpointIds.get(i);
            rpoint.getRPointNameById(rpointId, new RoutePoint.RPointCallback() {
                @Override
                public void onSuccess(String rpointName) {
                    orderedRPointNames[currentIndex] = rpointName;
                    if (completedCount.incrementAndGet() == rpointIds.size()) {
                        callback.onSuccess(Arrays.asList(orderedRPointNames));
                    }
                }
                @Override
                public void onError(Exception e) {
                    callback.onError(e);
                }
            });
        }
    }
}