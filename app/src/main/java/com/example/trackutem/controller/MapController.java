// MapController.java
package com.example.trackutem.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.trackutem.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.List;

public class MapController {
    private final Context context;
    private final GoogleMap mMap;
    private final FusedLocationProviderClient fusedLocationClient;
    private Polyline currentRoutePolyline;
    private Marker currentMarker;

    public MapController(Context context, GoogleMap mMap, FusedLocationProviderClient fusedLocationClient) {
        this.context = context;
        this.mMap = mMap;
        this.fusedLocationClient = fusedLocationClient;
    }

    // Public Methods
    public void initializeMapFeatures() {
        enableUserLocation();
        setMapStyle();
        setupMapClickListener();
//        addBusStations();
    }
    public void drawRoute(List<LatLng> path) {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
        }
        currentRoutePolyline = mMap.addPolyline(
                new PolylineOptions()
                        .addAll(path)
                        .width(12f)
                        .color(ContextCompat.getColor(context, R.color.route_color)) // Define in colors.xml
        );
    }
    public void addRouteMarkers(List<LatLng> rpoints, List<String> rpointNames) {
        if (rpoints == null || rpointNames == null || rpoints.size() != rpointNames.size()) {
            return;
        }
        for (int i = 0; i < rpoints.size(); i++) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(rpoints.get(i))
                    .title(rpointNames.get(i))
                    .icon(getBusIcon()));
            if (marker != null) {
                marker.setTag(rpointNames.get(i));
            }
        }
    }
    public void zoomToRoute(List<LatLng> path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : path) {
            builder.include(point);
        }
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        } catch (Exception e) {
            // Handle exception for small routes
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(path.get(0), 14f));
        }
    }

    // Location & Map Setup
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        setupInitialLocation();
    }
    private void setupInitialLocation() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                android.location.Location location = locationResult.getLastLocation();

                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Move camera to current location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16.5f));

                    if (currentMarker != null) {
                        currentMarker.remove();
                    }
                    // Add marker at current location
                    currentMarker = mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("You are here"));
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        };
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e("LocationError", "SecurityException in location updates: " + e.getMessage());
        }
    }
    private void setMapStyle() {
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));
            if (!success) {
                Log.e("MapStyle", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapStyle", "Can't find style. Error: ", e);
        }
    }

    // Marker & Interaction
    @SuppressLint("PotentialBehaviorOverride")
    private void setupMapClickListener() {
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null) {
                String rpointName = (String) marker.getTag();
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                currentMarker = mMap.addMarker(new MarkerOptions()
                        .position(marker.getPosition())
                        .title("Selected: " + rpointName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .zIndex(1.0f));
//                currentMarker.showInfoWindow();
                if (currentMarker != null) {
                    currentMarker.showInfoWindow();
                }
                return true;
            }
            return false;
        });
    }
//    private void addBusStops() {
//        LatLng[] stopLocations = {
//                new LatLng(2.3123, 102.3201),
//                new LatLng(2.3135, 102.3190),
//                new LatLng(2.3150, 102.3178)
//        };
//
//        String[] stopNames = {
//                "UTeM Main Gate",
//                "FKEKK Bus Stop",
//                "UTeM Library Stop"
//        };
//
//        for (int i = 0; i < stopLocations.length; i++) {
//            Marker marker = mMap.addMarker(new MarkerOptions()
//                    .position(stopLocations[i])
//                    .title(stopNames[i])
//                    .icon(getBusIcon()));
////                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_marker)));
//            marker.setTag(stopNames[i]);
//        }
//    }
    private BitmapDescriptor getBusIcon() {
        int height = 110;
        int width = 80;
        BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.bus_station_marker);
        if (bitmapdraw == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(smallMarker);
    }
}