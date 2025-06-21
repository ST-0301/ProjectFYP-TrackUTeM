package com.example.trackutem.model;

import com.google.firebase.database.PropertyName;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class Schedule {
    private String scheduleId;
    private String day;
    private String date;
    private String type;
    private String time;
    private String routeId;
    private String driverId;
    private String busId;
    private String status; // "scheduled", "in_progress", "completed"
    @PropertyName("rpoints")
    private List<RPointDetail> rpoints;
    private long tripStartTime;
    private long tripEndTime;
    private int currentRPointIndex = -1;

    public Schedule() {
        this.day = "";
        this.status = "scheduled";
    }
    public static class RPointDetail {
        @PropertyName("rpointId")
        private String rpointId;
        private String expArrTime;
        private String expDepTime;
        private Long actArrTime;
        private Long actDepTime;
        private int latenessMinutes;
        private String status; // "scheduled", "departed", "arrived"

        public RPointDetail() {}

        @PropertyName("rpointId")
        public String getRPointId() { return rpointId; }
        @PropertyName("rpointId")
        public void setRPointId(String rpointId) { this.rpointId = rpointId; }
        @PropertyName("expArrTime")
        public String getExpArrTime() { return expArrTime; }
        @PropertyName("expArrTime")
        public void setExpArrTime(String expArrTime) { this.expArrTime = expArrTime; }
        @PropertyName("expDepTime")
        public String getExpDepTime() { return expDepTime; }
        @PropertyName("expDepTime")
        public void setExpDepTime(String expDepTime) { this.expDepTime = expDepTime; }
        @PropertyName("actArrTime")
        public Long getActArrTime() { return actArrTime; }
        @PropertyName("actArrTime")
        public void setActArrTime(Long actArrTime) { this.actArrTime = actArrTime; }
        @PropertyName("actDepTime")
        public Long getActDepTime() { return actDepTime; }
        @PropertyName("actDepTime")
        public void setActDepTime(Long actDepTime) { this.actDepTime = actDepTime; }
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
    @PropertyName("date")
    public String getDate() { return date; }
    @PropertyName("date")
    public void setDate(String date) { this.date = date; }
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
    @PropertyName("rpoints")
    public List<RPointDetail> getRPoints() { return rpoints; }
    @PropertyName("rpoints")
    public void setRPoints(List<RPointDetail> rpoints) { this.rpoints = rpoints; }
    @PropertyName("tripStartTime")
    public long getTripStartTime() { return tripStartTime; }
    @PropertyName("tripStartTime")
    public void setTripStartTime(long tripStartTime) { this.tripStartTime = tripStartTime; }
    @PropertyName("tripEndTime")
    public long getTripEndTime() { return tripEndTime; }
    @PropertyName("tripEndTime")
    public void setTripEndTime(long tripEndTime) { this.tripEndTime = tripEndTime; }
    @PropertyName("currentRPointIndex")
    public int getCurrentRPointIndex() { return currentRPointIndex; }
    @PropertyName("currentRPointIndex")
    public void setCurrentRPointIndex(int currentRPointIndex) { this.currentRPointIndex = currentRPointIndex; }

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
    
    public static void getAllSchedules(OnSchedulesRetrieved listener) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Schedule> schedules = querySnapshot.toObjects(Schedule.class);
                    listener.onSuccess(schedules);
                })
                .addOnFailureListener(listener::onError);
    }
}