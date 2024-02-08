package com.example.maaaappps;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ConfirmLoca extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_LOCATION = 1;
    private GoogleMap mMap;
    private double currentLat, currentLong;
    private Button btFind;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean locationPermissionGranted = false;
    private LatLng currentUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btFind = findViewById(R.id.bt_find);

        SearchView searchView = findViewById(R.id.searchView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        getLocationPermission();

        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnConfirm.setOnClickListener(view -> {
            if (locationPermissionGranted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                    } else {
                        requestLocationUpdates();
                        Toast.makeText(this, "Location not available. Requesting updates...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnCancel.setOnClickListener(view -> {
            // Handle cancel button click
        });

        btFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationPermissionGranted) {
                    if (ActivityCompat.checkSelfPermission(ConfirmLoca.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(ConfirmLoca.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    fusedLocationClient.getLastLocation().addOnSuccessListener(ConfirmLoca.this, location -> {
                        if (location != null) {
                            currentLat = location.getLatitude();
                            currentLong = location.getLongitude();

                            String searchText = ((SearchView) findViewById(R.id.searchView)).getQuery().toString();
                            String formattedSearchText = searchText.replaceAll(" ", "+");

                            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                                    "?location=" + currentLat + "," + currentLong +
                                    "&radius=5000" +
                                    "&keyword=" + formattedSearchText +
                                    "&sensor=true" +
                                    "&key=" + "YOUR_API_KEY";

                            new PlaceTask().execute(url);
                        } else {
                            requestLocationUpdates();
                            Toast.makeText(ConfirmLoca.this, "Location not available. Requesting updates...", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (locationPermissionGranted) {
            getDeviceLocation();
        }
    }

    private void handleApiResponse(String response) {
        if (response != null) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                List<HashMap<String, String>> placesList = new JsonParser().parseResult(jsonObject);

                mMap.clear();

                if (!placesList.isEmpty()) {
                    for (HashMap<String, String> place : placesList) {
                        double lat = Double.parseDouble(place.get("lat"));
                        double lng = Double.parseDouble(place.get("lng"));
                        String name = place.get("name");

                        LatLng latLng = new LatLng(lat, lng);

                        MarkerOptions options = new MarkerOptions();
                        options.position(latLng);
                        options.title(name);
                        mMap.addMarker(options);
                    }

                    LatLng firstMarker = new LatLng(Double.parseDouble(placesList.get(0).get("lat")), Double.parseDouble(placesList.get(0).get("lng")));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstMarker, 15));

                    if (currentUserLocation != null) {
                        MarkerOptions userMarkerOptions = new MarkerOptions()
                                .position(currentUserLocation)
                                .title("Your Location")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        mMap.addMarker(userMarkerOptions);
                    }
                } else {
                    Toast.makeText(ConfirmLoca.this, "No places found", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(ConfirmLoca.this, "Failed to parse JSON response", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(ConfirmLoca.this, "Empty response from server", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateCurrentUserLocation(Location location) {
        if (location != null) {
            currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
        }
    }

    private void getDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLong = location.getLongitude();
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                } else {
                    requestLocationUpdates();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void requestLocationUpdates() {
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(10000);

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult.getLastLocation() != null) {
                LatLng currentLatLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                fusedLocationClient.removeLocationUpdates(this);
            }
        }
    };

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            getDeviceLocation();
        }
    }

    public class PlaceTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                String url = strings[0];
                return downloadUrl(url);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                handleApiResponse(s);
            } else {
                Toast.makeText(ConfirmLoca.this, "Failed to fetch data from server", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String downloadUrl(String string) throws IOException {
        URL url = new URL(string);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        InputStream stream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String data = builder.toString();
        reader.close();
        return data;
    }
}

