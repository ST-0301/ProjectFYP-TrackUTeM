// ScheduleDetailsFragment.java
package com.example.trackutem.view;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.MainDrvActivity;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.controller.RestLogController;
import com.example.trackutem.controller.ScheduleController;
import com.example.trackutem.controller.TimerController;
import com.example.trackutem.model.DirectionsResponse;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.Stop;
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
import com.google.maps.android.PolyUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScheduleDetailsFragment extends Fragment implements TimerController.TimerCallback {
    private static final long REST_DURATION_MILLIS = 2 * 60 * 1000;
    private static final long FIVE_MINUTES_WARNING = 1 * 60 * 1000;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private MaterialToolbar toolbar;
    private RecyclerView rvStops;
    private List<String> stopList = new ArrayList<>();
    private StopsTimelineAdapter adapter;
    private LinearLayout controlGroup;
    private Button btnStart, btnRest, btnContinue, btnStop;
    private TextView tvTimer;
    private SharedPreferences prefs;
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
    private List<LatLng> stopLocations = new ArrayList<>();
    private int currentStopIndex = -1;
    private Schedule currentSchedule;
    private static final float GEOFENCE_RADIUS_METERS = 5f;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;
    private GeofenceBroadcastReceiver geofenceReceiver;
    public static final String GEOFENCE_BROADCAST_ACTION = "com.example.trackutem.GEOFENCE_TRANSITION";
    private LocationCallback locationCallback; // For more frequent location updates
    private boolean requestingLocationUpdates = false;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_details, container, false);

        // Retrieve arguments
        Bundle args = getArguments();
        if (args != null) {
            routeId = args.getString("routeId");
            scheduleId = args.getString("scheduleId");
        } else {
            Toast.makeText(requireContext(), "Route details not found.", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }

        // find views
        btnStart   = view.findViewById(R.id.btnStart);
        btnRest    = view.findViewById(R.id.btnRest);
        btnContinue= view.findViewById(R.id.btnContinue);
        btnStop    = view.findViewById(R.id.btnStop);
        tvTimer    = view.findViewById(R.id.tvTimer);
        controlGroup = view.findViewById(R.id.controlGroup);
        rvStops    = view.findViewById(R.id.rvStops);

        // RecyclerView setup
        rvStops.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StopsTimelineAdapter(requireContext(), stopList);
        rvStops.setAdapter(adapter);

        // prefs/controllers
        prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        driverId = prefs.getString("driverId", null);
        appStateManager = new AppStateManager(requireContext());
        restLogController = new RestLogController();
        timerController = new TimerController(this, new NotificationHelper(requireContext()));
        notificationHelper = new NotificationHelper(requireContext());

        // Initialize GeofencingClient
        geofencingClient = LocationServices.getGeofencingClient(requireActivity());
        geofenceReceiver = new GeofenceBroadcastReceiver();

        restoreUIState();

        btnStart.setOnClickListener(v -> {
            View root = view.findViewById(R.id.rootContainer);
            if (root instanceof ViewGroup) {
                TransitionManager.beginDelayedTransition((ViewGroup) root);
            }

            btnStart.setVisibility(View.GONE);
            controlGroup.setVisibility(View.VISIBLE);
            btnRest.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);

            // Initialize rest log & Record start working time
            restLogController.createTodayRestLog(driverId, ref -> {});

            requireActivity().startService(new Intent(requireActivity(), TrackingService.class));
            appStateManager.saveState("start", 0, true);

            startTrip();
        });

        btnRest.setOnClickListener(v -> {
            btnRest.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);
            tvTimer.setVisibility(View.VISIBLE);

            // Increase rest count
            restLogController.incrementRestCount(driverId);

            // Stop tracking
            requireActivity().stopService(new Intent(requireActivity(), TrackingService.class));
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
            requireActivity().startService(new Intent(requireActivity(), TrackingService.class));
            startGeofencing();
            startLocationUpdatesForProximity();

            // Stop timer
            timerController.stopCountdown();

            appStateManager.saveState("continue", 0, true);
        });

        btnStop.setOnClickListener(v -> endTrip());

        // Fetch schedule with stop details
        fetchScheduleWithStopDetails(scheduleId);

        setHasOptionsMenu(true);
        return view;
    }
    private void endTrip() {
        // Record end working time
        restLogController.updateEndWorkTime(driverId);

        requireActivity().stopService(new Intent(requireActivity(), TrackingService.class));
        timerController.stopCountdown();
        appStateManager.clear();

        // Stop geofencing when trip ends
        stopGeofencing();
        stopLocationUpdatesForProximity();

        btnStart.setVisibility(View.VISIBLE);
        btnRest.setVisibility(View.GONE);
        btnContinue.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup toolbar
        MainDrvActivity activity = (MainDrvActivity) getActivity();
        if (activity != null) {
            activity.updateToolbar("Details", true);
        }

        // init the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        checkPermissions();

        adapter.setOnActionButtonClickListener((position, isLastStop) -> {
            if (currentSchedule != null && position == currentStopIndex) {
                ScheduleController.recordArrivalAndNextDeparture(currentSchedule, position);

                // Update UI
                currentStopIndex = currentSchedule.getCurrentStopIndex();
                adapter.setCurrentStopIndex(currentStopIndex);
                adapter.notifyDataSetChanged();

                // If this was the last stop, end the trip
                if (isLastStop) {
                    endTrip();
                    Toast.makeText(requireContext(), "Route completed!", Toast.LENGTH_SHORT).show();
                }
                // Prepare for next stop if not completed
                else if (currentStopIndex != -1) {
                    startGeofencing();
                }
            }
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
        Intent intent = new Intent(requireContext(), TrackingService.class);
        intent.setAction(GEOFENCE_BROADCAST_ACTION);
        geofencePendingIntent = PendingIntent.getService(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0));
        return geofencePendingIntent;
    }
    private void startGeofencing() {
        if (currentSchedule == null || currentSchedule.getStops() == null || currentSchedule.getStops().isEmpty()) {
            Log.d("Geofence", "No schedule or stops to set geofences for.");
            return;
        }
        stopGeofencing();
        if (currentStopIndex == -1 || currentStopIndex >= currentSchedule.getStops().size()) {
            Log.d("Geofence", "No more pending stops for geofencing.");
            return;
        }
        Schedule.StopDetail nextStopToMonitor = currentSchedule.getStops().get(currentStopIndex);
        String stopId = nextStopToMonitor.getStopId();
        new Stop().getStopLocationById(stopId, new Stop.StopLocationCallback() {
            @Override
            public void onSuccess(LatLng location) {
                Geofence geofence = new Geofence.Builder()
                        .setRequestId(stopId)
                        .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS_METERS)
                        .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build();
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    geofencingClient.addGeofences(getGeofencingRequest(geofence), getGeofencePendingIntent())
                            .addOnSuccessListener(aVoid -> Log.d("Geofence", "Geofence added for stop: " + stopId))
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
                Log.e("Geofence", "Error getting stop location for geofence: " + e.getMessage());
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("LocationProximity", "Location permission not granted. Cannot start proximity updates.");
            return;
        }
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(3000)       // Update every 3 seconds
                .setFastestInterval(1000) // Smallest interval for updates
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(requireContext())
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
                            resolvable.startResolutionForResult(requireActivity(), 1002);
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
                            checkProximityToNextStop(location);
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

    private void checkProximityToNextStop(Location currentLocation) {
        if (currentSchedule == null || currentStopIndex < 0 || currentStopIndex >= currentSchedule.getStops().size()) {
            return;
        }
        Schedule.StopDetail nextStop = currentSchedule.getStops().get(currentStopIndex);
        new Stop().getStopLocationById(nextStop.getStopId(), new Stop.StopLocationCallback() {
            @Override
            public void onSuccess(LatLng stopLatLng) {
                float[] results = new float[1];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), stopLatLng.latitude, stopLatLng.longitude, results);
                float distanceInMeters = results[0];
                Log.d("ProximityCheck", "Distance to " + stopList.get(currentStopIndex) + ": " + distanceInMeters + " meters");

                if (distanceInMeters <= 5) {
                    long arrivalTime = System.currentTimeMillis();
                    nextStop.setActualArrivalTime(arrivalTime);
                    nextStop.setStatus("arrived");

                    // Calculate lateness
                    int lateness = ScheduleController.calculateLateness(
                            nextStop.getExpectedArrivalTime(),
                            arrivalTime,
                            currentSchedule.getTripStartTime()
                    );
                    nextStop.setLatenessMinutes(lateness);

                    // Record next stop departure if exists
                    int nextIndex = currentStopIndex + 1;
                    if (nextIndex < currentSchedule.getStops().size()) {
                        Schedule.StopDetail nextStopDetail = currentSchedule.getStops().get(nextIndex);
                        nextStopDetail.setActualDepartureTime(System.currentTimeMillis());
                        nextStopDetail.setStatus("departed");
                        currentSchedule.setCurrentStopIndex(nextIndex);
                    } else {
                        currentSchedule.setCurrentStopIndex(-1);
                        currentSchedule.setStatus("completed");
                        currentSchedule.setTripEndTime(System.currentTimeMillis());
                    }

                    // Update Firestore
                    updateScheduleInFirestore();

                    // Update UI
                    currentStopIndex = currentSchedule.getCurrentStopIndex();
                    adapter.setCurrentStopIndex(currentStopIndex);
                    adapter.notifyDataSetChanged();

                    // Prepare for next stop or finish
                    if (currentStopIndex != -1) {
                        startGeofencing();
                    } else {
                        stopGeofencing();
                        stopLocationUpdatesForProximity();
                        endTrip();
                        Toast.makeText(requireContext(), "Route completed!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e("ProximityCheck", "Error getting stop location for proximity check: " + e.getMessage());
            }
        });
    }
    private void updateScheduleInFirestore() {
        FirebaseFirestore.getInstance()
                .collection("schedules")
                .document(currentSchedule.getScheduleId())
                .set(currentSchedule);
    }
    private void fetchScheduleWithStopDetails(String scheduleId) {
        FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentSchedule = documentSnapshot.toObject(Schedule.class);
                    if (currentSchedule != null) {
                        currentStopIndex = currentSchedule.getCurrentStopIndex(); // Assuming Schedule class has this field
                        if (currentStopIndex == -1) { // If not explicitly set or completed
                            currentStopIndex = findInitialCurrentStopIndex(currentSchedule);
                        }

                        resolveStopNames(currentSchedule.getStops(), stopNames -> {
                            stopList.clear();
                            stopList.addAll(stopNames);
                            adapter.setStopDetails(currentSchedule.getStops());
                            adapter.setCurrentStopIndex(currentStopIndex);
                            adapter.notifyDataSetChanged();
                        });
                    }
                });
    }
    private int findInitialCurrentStopIndex(Schedule schedule) {
        if (schedule == null || schedule.getStops() == null || schedule.getStops().isEmpty()) {
            return -1;
        }
         return schedule.getCurrentStopIndex(); // Assuming Schedule object stores this
    }
    private void resolveStopNames(List<Schedule.StopDetail> stops, Consumer<List<String>> callback) {
        String[] stopNamesArray = new String[stops.size()];
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < stops.size(); i++) {
            int currentIndex = i;
            Schedule.StopDetail stop = stops.get(i);
            new Stop().getStopNameById(stop.getStopId(), new Stop.StopCallback() {
                @Override
                public void onSuccess(String name) {
                    stopNamesArray[currentIndex] = name;
                    if (counter.incrementAndGet() == stops.size()) {
                        List<String> orderedNames = new ArrayList<>(Arrays.asList(stopNamesArray));
                        callback.accept(orderedNames);
                    }
                }
                @Override
                public void onError(Exception e) {
                    stopNamesArray[currentIndex] = "Unknown Stop";
                    if (counter.incrementAndGet() == stops.size()) {
                        List<String> orderedNames = new ArrayList<>(Arrays.asList(stopNamesArray));
                        callback.accept(orderedNames);
                    }
                }
            });
        }
    }
    private void startTrip() {
        currentSchedule.setTripStartTime(System.currentTimeMillis());
        currentSchedule.setStatus("in_progress");
        currentStopIndex = 0;
        currentSchedule.setCurrentStopIndex(currentStopIndex);

        // Record departure for first stop
        if (!currentSchedule.getStops().isEmpty()) {
            Schedule.StopDetail firstStop = currentSchedule.getStops().get(0);
            firstStop.setActualDepartureTime(System.currentTimeMillis());
            firstStop.setStatus("departed");
            currentSchedule.setCurrentStopIndex(0);
        }
        updateScheduleInFirestore();
        adapter.setCurrentStopIndex(currentStopIndex);
        adapter.notifyDataSetChanged();
        startGeofencing();
        startLocationUpdatesForProximity();
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
            requireActivity().startService(new Intent(requireActivity(), TrackingService.class));

            // Restart geofencing and proximity checks
            if (currentSchedule != null && currentStopIndex != -1) {
                startGeofencing();
                startLocationUpdatesForProximity();
            }
        }
    }

    // Permissions
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_LOCATION_PERMISSION + 1);
        }

        // Android 13+ notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
                        Toast.makeText(requireContext(), "Location permission required for tracking", Toast.LENGTH_LONG).show();
                    } else if (permissions[i].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        Toast.makeText(requireContext(), "Background permission is recommended for reliable geofencing.", Toast.LENGTH_LONG).show();
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
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mapController = new MapController(requireContext(), googleMap, fusedLocationClient);
                mapController.initializeMapFeatures();

                fetchAndDrawRoute();
                if (!stopLocations.isEmpty()) {
                    mapController.addRouteMarkers(stopLocations, stopList);
                    getRoutePathFromDirections(stopLocations);
                }
            });
        }
    }
    private void fetchAndDrawRoute() {
        Route.getRouteStopLocations(routeId, new Route.OnStopsLocationResolvedListener() {
            @Override
            public void onStopsLocationResolved(List<LatLng> locations) {
                stopLocations = locations;
                if (mapController != null && !locations.isEmpty()) {
                    mapController.addRouteMarkers(locations, stopList);
                    getRoutePathFromDirections(locations);
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error getting locations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getRoutePathFromDirections(List<LatLng> locations) {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if (locations.size() < 2) return;
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://maps.googleapis.com/maps/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            directionsService = retrofit.create(DirectionsService.class);
        }
        LatLng origin = locations.get(0);
        LatLng destination = locations.get(locations.size() - 1);
        List<LatLng> waypoints = locations.subList(1, locations.size() - 1);

        String originStr = origin.latitude + "," + origin.longitude;
        String destinationStr = destination.latitude + "," + destination.longitude;
        String waypointsStr = null;
        if (!waypoints.isEmpty()) {
            StringBuilder sb = new StringBuilder("optimize:true|");
            for (LatLng point : waypoints) {
                sb.append(point.latitude)
                        .append(",")
                        .append(point.longitude)
                        .append("|");
            }
            waypointsStr = sb.substring(0, sb.length() - 1);
        }

        String apiKey = getString(R.string.directions_api_key);
        directionsService.getDirections(originStr, destinationStr, waypointsStr, apiKey)
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            DirectionsResponse directionsResponse = response.body();
                            if (directionsResponse.routes != null &&
                                    !directionsResponse.routes.isEmpty() &&
                                    directionsResponse.routes.get(0).overviewPolyline != null) {

                                String polyline = directionsResponse.routes.get(0).overviewPolyline.points;
                                List<LatLng> path = PolyUtil.decode(polyline);

                                mapController.drawRoute(path);
                                mapController.zoomToRoute(path);
                            }
                        } else {
                            Toast.makeText(requireContext(), "Directions API error", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Toast.makeText(requireContext(), "Directions request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // TimerController callbacks
    @Override
    public void onTimerTick(String formattedTime) {
        int color = ContextCompat.getColor(requireContext(), R.color.colorError);
        tvTimer.setTextColor(color);
        tvTimer.setText(formattedTime);
    }
    @Override
    public void onTimerFinish() {
        tvTimer.setText("00:00");
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // no menu items here
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle Up arrow
        if (item.getItemId() == android.R.id.home) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onResume() {
        super.onResume();
        hideTabLayout();

        // Hide bottom navigation
        MainDrvActivity activity = (MainDrvActivity) getActivity();
        if (activity != null) {
            activity.hideBottomNav();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        showTabLayout();

        // Show bottom navigation
        MainDrvActivity activity = (MainDrvActivity) getActivity();
        if (activity != null) {
            activity.showBottomNav();
        }
    }
    private void hideTabLayout() {
        MainDrvActivity activity = (MainDrvActivity) getActivity();
        if (activity != null) {
            TabLayout tabLayout = activity.getTabLayout();
            if (tabLayout != null) {
                tabLayout.setVisibility(View.GONE);
            }
        }
    }
    private void showTabLayout() {
        MainDrvActivity activity = (MainDrvActivity) getActivity();
        if (activity != null) {
            TabLayout tabLayout = activity.getTabLayout();
            if (tabLayout != null) {
                tabLayout.setVisibility(View.VISIBLE);
            }
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(GEOFENCE_BROADCAST_ACTION);
        ContextCompat.registerReceiver(requireActivity(), geofenceReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        if (appStateManager.isTracking()) {
            startGeofencing();
            startLocationUpdatesForProximity();
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        requireActivity().unregisterReceiver(geofenceReceiver);
        stopGeofencing();
        stopLocationUpdatesForProximity();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopGeofencing();
        stopLocationUpdatesForProximity();
    }
    private class GeofenceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && GEOFENCE_BROADCAST_ACTION.equals(intent.getAction())) {
                int geofenceTransition = intent.getIntExtra("transitionType", -1);
                String triggeredGeofenceId = intent.getStringExtra("geofenceId");
                String stopName = intent.getStringExtra("stopName"); // Passed from TrackingService

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER &&
                        currentSchedule != null && currentStopIndex != -1 &&
                        currentStopIndex < currentSchedule.getStops().size() &&
                        triggeredGeofenceId.equals(currentSchedule.getStops().get(currentStopIndex).getStopId())) {
                    Log.d("GeofenceReceiver", "Entered geofence for stop: " + stopName);
                }
            }
        }
    }
}