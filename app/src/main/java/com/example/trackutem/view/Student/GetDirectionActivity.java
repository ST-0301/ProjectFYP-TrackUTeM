package com.example.trackutem.view.Student;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.model.Schedule;
import com.example.trackutem.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GetDirectionActivity extends AppCompatActivity {
    private AutoCompleteTextView dropdownFrom, dropdownTo;
    private ImageButton btnReverse;
    private RecyclerView rvSchedules;
    private List<RoutePoint> busStops = new ArrayList<>();
    private Map<String, RoutePoint> nameToRoutePoint = new HashMap<>();
    private List<Schedule> matchingSchedules = new ArrayList<>();
    private TextView tvEmpty;
    private String selectedFromRPointId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_direction);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
//            actionBar.setHomeButtonEnabled(true);

        dropdownFrom = findViewById(R.id.dropdownFrom);
        dropdownTo = findViewById(R.id.dropdownTo);
        btnReverse = findViewById(R.id.btnReverse);
        rvSchedules = findViewById(R.id.rvSchedules);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setVisibility(View.GONE);

        btnReverse.setOnClickListener(v -> {
            String from = dropdownFrom.getText().toString();
            String to = dropdownTo.getText().toString();
            if (!from.isEmpty() || !to.isEmpty()) {

                dropdownFrom.setText(to, false);
                dropdownTo.setText(from, false);
                tryShowSchedules();
                // Play animation
                ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(btnReverse, "rotation", 0f, 360f);
                rotateAnim.setDuration(400);
                rotateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                rotateAnim.start();
            }
        });
        fetchBusStops();
        setupTabs();
    }
    
    private void fetchBusStops() {
        RoutePoint.getAllRPoints(new RoutePoint.AllRPointsCallback() {
            @Override
            public void onSuccess(List<RoutePoint> rpoints) {
                busStops.clear();
                nameToRoutePoint.clear();
                List<String> stopNames = new ArrayList<>();
                for (RoutePoint rp : rpoints) {
                    if ("bus_stop".equals(rp.getType())) {
                        busStops.add(rp);
                        stopNames.add(rp.getName());
                        nameToRoutePoint.put(rp.getName(), rp);
                    }
                }
                java.util.Collections.sort(stopNames);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(GetDirectionActivity.this,
                        android.R.layout.simple_dropdown_item_1line, stopNames);
                dropdownFrom.setAdapter(adapter);
                dropdownTo.setAdapter(adapter);

                setupSelectionListener();
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(GetDirectionActivity.this, "Failed to load bus stops", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSelectionListener() {
        TextInputLayout fromLayout = (TextInputLayout) dropdownFrom.getParent().getParent();
        TextInputLayout toLayout = (TextInputLayout) dropdownTo.getParent().getParent();

        // Set initial hint
        fromLayout.setHint("Select stop");
        toLayout.setHint("Select stop");

        dropdownFrom.setOnItemClickListener((parent, view, position, id) -> {
            fromLayout.setHint(""); // Hide hint when selected
            tryShowSchedules();
        });
        dropdownTo.setOnItemClickListener((parent, view, position, id) -> {
            toLayout.setHint(""); // Hide hint when selected
            tryShowSchedules();
        });

        // Restore hint if cleared
        dropdownFrom.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && dropdownFrom.getText().toString().isEmpty()) {
                fromLayout.setHint("Select stop");
            }
        });
        dropdownTo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && dropdownTo.getText().toString().isEmpty()) {
                toLayout.setHint("Select stop");
            }
        });
    }

    private void tryShowSchedules() {
        String fromName = dropdownFrom.getText().toString();
        String toName = dropdownTo.getText().toString();
        if (!fromName.isEmpty() && !toName.isEmpty() && !fromName.equals(toName)) {
            RoutePoint fromPoint = nameToRoutePoint.get(fromName);
            RoutePoint toPoint = nameToRoutePoint.get(toName);
            if (fromPoint != null && toPoint != null) {
                selectedFromRPointId = fromPoint.getRPointId();
                findViewById(R.id.tabLayout).setVisibility(View.VISIBLE);
                findViewById(R.id.viewPager).setVisibility(View.VISIBLE);
                findViewById(R.id.rvSchedules).setVisibility(View.VISIBLE);
                fetchSchedules(fromPoint.getRPointId(), toPoint.getRPointId());
            }
        }
    }

    private void fetchSchedules(String fromRPointId, String toRPointId) {
        Schedule.getAllSchedules(new Schedule.OnSchedulesRetrieved() {
            @Override
            public void onSuccess(List<Schedule> schedules) {
                matchingSchedules.clear();
                for (Schedule schedule : schedules) {
                    List<Schedule.RPointDetail> rpoints = schedule.getRPoints();
                    // if (rpoints != null) {
                    //     List<String> rpointIds = new ArrayList<>();
                    //     for (Schedule.RPointDetail rpd : rpoints) {
                    //         rpointIds.add(rpd.getRPointId());
                    //     }
                    //     if (rpointIds.contains(fromRPointId) && rpointIds.contains(toRPointId)) {
                    //         matchingSchedules.add(schedule);
                    //     }
                    // }
                    if (rpoints != null) {
                        int fromIndex = -1;
                        int toIndex = -1;
                        for (int i = 0; i < rpoints.size(); i++) {
                            String rpointId = rpoints.get(i).getRPointId();
                            if (rpointId.equals(fromRPointId) && fromIndex == -1) {
                                fromIndex = i;
                            }
                            if (rpointId.equals(toRPointId) && fromIndex != -1 && toIndex == -1) {
                                toIndex = i;
                                break; // found both in order
                            }
                        }
                        if (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex) {
                            matchingSchedules.add(schedule);
                        }
                    }
                }
                TabLayout tabLayout = findViewById(R.id.tabLayout);
                if (tabLayout.getTabCount() > 0) {
                    tabLayout.selectTab(tabLayout.getTabAt(0)); 
                    filterSchedulesForDay(0);
                }
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(GetDirectionActivity.this, "Failed to load schedules", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSchedules(List<Schedule> schedules) {
        RecyclerView rv = findViewById(R.id.rvSchedules);
        tvEmpty = findViewById(R.id.tvEmpty);
        if (schedules.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            rv.setLayoutManager(new LinearLayoutManager(this));
            ScheduleStuAdapter adapter = new ScheduleStuAdapter(this, schedules, selectedFromRPointId);
            rv.setAdapter(adapter);
        }
    }

    private void setupTabs() {
        int primaryBlue = getResources().getColor(R.color.primaryBlue);
        int textSecondary = getResources().getColor(R.color.textSecondary);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.ENGLISH);
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM", Locale.ENGLISH);
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            View tabView = inflater.inflate(R.layout.tab_schedule_day, null);
            TextView tvDay = tabView.findViewById(R.id.tvTabDay);
            TextView tvDate = tabView.findViewById(R.id.tvTabDate);
                                                                                            
            if (i == 0) {
                tvDay.setText("Today");
            } else {
                tvDay.setText(dayFormat.format(cal.getTime()));
            }
            tvDate.setText(dateFormat.format(cal.getTime()));
            tvDay.setTextColor(i == 0 ? primaryBlue : textSecondary);
            tvDate.setTextColor(i == 0 ? primaryBlue : textSecondary);
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setCustomView(tabView);
            tabLayout.addTab(tab);

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View tabView = tab.getCustomView();
                if (tabView != null) {
                    TextView tvDay = tabView.findViewById(R.id.tvTabDay);
                    TextView tvDate = tabView.findViewById(R.id.tvTabDate);
                    tvDay.setTextColor(primaryBlue);
                    tvDate.setTextColor(primaryBlue);
                }
                int position = tab.getPosition();
                filterSchedulesForDay(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View tabView = tab.getCustomView();
                if (tabView != null) {
                    TextView tvDay = tabView.findViewById(R.id.tvTabDay);
                    TextView tvDate = tabView.findViewById(R.id.tvTabDate);
                    tvDay.setTextColor(textSecondary);
                    tvDate.setTextColor(textSecondary);
                }
            }
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    private void filterSchedulesForDay(int tabPosition) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, tabPosition);

//        String dayName = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.getTime()).toLowerCase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String targetDate = dateFormat.format(cal.getTime());

        List<Schedule> filtered = new ArrayList<>();
        for (Schedule schedule : matchingSchedules) {
            String scheduleDate = dateFormat.format(schedule.getScheduledDatetime());
            if (scheduleDate.equals(targetDate)) {
                filtered.add(schedule);
            }
        }
        if (filtered.isEmpty()) {
            rvSchedules.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            showSchedules(filtered);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
