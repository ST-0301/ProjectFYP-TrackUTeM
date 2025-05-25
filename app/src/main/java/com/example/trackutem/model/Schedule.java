package com.example.trackutem.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class Schedule {
    private String scheduleId;
    private String day;
    private String type;
    private String time;
    private String routeId;
    private String driverId;
    private String busId;
    private String status;

    private String routeName;

    public Schedule() {}

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


    @Exclude
    public String getRouteName() {
        return routeName;
    }

    @Exclude
    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }



    public interface OnSchedulesRetrieved {
        void onSuccess(List<Schedule> schedules);
        void onError(Exception e);
    }

//    public interface RouteNameCallback {
//        void onSuccess(String routeName);
//        void onError(Exception e);
//    }
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