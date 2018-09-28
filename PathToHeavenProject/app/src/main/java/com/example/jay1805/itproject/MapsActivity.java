package com.example.jay1805.itproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.jay1805.itproject.Map.CurrentLocation;
import com.example.jay1805.itproject.Map.GetDirectionsData;
import com.example.jay1805.itproject.Map.GetNearbyPlacesData;
import com.example.jay1805.itproject.Map.Map;
import com.example.jay1805.itproject.Map.URLCreator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

///
    private GoogleApiClient client;
   //////
    private LocationRequest locationRequest;
    private CurrentLocation currentLocation;
    private Map map;

    private URLCreator urlCreator;
    public static final int PERMISSION_REQUEST_LOCATION_CODE = 99;

    LatLng currentDestination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //////////////////////////////////////////////////////////////
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        /////////////////////////////////////////////////////////////
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        urlCreator = new URLCreator();

    }
/////////////////////////////////////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (client == null) {
                            buildGoogleApiClient();
                        }
                        map.enableMyLocation();
                    }
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_LONG).show();
                }
                return;
        }
    }
//////////////////////////////////////////////////////////////////

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            map = new Map(googleMap);
            currentLocation = new CurrentLocation(map);
        }


    }
    ///////////////////////////////////////////////////////////////
    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        client.connect();

    }
    ///////////////////////////////////////////////////////////////

    @Override
    public void onLocationChanged(Location location) {
        currentLocation.changeCurrentLocation(location);


        if (client != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
        }
    }
    /////////////////////////////////////////////////////////////////////////////
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION_CODE);
            }
            return false;
        } else {
            return true;
        }
    }
    /////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
            LocationServices.FusedLocationApi.requestLocationUpdates(client,locationRequest,this);
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void onClick(View v) {
        double latitude = currentLocation.getLatitude();
        double longitude = currentLocation.getLongitude();

        switch (v.getId()) {
            case R.id.B_SEARCH:
                map.clearMap();
                EditText tf_location = (EditText) findViewById(R.id.TF_LOCATION);
                String location = tf_location.getText().toString();
                List<Address> addressList = null;
                MarkerOptions mo = new MarkerOptions();

                if (!location.equals("")) {
                    Geocoder geocoder = new Geocoder(this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 5);
                        for (int i = 0; i < addressList.size(); i++) {
                            Address myAddress = addressList.get(i);
                            currentDestination = new LatLng(myAddress.getLatitude(), myAddress.getLongitude());
                            mo.position(currentDestination);
                            mo.title("Your search results");

                            map.addMarker(mo,currentDestination);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case R.id.B_Hospital:
                showNearbyPlaces("hospital",latitude,longitude);
                break;

            case R.id.B_Restaurant:
                showNearbyPlaces("restaurant",latitude,longitude);
                break;

            case R.id.B_School:
                showNearbyPlaces("school",latitude,longitude);
                break;

            case R.id.B_to:
//                mMap.clear();
//
//                MarkerOptions markerOptions = new MarkerOptions();
//                markerOptions.position(new LatLng(endLatitude,endLongitude));
//                markerOptions.title("Destination");
//
//
//                float results[] = new float[10];
//                Location.distanceBetween(latitude,longitude,endLatitude,endLongitude,results);
//

//                markerOptions.snippet("Distance = " +results[0]);
//                mMap.addMarker(markerOptions);
                Object dataTransfer[] = new Object[3];
                String url = urlCreator.getDirectionsUrl(latitude, longitude, currentDestination.latitude, currentDestination.longitude);
                Log.d("LOL",url);
                GetDirectionsData getDirectionsData = new GetDirectionsData();
                dataTransfer[0] = map;
                dataTransfer[1] = url;
                dataTransfer[2] = currentDestination;
                getDirectionsData.execute(dataTransfer);
                break;

//            case R.id.LATDOWN:
//                latitude--;
//                changeCurrentLocation(latitude, longitude);
//                break;
//            case R.id.LATUP:
//                latitude++;
//                changeCurrentLocation(latitude, longitude);
//                break;
//            case R.id.LNGDOWN:
//                longitude--;
//                changeCurrentLocation(latitude, longitude);
//                break;
//            case R.id.LNGUP:
//                longitude++;
//                changeCurrentLocation(latitude, longitude);
//                break;
        }

    }

    private void showNearbyPlaces(String tag, double latitude, double longitude){
        GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
        Object dataTransfer[] = new Object[2];
        map.clearMap();
        String url = urlCreator.getUrl(latitude, longitude, tag);
        dataTransfer[0] = map;
        dataTransfer[1] = url;

        getNearbyPlacesData.execute(dataTransfer);
        makeToast("Showing Nearby "+tag);

    }

    public void makeToast(String message){
        Toast.makeText(MapsActivity.this, message, Toast.LENGTH_LONG).show();
    }



}