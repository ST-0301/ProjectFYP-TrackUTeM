package com.example.trackutem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.model.Route;
import com.example.trackutem.model.RoutePoint;
import com.example.trackutem.view.RouteAdapter;
import com.example.trackutem.view.Student.GetDirectionActivity;
import com.example.trackutem.view.Student.RouteScheduleActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class MainStuActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private MapController mapController;
    private MaterialCardView searchCard;
    private MaterialButton btnWhereToGo;
    private MaterialCardView bottomSheet;
    private TextView tvStopName;
    private RecyclerView rvRoutes;
    private RouteAdapter routeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_stu);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermissions();

        btnWhereToGo = findViewById(R.id.btnWhereToGo);
        btnWhereToGo.setOnClickListener(v -> {
            Intent intent = new Intent(MainStuActivity.this, GetDirectionActivity.class);
            startActivity(intent);
        });

        bottomSheet = findViewById(R.id.bottomSheetCard);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int peekHeight = (int) (metrics.heightPixels * 0.25); // 15% of screen
        int halfHeight = (int) (metrics.heightPixels * 0.55); // 55% of screen
        behavior.setPeekHeight(peekHeight, true);
        behavior.setHideable(true);
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

        tvStopName = bottomSheet.findViewById(R.id.tvStopName);
        rvRoutes = bottomSheet.findViewById(R.id.rvRoutes);

        // Setup RecyclerView
        rvRoutes.setLayoutManager(new LinearLayoutManager(this));
        routeAdapter = new RouteAdapter(new ArrayList<>(), route -> {
            // Open schedule activity for this route
            Intent intent = new Intent(this, RouteScheduleActivity.class);
            intent.putExtra(RouteScheduleActivity.EXTRA_ROUTE_ID, route.getRouteId());
            startActivity(intent);
        });
        rvRoutes.setAdapter(routeAdapter);
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        } else {
            initializeMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               initializeMap();
            } else {
                Toast.makeText(this, "Permission to ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mapController = new MapController(this, googleMap, fusedLocationClient);
                mapController.initializeMapFeatures();
                mapController.addAllBusStops();
                
                View mapView = mapFragment.getView();
                if (mapView != null) {
                    View locationButton = mapView.findViewWithTag("GoogleMapMyLocationButton");
                    if (locationButton == null) {
                        try {
                            locationButton = ((View) mapView.findViewById(Integer.parseInt("1")))
                                    .findViewById(Integer.parseInt("2"));
                        } catch (Exception ignored) {
                        }
                    }
                    if (locationButton != null) {
                        int topMarginPx = (int) (getResources().getDisplayMetrics().density * 100); // e.g. 100dp below
                        View btnWhereToGo = findViewById(R.id.btnWhereToGo);
                        if (btnWhereToGo != null) {
                            final View finalLocationButton = locationButton; // make final for lambda

                            btnWhereToGo.post(() -> {
                                int[] location = new int[2];
                                btnWhereToGo.getLocationOnScreen(location);
                                int btnWhereToGoBottom = location[1] + btnWhereToGo.getHeight();
                                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) finalLocationButton
                                        .getLayoutParams();
                                params.topMargin = btnWhereToGoBottom
                                        + (int) (16 * getResources().getDisplayMetrics().density); // 16dp below
                                params.rightMargin = (int) (24 * getResources().getDisplayMetrics().density); // keep
                                                                                                              // right
                                                                                                              // margin
                                finalLocationButton.setLayoutParams(params);                                                                                                            });
                        } else {
                            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) locationButton
                                    .getLayoutParams();
                            params.topMargin = topMarginPx;
                            params.rightMargin = (int) (24 * getResources().getDisplayMetrics().density);
                            locationButton.setLayoutParams(params);
                        }
                    }
                }
            });
        }
    }
    public void showBusStopDetails(RoutePoint rpoint) {
        tvStopName.setText(rpoint.getName());
        fetchRoutesForStop(rpoint);
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    private void fetchRoutesForStop(RoutePoint rpoint) {
        String rpointId = rpoint.getRPointId();
        routeAdapter.updateRoutes(new ArrayList<>());
        tvStopName.setText(rpoint.getName());

        Route.getAllRoutes(new Route.AllRoutesCallback() {
            @Override
            public void onSuccess(List<Route> allRoutes) {
                List<Route> routesContainingStop = new ArrayList<>();
                for (Route route : allRoutes) {
                    if (route.getRPoints() != null && route.getRPoints().contains(rpointId)) {
                        routesContainingStop.add(route);
                    }
                }
                if (routesContainingStop.isEmpty()) {
                    Toast.makeText(MainStuActivity.this, "No routes found for this stop", Toast.LENGTH_SHORT).show();
                }
                routeAdapter.updateRoutes(routesContainingStop);
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(MainStuActivity.this, "Error loading routes", Toast.LENGTH_SHORT).show();
                routeAdapter.updateRoutes(new ArrayList<>());
            }
        });
    }
}