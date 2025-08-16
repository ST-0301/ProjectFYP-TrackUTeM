package com.example.trackutem.model;

import com.google.firebase.database.PropertyName;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class Schedule {
    private String scheduleId;
//    private String day;
//    private String date;
    private Date scheduledDatetime;
    private String type;
//    private String time;
    private String busDriverPairId;
    private String routeId;
    private String driverId;
    private String busId;
    private String status; // "scheduled", "in_progress", "completed"
//    private long actTripStartTime;
//    private long tripEndTime;
    private int queueOpenMinutes;
    private int queueCloseMinutes;
    private boolean queueEnabled;
    private int currentRPointIndex = -1;
    @PropertyName("rpoints")
    private List<RPointDetail> rpoints;

    public Schedule() {
        this.status = "scheduled";
    }
    public static class RPointDetail {
        @PropertyName("rpointId")
        private String rpointId;
        private String planTime;
        private Long actTime;
        private int latenessMinutes;
        private String status; // "scheduled", "departed", "arrived"
        private List<String> queuedStudents;

        public RPointDetail() {}

        @PropertyName("rpointId")
        public String getRPointId() { return rpointId; }
        @PropertyName("rpointId")
        public void setRPointId(String rpointId) { this.rpointId = rpointId; }
        @PropertyName("planTime")
        public String getPlanTime() { return planTime; }
        @PropertyName("planTime")
        public void setPlanTime(String planTime) { this.planTime = planTime; }
        @PropertyName("actTime")
        public Long getActTime() { return actTime; }
        @PropertyName("actTime")
        public void setActTime(Long actTime) { this.actTime = actTime; }
        @PropertyName("latenessMinutes")
        public int getLatenessMinutes() { return latenessMinutes; }
        @PropertyName("latenessMinutes")
        public void setLatenessMinutes(int latenessMinutes) { this.latenessMinutes = latenessMinutes; }
        @PropertyName("status")
        public String getStatus() { return status; }
        @PropertyName("status")
        public void setStatus(String status) { this.status = status; }
        @PropertyName("queuedStudents")
        public List<String> getQueuedStudents() { return queuedStudents; }
        @PropertyName("queuedStudents")
        public void setQueuedStudents(List<String> queuedStudents) { this.queuedStudents = queuedStudents; }
    }
    @PropertyName("scheduleId")
    public String getScheduleId() { return scheduleId; }
    @PropertyName("scheduleId")
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }
    @PropertyName("scheduledDatetime")
    public Date getScheduledDatetime() { return scheduledDatetime; }
    @PropertyName("scheduledDatetime")
    public void setScheduledDatetime(Date scheduledDatetime) { this.scheduledDatetime = scheduledDatetime; }
    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }
    @PropertyName("busDriverPairId")
    public String getBusDriverPairId() { return busDriverPairId; }
    @PropertyName("busDriverPairId")
    public void setBusDriverPairId(String busDriverPairId) { this.busDriverPairId = busDriverPairId; }
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
//    @PropertyName("tripStartTime")
//    public long getTripStartTime() { return tripStartTime; }
//    @PropertyName("tripStartTime")
//    public void setTripStartTime(long tripStartTime) { this.tripStartTime = tripStartTime; }
    @PropertyName("queueOpenMinutes")
    public int getQueueOpenMinutes() { return queueOpenMinutes; }
    @PropertyName("queueOpenMinutes")
    public void setQueueOpenMinutes(int queueOpenMinutes) { this.queueOpenMinutes = queueOpenMinutes; }
    @PropertyName("queueCloseMinutes")
    public int getQueueCloseMinutes() { return queueCloseMinutes; }
    @PropertyName("queueCloseMinutes")
    public void setQueueCloseMinutes(int queueCloseMinutes) { this.queueCloseMinutes = queueCloseMinutes; }
    @PropertyName("queueEnabled")
    public boolean isQueueEnabled() { return queueEnabled; }
    @PropertyName("queueEnabled")
    public void setQueueEnabled(boolean queueEnabled) { this.queueEnabled = queueEnabled; }
    @PropertyName("currentRPointIndex")
    public int getCurrentRPointIndex() { return currentRPointIndex; }
    @PropertyName("currentRPointIndex")
    public void setCurrentRPointIndex(int currentRPointIndex) { this.currentRPointIndex = currentRPointIndex; }
    @PropertyName("rpoints")
    public List<RPointDetail> getRPoints() { return rpoints; }
    @PropertyName("rpoints")
    public void setRPoints(List<RPointDetail> rpoints) { this.rpoints = rpoints; }

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
    public void addStuToQueue(String rpointId, String studentId) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(this.scheduleId) // Use this.scheduleId
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Schedule schedule = documentSnapshot.toObject(Schedule.class);
                    if (schedule != null && schedule.getRPoints() != null) {
                        for (RPointDetail rPointDetail : schedule.getRPoints()) {
                            if (rPointDetail.getRPointId().equals(rpointId)) {
                                if (rPointDetail.getQueuedStudents() == null) {
                                    rPointDetail.setQueuedStudents(new ArrayList<>());
                                }
                                if (!rPointDetail.getQueuedStudents().contains(studentId)) {
                                    rPointDetail.getQueuedStudents().add(studentId);
                                    // Update the entire schedule document
                                    FirebaseFirestore.getInstance().collection("schedules").document(this.scheduleId)
                                            .set(schedule)
                                            .addOnSuccessListener(aVoid -> System.out.println("Student added to queue successfully!"))
                                            .addOnFailureListener(e -> System.err.println("Error adding student to queue: " + e.getMessage()));
                                } else {
                                    System.out.println("Student already in queue for this rpoint.");
                                }
                                return;
                            }
                        }
                        System.err.println("Error: RPoint with ID " + rpointId + " not found in schedule.");
                    } else {
                        System.err.println("Error: Schedule or rpoints list is null.");
                    }
                })
                .addOnFailureListener(e -> System.err.println("Error fetching schedule for adding student to queue: " + e.getMessage()));
    }
    public void removeStuFromQueue(String rpointId, String studentId) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(this.scheduleId) // Use this.scheduleId
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Schedule schedule = documentSnapshot.toObject(Schedule.class);
                    if (schedule != null && schedule.getRPoints() != null) {
                        for (RPointDetail rPointDetail : schedule.getRPoints()) {
                            if (rPointDetail.getRPointId().equals(rpointId)) {
                                if (rPointDetail.getQueuedStudents() != null && rPointDetail.getQueuedStudents().contains(studentId)) {
                                    rPointDetail.getQueuedStudents().remove(studentId);
                                    // Update the entire schedule document
                                    FirebaseFirestore.getInstance().collection("schedules").document(this.scheduleId)
                                            .set(schedule)
                                            .addOnSuccessListener(aVoid -> System.out.println("Student removed from queue successfully!"))
                                            .addOnFailureListener(e -> System.err.println("Error removing student from queue: " + e.getMessage()));
                                } else {
                                    System.out.println("Student not found in queue for this rpoint.");
                                }
                                return;
                            }
                        }
                        System.err.println("Error: RPoint with ID " + rpointId + " not found in schedule.");
                    } else {
                        System.err.println("Error: Schedule or rpoints list is null.");
                    }
                })
                .addOnFailureListener(e -> System.err.println("Error fetching schedule for removing student from queue: " + e.getMessage()));
    }
}