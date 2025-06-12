//// LocationUtils.java
//package com.example.trackutem.utils;
//
//import android.location.Location;
//
//import com.google.android.gms.maps.model.LatLng;
//
//public class LocationUtils {
//    public static final float PROXIMITY_THRESHOLD_METERS = 1;
//
//    public static boolean isNearLocation(Location current, Location target) {
//        if (current == null || target == null) return false;
//
//        // Normalize precision to 7 decimals
//        double currentLat = roundCoordinate(current.getLatitude());
//        double currentLon = roundCoordinate(current.getLongitude());
//        double targetLat = roundCoordinate(target.getLatitude());
//        double targetLon = roundCoordinate(target.getLongitude());
//
//        Location normCurrent = new Location("");
//        normCurrent.setLatitude(currentLat);
//        normCurrent.setLongitude(currentLon);
//
//        Location normTarget = new Location("");
//        normTarget.setLatitude(targetLat);
//        normTarget.setLongitude(targetLon);
//
//        return normCurrent.distanceTo(normTarget) <= PROXIMITY_THRESHOLD_METERS;
//    }
//
//    private static double roundCoordinate(double value) {
//        return Math.round(value * 1e7) / 1e7;
//    }
////    public static boolean isNearLocation(Location current, Location target) {
////        if (current == null || target == null) return false;
////        return current.distanceTo(target) <= PROXIMITY_THRESHOLD_METERS;
////    }
////
////    public static Location toLocation(LatLng latLng) {
////        Location location = new Location("");
////        location.setLatitude(latLng.latitude);
////        location.setLongitude(latLng.longitude);
////        return location;
////    }
//}