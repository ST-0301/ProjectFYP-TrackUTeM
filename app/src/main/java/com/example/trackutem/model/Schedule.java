package com.example.trackutem.model;

import com.google.firebase.database.PropertyName;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class Schedule {
    private String scheduleId;
    private String day;
    private String type;
    private String time;
    private String routeId;
    private String driverId;
    private String busId;
    private String status; // "scheduled", "in_progress", "completed"
    private List<StopDetail> stops;
    private long tripStartTime;
    private long tripEndTime;
    private int currentStopIndex = -1;

    public Schedule() {}
    public static class StopDetail {
        private String stopId;
        private String expectedArrivalTime;
        private String expectedDepartureTime;
        private Long actualArrivalTime;
        private Long actualDepartureTime;
        private int latenessMinutes;
        private String status; // "scheduled", "departed", "arrived"

        public StopDetail() {}
        public StopDetail(String stopId, String expectedArrivalTime, String expectedDepartureTime) {
            this.stopId = stopId;
            this.expectedArrivalTime = expectedArrivalTime;
            this.expectedDepartureTime = expectedDepartureTime;
            this.status = "scheduled";
        }

        @PropertyName("stopId")
        public String getStopId() { return stopId; }
        @PropertyName("stopId")
        public void setStopId(String stopId) { this.stopId = stopId; }
        @PropertyName("expectedArrivalTime")
        public String getExpectedArrivalTime() { return expectedArrivalTime; }
        @PropertyName("expectedArrivalTime")
        public void setExpectedArrivalTime(String expectedArrivalTime) { this.expectedArrivalTime = expectedArrivalTime; }
        @PropertyName("expectedDepartureTime")
        public String getExpectedDepartureTime() { return expectedDepartureTime; }
        @PropertyName("expectedDepartureTime")
        public void setExpectedDepartureTime(String expectedDepartureTime) { this.expectedDepartureTime = expectedDepartureTime; }
        @PropertyName("actualArrivalTime")
        public Long getActualArrivalTime() { return actualArrivalTime; }
        @PropertyName("actualArrivalTime")
        public void setActualArrivalTime(Long actualArrivalTime) { this.actualArrivalTime = actualArrivalTime; }
        @PropertyName("actualDepartureTime")
        public Long getActualDepartureTime() { return actualDepartureTime; }
        @PropertyName("actualDepartureTime")
        public void setActualDepartureTime(Long actualDepartureTime) { this.actualDepartureTime = actualDepartureTime; }
        @PropertyName("latenessMinutes")
        public int getLatenessMinutes() { return latenessMinutes; }
        @PropertyName("latenessMinutes")
        public void setLatenessMinutes(int latenessMinutes) { this.latenessMinutes = latenessMinutes; }
        @PropertyName("status")
        public String getStatus() { return status; }
        @PropertyName("status")
        public void setStatus(String status) { this.status = status; }
    }
    @PropertyName("scheduleId")
    public String getScheduleId() { return scheduleId; }
    @PropertyName("scheduleId")
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    @PropertyName("day")
    public String getDay() { return day; }
    @PropertyName("day")
    public void setDay(String day) { this.day = day.toLowerCase(Locale.ENGLISH); }
    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }
    @PropertyName("time")
    public String getTime() { return time; }
    @PropertyName("time")
    public void setTime(String time) { this.time = time; }
    @PropertyName("routeId")
    public String getRouteId() { return routeId; }
    @PropertyName("routeId")
    public void setRouteId(String routeId) { this.routeId = routeId; }
    @PropertyName("driverId")
    public String getDriverId() { return driverId; }
    @PropertyName("driverId")
    public void setDriverId(String driverId) { this.driverId = driverId; }
    @PropertyName("busId")
    public String getBusId() { return busId; }
    @PropertyName("busId")
    public void setBusId(String busId) { this.busId = busId; }
    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }
    @PropertyName("stops")
    public List<StopDetail> getStops() { return stops; }
    public void setStops(List<StopDetail> stops) { this.stops = stops; }
    @PropertyName("tripStartTime")
    public long getTripStartTime() { return tripStartTime; }
    @PropertyName("tripStartTime")
    public void setTripStartTime(long tripStartTime) { this.tripStartTime = tripStartTime; }
    @PropertyName("tripStartTime")
    public long getTripEndTime() { return tripEndTime; }
    @PropertyName("tripStartTime")
    public void setTripEndTime(long tripEndTime) { this.tripEndTime = tripEndTime; }
    @PropertyName("currentStopIndex")
    public int getCurrentStopIndex() { return currentStopIndex; }
    @PropertyName("currentStopIndex")
    public void setCurrentStopIndex(int currentStopIndex) { this.currentStopIndex = currentStopIndex; }

    // Interface
    public interface OnSchedulesRetrieved {
        void onSuccess(List<Schedule> schedules);
        void onError(Exception e);
    }

    // Firebase Operations
    public void updateInFirestore() {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(scheduleId)
                .set(this);
    }
    public static void getScheduleById(String scheduleId, Consumer<Schedule> callback) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(scheduleId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Schedule schedule = snapshot.toObject(Schedule.class);
                    callback.accept(schedule);
                });
    }
    public static void getSchedulesByDriverId(String driverId, OnSchedulesRetrieved listener) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .whereEqualTo("driverId", driverId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Schedule> schedules = querySnapshot.toObjects(Schedule.class);
                    listener.onSuccess(schedules);
                })
                .addOnFailureListener(listener::onError);
    }
}