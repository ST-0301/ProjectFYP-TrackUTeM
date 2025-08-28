package com.example.trackutem.view.Driver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.controller.ScheduleController;
import com.example.trackutem.model.Driver;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.model.Schedule;
import com.example.trackutem.service.TrackingService;
import com.example.trackutem.utils.AppStateManager;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ScheduleDetailsActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private RecyclerView rvRPoints;
    private final List<String> rpointList = new ArrayList<>();
    private RPointsTimelineAdapter adapter;
    private ImageButton floatingBackButton;
    private Button btnStart, btnStop;
    private SharedPreferences prefs;
    private Driver currentDriver;
    private String driverId;
    private String routeId;
    private String scheduleId;
    private AppStateManager appStateManager;
    private MapController mapController;
    private FusedLocationProviderClient fusedLocationClient;
    private List<LatLng> rpointLocations = new ArrayList<>();
    private int currentRPointIndex = -1;
    private Schedule currentSchedule;
    private static final float GEOFENCE_RADIUS_METERS = 5f;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private ScheduleDetailsActivity.GeofenceBroadcastReceiver geofenceReceiver;
    public static final String GEOFENCE_BROADCAST_ACTION = "com.example.trackutem.GEOFENCE_TRANSITION";
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_details);

        routeId = getIntent().getStringExtra("routeId");
        scheduleId = getIntent().getStringExtra("scheduleId");
        if (routeId == null || scheduleId == null) {
            Toast.makeText(this, "Route details not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // find views
        floatingBackButton = findViewById(R.id.floatingBackButton);
        btnStart   = findViewById(R.id.btnStart);
        btnStop    = findViewById(R.id.btnStop);
        rvRPoints = findViewById(R.id.rvRPoints);

        // RecyclerView setup
        rvRPoints.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RPointsTimelineAdapter(this, rpointList);
        adapter.setOnActionButtonClickListener((position, isLastRPoint) -> {
            if (currentSchedule != null &&
                    position == currentRPointIndex &&
                    currentSchedule.getRPoints() != null &&
                    position >= 0 &&
                    position < currentSchedule.getRPoints().size()) {
                long arrivalTime = System.currentTimeMillis();
                List<Schedule.RPointDetail> rpoints = currentSchedule.getRPoints();

                Schedule.RPointDetail currentRPoint = rpoints.get(position);

                // 1. Calculate lateness
                int lateness = ScheduleController.calculateLateness(
                        currentRPoint.getPlanTime(),
                        arrivalTime,
                        currentSchedule.getScheduledDatetime().getTime()
                );
                if (lateness < 0) {
                    lateness = 0;
                }
                currentRPoint.setLatenessMinutes(lateness);
                currentRPoint.setStatus("arrived");
                currentRPoint.setActTime(arrivalTime);
                rpoints.set(position, currentRPoint);

                Map<String, Object> updates = new HashMap<>();

                int nextIndex = position + 1;
                if (nextIndex < rpoints.size()) {
                    Schedule.RPointDetail nextRPoint = rpoints.get(nextIndex);
                    nextRPoint.setStatus("departed");
                    rpoints.set(nextIndex, nextRPoint);
                    updates.put("currentRPointIndex", nextIndex);
                    currentSchedule.setCurrentRPointIndex(nextIndex);
                } else {
                    updates.put("currentRPointIndex", -1);
                    updates.put("status", "completed");
                    currentSchedule.setCurrentRPointIndex(-1);
                    currentSchedule.setStatus("completed");
                }
                updates.put("rpoints", rpoints);

                FirebaseFirestore.getInstance()
                        .collection("schedules")
                        .document(currentSchedule.getScheduleId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("ScheduleUpdate", "Updated entire rpoints array successfully.");
                            currentRPointIndex = currentSchedule.getCurrentRPointIndex();
                            adapter.setCurrentRPointIndex(currentRPointIndex);
                            adapter.notifyDataSetChanged();

                            if (isLastRPoint) {
                                endTrip();
                                Toast.makeText(this, "Route completed!", Toast.LENGTH_SHORT).show();
                            } else if (currentRPointIndex != -1) {
                                startGeofencing();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ScheduleUpdate", "FATAL: Firestore update failed!", e);
                            Toast.makeText(this, "Error updating trip status.", Toast.LENGTH_LONG).show();
                        });
            }
        });
        adapter.setOnRPointClickListener((position, location) -> {
            if (mapController != null) {
                mapController.moveCameraToLocation(location, 15f);
            }
        });
        rvRPoints.setAdapter(adapter);

        currentDriver = new Driver();

        // Initialize controllers and services
        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        driverId = prefs.getString("driverId", null);
        appStateManager = new AppStateManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceReceiver = new GeofenceBroadcastReceiver();

        restoreUIState();

        // Button click listeners
        floatingBackButton.setOnClickListener(v -> onBackPressed());

        btnStart.setOnClickListener(v -> {
            View root = findViewById(R.id.rootContainer);
            if (root instanceof ViewGroup) {
                TransitionManager.beginDelayedTransition((ViewGroup) root);
            }

            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);

            currentDriver.updateDriverStatus(driverId, "on_duty");
            currentDriver.updateDriverCurrentSchedule(driverId, scheduleId);

            startService(new Intent(this, TrackingService.class));
            appStateManager.saveState("start", true);
            startTrip();
        });

        btnStop.setOnClickListener(v -> endTrip());

        fetchScheduleWithRPointDetails(scheduleId);
        if (currentRPointIndex != -1 &&
                (currentSchedule == null || currentSchedule.getRPoints() == null ||
                        currentRPointIndex >= currentSchedule.getRPoints().size())) {
            currentRPointIndex = -1;
            if (currentSchedule != null) {
                currentSchedule.setCurrentRPointIndex(-1);
            }
        }

        checkPermissions();
    }
    private void endTrip() {
        currentDriver.updateDriverCurrentSchedule(driverId, null);
        checkAndUpdateDriverStatus();

        Intent serviceIntent = new Intent(this, TrackingService.class);
        serviceIntent.setAction("STOP_TRACKING");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        stopLocationUpdatesForProximity();
        stopGeofencing();
        appStateManager.saveState("stop", false);

        if (currentSchedule != null && "event".equalsIgnoreCase(currentSchedule.getType())) {
            currentSchedule.setStatus("completed");
            updateScheduleInFirestore();
        }

        btnStart.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.GONE);
    }
    private void checkAndUpdateDriverStatus() {
        if (driverId == null) {
            currentDriver.updateDriverStatus(null, "available");
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        FirebaseFirestore.getInstance().collection("busDriverPairings")
                .whereEqualTo("driverId", driverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<String> busDriverPairIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            busDriverPairIds.add(document.getId());
                        }

                        FirebaseFirestore.getInstance().collection("schedules")
                                .whereIn("busDriverPairId", busDriverPairIds)
                                .whereGreaterThanOrEqualTo("scheduledDatetime", startOfDay)
                                .whereLessThanOrEqualTo("scheduledDatetime", endOfDay)
                                .get()
                                .addOnSuccessListener(scheduleSnapshots -> {
                                    boolean hasMoreSchedulesToday = false;

                                    for (QueryDocumentSnapshot document : scheduleSnapshots) {
                                        Schedule schedule = document.toObject(Schedule.class);
                                        String status = schedule.getStatus();
                                        if (!"completed".equals(status) &&
                                                !"cancelled".equals(status) &&
                                                !document.getId().equals(scheduleId)) {
                                            hasMoreSchedulesToday = true;
                                            break;
                                        }
                                    }
                                    String newStatus = hasMoreSchedulesToday ? "available" : "off_duty";
                                    currentDriver.updateDriverStatus(driverId, newStatus);

                                    Log.d("DriverStatus", "Driver status set to: " + newStatus +
                                            " (hasMoreSchedulesToday: " + hasMoreSchedulesToday + ")");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("DriverStatus", "Error checking remaining schedules: " + e.getMessage());
                                    currentDriver.updateDriverStatus(driverId, "available");
                                });
                    } else {
                        currentDriver.updateDriverStatus(driverId, "off_duty");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DriverStatus", "Error getting bus pairings: " + e.getMessage());
                    currentDriver.updateDriverStatus(driverId, "available");
                });
    }
    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, TrackingService.class);
        intent.setAction(GEOFENCE_BROADCAST_ACTION);
        geofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0));
        return geofencePendingIntent;
    }
    private void startGeofencing() {
        if (currentSchedule == null ||
                currentSchedule.getRPoints() == null ||
                currentSchedule.getRPoints().isEmpty()) {

            Log.d("Geofence", "No schedule or route points to set geofences for.");
            return;
        }
        stopGeofencing();
        if (currentRPointIndex == -1 ||
                currentRPointIndex >= currentSchedule.getRPoints().size()) {

            Log.d("Geofence", "No more pending route points for geofencing.");
            return;
        }

        Schedule.RPointDetail nextRPointToMonitor = currentSchedule.getRPoints().get(currentRPointIndex);
        if (nextRPointToMonitor == null) {
            Log.e("Geofence", "Next route point to monitor is null");
            return;
        }
        String rpointId = nextRPointToMonitor.getRpointId();
        new RoutePoint().getRPointLocationById(rpointId, new RoutePoint.RPointLocationCallback() {
            @Override
            public void onSuccess(LatLng location) {
                Geofence geofence = new Geofence.Builder()
                        .setRequestId(rpointId)

                        .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS_METERS)
                        .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build();
                if (ContextCompat.checkSelfPermission(ScheduleDetailsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    geofencingClient.addGeofences(getGeofencingRequest(geofence), getGeofencePendingIntent())
                            .addOnSuccessListener(aVoid -> Log.d("Geofence", "Geofence added for route point: " + rpointId))
                            .addOnFailureListener(e -> {
                                Log.e("Geofence", "Failed to add geofence: " + e.getMessage());
                                if (e instanceof ApiException) {
                                    ApiException apiException = (ApiException) e;
                                    Log.e("Geofence", "Geofence error code: " + apiException.getStatusCode());
                                }
                            });
                } else {
                    Log.w("Geofence", "Location permission not granted for geofencing.");
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e("Geofence", "Error getting route point location for geofence: " + e.getMessage());
            }
        });
    }
    private void stopGeofencing() {
        if (geofencePendingIntent != null) {
            geofencingClient.removeGeofences(geofencePendingIntent)
                    .addOnSuccessListener(aVoid -> Log.d("Geofence", "All geofences removed."))
                    .addOnFailureListener(e -> Log.e("Geofence", "Failed to remove geofences: " + e.getMessage()));
            geofencePendingIntent = null;
        }
    }
    private void startLocationUpdatesForProximity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("LocationProximity", "Location permission not granted. Cannot start proximity updates.");
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(3000)
                .setMinUpdateIntervalMillis(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build());

        task.addOnCompleteListener(task1 -> {
            try {
                task1.getResult(ApiException.class);
                startActualLocationUpdates(locationRequest);
            } catch (ApiException exception) {
                switch (exception.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            resolvable.startResolutionForResult(this, 1002);
                        } catch (Exception e) {
                            Log.e("LocationProximity", "Error showing resolution dialog: " + e.getMessage());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e("LocationProximity", "Location settings are inadequate, and cannot be fixed here.");
                        break;
                }
            }
        });
    }
    @SuppressLint("MissingPermission")
    private void startActualLocationUpdates(LocationRequest locationRequest) {
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            Log.d("LocationProximity", "Received location update: " + location.getLatitude() + ", " + location.getLongitude());
                            checkProximityToNextRPoint(location);
                        }
                    }
                }
            };
        }
        if (!requestingLocationUpdates) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            requestingLocationUpdates = true;
            Log.d("LocationProximity", "Started requesting location updates for proximity.");
        }
    }
    private void stopLocationUpdatesForProximity() {
        if (locationCallback != null && requestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            requestingLocationUpdates = false;
            Log.d("LocationProximity", "Stopped requesting location updates for proximity.");
        }
    }

    private void checkProximityToNextRPoint(Location currentLocation) {
        if (currentSchedule == null ||
                currentSchedule.getRPoints() == null ||
                currentSchedule.getRPoints().isEmpty() ||
                currentRPointIndex < 0 ||
                currentRPointIndex >= currentSchedule.getRPoints().size()) {

            Log.d("ProximityCheck", "No valid route points to check proximity");
            stopLocationUpdatesForProximity();
            return;
        }
        List<Schedule.RPointDetail> rpoints = currentSchedule.getRPoints();
        if (rpoints == null || rpoints.isEmpty() || currentRPointIndex >= rpoints.size()) {
            Log.d("ProximityCheck", "Route points list is empty or index out of bounds");
            return;
        }

        Schedule.RPointDetail targetRPoint = rpoints.get(currentRPointIndex);
        if (targetRPoint == null) {
            Log.e("ProximityCheck", "Target route point is null");
            return;
        }
        new RoutePoint().getRPointLocationById(targetRPoint.getRpointId(), new RoutePoint.RPointLocationCallback() {
            @Override
            public void onSuccess(LatLng rpointLatLng) {
                if (rpointLatLng == null) {
                    Log.e("ProximityCheck", "Route point location is null");
                    return;
                }

                float[] results = new float[1];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                        rpointLatLng.latitude, rpointLatLng.longitude, results);
                float distanceInMeters = results[0];
                String rpointName = "Unknown Route Point";
                if (currentRPointIndex >= 0 && currentRPointIndex < rpointList.size()) {
                    rpointName = rpointList.get(currentRPointIndex);
                } else {
                    Log.w("ProximityCheck", "rpointList index out of bounds: " + currentRPointIndex + ", size: " + rpointList.size());
                }
                Log.d("ProximityCheck", "Distance to " + rpointName + ": " + distanceInMeters + " meters");

                if (distanceInMeters <= 5) {
                    long arrivalTime = System.currentTimeMillis();

                    // Calculate lateness
                    int lateness = ScheduleController.calculateLateness(
                            targetRPoint.getPlanTime(),
                            arrivalTime,
                            currentSchedule.getScheduledDatetime().getTime()
                    );

                    Schedule.RPointDetail currentRPoint = rpoints.get(currentRPointIndex);
                    currentRPoint.setStatus("arrived");
                    currentRPoint.setActTime(arrivalTime);
                    currentRPoint.setLatenessMinutes(lateness);
                    rpoints.set(currentRPointIndex, currentRPoint);

                    Map<String, Object> updates = new HashMap<>();
                    int nextIndex = currentRPointIndex + 1;
                    if (nextIndex < rpoints.size()) {
                        Schedule.RPointDetail nextRPoint = rpoints.get(nextIndex);
                        nextRPoint.setStatus("departed");
                        rpoints.set(nextIndex, nextRPoint);
                        updates.put("currentRPointIndex", nextIndex);
                        currentSchedule.setCurrentRPointIndex(nextIndex);
                    } else {
                        updates.put("currentRPointIndex", -1);
                        updates.put("status", "completed");
                        currentSchedule.setCurrentRPointIndex(-1);
                        currentSchedule.setStatus("completed");
                    }
                    updates.put("rpoints", rpoints);

                    FirebaseFirestore.getInstance()
                            .collection("schedules")
                            .document(currentSchedule.getScheduleId())
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("ProximityCheck", "Successfully updated schedule via proximity detection");
                                adapter.setRPointDetails(rpoints);
                                adapter.setCurrentRPointIndex(currentSchedule.getCurrentRPointIndex());
                                adapter.notifyDataSetChanged();

                                if (currentSchedule.getStatus().equals("completed")) {
                                    endTrip();
                                    Toast.makeText(ScheduleDetailsActivity.this,
                                            "Route completed! Tracking stopped.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> Log.e("ProximityCheck", "Failed to update schedule: " + e.getMessage()));
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e("ProximityCheck", "Error getting route point location for proximity check: " + e.getMessage());
            }
        });
    }
    private void updateScheduleInFirestore() {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(currentSchedule.getScheduleId())
                .set(currentSchedule);
    }
    private void fetchScheduleWithRPointDetails(String scheduleId) {
        FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentSchedule = documentSnapshot.toObject(Schedule.class);
                    if (currentSchedule != null) {
                        if (currentSchedule.getRPoints() == null) {
                            currentSchedule.setRPoints(new ArrayList<>());
                            Log.w("ScheduleFetch", "RPoints was null, initialized empty list");
                        }

                        if ("event".equalsIgnoreCase(currentSchedule.getType())
                                && currentSchedule.getRPoints() != null) {
                            for (Schedule.RPointDetail rpoint : currentSchedule.getRPoints()) {
                                if (rpoint.getStatus() == null)
                                    rpoint.setStatus("scheduled");
                                if (rpoint.getPlanTime() == null)
                                    rpoint.setPlanTime("");
                                if (rpoint.getActTime() == null)
                                    rpoint.setActTime(null);
                            }
                        }

                        currentRPointIndex = currentSchedule.getCurrentRPointIndex();
                        if (currentRPointIndex == -1) {
                            currentRPointIndex = findInitialCurrentRPointIndex(currentSchedule);
                        }

                        resolveRPointNames(currentSchedule.getRPoints(), rpointNames -> {
                            rpointList.clear();
                            rpointList.addAll(rpointNames);
                            if (currentRPointIndex >= rpointList.size()) {
                                currentRPointIndex = -1;
                                if (currentSchedule != null) {
                                    currentSchedule.setCurrentRPointIndex(-1);
                                }
                            }

                            if (currentSchedule != null) {
                                adapter.setRPointDetails(currentSchedule.getRPoints());
                            }
                            adapter.setCurrentRPointIndex(currentRPointIndex);
                            adapter.notifyDataSetChanged();
                        });
                    }
                });
    }
    private int findInitialCurrentRPointIndex(Schedule schedule) {
        if (schedule == null ||
                schedule.getRPoints() == null ||
                schedule.getRPoints().isEmpty()) {
            return -1;
        }
        int storedIndex = schedule.getCurrentRPointIndex();
        if (storedIndex < 0 || storedIndex >= schedule.getRPoints().size()) {
            return 0;
        }
        return storedIndex;
    }
    private void resolveRPointNames(List<Schedule.RPointDetail> rpoints, Consumer<List<String>> callback) {
        String[] rpointNamesArray = new String[rpoints.size()];
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < rpoints.size(); i++) {
            int currentIndex = i;
            Schedule.RPointDetail rpoint = rpoints.get(i);
            new RoutePoint().getRPointNameById(rpoint.getRpointId(), new RoutePoint.RPointCallback() {
                @Override
                public void onSuccess(String rpointName) {
                    rpointNamesArray[currentIndex] = rpointName;
                    if (counter.incrementAndGet() == rpoints.size()) {
                        List<String> orderedNames = new ArrayList<>(Arrays.asList(rpointNamesArray));
                        callback.accept(orderedNames);
                    }
                }
                @Override
                public void onError(Exception e) {
                    rpointNamesArray[currentIndex] = "Unknown Route Point";
                    if (counter.incrementAndGet() == rpoints.size()) {
                        List<String> orderedNames = new ArrayList<>(Arrays.asList(rpointNamesArray));
                        callback.accept(orderedNames);
                    }
                }
            });
        }
    }
    private void startTrip() {
        currentSchedule.setStatus("in_progress");

        if (!"event".equalsIgnoreCase(currentSchedule.getType())) {
            currentRPointIndex = 0;
            currentSchedule.setCurrentRPointIndex(currentRPointIndex);

            if (!currentSchedule.getRPoints().isEmpty()) {
                Schedule.RPointDetail firstRPoint = currentSchedule.getRPoints().get(0);
                firstRPoint.setStatus("departed");
            }
            startGeofencing();
            startLocationUpdatesForProximity();
        } else {
            currentRPointIndex = -1;
            currentSchedule.setCurrentRPointIndex(-1);
        }
        updateScheduleInFirestore();
        adapter.setCurrentRPointIndex(currentRPointIndex);
        adapter.notifyDataSetChanged();

        if (!"event".equalsIgnoreCase(currentSchedule.getType())) {
            startGeofencing();
            startLocationUpdatesForProximity();
        }
    }

    @SuppressLint("SetTextI18n")
    private void restoreUIState() {
        String buttonState = appStateManager.getButtonState();
        boolean isTracking = appStateManager.isTracking();

        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);

        if (buttonState.equals("start")) {
            btnStop.setVisibility(View.VISIBLE);
        } else {
            btnStart.setVisibility(View.VISIBLE);
        }
        if (isTracking) {
            startService(new Intent(this, TrackingService.class));

            if (currentSchedule != null && currentRPointIndex != -1 &&
                    currentSchedule.getRPoints() != null &&
                    currentRPointIndex < currentSchedule.getRPoints().size()) {
                startGeofencing();
                startLocationUpdatesForProximity();
            }
        }
    }

    // Permissions
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_LOCATION_PERMISSION + 1);
        }

        // Android 13+ notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), REQUEST_LOCATION_PERMISSION);
        } else {
            initializeMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            boolean allGranted = true;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Toast.makeText(this, "Location permission required for tracking", Toast.LENGTH_LONG).show();
                    } else if (permissions[i].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        Toast.makeText(this, "Background permission is recommended for reliable geofencing.", Toast.LENGTH_LONG).show();
                    }
                }
            }
            if (allGranted) {
                initializeMap();
                if (appStateManager.isTracking()) {
                    startGeofencing();
                    startLocationUpdatesForProximity();
                }
            }
        }
    }
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mapController = new MapController(this, googleMap, fusedLocationClient);
                mapController.initializeMapFeatures();

                fetchAndDrawRoute();
            });
        }
    }
    private void fetchAndDrawRoute() {
        Route.getRPointLocations(routeId, new Route.OnRPointsLocationResolvedListener() {
            @Override
            public void onRPointsLocationResolved(List<LatLng> locations) {
                rpointLocations = locations;
                if (mapController != null && !locations.isEmpty()) {
                    mapController.addRouteMarkers(locations, rpointList);
                    mapController.drawRouteFromPoints(locations, getString(R.string.directions_api));
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ScheduleDetailsActivity.this, "Error getting locations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(GEOFENCE_BROADCAST_ACTION);
        ContextCompat.registerReceiver(this, geofenceReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        if (appStateManager.isTracking()) {
            startGeofencing();
            startLocationUpdatesForProximity();
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(geofenceReceiver);
        stopGeofencing();
        stopLocationUpdatesForProximity();
    }
    private class GeofenceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && GEOFENCE_BROADCAST_ACTION.equals(intent.getAction())) {
                int geofenceTransition = intent.getIntExtra("transitionType", -1);
                String triggeredGeofenceId = intent.getStringExtra("geofenceId");
                String rpointName = intent.getStringExtra("rpointName");

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER &&
                        currentSchedule != null && currentRPointIndex != -1 &&
                        currentRPointIndex < currentSchedule.getRPoints().size() &&
                        triggeredGeofenceId.equals(currentSchedule.getRPoints().get(currentRPointIndex).getRpointId())) {
                    Log.d("GeofenceReceiver", "Entered geofence for route point: " + rpointName);
                }
            }
        }
    }
}