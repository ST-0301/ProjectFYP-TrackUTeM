// TrackingService.java
package com.example.trackutem.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.trackutem.controller.TrackingController;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.utils.NotificationHelper;
import com.example.trackutem.view.Driver.ScheduleDetailsActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.util.List;
import java.util.Objects;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";
    private TrackingController trackingController;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        // Get driverId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String driverId = prefs.getString("driverId", null);

        trackingController = new TrackingController(driverId);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        NotificationHelper notificationHelper = new NotificationHelper(this);
        startForeground(1, notificationHelper.buildForegroundTrackingNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called with action: " + (intent != null ? intent.getAction() : "null"));
        if (intent != null && ScheduleDetailsActivity.GEOFENCE_BROADCAST_ACTION.equals(intent.getAction())) {
            handleGeofenceTransition(intent);
        } else {
            startLocationUpdates();
        }
        return START_STICKY;
    }
    private void handleGeofenceTransition(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null. Invalid intent?");
            return;
        }

        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            Log.e(TAG, "Geofence error: " + errorCode);
            return;
        }
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            for (Geofence geofence : Objects.requireNonNull(triggeringGeofences)) {
                String geofenceId = geofence.getRequestId();
                Log.d(TAG, "Entered geofence ID: " + geofenceId);

                new RoutePoint().getRPointNameById(geofenceId, new RoutePoint.RPointCallback() {
                    @Override
                    public void onSuccess(String rpointName) {
                        Intent broadcastIntent = new Intent(ScheduleDetailsActivity.GEOFENCE_BROADCAST_ACTION);
                        broadcastIntent.putExtra("transitionType", geofenceTransition);
                        broadcastIntent.putExtra("geofenceId", geofenceId);
                        broadcastIntent.putExtra("rpointName", rpointName);
                        sendBroadcast(broadcastIntent);

                        Notification notification = new NotificationHelper(TrackingService.this).sendGeofenceNotification(rpointName);
                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(2, notification);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error getting route point name for geofence: " + e.getMessage());
                    }
                });
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed, tracking stopped");

        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        trackingController.updateLocationToFirestore(
                                location.getLatitude(),
                                location.getLongitude()
                        );
                    }
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }
}