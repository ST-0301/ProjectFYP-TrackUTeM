// ScheduleController.java
package com.example.trackutem.controller;

import com.example.trackutem.model.Schedule;
import com.example.trackutem.model.Schedule.StopDetail;
import java.util.Calendar;

public class ScheduleController {
    public static int calculateLateness(String expectedTime, long actualTime, long tripStartTime) {
        try {
            // Create calendar with trip start date
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(tripStartTime);

            // Parse expected time (HH:mm)
            String[] parts = expectedTime.split(":");
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
    public static void recordArrivalAndNextDeparture(Schedule schedule, int stopIndex) {
        if (stopIndex < 0 || stopIndex >= schedule.getStops().size()) return;

        // Record current stop arrival
        StopDetail currentStop = schedule.getStops().get(stopIndex);
        long arrivalTime = System.currentTimeMillis();
        currentStop.setActualArrivalTime(arrivalTime);
        currentStop.setStatus("arrived");

        // Calculate lateness
        int lateness = calculateLateness(currentStop.getExpectedArrivalTime(), arrivalTime, schedule.getTripStartTime());
        if (lateness > 0) {
            currentStop.setLatenessMinutes(lateness);
        } else {
            currentStop.setLatenessMinutes(0);
        }

        // Record next stop departure if exists
        int nextIndex = stopIndex + 1;
        if (nextIndex < schedule.getStops().size()) {
            StopDetail nextStop = schedule.getStops().get(nextIndex);
            nextStop.setActualDepartureTime(System.currentTimeMillis());
            nextStop.setStatus("departed");
            schedule.setCurrentStopIndex(nextIndex);
        } else {
            schedule.setCurrentStopIndex(-1);
            schedule.setStatus("completed");
            schedule.setTripEndTime(System.currentTimeMillis());
        }
        schedule.updateInFirestore();
    }
}