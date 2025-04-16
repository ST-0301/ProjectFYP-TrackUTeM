package com.example.trackutem.model;

import com.google.android.gms.maps.model.LatLng;

public class Station {
    private final String stationId;
    private final LatLng location;
    private final String name;

    public Station(String stationId, LatLng location, String name) {
        this.stationId = stationId;
        this.location = location;
        this.name = name;
    }

    public String getStationId() { return stationId; }
    public LatLng getLocation() { return location; }
    public String getName() { return name; }
}
