package com.example.trackutem.view.Student;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.model.Schedule;
import com.example.trackutem.service.DirectionsService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScheduleMapStuActivity extends AppCompatActivity implements OnMapReadyCallback, RPointsTimelineStuAdapter.OnItemClickListener {
    private GoogleMap mMap;
    private String scheduleId, driverId, routeId, studentId;
    private SharedPreferences prefs;
    private MapController mapController;
    private Retrofit retrofit;
    private DirectionsService directionsService;
    private Marker busLocationMarker;
    private List<LatLng> rpointLocations = new ArrayList<>();
    private MaterialCardView bottomSheet;
    private BottomSheetBehavior<MaterialCardView> behavior;
    private RecyclerView rvRoutePoints;
    private RPointsTimelineStuAdapter adapter;
    private TextView tvQueuePrompt;
    private Button btnQueueMe;
    private Button btnCancelQueue;
    private String selectedRPointIdToQueue;
     private String selectedRPointNameToQueue;
    private boolean isQueued = false;
    private List<Schedule.RPointDetail> currentRPoints;
    private List<String> currentRPointNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_map_stu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);
        driverId = getIntent().getStringExtra("driverId");
        scheduleId = getIntent().getStringExtra("scheduleId");

        bottomSheet = findViewById(R.id.bottomSheetRoutePoints);
        behavior = BottomSheetBehavior.from(bottomSheet);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int peekHeight = (int) (metrics.heightPixels * 0.25); // 25% of screen
        int halfHeight = (int) (metrics.heightPixels * 0.55); // 55% of screen
        behavior.setPeekHeight(peekHeight, true);
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // No-op
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // No-op
            }
        });

        rvRoutePoints = findViewById(R.id.rvRoutePoints);
        rvRoutePoints.setLayoutManager(new LinearLayoutManager(this));

        tvQueuePrompt = findViewById(R.id.tvQueuePrompt);
        btnQueueMe = findViewById(R.id.btnQueueMeUp);
        btnCancelQueue = findViewById(R.id.btnCancelQueue);
        selectedRPointIdToQueue = getIntent().getStringExtra("fromRPointId");
        if (selectedRPointIdToQueue != null) {
            new RoutePoint().getRPointNameById(selectedRPointIdToQueue, new RoutePoint.RPointCallback() {
                @Override
                public void onSuccess(String rpointName) {
                    selectedRPointNameToQueue = rpointName;
                    tvQueuePrompt.setText("Queue at " + rpointName);
                    checkIfAlreadyQueued();
                }
                @Override
                public void onError(Exception e) {
                    tvQueuePrompt.setText("Queue at selected stop");
                    isQueued = false;
                    updateQueueButtonsVisibility();
                    updateRPointsAdapter();
                }
            });
        } else {
            checkIfAlreadyQueued();
        }

        btnQueueMe.setOnClickListener(v -> {
            if (selectedRPointIdToQueue != null && studentId != null) {
                Schedule schedule = new Schedule();
                schedule.setScheduleId(scheduleId);
                schedule.addStuToQueue(selectedRPointIdToQueue, studentId);
                Toast.makeText(this, "Queued at " + selectedRPointNameToQueue, Toast.LENGTH_SHORT).show();

                isQueued = true;
                updateQueueButtonsVisibility();
                updateRPointsAdapter();
            } else {
                Toast.makeText(this, "Please select a route point to queue.", Toast.LENGTH_SHORT).show();
            }
        });
        btnCancelQueue.setOnClickListener(v -> {
            if (selectedRPointIdToQueue != null && studentId != null) {
                Schedule schedule = new Schedule();
                schedule.setScheduleId(scheduleId);
                schedule.removeStuFromQueue(selectedRPointIdToQueue, studentId);
                Toast.makeText(this, "Queue cancelled for " + selectedRPointNameToQueue, Toast.LENGTH_SHORT).show();

                isQueued = false;
                updateQueueButtonsVisibility();
                updateRPointsAdapter();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize Retrofit for Directions API
        retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        directionsService = retrofit.create(DirectionsService.class);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapController = new MapController(this, mMap, fusedLocationClient);
        mapController.initializeMapFeatures();

        fetchScheduleDetails();
        fetchDriverLocation();
    }
    private void fetchScheduleDetails() {
        FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("ScheduleMapStuActivity", "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        routeId = documentSnapshot.getString("routeId");
                        Schedule schedule = documentSnapshot.toObject(Schedule.class);
                        if (schedule != null && schedule.getRPoints() != null) {
                            currentRPoints = schedule.getRPoints();
                            resolveRPointNames(currentRPoints, rpointNames -> {
                                currentRPointNames = rpointNames;
                                updateRPointsAdapter();
                            });
                            if (routeId != null) {
                                fetchRoutePoints(routeId);
                            }
                        } else {
                            Log.d("ScheduleMapStuActivity", "Schedule or RPoints are null/empty.");
                            currentRPoints = new ArrayList<>();
                            currentRPointNames = new ArrayList<>();
                            updateRPointsAdapter();
                        }
                    } else {
                        Log.d("ScheduleMapStuActivity", "Current data: null");
                        currentRPoints = new ArrayList<>();
                        currentRPointNames = new ArrayList<>();
                        updateRPointsAdapter();
                    }
                });
    }
    private void resolveRPointNames(List<Schedule.RPointDetail> rpoints, java.util.function.Consumer<List<String>> callback) {
        String[] rpointNamesArray = new String[rpoints.size()];
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < rpoints.size(); i++) {
            final int idx = i;
            new RoutePoint().getRPointNameById(rpoints.get(i).getRPointId(), new RoutePoint.RPointCallback() {
                @Override
                public void onSuccess(String rpointName) {
                    rpointNamesArray[idx] = rpointName;
                    if (counter.incrementAndGet() == rpoints.size()) {
                        callback.accept(java.util.Arrays.asList(rpointNamesArray));
                    }
                }
                @Override
                public void onError(Exception e) {
                    rpointNamesArray[idx] = "Unknown";
                    if (counter.incrementAndGet() == rpoints.size()) {
                        callback.accept(java.util.Arrays.asList(rpointNamesArray));
                    }
                }
            });
        }
    }
    private void fetchRoutePoints(String routeId) {
        Route.getRPointLocations(routeId, new Route.OnRPointsLocationResolvedListener() {
            @Override
            public void onRPointsLocationResolved(List<LatLng> locations) {
                rpointLocations = locations;
                mapController.addRouteMarkers(locations);
                mapController.drawRouteFromPoints(locations, getString(R.string.directions_api_key));
            }
            @Override
            public void onError(Exception e) {
                // Handle error
            }
        });
    }
    private void fetchDriverLocation() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference driverRef = db.collection("drivers").document(driverId);
        driverRef.addSnapshotListener((snapshot, error) -> {
            if (snapshot != null && snapshot.exists()) {
                String currentScheduleId = snapshot.getString("currentScheduleId");
                String status = snapshot.getString("status");

                if ("on_duty".equals(status) && scheduleId.equals(currentScheduleId)) {
                    GeoPoint geoPoint = snapshot.getGeoPoint("currentLocation");
                    if (geoPoint != null) {
                        LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                        if (busLocationMarker != null) {
                            busLocationMarker.remove();
                        }

                        busLocationMarker = mMap.addMarker(
                                new MarkerOptions().position(latLng).title("Bus Location").icon(mapController.getBusLocationIcon()));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                    }
                } else {
                    tvQueuePrompt.setText("Bus not started yet!");
                    isQueued = false;
                    updateQueueButtonsVisibility();
                    updateRPointsAdapter();
                    if (busLocationMarker != null) {
                        busLocationMarker.remove();
                        busLocationMarker = null;
                    }
                }
            }
        });
    }
    private void checkIfAlreadyQueued() {
        if (studentId == null) { // Handle case where studentId is null
            isQueued = false;
            updateQueueButtonsVisibility();
            updateRPointsAdapter();
            return;
        }
        FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Schedule schedule = documentSnapshot.toObject(Schedule.class);
                        if (schedule != null && schedule.getRPoints() != null) {
                            boolean foundInQueue = false;
                            for (Schedule.RPointDetail rPointDetail : schedule.getRPoints()) {
                                if (rPointDetail.getQueuelist() != null && rPointDetail.getQueuelist().contains(studentId)) {
                                    foundInQueue = true;
                                    selectedRPointIdToQueue = rPointDetail.getRPointId();
                                    new RoutePoint().getRPointNameById(selectedRPointIdToQueue, new RoutePoint.RPointCallback() {
                                        @Override
                                        public void onSuccess(String rpointName) {
                                            selectedRPointNameToQueue = rpointName;
                                            tvQueuePrompt.setText("You are queued at " + rpointName);
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            tvQueuePrompt.setText("You are queued at an unknown stop.");
                                        }
                                    });
                                    break;
                                }
                            }
                            isQueued = foundInQueue;
                        } else {
                            // If schedule has no rpoints
                            tvQueuePrompt.setText("No route points available for this schedule.");
                            isQueued = false;
                        }
                    } else {
                        // If schedule document does not exist
                        tvQueuePrompt.setText("Schedule not found.");
                        isQueued = false;
                    }
                    updateQueueButtonsVisibility();
                    updateRPointsAdapter();
                })
                .addOnFailureListener(e -> {
                    Log.e("ScheduleMapStuActivity", "Error checking queue status", e);
                    tvQueuePrompt.setText("Error loading schedule details.");
                    isQueued = false;
                    updateQueueButtonsVisibility();
                    updateRPointsAdapter();
                });
    }
    private void updateRPointsAdapter() {
        if (currentRPoints != null && currentRPointNames != null) {
            adapter = new RPointsTimelineStuAdapter(currentRPoints, currentRPointNames, this, !isQueued);
            rvRoutePoints.setAdapter(adapter);
        }
    }
    private void updateQueueButtonsVisibility() {
        if (isQueued) {
            btnQueueMe.setVisibility(View.GONE);
            btnCancelQueue.setVisibility(View.VISIBLE);
        } else {
            if (selectedRPointIdToQueue != null) {
                btnQueueMe.setVisibility(View.VISIBLE);
            } else {
                btnQueueMe.setVisibility(View.GONE);
            }
            btnCancelQueue.setVisibility(View.GONE);
        }
    }
    @Override
    public void onItemClick(String rpointId, String rpointName) {
        if (!isQueued) {
            this.selectedRPointIdToQueue = rpointId;
            this.selectedRPointNameToQueue = rpointName;
            tvQueuePrompt.setText("Queue at " + rpointName);
            updateQueueButtonsVisibility();
        } else {
            Toast.makeText(this, "You are already queued. Please cancel your current queue first.", Toast.LENGTH_SHORT).show();
        }

    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}