package com.example.trackutem.model;

import com.google.firebase.database.PropertyName;
import com.google.firebase.firestore.DocumentId;

public class BusDriverPairing {
    @DocumentId
    private String id;
    private String busId;
    private String driverId;
    @PropertyName("isActive")
    private boolean isActive;

    public BusDriverPairing() { }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    @PropertyName("isActive")
    public boolean getIsActive() { return isActive; }
    @PropertyName("isActive")
    public void setIsActive(boolean isActive) { isActive = isActive;}
}