package com.example.trackutem.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Schedule {
    private String scheduleId;
    private Date scheduledDatetime;
    private String type;
    private String busDriverPairId;
    private String routeId;
    private String status;
    private int queueOpenMinutes;
    private int queueCloseMinutes;
    private boolean queueEnabled;
    private int currentRPointIndex = -1;
    @PropertyName("rpoints")
    private List<RPointDetail> rpoints;

    private String preloadedRouteName;
    private String preloadedBusPlate;

    public Schedule() {
        this.status = "scheduled";
    }
    public static class RPointDetail {
        private String rpointId;
        private String planTime;
        private Long actTime;
        private int latenessMinutes;
        private String status;
        private List<String> queuedStudents = new ArrayList<>();

        public RPointDetail() {}

        public String getRpointId() { return rpointId; }
        public void setRpointId(String rpointId) { this.rpointId = rpointId; }

        public String getPlanTime() { return planTime; }
        public void setPlanTime(String planTime) { this.planTime = planTime; }

        public Long getActTime() { return actTime; }
        public void setActTime(Long actTime) { this.actTime = actTime; }

        public int getLatenessMinutes() { return latenessMinutes; }
        public void setLatenessMinutes(int latenessMinutes) { this.latenessMinutes = latenessMinutes; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<String> getQueuedStudents() {
            if (queuedStudents == null) {
                queuedStudents = new ArrayList<>();
            }
            return queuedStudents;
        }
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
    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }
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

    @Exclude
    public String getPreloadedRouteName() { return preloadedRouteName; }
    public void setPreloadedRouteName(String preloadedRouteName) { this.preloadedRouteName = preloadedRouteName; }
    @Exclude
    public String getPreloadedBusPlate() { return preloadedBusPlate; }
    public void setPreloadedBusPlate(String preloadedBusPlate) { this.preloadedBusPlate = preloadedBusPlate; }

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
    public void updateRPointFields(int rpointIndex, Map<String, Object> updates) {
        String basePath = "rpoints." + rpointIndex;
        Map<String, Object> updateData = new HashMap<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            updateData.put(basePath + "." + entry.getKey(), entry.getValue());
        }

        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(scheduleId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> System.out.println("RPoint updated successfully!"))
                .addOnFailureListener(e -> System.err.println("Error updating RPoint: " + e.getMessage()));
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
// In: com/example/trackutem/model/Schedule.java

    /**
     * Updates the status, lateness, and actual arrival time for a specific route point
     * and persists the changes to Firestore using dot notation for efficiency.
     *
     * @param rpointId The ID of the route point to update.
     * @param latenessMinutes The calculated lateness in minutes.
     * @param status The new status (e.g., "arrived", "departed").
     * @param actTime The actual arrival time in milliseconds (can be null).
     */
    public void updateRPointStatus(String rpointId, int latenessMinutes, String status, Long actTime) {
        if (rpoints == null || scheduleId == null) {
            System.err.println("Schedule data is incomplete. Cannot update RPoint.");
            return;
        }

        // Find the index of the route point to update
        int rpointIndex = -1;
        for (int i = 0; i < rpoints.size(); i++) {
            if (rpoints.get(i).getRpointId().equals(rpointId)) {
                rpointIndex = i;
                break;
            }
        }

        if (rpointIndex != -1) {
            // Update the local object first
            RPointDetail rpointToUpdate = rpoints.get(rpointIndex);
            rpointToUpdate.setLatenessMinutes(latenessMinutes);
            rpointToUpdate.setStatus(status);
            rpointToUpdate.setActTime(actTime);

            // Prepare the update map for Firestore using dot notation
            // This updates only the specific fields in the array element
            String basePath = "rpoints." + rpointIndex;
            Map<String, Object> updates = new HashMap<>();
            updates.put(basePath + ".latenessMinutes", latenessMinutes);
            updates.put(basePath + ".status", status);
            updates.put(basePath + ".actTime", actTime);

            // Update the document in Firestore
            FirebaseFirestore.getInstance()
                    .collection("schedules")
                    .document(this.scheduleId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> System.out.println("RPoint " + rpointId + " updated successfully!"))
                    .addOnFailureListener(e -> System.err.println("Error updating RPoint " + rpointId + ": " + e.getMessage()));
        } else {
            System.err.println("Error: RPoint with ID " + rpointId + " not found in schedule.");
        }
    }
    public void addStuToQueue(String rpointId, String studentId, Runnable onSuccess, Consumer<Exception> onFailure) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(this.scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Schedule schedule = documentSnapshot.toObject(Schedule.class);
                    if (schedule != null && schedule.getRPoints() != null) {
                        for (RPointDetail rPointDetail : schedule.getRPoints()) {
                            if (rPointDetail.getRpointId().equals(rpointId)) {
                                if (rPointDetail.getQueuedStudents() == null) {
                                    rPointDetail.setQueuedStudents(new ArrayList<>());
                                }
                                if (!rPointDetail.getQueuedStudents().contains(studentId)) {
                                    rPointDetail.getQueuedStudents().add(studentId);
                                    Map<String, Object> updateData = new HashMap<>();
                                    updateData.put("rpoints", schedule.getRPoints());

                                    FirebaseFirestore.getInstance().collection("schedules").document(this.scheduleId)
                                            .update(updateData)
                                            .addOnSuccessListener(aVoid -> onSuccess.run())
                                            .addOnFailureListener(onFailure::accept);
                                } else {
                                    onSuccess.run();
                                }
                                return;
                            }
                        }
                        onFailure.accept(new Exception("RPoint with ID " + rpointId + " not found in schedule."));
                    } else {
                        onFailure.accept(new Exception("Schedule or rpoints list is null."));
                    }
                })
                .addOnFailureListener(onFailure::accept);
    }

    public void removeStuFromQueue(String rpointId, String studentId) {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(this.scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Schedule schedule = documentSnapshot.toObject(Schedule.class);
                    if (schedule != null && schedule.getRPoints() != null) {
                        for (RPointDetail rPointDetail : schedule.getRPoints()) {
                            if (rPointDetail.getRpointId().equals(rpointId)) {
                                if (rPointDetail.getQueuedStudents() != null && rPointDetail.getQueuedStudents().contains(studentId)) {
                                    rPointDetail.getQueuedStudents().remove(studentId);
                                    Map<String, Object> updateData = new HashMap<>();
                                    updateData.put("rpoints", schedule.getRPoints());

                                    FirebaseFirestore.getInstance().collection("schedules").document(this.scheduleId)
                                            .update(updateData)
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