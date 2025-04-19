package com.example.trackutem.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.trackutem.controller.TrackingController;
import com.example.trackutem.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private DatabaseReference busLocationRef;
    private TrackingController trackingController;
    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        notificationHelper = new NotificationHelper(this);
        trackingController = new TrackingController(
                FirebaseDatabase.getInstance().getReference("bus_locations").child("bus1"),
                LocationServices.getFusedLocationProviderClient(this),
                this);

        startForeground(1, notificationHelper.buildForegroundTrackingNotification());
        trackingController.startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        trackingController.startLocationUpdates();
        return START_STICKY; // Restart if the system kills the service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackingController.stopLocationUpdates();
        Log.d(TAG, "Service destroyed, tracking stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not using bound service
    }
}