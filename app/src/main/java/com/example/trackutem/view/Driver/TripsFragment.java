package com.example.trackutem.view.Driver;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.R;
import com.example.trackutem.model.BusDriverPairing;
import com.example.trackutem.model.Schedule;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TripsFragment extends Fragment {
    // Views
    private HorizontalScrollView hsvDates;
    private LinearLayout chipContainer;
    private ImageButton btnPickDate;
    private ProgressBar pbLoadingTrips;
    private RecyclerView rvSchedulesDrv;
    private TextView tvLoadingTrips, tvEmpty;

    // Data
    private ScheduleAdapter adapter;
    private Map<String, List<Schedule>> schedulesByDate = new HashMap<>();
    private Calendar selectedDate = Calendar.getInstance();

    // Utils
    private final SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.ENGLISH);

    private String currentDriverId;
    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private ListenerRegistration busPairingListener;
    private ListenerRegistration schedulesListener;

    public TripsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trips, container, false);
        db = FirebaseFirestore.getInstance();

        // Initialize views
        hsvDates = view.findViewById(R.id.hsvDates);
        chipContainer = view.findViewById(R.id.chipContainer);
        btnPickDate = view.findViewById(R.id.btnPickDate);
        pbLoadingTrips = view.findViewById(R.id.pbLoadingTrips);
        tvLoadingTrips = view.findViewById(R.id.tvLoadingTrips);
        rvSchedulesDrv = view.findViewById(R.id.rvSchedulesDrv);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        // Setup RecyclerView
        rvSchedulesDrv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScheduleAdapter(new ArrayList<>());
        rvSchedulesDrv.setAdapter(adapter);

        prefs = requireActivity().getSharedPreferences("user_prefs", requireActivity().MODE_PRIVATE);
        currentDriverId = prefs.getString("driverId", null);

        if (currentDriverId != null) {
            setupRealTimeListeners();
        } else {
            Toast.makeText(getContext(), "Driver not logged in", Toast.LENGTH_SHORT).show();
        }

        btnPickDate.setOnClickListener(v ->{
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (dialogView, year, month, dayOfMonth) -> {
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(year, month, dayOfMonth);
                        selectedDate = newDate;
                        showSchedulesForDate(newDate);
                        scrollToDate(newDate);
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        return view;
    }
    private void setupRealTimeListeners() {
        showLoading(true);
        if (busPairingListener != null) {
            busPairingListener.remove();
        }

        busPairingListener = db.collection("busDriverPairings")
                .whereEqualTo("driverId", currentDriverId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showLoading(false);
                        Log.e("TripsFragment", "Error listening for pairings: " + error.getMessage());
                        Toast.makeText(getContext(), "Error listening for pairings: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null && !value.isEmpty()) {
                        List<BusDriverPairing> pairings = value.toObjects(BusDriverPairing.class);
                        Log.d("TripsFragment", "Found " + pairings.size() + " bus pairings for driver");
                        List<String> busDriverPairIds = new ArrayList<>();
                        for (BusDriverPairing pairing : pairings) {
                            busDriverPairIds.add(pairing.getId());
                        }
                        listenForSchedulesForMultiplePairings(busDriverPairIds);
                    } else {
                        showLoading(false);
                        Log.e("TripsFragment", "No active bus pairings found for driver: " + currentDriverId);
                        if (schedulesListener != null) {
                            schedulesListener.remove();
                        }
                        schedulesByDate.clear();
                        setupDateChips();
                        showSchedulesForDate(Calendar.getInstance());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "No active bus pairing found", Toast.LENGTH_SHORT).show();
                        }                    }
                });
    }
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            pbLoadingTrips.setVisibility(View.VISIBLE);
            tvLoadingTrips.setVisibility(View.VISIBLE);
            rvSchedulesDrv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        } else {
            pbLoadingTrips.setVisibility(View.GONE);
            tvLoadingTrips.setVisibility(View.GONE);
        }
    }
    private void listenForSchedulesForMultiplePairings(List<String> busDriverPairIds) {
        if (schedulesListener != null) {
            schedulesListener.remove();
        }
        if (busDriverPairIds.isEmpty()) {
            showLoading(false);
            return;
        }
        schedulesListener = db.collection("schedules")
                .whereIn("busDriverPairId", busDriverPairIds)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showLoading(false);
                        Toast.makeText(getContext(), "Error listening for schedules: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        List<Schedule> schedules = value.toObjects(Schedule.class);
                        Log.d("TripsFragment", "Found " + schedules.size() + " schedules across all pairings");
                        preloadScheduleData(schedules, busDriverPairIds);
                    } else {
                        showLoading(false);
                    }
                });
    }
    private void preloadScheduleData(List<Schedule> schedules, List<String> busDriverPairIds) {
        Map<String, String> pairingToBusMap = new HashMap<>();
        final AtomicInteger pairingCount = new AtomicInteger(0);
        for (String busDriverPairId : busDriverPairIds) {
            db.collection("busDriverPairings").document(busDriverPairId)
                .get()
                .addOnSuccessListener(pairingDocument -> {
                    if (pairingDocument.exists()) {
                        String busId = pairingDocument.getString("busId");
                        if (busId != null) {
                            pairingToBusMap.put(busDriverPairId, busId);
                        }
                        if (pairingCount.incrementAndGet() == busDriverPairIds.size()) {
                            // All pairings processed, now get bus plate numbers
                            preloadBusPlateNumbers(schedules, pairingToBusMap);
                        }
                    } else {
                        showLoading(false);
                        if (pairingCount.incrementAndGet() == busDriverPairIds.size()) {
                            preloadBusPlateNumbers(schedules, pairingToBusMap);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    if (pairingCount.incrementAndGet() == busDriverPairIds.size()) {
                        preloadBusPlateNumbers(schedules, pairingToBusMap);
                    }
                });
        }
    }
    private void preloadBusPlateNumbers(List<Schedule> schedules, Map<String, String> pairingToBusMap) {
        Set<String> busIds = new HashSet<>(pairingToBusMap.values());
        Set<String> routeIds = new HashSet<>();

        for (Schedule schedule : schedules) {
            if (schedule.getRouteId() != null) {
                routeIds.add(schedule.getRouteId());
            }
        }

        if (busIds.isEmpty()) {
            preloadRoutesForSchedules(schedules, new HashMap<>(), new HashMap<>(), new HashMap<>());
            return;
        }

        // Get all bus plate numbers
        db.collection("buses").whereIn(FieldPath.documentId(), new ArrayList<>(busIds))
                .get()
                .addOnSuccessListener(busQuerySnapshot -> {
                    Map<String, String> busPlateNumbers = new HashMap<>();
                    for (DocumentSnapshot busDocument : busQuerySnapshot.getDocuments()) {
                        String plateNumber = busDocument.getString("plateNumber");
                        if (plateNumber != null) {
                            busPlateNumbers.put(busDocument.getId(), plateNumber);
                        }
                    }

                    // Now get route names
                    if (routeIds.isEmpty()) {
                        preloadRoutesForSchedules(schedules, busPlateNumbers, pairingToBusMap, new HashMap<>());
                        return;
                    }

                    db.collection("routes").whereIn(FieldPath.documentId(), new ArrayList<>(routeIds))
                            .get()
                            .addOnSuccessListener(routeQuerySnapshot -> {
                                Map<String, String> routeNames = new HashMap<>();
                                for (DocumentSnapshot routeDocument : routeQuerySnapshot.getDocuments()) {
                                    String routeName = routeDocument.getString("name");
                                    if (routeName != null) {
                                        routeNames.put(routeDocument.getId(), routeName);
                                    }
                                }
                                preloadRoutesForSchedules(schedules, busPlateNumbers, pairingToBusMap, routeNames);
                            })
                            .addOnFailureListener(e -> {
                                preloadRoutesForSchedules(schedules, busPlateNumbers, pairingToBusMap, new HashMap<>());
                            });
                })
                .addOnFailureListener(e -> {
                    preloadRoutesForSchedules(schedules, new HashMap<>(), pairingToBusMap, new HashMap<>());
                });
    }
    private void preloadRoutesForSchedules(List<Schedule> schedules, Map<String, String> busPlateNumbers,
                                           Map<String, String> pairingToBusMap, Map<String, String> routeNames) {
        for (Schedule schedule : schedules) {
            String busDriverPairId = schedule.getBusDriverPairId();
            String busId = pairingToBusMap.get(busDriverPairId);
            String plateNumber = busId != null ? busPlateNumbers.getOrDefault(busId, "No Plate") : "No Bus";

            String routeName = routeNames.getOrDefault(schedule.getRouteId(), "Route Not Found");
            String scheduleType = schedule.getType() != null ? schedule.getType().toLowerCase(Locale.ENGLISH) : "";
            String formattedType = "";

            if ("incampus".equals(scheduleType)) {
                formattedType = "IN CAMPUS";
            } else if ("outcampus".equals(scheduleType)) {
                formattedType = "OUT CAMPUS";
            } else if ("event".equals(scheduleType)) {
                formattedType = "EVENT";
            } else if (!scheduleType.isEmpty()) {
                formattedType = scheduleType.substring(0, 1).toUpperCase() +
                        scheduleType.substring(1).toLowerCase();
            }
            String combinedRouteText = routeName;
            if (!formattedType.isEmpty()) {
                combinedRouteText += " (" + formattedType + ")";
            }
            schedule.setPreloadedRouteName(combinedRouteText);
            schedule.setPreloadedBusPlate(plateNumber);
        }
        categorizeSchedulesByDate(schedules);
    }

    private void categorizeSchedulesByDate(List<Schedule> schedules) {
        schedulesByDate.clear();

        Map<String, List<Schedule>> activeSchedulesByDate = new HashMap<>();
        Map<String, List<Schedule>> completedSchedulesByDate = new HashMap<>();

        for (Schedule schedule : schedules) {
            String status = schedule.getStatus() != null ? schedule.getStatus().toLowerCase(Locale.ENGLISH) : "scheduled";
            if (status.equals("cancelled")) {
                continue;
            }

            if (schedule.getScheduledDatetime() != null) {
                String dateString = ymdFormat.format(schedule.getScheduledDatetime());

                if (status.equals("completed")) {
                    completedSchedulesByDate.computeIfAbsent(dateString, k -> new ArrayList<>()).add(schedule);
                } else {
                    activeSchedulesByDate.computeIfAbsent(dateString, k -> new ArrayList<>()).add(schedule);
                }
            }
        }

        // Sort active schedules: in_progress first (then by time), then others by time
        for (List<Schedule> activeList : activeSchedulesByDate.values()) {
            activeList.sort((s1, s2) -> {
                boolean s1InProgress = "in_progress".equals(s1.getStatus());
                boolean s2InProgress = "in_progress".equals(s2.getStatus());

                if (s1InProgress && !s2InProgress) {
                    return -1;
                } else if (!s1InProgress && s2InProgress) {
                    return 1;
                } else {
                    // Both are in_progress or both are not, sort by time
                    return s1.getScheduledDatetime().compareTo(s2.getScheduledDatetime());
                }
            });
        }
        for (List<Schedule> completedList : completedSchedulesByDate.values()) {
            completedList.sort((s1, s2) -> s1.getScheduledDatetime().compareTo(s2.getScheduledDatetime()));
        }
        for (String date : activeSchedulesByDate.keySet()) {
            List<Schedule> combinedList = new ArrayList<>();
            combinedList.addAll(activeSchedulesByDate.get(date));

            // Add completed schedules for the same date at the end
            if (completedSchedulesByDate.containsKey(date)) {
                combinedList.addAll(completedSchedulesByDate.get(date));
            }

            schedulesByDate.put(date, combinedList);
        }
        for (String date : completedSchedulesByDate.keySet()) {
            if (!schedulesByDate.containsKey(date)) {
                schedulesByDate.put(date, completedSchedulesByDate.get(date));
            }
        }
        showLoading(false);
        setupDateChips();
        showSchedulesForDate(Calendar.getInstance());
    }
    private void setupDateChips() {
        chipContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        for (int i = 0; i < 90; i++) {
            View chipView = inflater.inflate(R.layout.chip_date, chipContainer, false);

            TextView tvDateMonth = chipView.findViewById(R.id.tvDateMonth);
            TextView tvDay = chipView.findViewById(R.id.tvDay);
            MaterialCardView cardView = (MaterialCardView) chipView;
            LinearLayout container = chipView.findViewById(R.id.dateChipContainer);

            String dateNumber = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
            String month = monthFormat.format(cal.getTime());
            String day = dayFormat.format(cal.getTime());
            tvDateMonth.setText(String.format(Locale.ENGLISH, "%s %s", dateNumber, month));
            tvDay.setText(day);

            String dateKey = ymdFormat.format(cal.getTime());
            chipView.setTag(dateKey);

            boolean isToday = isSameDay(cal, Calendar.getInstance());
            if (isToday) {
                setChipSelected(chipView, true);
            } else {
                setChipSelected(chipView, false);
            }
            chipView.setOnClickListener(v -> {
                for (int j = 0; j < chipContainer.getChildCount(); j++) {
                    setChipSelected(chipContainer.getChildAt(j), false);
                }
                setChipSelected(v, true);
                try {
                    Date d = ymdFormat.parse((String) v.getTag());
                    Calendar newSelectedDate = Calendar.getInstance();
                    newSelectedDate.setTime(Objects.requireNonNull(d));
                    showSchedulesForDate(newSelectedDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
            chipContainer.addView(chipView);
            if (isToday) {
                chipView.post(() -> {
                    int scrollX = chipView.getLeft() - (hsvDates.getWidth() / 2) + (chipView.getWidth() / 2);
                    hsvDates.smoothScrollTo(scrollX, 0);
                });
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }
    private void setChipSelected(View chipView, boolean selected) {
        MaterialCardView cardView = (MaterialCardView) chipView;
        LinearLayout container = chipView.findViewById(R.id.dateChipContainer);
        TextView tvDateMonth = chipView.findViewById(R.id.tvDateMonth);
        TextView tvDay = chipView.findViewById(R.id.tvDay);

        if (selected) {
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryBlue));
            tvDateMonth.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tvDay.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            tvDateMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));
        }
    }
    private void scrollToDate(Calendar date) {
        String targetDateKey = ymdFormat.format(date.getTime());
        ViewGroup chipContainer = getView().findViewById(R.id.chipContainer);

        for (int i = 0; i < chipContainer.getChildCount(); i++) {
            View chipView = chipContainer.getChildAt(i);
            if (targetDateKey.equals(chipView.getTag())) {
                for (int j = 0; j < chipContainer.getChildCount(); j++) {
                    setChipSelected(chipContainer.getChildAt(j), false);
                }
                setChipSelected(chipView, true);
                showSchedulesForDate(date);
                chipView.post(() -> {
                    int scrollX = chipView.getLeft() - (hsvDates.getWidth() / 2) + (chipView.getWidth() / 2);
                    hsvDates.smoothScrollTo(scrollX, 0);
                });
                return;
            }
        }
        Toast.makeText(getContext(), "Date is out of the displayed range.", Toast.LENGTH_SHORT).show();
    }
    private void showSchedulesForDate(Calendar date) {
        this.selectedDate = date;
        String dateKey = ymdFormat.format(date.getTime());

        List<Schedule> schedulesToShow = schedulesByDate.getOrDefault(dateKey, new ArrayList<>());
        adapter.updateSchedules(schedulesToShow);

        rvSchedulesDrv.setVisibility(schedulesToShow.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(schedulesToShow.isEmpty() ? View.VISIBLE : View.GONE);
    }
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    @Override
    public void onResume() {
        super.onResume();
        if (currentDriverId != null) {
            // Refresh the data
            setupRealTimeListeners();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (chipContainer != null) {
            chipContainer.removeAllViews();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (busPairingListener != null) {
            busPairingListener.remove();
        }
        if (schedulesListener != null) {
            schedulesListener.remove();
        }
        if (chipContainer != null) {
            chipContainer.removeAllViews();
        }
    }
}