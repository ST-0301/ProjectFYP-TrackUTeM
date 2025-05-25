package com.example.trackutem.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.MainDrvActivity;
import com.example.trackutem.R;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.Schedule;
import com.google.android.material.tabs.TabLayout;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class TripFragment extends Fragment {
    private TabLayout tabLayout;
    private RecyclerView rvRoutes;
    private TextView tvEmpty;
    private ScheduleAdapter adapter;
    private String currentDriverId;
    private List<Schedule> allSchedules = new ArrayList<>();
    private List<Schedule> todaySchedules = new ArrayList<>();
    private List<Schedule> upcomingSchedules = new ArrayList<>();
    private List<Schedule> completedSchedules = new ArrayList<>();

    // region Lifecycle Methods
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip, container, false);

        rvRoutes = view.findViewById(R.id.rvTrips);
        rvRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        tvEmpty = view.findViewById(R.id.tvEmpty);
        adapter = new ScheduleAdapter(new ArrayList<>(), schedule -> {
            Intent i = new Intent(getActivity(), RouteDetailsActivity.class);
            i.putExtra("scheduleId", schedule.getScheduleId());
            startActivity(i);
        });
        rvRoutes.setAdapter(adapter);

        Bundle args = getArguments();
        currentDriverId = args != null ? args.getString("driverId") : null;
        if (currentDriverId == null) {
            Toast.makeText(requireContext(), "Driver not logged in", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return view;
        } else {
            loadDriverRoutes();
        }
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        MainDrvActivity activity = (MainDrvActivity) getActivity();
        if (activity != null) {
            tabLayout = activity.getTabLayout();
            if (!allSchedules.isEmpty()) {
                setupTabs();
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        if (tabLayout != null) {
            tabLayout.removeAllTabs();
            tabLayout.clearOnTabSelectedListeners();
        }
    }

    // region Menu Handling
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.trip_toolbar_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            handleSearchClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // region Data Loading & Processing
    private void loadDriverRoutes() {
        Schedule.getSchedulesByDriverId(currentDriverId, new Schedule.OnSchedulesRetrieved() {
            @Override
            public void onSuccess(List<Schedule> schedules) {
                resolveRouteNames(schedules, new RouteNamesResolvedListener() {
                    @Override
                    public void onAllResolved(List<Schedule> resolvedSchedules) {
                        categorizeSchedules(resolvedSchedules);
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(), "Error resolving routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error loading schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void resolveRouteNames(List<Schedule> schedules, RouteNamesResolvedListener listener) {
        synchronized(this) {
            List<Schedule> resolvedSchedules = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger count = new AtomicInteger(0);

            for (Schedule schedule : schedules) {
                Route.resolveRouteName(schedule.getRouteId(), new Route.RouteNameCallback() {
                    @Override
                    public void onSuccess(String routeName) {
                        schedule.setRouteName(routeName);
                        resolvedSchedules.add(schedule);
                        if (count.incrementAndGet() == schedules.size()) {
                            listener.onAllResolved(resolvedSchedules);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        if (count.incrementAndGet() == schedules.size()) {
                            listener.onAllResolved(resolvedSchedules);
                        }
                    }
                });
            }
        }
    }
    private void categorizeSchedules(List<Schedule> schedules) {
        allSchedules = schedules;
        todaySchedules.clear();
        upcomingSchedules.clear();
        completedSchedules.clear();

        // Get current day
        Calendar calendar = Calendar.getInstance();
        int currentDayNumber = calendar.get(Calendar.DAY_OF_WEEK);
        String[] days = new DateFormatSymbols(Locale.ENGLISH).getWeekdays();
        String currentDayName = days[currentDayNumber].toLowerCase();

        for (Schedule schedule : allSchedules) {
            String status = schedule.getStatus() != null ? schedule.getStatus().toLowerCase(Locale.ENGLISH) : "scheduled";
            String scheduleDay = schedule.getDay().toLowerCase(Locale.ENGLISH);

            if (status.equals("completed")) {
                completedSchedules.add(schedule);
            } else if (scheduleDay.equals(currentDayName)) {
                todaySchedules.add(schedule);
            } else {
                upcomingSchedules.add(schedule);
            }
        }
        setupTabs();
        showSelectedTab(todaySchedules);
    }

    // region UI Operations
    private void setupTabs() {
        if (tabLayout == null) return;

        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Today (" + todaySchedules.size() + ")"));
        tabLayout.addTab(tabLayout.newTab().setText("Upcoming (" + upcomingSchedules.size() + ")"));
        tabLayout.addTab(tabLayout.newTab().setText("Completed"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showSelectedTab(todaySchedules);
                        break;
                    case 1:
                        showSelectedTab(upcomingSchedules);
                        break;
                    case 2:
                        showSelectedTab(completedSchedules);
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    private void showSelectedTab(List<Schedule> schedulesToShow) {
        adapter.updateSchedules(schedulesToShow);
        rvRoutes.setVisibility(schedulesToShow.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(schedulesToShow.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // region Helper Methods
    private void handleSearchClick() {
        Toast.makeText(requireContext(), "Search clicked", Toast.LENGTH_SHORT).show();
    }

    // region Interface
    interface RouteNamesResolvedListener {
        void onAllResolved(List<Schedule> resolvedSchedules);
        void onError(Exception e);
    }
}