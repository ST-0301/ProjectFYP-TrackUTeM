package com.example.trackutem.model;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class Driver {
    private String driverId;
    private String name;
    private String email;
    private String phone;
    private String licenseNumber;
    private String status;
    private GeoPoint currentLocation;
    private Timestamp lastUpdate;
    private String currentScheduleId;

    // Required no-argument constructor for Firestore
    public Driver() {}
    public Driver(String driverId, String name, String email, String phone, String licenseNumber) {
        this.driverId = driverId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
    }

    // Getters
    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getStatus() { return status; }
    public GeoPoint getCurrentLocation() { return currentLocation; }
    public Timestamp getLastUpdate() { return lastUpdate; }
    public String getCurrentScheduleId() { return currentScheduleId; }

    // Setters
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public void setStatus(String status) { this.status = status; }
    public void setCurrentLocation(GeoPoint currentLocation) { this.currentLocation = currentLocation; }
    public void setLastUpdate(Timestamp lastUpdate) { this.lastUpdate = lastUpdate; }
    public void setCurrentScheduleId(String currentScheduleId) { this.currentScheduleId = currentScheduleId; }

    public void updateDriverStatus(String driverId, String newStatus) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        db.collection("drivers").document(driverId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Driver", "Driver status updated to: " + newStatus))
                .addOnFailureListener(e -> Log.e("Driver", "Error updating driver status: " + e.getMessage()));
    }
    public void updateDriverCurrentSchedule(String driverId, String scheduleId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentScheduleId", scheduleId);
        db.collection("drivers").document(driverId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Driver", "Driver currentScheduleId updated to: " + scheduleId))
                .addOnFailureListener(e -> Log.e("Driver", "Error updating driver currentScheduleId: " + e.getMessage()));
    }
}