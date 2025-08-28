package com.example.trackutem.view.Driver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.trackutem.R;
import com.example.trackutem.databinding.FragmentHomeDrvBinding;
import com.example.trackutem.model.Schedule;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HomeDrvFragment extends Fragment {
    private ProgressBar pbLoadingAssignments;
    private TextView tvLoadingAssignments;
    private MaterialCardView todaysWorkCard;
    private MaterialCardView completedTripsCard;
    private FragmentHomeDrvBinding binding;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable;
    private String driverId;
    private List<Schedule> todaySchedules = new ArrayList<>();
    private FirebaseFirestore db;
    private boolean isViewDestroyed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeDrvBinding.inflate(inflater, container, false);
        isViewDestroyed = false;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        driverId = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("driverId", null);

        pbLoadingAssignments = view.findViewById(R.id.pbLoadingAssignments);
        tvLoadingAssignments = view.findViewById(R.id.tvLoadingAssignments);
        todaysWorkCard = view.findViewById(R.id.todaysWorkCard);
        completedTripsCard = view.findViewById(R.id.completedTripsCard);

        // Setup UI components
        setupGreeting();
        setupEmergencyCallButton();

        // Start updating the date and time
        startDateTimeUpdater();

        // Load today's schedules
        loadTodaySchedules();
    }

    private void showAssignmentsLoading() {
        if (isViewDestroyed) return;

        pbLoadingAssignments.setVisibility(View.VISIBLE);
        tvLoadingAssignments.setVisibility(View.VISIBLE);

        // Hide actual assignment content
        binding.tvProgressSummary.setVisibility(View.GONE);
        binding.pbToday.setVisibility(View.GONE);
        binding.tvAssignment1.setVisibility(View.GONE);
        binding.tvAssignment2.setVisibility(View.GONE);
        binding.tvAssignment3.setVisibility(View.GONE);

        // Hide completed trips card
        completedTripsCard.setVisibility(View.GONE);
    }

    private void hideAssignmentsLoading() {
        if (isViewDestroyed) return;

        pbLoadingAssignments.setVisibility(View.GONE);
        tvLoadingAssignments.setVisibility(View.GONE);
    }

    private void setupGreeting() {
        // Get driver's name from Firestore
        if (driverId != null) {
            db.collection("drivers").document(driverId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isViewDestroyed && documentSnapshot.exists()) {
                            String driverName = documentSnapshot.getString("name");
                            if (driverName != null) {
                                updateGreetingWithName(driverName);
                            }
                        }
                    });
        }
    }

    private void updateGreetingWithName(String driverName) {
        if (isViewDestroyed || binding == null) return;

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning, ";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Good Afternoon, ";
        } else {
            greeting = "Good Evening, ";
        }

        binding.tvGreeting.setText(greeting + driverName + " 👋");
    }

    private void startDateTimeUpdater() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                if (isViewDestroyed || binding == null) {
                    timeHandler.removeCallbacks(this);
                    return;
                }

                TimeZone timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy | hh:mm a", Locale.getDefault());
                sdf.setTimeZone(timeZone);
                String currentDateTime = sdf.format(new Date());

                binding.tvDateTime.setText(currentDateTime);
                timeHandler.postDelayed(this, 60000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void setupEmergencyCallButton() {
        if (isViewDestroyed || binding == null) return;

        binding.fabEmergencyCall.setOnClickListener(v -> {
            String emergencyNumber = "0123456789";
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + emergencyNumber));
            startActivity(intent);
        });
    }

    private void loadTodaySchedules() {
        if (driverId == null || isViewDestroyed) return;
        showAssignmentsLoading();

        // Get today's date range
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

        db.collection("busDriverPairings")
                .whereEqualTo("driverId", driverId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isViewDestroyed && !queryDocumentSnapshots.isEmpty()) {
                        List<String> busDriverPairIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            busDriverPairIds.add(document.getId());
                        }
                        Timestamp startTs = new Timestamp(startOfDay);
                        Timestamp endTs = new Timestamp(endOfDay);

                        // Now get schedules for this pairing and today's date
                        db.collection("schedules")
                                .whereIn("busDriverPairId", busDriverPairIds)
                                .whereGreaterThanOrEqualTo("scheduledDatetime", startTs)
                                .whereLessThanOrEqualTo("scheduledDatetime", endTs)
                                .orderBy("scheduledDatetime", Query.Direction.ASCENDING)
                                .get()
                                .addOnSuccessListener(scheduleSnapshots -> {
                                    if (isViewDestroyed) return;
                                    hideAssignmentsLoading();

                                    todaySchedules.clear();
                                    List<String> routeIds = new ArrayList<>();
                                    List<String> busDriverPairIdsForPreload = new ArrayList<>();

                                    for (QueryDocumentSnapshot document : scheduleSnapshots) {
                                        Schedule schedule = document.toObject(Schedule.class);
                                        schedule.setScheduleId(document.getId());
                                        if (!"cancelled".equalsIgnoreCase(schedule.getStatus())) {
                                            todaySchedules.add(schedule);
                                            if (schedule.getRouteId() != null && !routeIds.contains(schedule.getRouteId())) {
                                                routeIds.add(schedule.getRouteId());
                                            }
                                            if (schedule.getBusDriverPairId() != null && !busDriverPairIdsForPreload.contains(schedule.getBusDriverPairId())) {
                                                busDriverPairIdsForPreload.add(schedule.getBusDriverPairId());
                                            }
                                        }
                                    }
                                    fetchRouteNames(routeIds);
                                    fetchBusPlateNumbers(busDriverPairIdsForPreload); // Preload bus plate numbers
                                });
                    } else {
                        hideAssignmentsLoading();
                        updateUIWithSchedules();
                    }
                })
                .addOnFailureListener(e -> {
                    hideAssignmentsLoading();
                    updateUIWithSchedules();
                });
    }

    private void fetchBusPlateNumbers(List<String> busDriverPairIds) {
        if (busDriverPairIds.isEmpty()) {
            updateUIWithSchedules();
            return;
        }

        // First get all bus IDs from the pairings
        db.collection("busDriverPairings")
                .whereIn(FieldPath.documentId(), busDriverPairIds)
                .get()
                .addOnSuccessListener(pairingSnapshots -> {
                    Map<String, String> pairingToBusMap = new HashMap<>();
                    List<String> busIds = new ArrayList<>();

                    for (QueryDocumentSnapshot pairingDoc : pairingSnapshots) {
                        String busId = pairingDoc.getString("busId");
                        if (busId != null) {
                            pairingToBusMap.put(pairingDoc.getId(), busId);
                            if (!busIds.contains(busId)) {
                                busIds.add(busId);
                            }
                        }
                    }

                    if (busIds.isEmpty()) {
                        updateUIWithSchedules();
                        return;
                    }

                    // Now get all bus plate numbers
                    db.collection("buses")
                            .whereIn(FieldPath.documentId(), busIds)
                            .get()
                            .addOnSuccessListener(busSnapshots -> {
                                Map<String, String> busPlateNumbers = new HashMap<>();
                                for (QueryDocumentSnapshot busDoc : busSnapshots) {
                                    String plateNumber = busDoc.getString("plateNumber");
                                    if (plateNumber != null) {
                                        busPlateNumbers.put(busDoc.getId(), plateNumber);
                                    }
                                }

                                // Set bus plate numbers for each schedule
                                for (Schedule schedule : todaySchedules) {
                                    String busDriverPairId = schedule.getBusDriverPairId();
                                    if (busDriverPairId != null && pairingToBusMap.containsKey(busDriverPairId)) {
                                        String busId = pairingToBusMap.get(busDriverPairId);
                                        if (busId != null && busPlateNumbers.containsKey(busId)) {
                                            schedule.setPreloadedBusPlate(busPlateNumbers.get(busId));
                                        } else {
                                            schedule.setPreloadedBusPlate("No Plate");
                                        }
                                    } else {
                                        schedule.setPreloadedBusPlate("No Bus");
                                    }
                                }
                                updateUIWithSchedules();
                            })
                            .addOnFailureListener(e -> {
                                for (Schedule schedule : todaySchedules) {
                                    schedule.setPreloadedBusPlate("No Bus");
                                }
                                updateUIWithSchedules();
                            });
                })
                .addOnFailureListener(e -> {
                    for (Schedule schedule : todaySchedules) {
                        schedule.setPreloadedBusPlate("No Bus");
                    }
                    updateUIWithSchedules();
                });
    }

    private void fetchRouteNames(List<String> routeIds) {
        if (routeIds.isEmpty()) {
            updateUIWithSchedules();
            return;
        }

        db.collection("routes")
                .whereIn(FieldPath.documentId(), routeIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, String> routeNames = new HashMap<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) { // Change to DocumentSnapshot
                        String routeName = document.getString("name");
                        if (routeName != null) {
                            routeNames.put(document.getId(), routeName);
                        }
                    }

                    // Set route names for each schedule
                    for (Schedule schedule : todaySchedules) {
                        if (schedule.getRouteId() != null && routeNames.containsKey(schedule.getRouteId())) {
                            String routeName = routeNames.get(schedule.getRouteId());
                            schedule.setPreloadedRouteName(routeName);
                        } else {
                            schedule.setPreloadedRouteName("Route");
                        }
                    }
                    updateUIWithSchedules();
                })
                .addOnFailureListener(e -> {
                    for (Schedule schedule : todaySchedules) {
                        schedule.setPreloadedRouteName("Route");
                    }
                    updateUIWithSchedules();
                });
    }

    private void updateUIWithSchedules() {
        if (isViewDestroyed || binding == null) return;
        hideAssignmentsLoading();

        binding.tvProgressSummary.setVisibility(View.VISIBLE);
        binding.pbToday.setVisibility(View.VISIBLE);

        // Separate schedules by status
        List<Schedule> inProgressSchedules = new ArrayList<>();
        List<Schedule> scheduledSchedules = new ArrayList<>();
        List<Schedule> completedSchedules = new ArrayList<>();

        for (Schedule schedule : todaySchedules) {
            switch (schedule.getStatus()) {
                case "in_progress":
                    inProgressSchedules.add(schedule);
                    break;
                case "scheduled":
                    scheduledSchedules.add(schedule);
                    break;
                case "completed":
                    completedSchedules.add(schedule);
                    break;
            }
        }

        // Case: no schedules at all
        if (todaySchedules.isEmpty()) {
            binding.tvAssignment1.setText("No assignments today 🎉");
            binding.tvAssignment1.setVisibility(View.VISIBLE);
            binding.tvAssignment2.setVisibility(View.GONE);
            binding.tvAssignment3.setVisibility(View.GONE);
            binding.tvProgressSummary.setText("0/0 completed today");
            binding.pbToday.setProgress(0);
            binding.completedTripsCard.setVisibility(View.GONE);
            return;
        }

        updateTripProgress(completedSchedules.size(), todaySchedules.size());

        // Select assignments according to rules
        List<Schedule> assignmentsToShow = new ArrayList<>();

        if (!inProgressSchedules.isEmpty()) {
            // Show 1 in_progress (earliest one)
            assignmentsToShow.add(inProgressSchedules.get(0));

            // Then max 2 scheduled
            int count = Math.min(2, scheduledSchedules.size());
            for (int i = 0; i < count; i++) {
                assignmentsToShow.add(scheduledSchedules.get(i));
            }
        } else {
            // No in_progress → show max 3 scheduled
            int count = Math.min(3, scheduledSchedules.size());
            for (int i = 0; i < count; i++) {
                assignmentsToShow.add(scheduledSchedules.get(i));
            }
        }

        // If still empty → show no assignments message
        if (assignmentsToShow.isEmpty()) {
            binding.tvAssignment1.setText("No assignments today 🎉");
            binding.tvAssignment1.setVisibility(View.VISIBLE);
            binding.tvAssignment2.setVisibility(View.GONE);
            binding.tvAssignment3.setVisibility(View.GONE);
        } else {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            // Assignment 1
            if (assignmentsToShow.size() >= 1) {
                Schedule s1 = assignmentsToShow.get(0);
                String statusLabel = s1.getStatus().equals("in_progress") ? " (In Progress)" : " (Next)";
                String busPlateText = s1.getPreloadedBusPlate() != null ?
                        "(" + s1.getPreloadedBusPlate() + ") " : "";
                binding.tvAssignment1.setText(
                        timeFormat.format(s1.getScheduledDatetime()) + " - " +
                                busPlateText + getRouteDisplayName(s1) + statusLabel
                );
                binding.tvAssignment1.setVisibility(View.VISIBLE);
            } else {
                binding.tvAssignment1.setVisibility(View.GONE);
            }

            // Assignment 2
            if (assignmentsToShow.size() >= 2) {
                Schedule s2 = assignmentsToShow.get(1);
                String busPlateText = s2.getPreloadedBusPlate() != null ?
                        "(" + s2.getPreloadedBusPlate() + ") " : "";
                binding.tvAssignment2.setText(
                        timeFormat.format(s2.getScheduledDatetime()) + " - " + busPlateText + getRouteDisplayName(s2)
                );
                binding.tvAssignment2.setVisibility(View.VISIBLE);
            } else {
                binding.tvAssignment2.setVisibility(View.GONE);
            }

            // Assignment 3
            if (assignmentsToShow.size() >= 3) {
                Schedule s3 = assignmentsToShow.get(2);
                String busPlateText = s3.getPreloadedBusPlate() != null ?
                        "(" + s3.getPreloadedBusPlate() + ") " : "";
                binding.tvAssignment3.setText(
                        timeFormat.format(s3.getScheduledDatetime()) + " - " + busPlateText + getRouteDisplayName(s3)
                );
                binding.tvAssignment3.setVisibility(View.VISIBLE);
            } else {
                binding.tvAssignment3.setVisibility(View.GONE);
            }
        }

        // Completed trips card
        if (completedSchedules.isEmpty()) {
            binding.completedTripsCard.setVisibility(View.GONE);
        } else {
            binding.completedTripsCard.setVisibility(View.VISIBLE);
            String tripsText = completedSchedules.size() + " Trip" +
                    (completedSchedules.size() > 1 ? "s" : "") + " Done ✅";
            binding.tvCompletedSummary.setText(tripsText);
        }
    }

    private String getRouteDisplayName(Schedule schedule) {
        return schedule.getPreloadedRouteName() != null ?
                schedule.getPreloadedRouteName() : "Route";
    }

    private void updateTripProgress(int completed, int total) {
        if (isViewDestroyed || binding == null) return;

        binding.tvProgressSummary.setText(completed + "/" + total + " completed today");
        int progress = total > 0 ? (int) (((double) completed / total) * 100) : 0;
        binding.pbToday.setProgress(progress);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewDestroyed = true;
        timeHandler.removeCallbacks(timeRunnable);
        binding = null;
    }
}