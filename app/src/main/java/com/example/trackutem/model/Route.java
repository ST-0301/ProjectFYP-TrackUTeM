package com.example.trackutem.model;

import com.google.firebase.database.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class Route {
    private String routeId;
    private String routeName;
    private Schedule schedule;
    private List<String> stations;

    // Nested Schedule class
    private static class Schedule {
        @PropertyName("monThu")
        private DaySchedule monThuSchedule;
        @PropertyName("fri")
        private DaySchedule friSchedule;

        // Getters
        public DaySchedule getMonThuSchedule() {
            return monThuSchedule;
        }
        public DaySchedule getFriSchedule() {
            return friSchedule;
        }

        // Setters
        public void setMonThuSchedule(DaySchedule monThuSchedule) {
            this.monThuSchedule = monThuSchedule;
        }
        public void setFriSchedule(DaySchedule friSchedule) {
            this.friSchedule = friSchedule;
        }
    }

    // Nested DaySchedule class
    public static class DaySchedule {
        @PropertyName("in")
        private List<String> inCampusTimes;
        @PropertyName("out")
        private List<String> outCampusTimes;

        // Getters
        public List<String> getInCampusTimes() {
            return inCampusTimes;
        }
        public List<String> getOutCampusTimes() {
            return outCampusTimes;
        }

        // Setters
        public void setInCampusTimes(List<String> inCampusTimes) {
            this.inCampusTimes = inCampusTimes;
        }
        public void setOutCampusTimes(List<String> outCampusTimes) {
            this.outCampusTimes = outCampusTimes;
        }
    }

    public Route() {}

    // Getter
    public String getRouteId() {
        return routeId;
    }
    public String getRouteName() {
        return routeName;
    }
    public Schedule getSchedule() {
        return schedule;
    }
    public List<String> getStations() {
        return stations;
    }

    // Setter
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }
    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }
    public void setStations(List<String> stations) {
        this.stations = stations;
    }

    // Helper method to map station IDs to Station objects
    // Why here? Model class can contain your data access code, application logic. So it's okay to place in Model.
    // The best approach is to create a folder Utility in Model folder and place utility class there.
    public List<Station> resolveStations(List<Station> allStations) {
        List<Station> routeStations = new ArrayList<>();
        for (String stationId : stations) {
            for (Station station : allStations) {
                if (station.getStationId().equals(stationId)) {
                    routeStations.add(station);
                    break;
                }
            }
        }
        return routeStations;
    }

//    // To access Friday's inbound times from a Route object:
//    List<String> fridayInbound = route.getSchedule()
//            .getFridaySchedule()
//            .getInboundTimes();
//
//    // To access Monday-Thursday outbound times:
//    List<String> monThuOutbound = route.getSchedule()
//            .getMondayThursdaySchedule()
//            .getOutboundTimes();

    //    // Plot stations on map
//    for (Station station : route.resolveStations(allStations)) {
//        LatLng position = station.getLocation();
//        String name = station.getName();
//        // Add marker to Google Map
//    }
//// Calculate travel time between stations
//Station start = getStationById(route.getStations().get(0));
//    Station end = getStationById(route.getStations().get(1));
//    double distance = calculateDistance(start.getLocation(), end.getLocation());

}