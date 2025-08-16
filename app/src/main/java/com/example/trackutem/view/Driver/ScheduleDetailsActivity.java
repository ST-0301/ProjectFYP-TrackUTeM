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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.MainDrvActivity;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.controller.RestLogController;
import com.example.trackutem.controller.ScheduleController;
import com.example.trackutem.controller.TimerController;
import com.example.trackutem.model.Driver;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.model.Schedule;
import com.example.trackutem.service.DirectionsService;
import com.example.trackutem.service.TrackingService;
import com.example.trackutem.utils.AppStateManager;
import com.example.trackutem.utils.NotificationHelper;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import retrofit2.Retrofit;

public class ScheduleDetailsActivity extends AppCompatActivity  implements TimerController.TimerCallback {
    private static final String EXTRA_ROUTE_ID = "routeId";
    private static final String EXTRA_SCHEDULE_ID = "scheduleId";
    private static final long REST_DURATION_MILLIS = 2 * 60 * 1000;
    private static final long FIVE_MINUTES_WARNING = 1 * 60 * 1000;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private MaterialToolbar toolbar;
    private RecyclerView rvRPoints;
    private List<String> rpointList = new ArrayList<>();
    private RPointsTimelineAdapter adapter;
    private LinearLayout controlGroup;
    private Button btnStart, btnRest, btnContinue, btnStop;
    private TextView tvTimer;
    private SharedPreferences prefs;
    private Driver currentDriver;
    private String driverId;
    private String routeId;
    private String scheduleId;
    private AppStateManager appStateManager;
    private RestLogController restLogController;
    private TimerController timerController;
    private NotificationHelper notificationHelper;
    private MapController mapController;
    private FusedLocationProviderClient fusedLocationClient;
    private Retrofit retrofit;
    private DirectionsService directionsService;
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
        toolbar = findViewById(R.id.toolbar);
        btnStart   = findViewById(R.id.btnStart);
        btnRest    = findViewById(R.id.btnRest);
        btnContinue= findViewById(R.id.btnContinue);
        btnStop    = findViewById(R.id.btnStop);
        tvTimer    = findViewById(R.id.tvTimer);
        controlGroup = findViewById(R.id.controlGroup);
        rvRPoints = findViewById(R.id.rvRPoints);

        // Set up toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // RecyclerView setup
        rvRPoints.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RPointsTimelineAdapter(this, rpointList);
        adapter.setOnActionButtonClickListener((position, isLastRPoint) -> {
            if (currentSchedule != null && position == currentRPointIndex) {
                ScheduleController.recordArrivalAndNextDeparture(currentSchedule, position);

                // Update UI
                currentRPointIndex = currentSchedule.getCurrentRPointIndex();
                adapter.setCurrentRPointIndex(currentRPointIndex);
                adapter.notifyDataSetChanged();

                // If this was the last route point, end the trip
                if (isLastRPoint) {
                    endTrip();
                    Toast.makeText(this, "Route completed!", Toast.LENGTH_SHORT).show();
                }
                // Prepare for next route point if not completed
                else if (currentRPointIndex != -1) {
                    startGeofencing();
                }
            }
        });
        rvRPoints.setAdapter(adapter);

        currentDriver = new Driver();

        // Initialize controllers and services
        prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        driverId = prefs.getString("driverId", null);
        appStateManager = new AppStateManager(this);
        restLogController = new RestLogController();
        timerController = new TimerController(this, new NotificationHelper(this));
        notificationHelper = new NotificationHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceReceiver = new GeofenceBroadcastReceiver();

        restoreUIState();

        // Button click listeners
        btnStart.setOnClickListener(v -> {
            View root = findViewById(R.id.rootContainer);
            if (root instanceof ViewGroup) {
                TransitionManager.beginDelayedTransition((ViewGroup) root);
            }

            btnStart.setVisibility(View.GONE);
            controlGroup.setVisibility(View.VISIBLE);
            btnRest.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);

            // Initialize status, rest log & Record start working time
            currentDriver.updateDriverStatus(driverId, "on_duty");
            currentDriver.updateDriverCurrentSchedule(driverId, scheduleId);
            restLogController.createTodayRestLog(driverId, ref -> {});

            startService(new Intent(this, TrackingService.class));
            appStateManager.saveState("start", 0, true);

            startTrip();
        });

        btnRest.setOnClickListener(v -> {
            btnRest.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.VISIBLE);

            // Increase rest count
            currentDriver.updateDriverStatus(driverId, "rest");
            restLogController.incrementRestCount(driverId);

            // Stop tracking
            Intent serviceIntent = new Intent(this, TrackingService.class);
            stopService(serviceIntent);
            stopGeofencing();
            stopLocationUpdatesForProximity();

            long endTime = System.currentTimeMillis() + REST_DURATION_MILLIS;
            appStateManager.saveState("rest", endTime, false);

            // Start 30-min timer
            timerController.startCountdown(REST_DURATION_MILLIS);
        });

        btnContinue.setOnClickListener(v -> {
            btnContinue.setVisibility(View.GONE);
            btnRest.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.GONE);

            // Resume tracking
            currentDriver.updateDriverStatus(driverId, "on_duty");
            startService(new Intent(this, TrackingService.class));
            startGeofencing();
            startLocationUpdatesForProximity();

            // Stop timer
            timerController.stopCountdown();

            appStateManager.saveState("continue", 0, true);
        });

        btnStop.setOnClickListener(v -> endTrip());

        // Fetch schedule with route point details
        fetchScheduleWithRPointDetails(scheduleId);

        checkPermissions();
    }
    private void endTrip() {
        // Record end working time
        restLogController.updateEndWorkTime(driverId);
        currentDriver.updateDriverStatus(driverId, "available");

        Intent serviceIntent = new Intent(this, TrackingService.class);
        stopService(serviceIntent);
        timerController.stopCountdown();
        appStateManager.clear();

        // Stop geofencing when trip ends
        stopGeofencing();
        stopLocationUpdatesForProximity();

        // For event type, only record status
        if (currentSchedule != null && "event".equalsIgnoreCase(currentSchedule.getType())) {
            currentSchedule.setStatus("completed");
            updateScheduleInFirestore();
        }

        btnStart.setVisibility(View.VISIBLE);
        btnRest.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);
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
        if (currentSchedule == null || currentSchedule.getRPoints() == null || currentSchedule.getRPoints().isEmpty()) {
            Log.d("Geofence", "No schedule or route points to set geofences for.");
            return;
        }
        stopGeofencing();
        if (currentRPointIndex == -1 || currentRPointIndex >= currentSchedule.getRPoints().size()) {
            Log.d("Geofence", "No more pending route points for geofencing.");
            return;
        }
        Schedule.RPointDetail nextRPointToMonitor = currentSchedule.getRPoints().get(currentRPointIndex);
        String rpointId = nextRPointToMonitor.getRPointId();
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
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(3000)       // Update every 3 seconds
                .setFastestInterval(1000) // Smallest interval for updates
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build());

        task.addOnCompleteListener(task1 -> {
            try {
                LocationSettingsResponse response = task1.getResult(ApiException.class);
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
                    if (locationResult == null) {
                        return;
                    }
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
        if (currentSchedule == null || currentRPointIndex < 0 || currentRPointIndex >= currentSchedule.getRPoints().size()) {
            return;
        }
        Schedule.RPointDetail nextRPoint = currentSchedule.getRPoints().get(currentRPointIndex);
        new RoutePoint().getRPointLocationById(nextRPoint.getRPointId(), new RoutePoint.RPointLocationCallback() {
            @Override
            public void onSuccess(LatLng rpointLatLng) {
                float[] results = new float[1];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), rpointLatLng.latitude, rpointLatLng.longitude, results);
                float distanceInMeters = results[0];
                Log.d("ProximityCheck", "Distance to " + rpointList.get(currentRPointIndex) + ": " + distanceInMeters + " meters");

                if (distanceInMeters <= 5) {
                    long arrivalTime = System.currentTimeMillis();
                    nextRPoint.setActTime(arrivalTime);
                    nextRPoint.setStatus("arrived");

                    // Calculate lateness
                    int lateness = ScheduleController.calculateLateness(
                            nextRPoint.getPlanTime(),
                            arrivalTime,
                            currentSchedule.getScheduledDatetime().getTime()
                    );
                    nextRPoint.setLatenessMinutes(lateness);

                    // Record next route point departure if exists
                    int nextIndex = currentRPointIndex + 1;
                    if (nextIndex < currentSchedule.getRPoints().size()) {
                        Schedule.RPointDetail nextRPointDetail = currentSchedule.getRPoints().get(nextIndex);
                        nextRPointDetail.setActTime(System.currentTimeMillis());
                        nextRPointDetail.setStatus("departed");
                        currentSchedule.setCurrentRPointIndex(nextIndex);
                    } else {
                        currentSchedule.setCurrentRPointIndex(-1);
                        currentSchedule.setStatus("completed");
//                        currentSchedule.setTripEndTime(System.currentTimeMillis());
                    }

                    // Update Firestore
                    updateScheduleInFirestore();

                    // Update UI
                    currentRPointIndex = currentSchedule.getCurrentRPointIndex();
                    adapter.setCurrentRPointIndex(currentRPointIndex);
                    adapter.notifyDataSetChanged();

                    // Prepare for next route point or finish
                    if (currentRPointIndex != -1) {
                        startGeofencing();
                    } else {
                        stopGeofencing();
                        stopLocationUpdatesForProximity();
                        endTrip();
                        Toast.makeText(ScheduleDetailsActivity.this, "Route completed!", Toast.LENGTH_SHORT).show();
                    }
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
                        if ("event".equalsIgnoreCase(currentSchedule.getType())
                                && currentSchedule.getRPoints() != null) {
                            for (Schedule.RPointDetail rpoint : currentSchedule.getRPoints()) {
                                if (rpoint.getStatus() == null)
                                    rpoint.setStatus("scheduled");
                                if (rpoint.getPlanTime() == null)
                                    rpoint.setPlanTime("");
                                if (rpoint.getActTime() == null)
                                    rpoint.setActTime(null);
                                // latenessMinutes is int, default 0
                            }
                        }

                        currentRPointIndex = currentSchedule.getCurrentRPointIndex();
                        if (currentRPointIndex == -1) {
                            currentRPointIndex = findInitialCurrentRPointIndex(currentSchedule);
                        }

                        resolveRPointNames(currentSchedule.getRPoints(), rpointNames -> {
                            rpointList.clear();
                            rpointList.addAll(rpointNames);
                            adapter.setRPointDetails(currentSchedule.getRPoints());
                            adapter.setCurrentRPointIndex(currentRPointIndex);
                            adapter.notifyDataSetChanged();
                        });
                    }
                });
    }
    private int findInitialCurrentRPointIndex(Schedule schedule) {
        if (schedule == null || schedule.getRPoints() == null || schedule.getRPoints().isEmpty()) {
            return -1;
        }
        return schedule.getCurrentRPointIndex();
    }
    private void resolveRPointNames(List<Schedule.RPointDetail> rpoints, Consumer<List<String>> callback) {
        String[] rpointNamesArray = new String[rpoints.size()];
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < rpoints.size(); i++) {
            int currentIndex = i;
            Schedule.RPointDetail rpoint = rpoints.get(i);
            new RoutePoint().getRPointNameById(rpoint.getRPointId(), new RoutePoint.RPointCallback() {
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
//        currentSchedule.setTripStartTime(System.currentTimeMillis());
        currentSchedule.setStatus("in_progress");

        if (!"event".equalsIgnoreCase(currentSchedule.getType())) {
            currentRPointIndex = 0;
            currentSchedule.setCurrentRPointIndex(currentRPointIndex);

            if (!currentSchedule.getRPoints().isEmpty()) {
                Schedule.RPointDetail firstRPoint = currentSchedule.getRPoints().get(0);
                firstRPoint.setActTime(System.currentTimeMillis());
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

        // Only start geofencing if NOT event type
        if (!"event".equalsIgnoreCase(currentSchedule.getType())) {
            startGeofencing();
            startLocationUpdatesForProximity();
        }
    }

    @SuppressLint("SetTextI18n")
    private void restoreUIState() {
        String buttonState = appStateManager.getButtonState();
        long timerEnd = appStateManager.getTimerEndTime();
        boolean isTracking = appStateManager.isTracking();

        btnStart.setVisibility(View.GONE);
        btnRest.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);

        switch (buttonState) {
            case "start":
                controlGroup.setVisibility(View.VISIBLE);
                btnRest.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);
                break;

            case "continue":
                controlGroup.setVisibility(View.VISIBLE);
                btnRest.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);
                break;

            case "rest":
                controlGroup.setVisibility(View.VISIBLE);
                btnContinue.setVisibility(View.VISIBLE);
                tvTimer.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.VISIBLE);

                long remainingTime = timerEnd - System.currentTimeMillis();
                if (remainingTime > 0) {
                    timerController.startCountdown(remainingTime);
                } else {
                    tvTimer.setText("00:00");
                }
                break;

            default:
                btnStart.setVisibility(View.VISIBLE);
        }
        if (isTracking) {
            startService(new Intent(this, TrackingService.class));

            // Restart geofencing and proximity checks
            if (currentSchedule != null && currentRPointIndex != -1) {
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
                // Try to start geofencing and location updates if not already started
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
                    mapController.drawRouteFromPoints(locations, getString(R.string.directions_api_key));
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(ScheduleDetailsActivity.this, "Error getting locations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    // TimerController callbacks
    @Override
    public void onTimerTick(String formattedTime) {
        int color = ContextCompat.getColor(this, R.color.colorError);
        tvTimer.setTextColor(color);
        tvTimer.setText(formattedTime);
    }
    @Override
    public void onTimerFinish() {
        tvTimer.setText("00:00");
    }
    @Override
    public void onResume() {
        super.onResume();
        hideTabLayout();
    }
    @Override
    public void onPause() {
        super.onPause();
        showTabLayout();
    }
    private void hideTabLayout() {
        if (getParent() instanceof MainDrvActivity) {
            MainDrvActivity activity = (MainDrvActivity) getParent();
            TabLayout tabLayout = activity.getTabLayout();
            if (tabLayout != null) tabLayout.setVisibility(View.GONE);
        }
    }
    private void showTabLayout() {
        if (getParent() instanceof MainDrvActivity) {
            MainDrvActivity activity = (MainDrvActivity)  getParent();
            TabLayout tabLayout = activity.getTabLayout();
            if (tabLayout != null) tabLayout.setVisibility(View.VISIBLE);
        }
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
                        triggeredGeofenceId.equals(currentSchedule.getRPoints().get(currentRPointIndex).getRPointId())) {
                    Log.d("GeofenceReceiver", "Entered geofence for route point: " + rpointName);
                }
            }
        }
    }
}