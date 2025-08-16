// ScheduleController.java
package com.example.trackutem.controller;

import com.example.trackutem.model.Schedule;
import com.example.trackutem.model.Schedule.RPointDetail;
import java.util.Calendar;

public class ScheduleController {
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

    public static void recordArrivalAndNextDeparture(Schedule schedule, int rpointIndex) {
        if (schedule == null) return;

        // If event type, only update status if completed
        if ("event".equalsIgnoreCase(schedule.getType())) {
            // Mark as completed and set tripEndTime if last point
            if (rpointIndex == schedule.getRPoints().size() - 1) {
                schedule.setStatus("completed");
                schedule.setCurrentRPointIndex(-1);
                schedule.updateInFirestore();
            }
            return;
        }
        if (rpointIndex < 0 || rpointIndex >= schedule.getRPoints().size()) return;

        // Record current route point arrival
        RPointDetail currentRPoint = schedule.getRPoints().get(rpointIndex);
        long arrivalTime = System.currentTimeMillis();
        currentRPoint.setActTime(arrivalTime);
        currentRPoint.setStatus("arrived");

        // Calculate lateness
        int lateness = calculateLateness(currentRPoint.getPlanTime(), arrivalTime, schedule.getScheduledDatetime().getTime());
        if (lateness > 0) {
            currentRPoint.setLatenessMinutes(lateness);
        } else {
            currentRPoint.setLatenessMinutes(0);
        }

        // Record next route point departure if exists
        int nextIndex = rpointIndex + 1;
        if (nextIndex < schedule.getRPoints().size()) {
            RPointDetail nextRPoint = schedule.getRPoints().get(nextIndex);
            nextRPoint.setActTime(System.currentTimeMillis());
            nextRPoint.setStatus("departed");
            schedule.setCurrentRPointIndex(nextIndex);
        } else {
            schedule.setCurrentRPointIndex(-1);
            schedule.setStatus("completed");
        }
        schedule.updateInFirestore();
    }
}