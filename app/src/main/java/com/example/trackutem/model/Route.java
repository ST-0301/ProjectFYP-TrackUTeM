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
    private List<String> stops;
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
    @PropertyName("stops")
    public List<String> getStops() { return stops; }
    @PropertyName("stops")
    public void setStops(List<String> stops) { this.stops = stops; }
    @PropertyName("created")
    public String getCreated() { return created; }
    @PropertyName("created")
    public void setCreated(String created) { this.created = created; }

    // Interfaces
    public interface RouteNameCallback {
        void onSuccess(String routeName);
        void onError(Exception e);
    }
    public interface StopNamesCallback {
        void onSuccess(List<String> stopNames);
        void onError(Exception e);
    }
    interface StopLocationsCallback {
        void onSuccess(List<LatLng> locations);
        void onError(Exception e);
    }
    public interface OnStopsResolvedListener {
        void onStopsResolved(List<String> stopNames);
        void onError(Exception e);
    }
    public interface OnStopsLocationResolvedListener {
        void onStopsLocationResolved(List<LatLng> locations);
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
    public static void getRouteStops(String routeId, OnStopsResolvedListener listener) {
        FirebaseFirestore.getInstance().collection("routes").document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Route route = documentSnapshot.toObject(Route.class);
                        if (route != null && route.getStops() != null) {
                            Stop stopResolver = new Stop();
                            resolveStopNames(route.getStops(), stopResolver, new StopNamesCallback() {
                                @Override
                                public void onSuccess(List<String> stopNames) {
                                    listener.onStopsResolved(stopNames);
                                }
                                @Override
                                public void onError(Exception e) {
                                    listener.onError(e);
                                }
                            });
                        } else {
                            listener.onError(new Exception("Route or stops not found"));
                        }
                    } else {
                        listener.onError(new Exception("Route document not found"));
                    }
                }).addOnFailureListener(listener::onError);
    }
    public static void getRouteStopLocations(String routeId, OnStopsLocationResolvedListener listener) {
        FirebaseFirestore.getInstance().collection("routes").document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Route route = documentSnapshot.toObject(Route.class);
                        if (route != null && route.getStops() != null) {
                            List<String> stopIds = route.getStops();
                            resolveStopLocations(stopIds, new StopLocationsCallback() {
                                @Override
                                public void onSuccess(List<LatLng> locations) {
                                    listener.onStopsLocationResolved(locations);
                                }
                                @Override
                                public void onError(Exception e) {
                                    listener.onError(e);
                                }
                            });
                        } else {
                            listener.onError(new Exception("Route or stops not found"));
                        }
                    } else {
                        listener.onError(new Exception("Route document not found"));
                    }
                }).addOnFailureListener(listener::onError);
    }

    // Helper Methods
    private static void resolveStopLocations(List<String> stopIds, StopLocationsCallback callback) {
        if (stopIds == null || stopIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        final List<LatLng> locations = new ArrayList<>();
        final AtomicInteger count = new AtomicInteger(0);
        final Stop stopResolver = new Stop();

        for (String stopId : stopIds) {
            stopResolver.getStopLocationById(stopId, new Stop.StopLocationCallback() {
                @Override
                public void onSuccess(LatLng location) {
                    locations.add(location);
                    if (count.incrementAndGet() == stopIds.size()) {
                        callback.onSuccess(locations);
                    }
                }
                @Override
                public void onError(Exception e) {
                    Log.e("Route", "Error resolving location for stopId: " + stopId + ", Error: " + e.getMessage());

                    if (count.incrementAndGet() == stopIds.size()) {
                        callback.onSuccess(locations); // Return partial results
                    }
                }
            });
        }
    }
    public static void resolveStopNames(List<String> stopIds, Stop stop, StopNamesCallback callback) {
        if (stopIds == null || stopIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        final String[] orderedStopNames = new String[stopIds.size()];
        final AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 0; i < stopIds.size(); i++) {
            final int currentIndex = i;
            String stopId = stopIds.get(i);
            stop.getStopNameById(stopId, new Stop.StopCallback() {
                @Override
                public void onSuccess(String stopName) {
                    orderedStopNames[currentIndex] = stopName;
                    if (completedCount.incrementAndGet() == stopIds.size()) {
                        callback.onSuccess(Arrays.asList(orderedStopNames));
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