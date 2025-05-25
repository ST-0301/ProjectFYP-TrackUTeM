package com.example.trackutem.model;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class Route {
    private String routeId;
    private String name;
    private List<String> stops;
    private String created;

    // Firestore requires empty constructor
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

//    public static class DaySchedule {
//        private List<ScheduleEntry> incampus;
//        private List<ScheduleEntry> outcampus;
//
//        public DaySchedule() {}
//
//        @PropertyName("incampus")
//        public List<ScheduleEntry> getIncampus() { return incampus; }
//
//        @PropertyName("incampus")
//        public void setIncampus(List<ScheduleEntry> incampus) { this.incampus = incampus; }
//
//        @PropertyName("outcampus")
//        public List<ScheduleEntry> getOutcampus() { return outcampus; }
//
//        @PropertyName("outcampus")
//        public void setOutcampus(List<ScheduleEntry> outcampus) { this.outcampus = outcampus; }
//    }

//    public static class ScheduleEntry {
//        private String time;
//        private List<Assignment> assignments;
//
//        @PropertyName("time")
//        public String getTime() { return time; }
//
//        @PropertyName("time")
//        public void setTime(String time) { this.time = time; }
//
//        @PropertyName("assignments")
//        public List<Assignment> getAssignments() { return assignments; }
//
//        @PropertyName("assignments")
//        public void setAssignments(List<Assignment> assignments) { this.assignments = assignments; }
//    }

//    public static class Assignment {
//        private String busId;
//        private String driverId;
//
//        public Assignment() {}
//
//        @PropertyName("bus")
//        public String getBusId() { return busId; }
//
//        @PropertyName("bus")
//        public void setBusId(String busId) { this.busId = busId; }
//
//        @PropertyName("driver")
//        public String getDriverId() { return driverId; }
//
//        @PropertyName("driver")
//        public void setDriverId(String driverId) { this.driverId = driverId; }
//    }

    public interface OnStopsResolvedListener {
        void onStopsResolved(List<String> stopNames);
        void onError(Exception e);
    }
    public static void getRouteStops(String routeId, OnStopsResolvedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("routes").document(routeId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Route route = documentSnapshot.toObject(Route.class);
                if (route != null && route.getStops() != null) {
                    Stop stopResolver = new Stop();
                    resolveStopNames(route.getStops(), stopResolver, new StopNamesCallback() {
                        @Override
                        public void onSuccess(List<String> stopNames) {
                            listener.onStopsResolved(stopNames);
                            Log.e("RouteActivity", "Stops retrieved: " + stopNames);
                        }
                        @Override
                        public void onError(Exception e) {
                            listener.onError(e);
                            Log.e("RouteActivity", "Error fetching stops: ", e);
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

    public interface StopNamesCallback {
        void onSuccess(List<String> stopNames);
        void onError(Exception e);
    }
    public interface RouteNameCallback {
        void onSuccess(String routeName);
        void onError(Exception e);
    }

    public static void resolveRouteName(String routeId, Route.RouteNameCallback callback) {
        FirebaseFirestore.getInstance().collection("routes").document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Route route = documentSnapshot.toObject(Route.class);
                        callback.onSuccess(route.getName());
                    } else {
                        callback.onError(new Exception("Route not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }
    // Utility method to resolve stop IDs to names
    public static void resolveStopNames(List<String> stopIds, Stop stop, StopNamesCallback callback) {
        List<String> stopNames = new ArrayList<>();
        int[] completedCount = {0};

        if (stopIds == null || stopIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        for (String stopId : stopIds) {
            stop.getStopNameById(stopId, new Stop.StopCallback() {
                @Override
                public void onSuccess(String stopName) {
                    synchronized (stopNames) {
                        stopNames.add(stopName);
                        completedCount[0]++;
                        if (completedCount[0] == stopIds.size()) {
                            callback.onSuccess(stopNames);
                        }
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