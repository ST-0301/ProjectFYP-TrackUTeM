package com.example.trackutem.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@SuppressWarnings("unused")
public class Notification {
    private String key;
    private String type;
    private String audienceType;
    private String audienceId;
    private String routeId;
    private String scheduleType;
    private String busDriverPairId;
    private String busPlateNumber;
    private String routeName;
    private Date scheduledDatetime;
    private int latenessMinutes;
    @ServerTimestamp
    private Date created;

    public Notification() {}

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAudienceType() { return audienceType; }
    public void setAudienceType(String audienceType) { this.audienceType = audienceType; }
    public String getAudienceId() { return audienceId; }
    public void setAudienceId(String audienceId) { this.audienceId = audienceId; }
    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }
    public String getBusDriverPairId() { return busDriverPairId; }
    public void setBusDriverPairId(String busDriverPairId) { this.busDriverPairId = busDriverPairId; }
    public String getBusPlateNumber() { return busPlateNumber; }
    public void setBusPlateNumber(String busPlateNumber) { this.busPlateNumber = busPlateNumber; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public Date getScheduledDatetime() { return scheduledDatetime; }
    public void setScheduledDatetime(Date scheduledDatetime) { this.scheduledDatetime = scheduledDatetime; }
    public int getLatenessMinutes() { return latenessMinutes; }
    public void setLatenessMinutes(int latenessMinutes) { this.latenessMinutes = latenessMinutes; }
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
}