package com.example.trackutem.controller;

import android.Manifest;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapController {
    private final Context context;
    private final GoogleMap mMap;
    private final FusedLocationProviderClient fusedLocationClient;
    private Marker currentMarker;

    public MapController(Context context, GoogleMap mMap, FusedLocationProviderClient fusedLocationClient) {
        this.context = context;
        this.mMap = mMap;
        this.fusedLocationClient = fusedLocationClient;
    }
    public void initializeMapFeatures() {
        enableUserLocation();
        setMapStyle(0, 25, 0, 0);
        setupMapClickListener();
        addBusStations();
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setNumUpdates(1);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                if (locationResult == null) return;
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

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void setMapStyle(int left, int top, int right, int bottom) {
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));
            if (!success) {
                Log.e("MapStyle", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapStyle", "Can't find style. Error: ", e);
        }

        float density = context.getResources().getDisplayMetrics().density;
        mMap.setPadding(
                (int)(left * density),
                (int)(top * density),
                (int)(right * density),
                (int)(bottom * density)
        );
    }

    private void setupMapClickListener() {
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null) {
                String stationName = (String) marker.getTag();

                if (currentMarker != null) {
                    currentMarker.remove();
                }

                currentMarker = mMap.addMarker(new MarkerOptions()
                        .position(marker.getPosition())
                        .title("Selected: " + stationName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .zIndex(1.0f)); // Force marker to top layer
                currentMarker.showInfoWindow();
                return true;
            }
            return false;
        });
    }


    // !!!!later get from database!!!!
    private void addBusStations() {
        LatLng[] stationLocations = {
                new LatLng(2.3123, 102.3201),
                new LatLng(2.3135, 102.3190),
                new LatLng(2.3150, 102.3178)
        };

        String[] stationNames = {
                "UTeM Main Gate",
                "FKEKK Bus Stop",
                "UTeM Library Stop"
        };

        for (int i = 0; i < stationLocations.length; i++) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(stationLocations[i])
                    .title(stationNames[i])
                    .icon(getBusIcon()));
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_marker)));
            marker.setTag(stationNames[i]);
        }
    }

    private BitmapDescriptor getBusIcon() {
        int height = 110;
        int width = 80;
        BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.bus_station_marker);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(smallMarker);
    }
}