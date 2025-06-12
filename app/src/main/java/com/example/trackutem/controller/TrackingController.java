// TrackingController.java
package com.example.trackutem.controller;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.firebase.database.DatabaseReference;
import java.util.HashMap;
import java.util.Map;

public class TrackingController {
    private static final String TAG = "TrackingController";
    private static final long LOCATION_INTERVAL = 10000;       // 10 seconds
    private static final long LOCATION_FASTEST_INTERVAL = 5000; // 5 seconds
    private final DatabaseReference busLocationRef;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;
    private boolean isTracking = false;
    private LocationCallback locationCallback;

    public TrackingController(DatabaseReference busLocationRef, FusedLocationProviderClient fusedLocationClient, Context context) {
        this.busLocationRef = busLocationRef;
        this.fusedLocationClient = fusedLocationClient;
        this.context = context;
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted. Cannot start updates.");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(LOCATION_INTERVAL)
                .setFastestInterval(LOCATION_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    if (location != null && isTracking) {
                        updateLocationToFirebase(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };
        isTracking = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Location updates started.");
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped.");
        }
        isTracking = false;
    }

    public void updateLocationToFirebase(double lat, double lng) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("lat", lat);
        locationMap.put("lng", lng);
        locationMap.put("timestamp", System.currentTimeMillis());

        busLocationRef.setValue(locationMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Update succeeded"))
                .addOnFailureListener(e -> Log.e(TAG, "Update failed", e));
    }
}