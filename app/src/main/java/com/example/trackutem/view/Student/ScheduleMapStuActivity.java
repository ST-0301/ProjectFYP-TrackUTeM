package com.example.trackutem.view.Student;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.controller.ScheduleController;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScheduleMapStuActivity extends AppCompatActivity implements OnMapReadyCallback,
        RPointsTimelineStuAdapter.OnItemClickListener,
        RPointsTimelineStuAdapter.OnRPointLocationClickListener {
    private GoogleMap mMap;
    private AutoCompleteTextView dropdownBusSelector;
    private String scheduleId, busDriverPairId, routeId, studentId, currentSelectedBusPlate;
    private String type;
    private long scheduledDatetime;
    private Map<String, List<Schedule>> schedulesByPairId = new HashMap<>();
    private Map<String, String> pairIdToBusId = new HashMap<>();
    private Map<String, String> busIdToPlate = new HashMap<>();
    private Map<String, String> pairIdToPlate = new HashMap<>();
    private ListenerRegistration groupSchedulesListener;
    private ListenerRegistration driverLocationListener;
    private SharedPreferences prefs;
    private ScheduleController scheduleController;
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
    private Button btnQueueMe, btnCancelQueue;
    private String selectedRPointIdToQueue;
     private String selectedRPointNameToQueue;
    private boolean isQueued = false;
    private List<Schedule.RPointDetail> currentRPoints;
    private List<String> currentRPointNames;
    private List<String> busPlateNumbers = new ArrayList<>();
    private Map<String, String> busPlateToDriverPairId = new HashMap<>();
    private Map<String, String> driverPairIdToBusPlate = new HashMap<>();
    private boolean isFirstDropdownInit = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_map_stu);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);
        scheduleId = getIntent().getStringExtra("scheduleId");
        busDriverPairId = getIntent().getStringExtra("busDriverPairId");
        routeId = getIntent().getStringExtra("routeId");
        type = getIntent().getStringExtra("type");
        scheduledDatetime = getIntent().getLongExtra("scheduledDatetime", 0);
        String initialBusDriverPairId = getIntent().getStringExtra("initialBusDriverPairId");
        scheduleController = new ScheduleController();

        bottomSheet = findViewById(R.id.bottomSheetRoutePoints);
        behavior = BottomSheetBehavior.from(bottomSheet);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int peekHeight = (int) (metrics.heightPixels * 0.25); // 25% of screen
        behavior.setPeekHeight(peekHeight, true);
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
                    tvQueuePrompt.setText("Queue at " + rpointName + "?");
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

        btnQueueMe.setOnClickListener(v -> queueMe());
        btnCancelQueue.setOnClickListener(v -> cancelQueue());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize Retrofit for Directions API
        retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        directionsService = retrofit.create(DirectionsService.class);

        initializeBusSelector();
        setupGroupSchedulesListener(initialBusDriverPairId);
    }
    private void cancelQueue() {
        FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Schedule schedule = documentSnapshot.toObject(Schedule.class);
                        if (schedule != null && schedule.isQueueEnabled()) {
                            if (selectedRPointIdToQueue != null && studentId != null) {
                                Schedule scheduleToUpdate = new Schedule();
                                scheduleToUpdate.setScheduleId(scheduleId);
                                scheduleToUpdate.removeStuFromQueue(selectedRPointIdToQueue, studentId);
                                Toast.makeText(this, "Queue cancelled for " + selectedRPointNameToQueue, Toast.LENGTH_SHORT).show();
                                isQueued = false;
                                selectedRPointIdToQueue = null;
                                selectedRPointNameToQueue = null;

                                updateQueueButtonsVisibility();
                                updateRPointsAdapter();
                            }
                        } else {
                            Toast.makeText(this, "Queue is not enabled for this schedule.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking queue status.", Toast.LENGTH_SHORT).show();
                });
    }
    private void queueMe() {
        String studentId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("studentId", null);
        if (studentId == null || selectedRPointIdToQueue == null || scheduleId == null) {
            Toast.makeText(this, "Error: Missing required information.", Toast.LENGTH_SHORT).show();
            return;
        }

        String routeId = getIntent().getStringExtra("routeId");
        String type = getIntent().getStringExtra("type");
        Date scheduledDatetime = new Date(getIntent().getLongExtra("scheduledDatetime", -1));

        FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Schedule schedule = documentSnapshot.toObject(Schedule.class);
                        if (schedule != null && schedule.isQueueEnabled()) {
                            // Queue is enabled, now check if student is already queued in group
                            scheduleController.isStudentQueuedInGroup(studentId, routeId, type, scheduledDatetime, (isQueuedInGroup, message) -> {
                                if (isQueuedInGroup) {
                                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                                } else if (message != null) {
                                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                                } else {
                                    // Proceed with queuing - queue is enabled and student not queued elsewhere
                                    Schedule scheduleToUpdate = new Schedule();
                                    scheduleToUpdate.setScheduleId(scheduleId);
                                    scheduleToUpdate.addStuToQueue(selectedRPointIdToQueue, studentId,
                                            () -> {
                                                // Success callback
                                                Toast.makeText(this, "Queued at " + selectedRPointNameToQueue, Toast.LENGTH_SHORT).show();
                                                isQueued = true;
                                                updateQueueButtonsVisibility();
                                                updateRPointsAdapter();
                                            },
                                            e -> {
                                                // Failure callback
                                                Toast.makeText(this, "Failed to queue: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                    );
                                }
                            });
                        } else {
                            Toast.makeText(this, "Queue is not enabled for this schedule.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking queue status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void setupGroupSchedulesListener(String initialBusDriverPairId) {
        Timestamp scheduledTs = new Timestamp(new Date(scheduledDatetime));
        groupSchedulesListener = FirebaseFirestore.getInstance()
                .collection("schedules")
                .whereEqualTo("routeId", routeId)
                .whereEqualTo("type", type)
                .whereEqualTo("scheduledDatetime", scheduledTs)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.w("ScheduleMapStu", "Listen failed.", e);
                        return;
                    }
                    schedulesByPairId.clear();
                    Set<String> pairIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Schedule schedule = doc.toObject(Schedule.class);
                        String pairId = schedule.getBusDriverPairId();

                        if (!schedulesByPairId.containsKey(pairId)) {
                            schedulesByPairId.put(pairId, new ArrayList<>());
                        }
                        schedulesByPairId.get(pairId).add(schedule);
                        pairIds.add(pairId);
                    }
                    fetchBusDriverPairings(new ArrayList<>(pairIds), initialBusDriverPairId);
                });
    }
    private void fetchBusDriverPairings(List<String> pairIds, String initialBusDriverPairId) {
        if (pairIds.isEmpty()) return;
        List<List<String>> chunks = chunkList(pairIds, 10);
        AtomicInteger remainingChunks = new AtomicInteger(chunks.size());
        for (List<String> chunk : chunks) {
            FirebaseFirestore.getInstance()
                    .collection("busDriverPairings")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String pairId = doc.getId();
                            String busId = doc.getString("busId");
                            pairIdToBusId.put(pairId, busId);
                        }

                        if (remainingChunks.decrementAndGet() == 0) {
                            fetchBuses(new ArrayList<>(pairIdToBusId.values()), initialBusDriverPairId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ScheduleMapStu", "Error fetching bus driver pairings", e);
                    });
        }
    }
    private void fetchBuses(List<String> busIds, String initialBusDriverPairId) {
        if (busIds.isEmpty()) return;
        List<List<String>> chunks = chunkList(busIds, 10);
        AtomicInteger remainingChunks = new AtomicInteger(chunks.size());
        for (List<String> chunk : chunks) {
            FirebaseFirestore.getInstance()
                    .collection("buses")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String busId = doc.getId();
                            String plateNumber = doc.getString("plateNumber");
                            busIdToPlate.put(busId, plateNumber);
                        }
                        if (remainingChunks.decrementAndGet() == 0) {
                            for (Map.Entry<String, String> entry : pairIdToBusId.entrySet()) {
                                String pairId = entry.getKey();
                                String busId = entry.getValue();
                                String plate = busIdToPlate.getOrDefault(busId, "Unknown bus");
                                pairIdToPlate.put(pairId, plate);
                            }
                            updateBusDropdown(initialBusDriverPairId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ScheduleMapStu", "Error fetching buses", e);
                    });
        }
    }
    private void updateBusDropdown(String initialBusDriverPairId) {
        List<String> plateNumbers = new ArrayList<>(pairIdToPlate.values());
        Collections.sort(plateNumbers);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                plateNumbers
        );
        dropdownBusSelector.setAdapter(adapter);
        setDropdownHeight(adapter, dropdownBusSelector);
        if (isFirstDropdownInit && initialBusDriverPairId != null && pairIdToPlate.containsKey(initialBusDriverPairId)) {
            String initialPlate = pairIdToPlate.get(initialBusDriverPairId);
            dropdownBusSelector.setText(initialPlate, false);
            onBusSelected(initialBusDriverPairId);
            isFirstDropdownInit = false;
        } else {
            String currentPlate = pairIdToPlate.get(busDriverPairId);
            if (currentPlate != null) {
                dropdownBusSelector.setText(currentPlate, false);
            }
        }
        dropdownBusSelector.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPlate = (String) parent.getItemAtPosition(position);
            for (Map.Entry<String, String> entry : pairIdToPlate.entrySet()) {
                if (entry.getValue().equals(selectedPlate)) {
                    onBusSelected(entry.getKey());
                    break;
                }
            }
        });
    }
    private void setDropdownHeight(ArrayAdapter<String> adapter, AutoCompleteTextView dropdown) {
        int itemCount = adapter.getCount();
        int maxVisibleItems = 5;
        int itemHeight = 64;
        int desiredHeight = Math.min(itemCount, maxVisibleItems) * dpToPx(itemHeight);
        int maxHeight = dpToPx(320);
        dropdown.setDropDownHeight(Math.min(desiredHeight, maxHeight));
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
    private void onBusSelected(String pairId) {
        busDriverPairId = pairId;
        Schedule chosenSchedule = pickPrimarySchedule(schedulesByPairId.get(pairId));
        if (chosenSchedule != null) {
            scheduleId = chosenSchedule.getScheduleId();
            boolean isCompleted = "completed".equals(chosenSchedule.getStatus());
            if (chosenSchedule.getRPoints() != null) {
                currentRPoints = chosenSchedule.getRPoints();
                resolveRPointNames(currentRPoints, rpointNames -> {
                    currentRPointNames = rpointNames;
                    updateRPointsAdapter();
                    updateScheduleStatus(chosenSchedule);
                    if (!isCompleted) {
                        checkIfAlreadyQueued();
                    } else {
                        // Hide queue buttons for completed schedules
                        hideQueueButtons();
                    }
                });
            }
            showOnlySelectedBusOnMap(pairId);
            listenToSelectedDriverLocation(pairId);
        }
    }
    private void hideQueueButtons() {
        isQueued = false;
        btnQueueMe.setVisibility(View.GONE);
        btnCancelQueue.setVisibility(View.GONE);
        tvQueuePrompt.setText("Schedule completed - Queue not available");

        // Also disable clicking on route points in the adapter
        if (adapter != null) {
            adapter = new RPointsTimelineStuAdapter(currentRPoints, currentRPointNames, this, this, false);
            rvRoutePoints.setAdapter(adapter);
        }
    }
    private void updateScheduleStatus(Schedule schedule) {
        TextView tvScheduleStatus = findViewById(R.id.tvScheduleStatus);
        if (schedule == null) {
            tvScheduleStatus.setText("Unknown");
            tvScheduleStatus.setTextColor(getResources().getColor(R.color.textSecondary));
            return;
        }
        String status = schedule.getStatus();
        switch (status) {
            case "scheduled":
                tvScheduleStatus.setText("Upcoming");
                tvScheduleStatus.setTextColor(getResources().getColor(R.color.status_scheduled));
                break;
            case "in_progress":
                boolean hasDelay = false;
                if (schedule.getRPoints() != null) {
                    for (Schedule.RPointDetail rpoint : schedule.getRPoints()) {
                        if (rpoint.getLatenessMinutes() > 0) {
                            hasDelay = true;
                            break;
                        }
                    }
                }
                if (hasDelay) {
                    tvScheduleStatus.setText("Delayed");
                    tvScheduleStatus.setTextColor(getResources().getColor(R.color.status_cancelled_delayed));
                } else {
                    tvScheduleStatus.setText("On Route");
                    tvScheduleStatus.setTextColor(getResources().getColor(R.color.status_in_progress));
                }
                break;
            case "completed":
                tvScheduleStatus.setText("Finished");
                tvScheduleStatus.setTextColor(getResources().getColor(R.color.status_completed));
                hideQueueButtons();
                break;
            default:
                tvScheduleStatus.setText(status);
                tvScheduleStatus.setTextColor(getResources().getColor(R.color.textSecondary));
                break;
        }
    }
    private Schedule pickPrimarySchedule(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) return null;
        schedules.sort((s1, s2) -> {
            int rank1 = rankStatus(s1.getStatus());
            int rank2 = rankStatus(s2.getStatus());
            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }
            return s1.getScheduledDatetime().compareTo(s2.getScheduledDatetime());
        });
        return schedules.get(0);
    }
    private int rankStatus(String status) {
        if ("in_progress".equals(status)) return 0;
        if ("scheduled".equals(status)) return 1;
        return 2;
    }
    private void showOnlySelectedBusOnMap(String pairId) {
        if (busLocationMarker != null) {
            busLocationMarker.remove();
            busLocationMarker = null;
        }
    }
    private void listenToSelectedDriverLocation(String pairId) {
        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }
        FirebaseFirestore.getInstance()
                .collection("busDriverPairings")
                .document(pairId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String driverId = documentSnapshot.getString("driverId");
                        if (driverId != null) {
                            setupDriverLocationListener(driverId);
                        }
                    }
                });
    }
    private void setupDriverLocationListener(String driverId) {
        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }

        driverLocationListener = FirebaseFirestore.getInstance()
                .collection("drivers")
                .document(driverId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("ScheduleMapStu", "Listen driver location failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String currentScheduleId = documentSnapshot.getString("currentScheduleId");
                        String status = documentSnapshot.getString("status");

                        if ("on_duty".equals(status) && scheduleId.equals(currentScheduleId)) {
                            GeoPoint geoPoint = documentSnapshot.getGeoPoint("currentLocation");
                            if (geoPoint != null) {
                                LatLng latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                                if (busLocationMarker != null) {
                                    busLocationMarker.remove();
                                }

                                String markerTitle = "Bus Location";
                                if (currentSelectedBusPlate != null) {
                                    markerTitle = currentSelectedBusPlate + " - Bus Location";
                                }

                                busLocationMarker = mMap.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(markerTitle)
                                        .icon(mapController.getBusLocationIcon()));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

                                FirebaseFirestore.getInstance().collection("schedules")
                                        .document(scheduleId)
                                        .get()
                                        .addOnSuccessListener(scheduleDoc -> {
                                            if (scheduleDoc.exists()) {
                                                Schedule schedule = scheduleDoc.toObject(Schedule.class);
                                                updateScheduleStatus(schedule);
                                            }
                                        });
                            }
                        } else {
                            if (busLocationMarker != null) {
                                busLocationMarker.remove();
                                busLocationMarker = null;
                            }
                        }
                    }
                });
    }
    private <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(list.size(), i + chunkSize)));
        }
        return chunks;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (groupSchedulesListener != null) {
            groupSchedulesListener.remove();
        }
        if (driverLocationListener != null) {
            driverLocationListener.remove();
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapController = new MapController(this, mMap, fusedLocationClient) {
            @Override
            public void initializeMapFeatures() {
                setMapStyle();
                setupMapClickListener();
            }
        };
        mapController.initializeMapFeatures();
        mapController.enableBasicLocationFeatures();

        fetchScheduleDetails();
    }
    private void initializeBusSelector() {
        dropdownBusSelector = findViewById(R.id.dropdownBusSelector);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                busPlateNumbers
        );
        dropdownBusSelector.setHint("Select Bus");
        dropdownBusSelector.setAdapter(adapter);
        setDropdownHeight(adapter, dropdownBusSelector);

        dropdownBusSelector.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPlate = (String) parent.getItemAtPosition(position);
            currentSelectedBusPlate = selectedPlate;
            String selectedBusDriverPairId = busPlateToDriverPairId.get(selectedPlate);
            dropdownBusSelector.setHint("");

            if (selectedBusDriverPairId != null && !selectedBusDriverPairId.equals(busDriverPairId)) {
                busDriverPairId = selectedBusDriverPairId;
                onBusSelected(selectedBusDriverPairId);
            }
        });
        dropdownBusSelector.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && dropdownBusSelector.getText().toString().isEmpty()) {
                dropdownBusSelector.setHint("Select Bus");
            }
        });
    }
    private void fetchBusPlateNumbers() {
        busPlateNumbers.clear();
        busPlateToDriverPairId.clear();
        driverPairIdToBusPlate.clear();
        Set<String> processedPairIds = new HashSet<>();
        for (String pairId : schedulesByPairId.keySet()) {
            if (processedPairIds.contains(pairId)) {
                continue;
            }
            processedPairIds.add(pairId);
            FirebaseFirestore.getInstance().collection("busDriverPairings")
                    .document(pairId)
                    .get()
                    .addOnSuccessListener(pairingDoc -> {
                        if (pairingDoc.exists()) {
                            String busId = pairingDoc.getString("busId");
                            if (busId != null) {
                                FirebaseFirestore.getInstance().collection("buses")
                                        .document(busId)
                                        .get()
                                        .addOnSuccessListener(busDoc -> {
                                            if (busDoc.exists()) {
                                                String plateNumber = busDoc.getString("plateNumber");
                                                if (plateNumber != null) {
                                                    busPlateNumbers.add(plateNumber);
                                                    busPlateToDriverPairId.put(plateNumber, pairId);
                                                    driverPairIdToBusPlate.put(pairId, plateNumber);
                                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                                            ScheduleMapStuActivity.this,
                                                            android.R.layout.simple_dropdown_item_1line,
                                                            busPlateNumbers
                                                    );
                                                    dropdownBusSelector.setAdapter(adapter);
                                                    setDropdownHeight(adapter, dropdownBusSelector);
                                                    if (pairId.equals(busDriverPairId)) {
                                                        currentSelectedBusPlate = plateNumber;
                                                        dropdownBusSelector.setText(plateNumber, false);
                                                        dropdownBusSelector.setHint("");
                                                    }
                                                }
                                            }
                                        });
                            }
                        }
                    });
        }
    }
    @Override
    public void onRPointLocationClick(String rpointId, String rpointName) {
        new RoutePoint().getRPointLocationById(rpointId, new RoutePoint.RPointLocationCallback() {
            @Override
            public void onSuccess(LatLng location) {
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));
                    if (busLocationMarker != null) {
                        busLocationMarker.remove();
                    }
                    busLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(location)
                            .title(rpointName)
                            .icon(mapController.getBusStopIcon()));
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e("ScheduleMapStu", "Error getting route point location: " + e.getMessage());
                Toast.makeText(ScheduleMapStuActivity.this, "Could not find location for " + rpointName, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void fetchScheduleDetails() {
        if (scheduleId == null || scheduleId.isEmpty()) {
            Log.e("ScheduleMap", "Schedule ID is null or empty");
            return;
        }
        FirebaseFirestore.getInstance().collection("schedules").document(scheduleId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("ScheduleMapStuActivity", "Listen failed.", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        routeId = documentSnapshot.getString("routeId");
                        busDriverPairId = documentSnapshot.getString("busDriverPairId");
                        Schedule schedule = documentSnapshot.toObject(Schedule.class);
                        updateScheduleStatus(schedule);
                        if ("completed".equals(schedule.getStatus())) {
                            hideQueueButtons();
                            return;
                        }
                        if (schedule != null && schedule.getRPoints() != null) {
                            currentRPoints = schedule.getRPoints();
                            resolveRPointNames(currentRPoints, rpointNames -> {
                                currentRPointNames = rpointNames;
                                checkIfInitialSelectedRPointIsArrivedAndAdjust();
                                checkIfAlreadyQueued();
                            });
                            if (routeId != null) {
                                fetchRoutePoints(routeId);
                                fetchBusPlateNumbers();
                            }
                        } else {
                            Log.d("ScheduleMapStuActivity", "Schedule or RPoints are null/empty.");
                            currentRPoints = new ArrayList<>();
                            currentRPointNames = new ArrayList<>();
                            checkIfAlreadyQueued();
                        }
                    } else {
                        Log.d("ScheduleMapStuActivity", "Current data: null");
                        currentRPoints = new ArrayList<>();
                        currentRPointNames = new ArrayList<>();
                        checkIfAlreadyQueued();
                    }
                });
    }
    private void resolveRPointNames(List<Schedule.RPointDetail> rpoints, Consumer<List<String>> callback) {
        String[] rpointNamesArray = new String[rpoints.size()];
        AtomicInteger counter = new AtomicInteger(0);
        if (rpoints.isEmpty()) {
            callback.accept(new ArrayList<>());
            return;
        }
        for (int i = 0; i < rpoints.size(); i++) {
            final int idx = i;
            new RoutePoint().getRPointNameById(rpoints.get(i).getRpointId(), new RoutePoint.RPointCallback() {
                @Override
                public void onSuccess(String rpointName) {
                    rpointNamesArray[idx] = rpointName;
                    if (counter.incrementAndGet() == rpoints.size()) {
                        callback.accept(Arrays.asList(rpointNamesArray));
                    }
                }
                @Override
                public void onError(Exception e) {
                    rpointNamesArray[idx] = "Unknown";
                    if (counter.incrementAndGet() == rpoints.size()) {
                        callback.accept(Arrays.asList(rpointNamesArray));
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
                mapController.drawRouteFromPoints(locations, getString(R.string.directions_api));
            }
            @Override
            public void onError(Exception e) {
                Log.e("ScheduleMapStu", "Error fetching route points", e);
                Toast.makeText(ScheduleMapStuActivity.this, "Failed to load route points", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void checkIfInitialSelectedRPointIsArrivedAndAdjust() {
        if (selectedRPointIdToQueue != null && currentRPoints != null && !currentRPoints.isEmpty()) {
            int originalIndex = -1;
            for (int i = 0; i < currentRPoints.size(); i++) {
                if (currentRPoints.get(i).getRpointId().equals(selectedRPointIdToQueue)) {
                    originalIndex = i;
                    break;
                }
            }
            if (originalIndex != -1) {
                Schedule.RPointDetail originalRPoint = currentRPoints.get(originalIndex);
                if ("arrived".equals(originalRPoint.getStatus())) {
                    int nextAvailableIndex = -1;
                    for (int i = originalIndex + 1; i < currentRPoints.size(); i++) {
                        if (!"arrived".equals(currentRPoints.get(i).getStatus())) {
                            nextAvailableIndex = i;
                            break;
                        }
                    }
                    if (nextAvailableIndex != -1) {
                        selectedRPointIdToQueue = currentRPoints.get(nextAvailableIndex).getRpointId();
                        selectedRPointNameToQueue = currentRPointNames.get(nextAvailableIndex);
                        tvQueuePrompt.setText("Queue at " + selectedRPointNameToQueue + "?");
                    } else {
                        selectedRPointIdToQueue = null;
                        selectedRPointNameToQueue = null;
                        tvQueuePrompt.setText("No available stops to queue.");
                    }
                } else {
                    selectedRPointNameToQueue = currentRPointNames.get(originalIndex);
                    tvQueuePrompt.setText("Queue at " + selectedRPointNameToQueue + "?");
                }
            } else {
                findFirstAvailableRPoint();
            }
        } else {
            findFirstAvailableRPoint();
        }
        updateQueueButtonsVisibility();
    }
    private void findFirstAvailableRPoint() {
        selectedRPointIdToQueue = null;
        selectedRPointNameToQueue = null;
        if (currentRPoints != null && !currentRPoints.isEmpty()) {
            for (int i = 0; i < currentRPoints.size(); i++) {
                if (!"arrived".equals(currentRPoints.get(i).getStatus())) {
                    selectedRPointIdToQueue = currentRPoints.get(i).getRpointId();
                    selectedRPointNameToQueue = currentRPointNames.get(i);
                    tvQueuePrompt.setText("Queue at " + selectedRPointNameToQueue + "?");
                    break;
                }
            }
        }
        if (selectedRPointIdToQueue == null) {
            tvQueuePrompt.setText("No available stops to queue.");
        }
    }
//    private String getGroupKey(Schedule schedule) {
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        String dateStr = dateFormat.format(schedule.getScheduledDatetime());
//        return schedule.getRouteId() + "_" + schedule.getType() + "_" + dateStr;
//    }
    private void checkIfAlreadyQueued() {
        if (studentId == null) {
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
                        if (schedule != null) {
                            updateScheduleStatus(schedule);
                            if ("completed".equals(schedule.getStatus())) {
                                hideQueueButtons();
                                return;
                            }

                            if (!schedule.isQueueEnabled()) {
                                tvQueuePrompt.setText("Queue is not enabled for this schedule.");
                                isQueued = false;
                                btnQueueMe.setVisibility(View.GONE);
                                btnCancelQueue.setVisibility(View.GONE);
                                updateRPointsAdapter();
                                return;
                            }
                            if (schedule.getRPoints() != null) {
                                boolean foundInQueue = false;
                                for (Schedule.RPointDetail rPointDetail : schedule.getRPoints()) {
                                    if (rPointDetail.getQueuedStudents() != null && rPointDetail.getQueuedStudents().contains(studentId)) {
                                        foundInQueue = true;
                                        selectedRPointIdToQueue = rPointDetail.getRpointId();
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
                                if (!foundInQueue) {
                                    updateQueuePrompt(schedule);
                                }
                                isQueued = foundInQueue;
                            } else {
                                tvQueuePrompt.setText("No route points available for this schedule.");
                                isQueued = false;
                            }
                        } else {
                            tvQueuePrompt.setText("Schedule not found.");
                            isQueued = false;
                        }
                        updateQueueButtonsVisibility();
                        updateRPointsAdapter();
                    } else {
                        tvQueuePrompt.setText("Schedule not found.");
                        isQueued = false;
                        updateQueueButtonsVisibility();
                        updateRPointsAdapter();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ScheduleMapStuActivity", "Error checking queue status", e);
                    tvQueuePrompt.setText("Error loading schedule details.");
                    isQueued = false;
                    updateQueueButtonsVisibility();
                    updateRPointsAdapter();
                });
    }
    private void updateQueuePrompt(Schedule schedule) {
        if ("completed".equals(schedule.getStatus())) {
            tvQueuePrompt.setText("Trip finished - Queue not available");
            return;
        }
        String fromRPointId = getIntent().getStringExtra("fromRPointId");
        if (fromRPointId != null) {
            int fromIndex = -1;
            for (int i = 0; i < schedule.getRPoints().size(); i++) {
                if (schedule.getRPoints().get(i).getRpointId().equals(fromRPointId)) {
                    fromIndex = i;
                    break;
                }
            }
            if (fromIndex != -1) {
                if ("arrived".equals(schedule.getRPoints().get(fromIndex).getStatus())) {
                    for (int i = fromIndex + 1; i < schedule.getRPoints().size(); i++) {
                        if (!"arrived".equals(schedule.getRPoints().get(i).getStatus())) {
                            selectedRPointIdToQueue = schedule.getRPoints().get(i).getRpointId();
                            new RoutePoint().getRPointNameById(selectedRPointIdToQueue, new RoutePoint.RPointCallback() {
                                @Override
                                public void onSuccess(String rpointName) {
                                    selectedRPointNameToQueue = rpointName;
                                    tvQueuePrompt.setText("Queue at " + rpointName + "?");
                                }
                                @Override
                                public void onError(Exception e) {
                                    tvQueuePrompt.setText("Queue at next available stop?");
                                }
                            });
                            return;
                        }
                    }
                    tvQueuePrompt.setText("No available stops to queue.");
                } else {
                    new RoutePoint().getRPointNameById(fromRPointId, new RoutePoint.RPointCallback() {
                        @Override
                        public void onSuccess(String rpointName) {
                            selectedRPointNameToQueue = rpointName;
                            tvQueuePrompt.setText("Queue at " + rpointName + "?");
                        }
                        @Override
                        public void onError(Exception e) {
                            tvQueuePrompt.setText("Queue at selected stop?");
                        }
                    });
                }
            } else {
                tvQueuePrompt.setText("Selected stop not found in this route.");
            }
        } else {
            tvQueuePrompt.setText("Tap on stops to queue");
        }
    }
    private void updateRPointsAdapter() {
        if (currentRPoints != null && currentRPointNames != null) {
            adapter = new RPointsTimelineStuAdapter(currentRPoints, currentRPointNames, this, this, !isQueued);
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
            tvQueuePrompt.setText("Queue at " + rpointName + "?");
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