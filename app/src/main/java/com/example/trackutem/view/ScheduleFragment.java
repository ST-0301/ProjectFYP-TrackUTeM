// ScheduleFragment.java
package com.example.trackutem.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
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

public class ScheduleFragment extends Fragment {
    private TabLayout tabLayout;
    private RecyclerView rvRoutes;
    private TextView tvEmpty;
    private ScheduleAdapter adapter;
    private String currentDriverId;
    private List<Schedule> allSchedules = new ArrayList<>();
    private final List<Schedule> todaySchedules = new ArrayList<>();
    private final List<Schedule> upcomingSchedules = new ArrayList<>();
    private final List<Schedule> completedSchedules = new ArrayList<>();

    // Lifecycle Methods
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRoutes = view.findViewById(R.id.rvTrips);
        rvRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        tvEmpty = view.findViewById(R.id.tvEmpty);
        adapter = new ScheduleAdapter(new ArrayList<>(), schedule -> {
            ScheduleDetailsFragment fragment = new ScheduleDetailsFragment();
            Bundle args = new Bundle();
            args.putString("routeId", schedule.getRouteId());
            args.putString("scheduleId", schedule.getScheduleId());
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack("schedule_details")
                    .commit();
        });
        rvRoutes.setAdapter(adapter);

        setupBackPressHandler();
        retrieveDriverId();
        setupMenuProvider();
    }
    @Override
    public void onResume() {
        super.onResume();
        MainDrvActivity activity = (MainDrvActivity) getActivity();
        if (activity != null) {
            activity.updateToolbar("Trips", false);
            activity.showBottomNav();
            tabLayout = activity.getTabLayout();
            if (tabLayout != null) {
                tabLayout.setVisibility(View.VISIBLE);
            }
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

    // Setup Methods
    private void setupBackPressHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finish();
            }
        });
    }
    private void retrieveDriverId() {
        Bundle args = getArguments();
        currentDriverId = args != null ? args.getString("driverId") : null;
        if (currentDriverId == null) {
            Toast.makeText(requireContext(), "Driver not logged in", Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        } else {
            loadDriverRoutes();
        }
    }
    private void setupMenuProvider() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.trip_toolbar_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_search) {
                    handleSearchClick();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    // Data Handling
    private void loadDriverRoutes() {
        Schedule.getSchedulesByDriverId(currentDriverId, new Schedule.OnSchedulesRetrieved() {
            @Override
            public void onSuccess(List<Schedule> schedules) {
                categorizeSchedules(schedules);
//                resolveRouteNames(schedules, new RouteNamesResolvedListener() {
//                    @Override
//                    public void onAllResolved(List<Schedule> resolvedSchedules) {
//                        categorizeSchedules(resolvedSchedules);
//                    }
//                    @Override
//                    public void onError(Exception e) {
//                        Toast.makeText(requireContext(), "Error resolving routes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error loading schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
//    private void resolveRouteNames(List<Schedule> schedules, RouteNamesResolvedListener listener) {
//        synchronized(this) {
//            List<Schedule> resolvedSchedules = Collections.synchronizedList(new ArrayList<>());
//            AtomicInteger count = new AtomicInteger(0);
//
//            for (Schedule schedule : schedules) {
//                Route.resolveRouteName(schedule.getRouteId(), new Route.RouteNameCallback() {
//                    @Override
//                    public void onSuccess(String routeName) {
//                        schedule.setRouteName(routeName);
//                        resolvedSchedules.add(schedule);
//                        if (count.incrementAndGet() == schedules.size()) {
//                            listener.onAllResolved(resolvedSchedules);
//                        }
//                    }
//                    @Override
//                    public void onError(Exception e) {
//                        if (count.incrementAndGet() == schedules.size()) {
//                            listener.onAllResolved(resolvedSchedules);
//                        }
//                    }
//                });
//            }
//        }
//    }
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

    // UI Operations
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

    // Event Handlers
    private void handleSearchClick() {
        Toast.makeText(requireContext(), "Search clicked", Toast.LENGTH_SHORT).show();
    }

    // Interface
    interface RouteNamesResolvedListener {
        void onAllResolved(List<Schedule> resolvedSchedules);
        void onError(Exception e);
    }
}