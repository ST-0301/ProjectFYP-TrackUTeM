package com.example.trackutem.view.Student;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.trackutem.R;
import com.example.trackutem.controller.MapController;
import com.example.trackutem.model.Route;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScheduleMapStuActivity extends AppCompatActivity implements OnMapReadyCallback{
    private GoogleMap mMap;
    private String scheduleId, driverId, routeId;
    private MapController mapController;
    private Retrofit retrofit;
    private DirectionsService directionsService;
    private Marker busLocationMarker;
    private List<LatLng> rpointLocations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_map_stu);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        scheduleId = getIntent().getStringExtra("scheduleId");
        driverId = getIntent().getStringExtra("driverId");

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
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        routeId = documentSnapshot.getString("routeId");
                        if (routeId != null) {
                            fetchRoutePoints(routeId);
                        }
                    }
                });
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

//    private void addRouteMarkers(List<LatLng> locations) {
//        for (LatLng location : locations) {
//            mMap.addMarker(new MarkerOptions().position(location).title("Route Point"));
//        }
//    }

//    private void getRoutePathFromDirections(List<LatLng> locations) {
//        if (locations.size() < 2) return;
//
//        LatLng origin = locations.get(0);
//        LatLng destination = locations.get(locations.size() - 1);
//        List<LatLng> waypoints = locations.subList(1, locations.size() - 1);
//
//        String originStr = origin.latitude + "," + origin.longitude;
//        String destinationStr = destination.latitude + "," + destination.longitude;
//        String waypointsStr = null;
//
//        if (!waypoints.isEmpty()) {
//            StringBuilder sb = new StringBuilder("optimize:true|");
//            for (LatLng point : waypoints) {
//                sb.append(point.latitude)
//                        .append(",")
//                        .append(point.longitude)
//                        .append("|");
//            }
//            waypointsStr = sb.substring(0, sb.length() - 1);
//        }
//
//        String apiKey = getString(R.string.directions_api_key);
//        directionsService.getDirections(originStr, destinationStr, waypointsStr, apiKey)
//                .enqueue(new Callback<DirectionsResponse>() {
//                    @Override
//                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//                        if (response.isSuccessful() && response.body() != null) {
//                            DirectionsResponse directionsResponse = response.body();
//                            if (directionsResponse.routes != null &&
//                                    !directionsResponse.routes.isEmpty() &&
//                                    directionsResponse.routes.get(0).overviewPolyline != null) {
//
//                                String polyline = directionsResponse.routes.get(0).overviewPolyline.points;
//                                List<LatLng> path = PolyUtil.decode(polyline);
//                                drawRoute(path);
//                                zoomToRoute(path);
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
//                        // Handle failure
//                    }
//                });
//    }
//    private void drawRoute(List<LatLng> path) {
//        PolylineOptions polylineOptions = new PolylineOptions()
//                .addAll(path)
//                .width(10f)
//                .color(getResources().getColor(R.color.route_color));
//        mMap.addPolyline(polylineOptions);
//    }
//    private void zoomToRoute(List<LatLng> path) {
//        LatLngBounds.Builder builder = new LatLngBounds.Builder();
//        for (LatLng point : path) {
//            builder.include(point);
//        }
//        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
//    }
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
                    // Optionally show "Bus not started yet" or similar
                    if (busLocationMarker != null) {
                        busLocationMarker.remove();
                        busLocationMarker = null;
                    }
                }
            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}