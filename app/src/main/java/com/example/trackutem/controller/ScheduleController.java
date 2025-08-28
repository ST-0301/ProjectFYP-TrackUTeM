// ScheduleController.java
package com.example.trackutem.controller;

import com.example.trackutem.model.Schedule;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;
import java.util.Date;

public class ScheduleController {
    public void isStudentQueuedInGroup(String studentId, String routeId, String type, Date scheduledDatetime, QueueCheckCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Calendar cal = Calendar.getInstance();
        cal.setTime(scheduledDatetime);
        cal.add(Calendar.MINUTE, -30);
        Date startTime = cal.getTime();

        cal.setTime(scheduledDatetime);
        cal.add(Calendar.MINUTE, 30);
        Date endTime = cal.getTime();

        db.collection("schedules")
                .whereEqualTo("routeId", routeId)
                .whereEqualTo("type", type)
                .whereGreaterThanOrEqualTo("scheduledDatetime", startTime)
                .whereLessThanOrEqualTo("scheduledDatetime", endTime)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Schedule schedule = document.toObject(Schedule.class);
                        if (schedule.getRPoints() != null) {
                            for (Schedule.RPointDetail rpoint : schedule.getRPoints()) {
                                if (rpoint.getQueuedStudents() != null && rpoint.getQueuedStudents().contains(studentId)) {
                                    callback.onResult(true, "You are already queued for another schedule in this route group.");
                                    return;
                                }
                            }
                        }
                    }
                    callback.onResult(false, null);
                })
                .addOnFailureListener(e -> callback.onResult(false, "Error checking queue status: " + e.getMessage()));
    }

    public static int calculateLateness(String planTime, long actualTime, long tripStartTime) {
        try {
            // Create calendar with trip start date
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(tripStartTime);

            // Parse expected time (HH:mm)
            String[] parts = planTime.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);

            // Set expected time on trip day
            Calendar expectedCalendar = (Calendar) calendar.clone();
            expectedCalendar.set(Calendar.HOUR_OF_DAY, hours);
            expectedCalendar.set(Calendar.MINUTE, minutes);
            expectedCalendar.set(Calendar.SECOND, 0);
            expectedCalendar.set(Calendar.MILLISECOND, 0);

            // Calculate difference in minutes
            long diffMs = actualTime - expectedCalendar.getTimeInMillis();
            return (int) (diffMs / (60 * 1000));
        } catch (Exception e) {
            return 0;
        }
    }

    public interface QueueCheckCallback {
        void onResult(boolean isQueued, String message);
    }
}