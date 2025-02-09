/**TEAM PATH TO HEAVEN
 * Authors:
 *  - Hasitha Dias:   789929
 *  - Jay Parikh:     864675
 *  - Anupama Sodhi:  791288
 *  - Kushagra Gupta: 804729
 *  - Manindra Arora: 827703
 * **/

//This class manages everything regarding the major UI elements dealing with the appication.//

package com.example.jay1805.itproject;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jay1805.itproject.Map.CurrentLocation;
import com.example.jay1805.itproject.Map.DirectionsViewAdapter;
import com.example.jay1805.itproject.Map.GetDirectionsData;
import com.example.jay1805.itproject.Map.Map;
import com.example.jay1805.itproject.Map.RouteData;
import com.example.jay1805.itproject.Map.URLCreator;
import com.example.jay1805.itproject.User.UserListAdapter;
import com.example.jay1805.itproject.User.UserObject;
import com.example.jay1805.itproject.Utilities.CountryToPhonePrefix;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends BaseActivity implements OnMapReadyCallback {

    static final String TAG = MapsActivity.class.getSimpleName();

    //variables about current location
    private CurrentLocation currentLocation;
    private Map map;
    private View mapView;
    private Location lastKnownLoc;
    private LatLng currentRouteLocation;

    //variables for calling
    private AudioPlayer mAudioPlayer;
    private String mCallId;
    private FloatingActionButton endCallButton;


    private String elderlyID;

    //variables for showing directions
    private RecyclerView DirectionsView;
    private RecyclerView.Adapter DirectionsViewAdapter;
    private RecyclerView.LayoutManager DirectionsViewLayoutManager;

    private PlaceAutocompleteFragment placeAutocompleteFragment;

    //variables for showing contacts list
    private RecyclerView userListView;
    private RecyclerView.Adapter userListViewAdapter;
    private RecyclerView.LayoutManager userListViewLayoutManager;
    public ArrayList<UserObject> contactList;
    ArrayList<UserObject> userList;

    private URLCreator urlCreator;

    //variables used with volunteers
    private double volLat=0;
    private double volLongi=0;
    private String currentVolunteerName;
    private boolean volunteerMode = false;


    private HashMap<Marker, String> markers;

    //variables for the menu
    private SlidingUpPanelLayout slidingLayout;
    HashMap<String,Integer> panelHeight;
    String currentPanel;
    String previousPanel="";
    private FloatingActionButton SosButton;
    private Button VolunteersButton;
    private Button ProfileButton;
    private Button LogoutButton;

    //variables for destination,route
    private Marker destinationMarker;
    private MarkerOptions destMarkerOptions;
    private List<Polyline> route;
    LatLng currentDestination;
    private RouteData currentRouteData;
    private String modeOfTransport;

    private boolean helpMode;
    private boolean isEnRoute;
    Marker marker;


    //info needed for help/gps sharing
    HashMap sendRouteInfo = new HashMap<String,String>();
    String shareIDOfElder = "";
    String nameOfElderly = "Elderly";
    Marker markerOfElderly;
    LatLng locationOfElderly;

//////////////////MAIN FUNCTION//////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        initializingVariables();

        gettingPermissions();
        loadMapFragment();
        createAutoCompleteSearch();

        findPanelHeights();
        showCurrentSlider();
        contactListInMenu();

        startingOneSignal();

        createVolunteerChildrenInDB();
        listenerForVolunteerRequest();

        gpsSharing();

        setUpBroadcastReceivers();
        setButtonListeners();
    }


//////////////////MENU AND SLIDING PANEL//////////////////

    //Enables menu to open and close on touch at the bottom of the screen.//
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        View w = getCurrentFocus();
        int scrcoords[] = new int[2];
        w.getLocationOnScreen(scrcoords);
        float y = event.getRawY() + w.getTop() - scrcoords[1];
        int condition = slidingLayout.getPanelHeight();
        if (event.getAction()!=MotionEvent.ACTION_BUTTON_PRESS &&event.getAction()==MotionEvent.
                ACTION_UP&&event.getAction()!=MotionEvent.ACTION_MOVE&&event.getAction()!=MotionEvent.ACTION_BUTTON_RELEASE&&y>(2220-(condition+500))&&y<(2220-condition)){
            if (currentPanel.equals("menu")){
                if (slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.
                        COLLAPSED)){
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                }else if(slidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.
                        HIDDEN)){
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }
        }


        boolean ret = super.dispatchTouchEvent(event);
        return ret;
    }

    //Shows the needed slider.//
    private void showCurrentSlider(){
        hideSliders();
        LinearLayout currentPanelLayout = null;
        switch (currentPanel){
            case "menu":
                currentPanelLayout = findViewById(R.id.sosSlider);
                break;
            case "routeInitial":
                currentPanelLayout = findViewById(R.id.route);
                break;
            case "help":
                currentPanelLayout = findViewById(R.id.help);
                break;
            case "helpRoute":
                currentPanelLayout = findViewById(R.id.helpRoute);
                break;
            case "volunteer":
                currentPanelLayout = findViewById(R.id.volunteerLayout);
                break;
        }
        if (currentPanelLayout!=null){
            currentPanelLayout.setVisibility(View.VISIBLE);
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            findPanelHeights();
        }

    }

    //Sets the panel height for the current slider.//
    private void setPanelHeight() {
        if (panelHeight.containsKey(currentPanel)){
            slidingLayout.setPanelHeight(panelHeight.get(currentPanel));
        }else{
            slidingLayout.setPanelHeight(144);
        }
    }

    //Hides all other sliders to display the correct one.//
    private void hideSliders(){
        LinearLayout route = findViewById(R.id.route);
        LinearLayout sos = findViewById(R.id.sosSlider);
        LinearLayout help = findViewById(R.id.help);
        LinearLayout helpRoute = findViewById(R.id.helpRoute);
        LinearLayout volunteer = findViewById(R.id.volunteerLayout);
        route.setVisibility(View.GONE);
        sos.setVisibility(View.GONE);
        help.setVisibility(View.GONE);
        helpRoute.setVisibility(View.GONE);
        volunteer.setVisibility(View.GONE);
    }

    //Finds panel height using the heights for the layouts.//
    private void findPanelHeights(){
        final LinearLayout initialRouteLayout = (LinearLayout)findViewById(R.id.initialRouteLayout);
        initialRouteLayout.post(new Runnable(){
            public void run(){
                panelHeight.put("routeInitial",initialRouteLayout.getHeight());
                setPanelHeight();
            }
        });

        final LinearLayout menuLayout = (LinearLayout)findViewById(R.id.menuLayout);
        menuLayout.post(new Runnable(){
            public void run(){
                panelHeight.put("menu",menuLayout.getHeight());
                setPanelHeight();
            }
        });
        final LinearLayout helpLayout = (LinearLayout)findViewById(R.id.helpLayout);
        helpLayout.post(new Runnable(){
            public void run(){
                panelHeight.put("help",helpLayout.getHeight());
                setPanelHeight();
            }
        });
        final LinearLayout helpRouteLayout = (LinearLayout)findViewById(R.id.helpRouteLayout);
        helpRouteLayout.post(new Runnable(){
            public void run(){
                panelHeight.put("helpRoute",helpRouteLayout.getHeight());
                setPanelHeight();
            }
        });
        final LinearLayout volunteerLayout = (LinearLayout)findViewById(R.id.volunteerSliderLayout);
        helpRouteLayout.post(new Runnable(){
            public void run(){
                panelHeight.put("volunteer",volunteerLayout.getHeight());
                setPanelHeight();
            }
        });
    }


//////////////////BROADCAST RECEIVERS//////////////////

    private void setUpBroadcastReceivers() {
        //Broadcast receiver for location updates.//
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("GPSLocationUpdates"));

        //Broadcast receiver for when volunteer disconnected.//
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        FirebaseDatabase.getInstance().getReference().child("user").
                                child(FirebaseAuth.getInstance().getCurrentUser().getUid()).
                                child("accepted").setValue("null");
                    }
                }, new IntentFilter("Make Null"));

        //Broadcast receiver for making a toast.//
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String message = intent.getStringExtra("message");
                        makeToast(message);
                    }
                }, new IntentFilter("Make Toast"));

        //Broadcast receiver for when route is successfully displayed on maps.//
        LocalBroadcastManager.getInstance(this).registerReceiver(
                onRouteSuccess, new IntentFilter("RouteSuccess"));

        //Broadcast receiver for when sharing ID is created. Creates stop GPS sharing button,
        // listener. sets the panels and add a firebase listener for when route gets added,
        // to be displayed.//
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final Button stopButton = findViewById(R.id.B_StopGPSShare);
                        stopButton.setVisibility(View.VISIBLE);
                        stopButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent("STOP GPS");
                                LocalBroadcastManager.getInstance(view.getContext()).
                                        sendBroadcast(intent);
                                SosButton.setVisibility(View.GONE);
                                placeAutocompleteFragment.setText("");
                                view.setVisibility(View.GONE);

                                LinearLayout enRouteLayout = findViewById(R.id.enRouteLayout);
                                enRouteLayout.setVisibility(View.GONE);

                                currentPanel = "menu";
                                showCurrentSlider();

                                map.zoomToLocation(currentLocation.getCurrentLocation());
                                modeOfTransport = "walking";
                                isEnRoute = false;

                                stopButton.setVisibility(View.GONE);
                                map.clearMap();
                                currentLocation.showCurrentLocation();
                            }
                        });
                        String shareID = intent.getStringExtra("ID");
                        FirebaseDatabase.getInstance().getReference().child("gps-sharing").
                                child(shareID).child("route").addValueEventListener(
                                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()){
                                    double latitude = Double.parseDouble(dataSnapshot.
                                            child("destLat").getValue().toString());
                                    double longitude = Double.parseDouble(dataSnapshot.
                                            child("destLon").getValue().toString());
                                    LatLng dest = new LatLng(latitude,longitude);
                                    String mode = dataSnapshot.child("mode").getValue().toString();
                                    ///////////////////change button when this is pressed
                                    modeOfTransport = mode;
                                    map.clearMap();
                                    LatLng curLoc = new LatLng(currentLocation.getLatitude(),
                                            currentLocation.getLongitude());
                                    currentDestination = dest;
                                    SosButton.setVisibility(View.VISIBLE);
                                    currentPanel = "routeInitial";
                                    showCurrentSlider();
                                    setDestinationMarker(dest,true);
                                    findRoute(curLoc,dest);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }, new IntentFilter("GPS ID"));

        //Broadcast receiver for when call is created.//
        LocalBroadcastManager.getInstance(this).registerReceiver(
                callInMaps, new IntentFilter("Call ID"));
    }

    //Broadcast receiver for call receivers within maps.//
    BroadcastReceiver callInMaps = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            mAudioPlayer = new AudioPlayer(getApplicationContext());
            mCallId = intent.getStringExtra(SinchService.CALL_ID);
            if(mCallId!=null) {
                endCallButton.setVisibility(View.VISIBLE);
            }
            else {
                endCallButton.setVisibility(View.GONE);
            }
            onServiceConnected();
        }
    };


//////////////////INTIALISING MAPS//////////////////

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     * It also moves the current location button to required position.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map = new Map(googleMap,getApplicationContext());
            currentLocation = new CurrentLocation(map);
            askForCurrentLocation();
        }

        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).
                    getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 1500);
        }

    }

    //Loads the map onto the fragment.//
    private void loadMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);
    }

    //Gets permission to use all services.//
    private void gettingPermissions() {
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.
                RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.
                checkSelfPermission(MapsActivity.this, android.Manifest.permission.
                        READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.
                checkSelfPermission(MapsActivity.this, android.Manifest.permission.
                        ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.
                        permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.
                        WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED || ContextCompat.
                checkSelfPermission(MapsActivity.this, Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO, android.Manifest.
                            permission.READ_PHONE_STATE,android.Manifest.permission.
                            ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS},
                    1);
        }
    }


//////////////////CURRENT LOCATION//////////////////

    //Sends broadcast to ask for current location from sensor service.//
    private void askForCurrentLocation() {
        Intent intent = new Intent("SEND GPS");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    //Sets the current location when new location is received and updates the route if moved
    // more than 10m.//
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("Status");
            Bundle b = intent.getBundleExtra("Location");
            lastKnownLoc = (Location) b.getParcelable("Location");
            if (lastKnownLoc != null) {
                if (currentLocation.getCurrentLocation()==null){
                    currentLocation.changeCurrentLocation(lastKnownLoc);
                    map.zoomToLocation(currentLocation.getCurrentLocation());
                }else{
                    currentLocation.changeCurrentLocation(lastKnownLoc);
                }
                if (isEnRoute){
                    map.currentLocationZoom(currentLocation.getCurrentLocation());
                    Location prevLoc = new Location("");
                    prevLoc.setLatitude(currentRouteLocation.latitude);
                    prevLoc.setLongitude(currentRouteLocation.longitude);
                    if (prevLoc.distanceTo(lastKnownLoc)>10){
//                        isEnRoute = false;
                        map.clearMap();
                        setDestinationMarker(currentDestination,false);
                        findRoute(currentLocation.getCurrentLocation(),new LatLng(
                                currentDestination.latitude,currentDestination.longitude));
                    }
                }
            }
        }
    };

//////////////////ROUTE//////////////////

    //Handles all on click events regarding route.//
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.B_to:
                map.clearMap();
                destinationMarker = map.addMarker(destMarkerOptions,currentDestination, true);
                LatLng location = new LatLng(currentLocation.getLatitude(),currentLocation.
                        getLongitude());
                findRoute(location,currentDestination);
                break;

            case R.id.B_RouteToElder:
                map.clearMap();
                setMarkerForElderlyPerson();
                LatLng location_current = new LatLng(currentLocation.getLatitude(),currentLocation.
                        getLongitude());
                findRoute(location_current,locationOfElderly);
                sendRouteInfo = new HashMap<String,String>();
                sendRouteInfo.put("destLat",location_current.latitude);
                sendRouteInfo.put("destLon",location_current.longitude);
                sendRouteInfo.put("mode",modeOfTransport);
                break;
            case R.id.B_SendRoute:
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference ref = database.getReference().child("gps-sharing").
                        child(shareIDOfElder);
                java.util.Map hashMap = new HashMap<>();
                hashMap.put("route",sendRouteInfo);
                ref.updateChildren(hashMap);
                break;
            case R.id.B_ElderToDestination:
                map.clearMap();
                setMarkerForElderlyPerson();
                setDestinationMarker(currentDestination,true);
                findRoute(locationOfElderly, currentDestination);
                sendRouteInfo = new HashMap<String,String>();
                sendRouteInfo.put("destLat",currentDestination.latitude);
                sendRouteInfo.put("destLon",currentDestination.longitude);
                sendRouteInfo.put("mode",modeOfTransport);
                break;
            case R.id.B_bike:
                setBackgroundForModesOfTransport(R.id.B_bike);
                modeOfTransport = "bicycling";
                break;
            case R.id.B_car:
                setBackgroundForModesOfTransport(R.id.B_car);
                modeOfTransport = "driving";
                break;
            case R.id.B_walk:
                setBackgroundForModesOfTransport(R.id.B_walk);
                modeOfTransport = "walking";
                break;
            case R.id.B_transit:
                setBackgroundForModesOfTransport(R.id.B_transit);
                modeOfTransport = "transit";
                break;

        }

    }

    //Finds and displays the route.//
    private void findRoute(final LatLng currentLocation, final LatLng currentDestination){
        final LatLng curr = currentLocation;
        final LatLng dest = currentDestination;
        Object dataTransfer[] = new Object[3];
        String url = urlCreator.getDirectionsUrl(currentLocation.latitude, currentLocation.
                longitude, currentDestination.latitude, currentDestination.longitude,
                modeOfTransport);
        currentRouteData = new RouteData();
        GetDirectionsData getDirectionsData = new GetDirectionsData(currentRouteData,
                new OnEventListener() {
            @Override
            public void onSuccess(Object object) {
                if (!isEnRoute){
                    map.showEntireRoute(curr,dest);
                }
                currentRouteLocation = currentLocation;
                printRouteInfo(currentRouteData.getRouteInformation(),currentRouteData.
                        getStepInformation());
            }

            @Override
            public void onFailure(Exception e) {
                makeToast("Route Not Found");
            }
        });
        dataTransfer[0] = map;
        dataTransfer[1] = url;
        dataTransfer[2] = currentDestination;
        getDirectionsData.execute(dataTransfer);
        sendMessageToActivity(url, currentDestination);
        route = getDirectionsData.getRoute();
    }

    //Changes the maneuver symbols to obtain the correct symbol.//
    private ArrayList change(ArrayList step){
        ArrayList <HashMap> stepUpdated = new ArrayList<>();

        for (int i=0;i<step.size();i++){
            HashMap info = (HashMap) step.get(i);
            String str = info.get("Maneuver").toString().replaceAll("-","_");
            String drawable = "direction_"+str;
            int resID =getResources().getIdentifier(drawable, "drawable", getPackageName());
            info.put("manRes", Integer.toString(resID));
            stepUpdated.add(info);
        }
        return stepUpdated;
    }

    //Changes the background to show selection.//
    private void setBackgroundForModesOfTransport(Integer selectedButton){
        final ImageButton button_Walk = (ImageButton) findViewById(R.id.B_walk);
        ImageButton button_Drive = (ImageButton) findViewById(R.id.B_car);
        ImageButton button_Transit = (ImageButton) findViewById(R.id.B_transit);
        ImageButton button_Bike = (ImageButton) findViewById(R.id.B_bike);
        button_Walk.setBackgroundResource(R.color.blue_A400);
        button_Drive.setBackgroundResource(R.color.blue_A400);
        button_Transit.setBackgroundResource(R.color.blue_A400);
        button_Bike.setBackgroundResource(R.color.blue_A400);
        ImageButton button = (ImageButton) findViewById(selectedButton);
        button.setBackgroundResource(R.color.grey_700);
    }

    //Shows distance, duration and the directions for the route.//
    private void printRouteInfo(HashMap<String,String> routeInformation, ArrayList stepInformation){
        LinearLayout enRouteLayout = findViewById(R.id.enRouteLayout);
        enRouteLayout.setVisibility(View.VISIBLE);
        findPanelHeights();
        TextView distDur = findViewById(R.id.distanceDuration);
        String summary="Path Information";
        if (routeInformation.containsKey("Summary")){
            summary = routeInformation.get("Summary");
        }
        distDur.setText(summary+":- "+routeInformation.get("Distance")+"("+routeInformation.
                get("Duration")+")");
        final Button enRoute = findViewById(R.id.enRoute);
        enRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEnRoute){
                    isEnRoute = false;
                    map.showEntireRoute(currentLocation.getCurrentLocation(),currentDestination);
                    enRoute.setText("En Route");
                }else{
                    isEnRoute=true;
                    enRoute.setText("Terminate");
                    map.currentLocationZoom(currentLocation.getCurrentLocation());
                }
            }
        });
        ArrayList newSteps = change(stepInformation);
        DirectionsView = findViewById(R.id.List_Directions);
        DirectionsView.setNestedScrollingEnabled(false);
        DirectionsView.setHasFixedSize(false);
        DirectionsViewLayoutManager = new LinearLayoutManager(getApplicationContext(),
                LinearLayout.VERTICAL, false);
        DirectionsView.setLayoutManager(DirectionsViewLayoutManager);
        DirectionsViewAdapter = new DirectionsViewAdapter(newSteps);
        DirectionsView.setAdapter(DirectionsViewAdapter);
    }

    //Calls necessary function when correct route is found and displayed.//
    private BroadcastReceiver onRouteSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            printRouteInfo(currentRouteData.getRouteInformation(),currentRouteData.
                    getStepInformation());
        }
    };

    //Sets a red marker for the destination.//
    private void setDestinationMarker(LatLng dest,boolean zoom){
        destMarkerOptions = new MarkerOptions();
        destMarkerOptions.position(dest);
        destMarkerOptions.title("Your search results");
        destMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.
                HUE_RED));
        destinationMarker = map.addMarker(destMarkerOptions, dest,zoom);
    }


//////////////////GPS AND ROUTE SHARING//////////////////

    //Finds whether in help or volunteer mode and sets the correct slider accordingly.//
    private void gpsSharing() {

        String userID;
        Intent intent = getIntent();
        if (intent.hasExtra("Share ID") && intent.getExtras().containsKey("Share ID") &&
                intent.getExtras().containsKey("userID")) {
            currentPanel = "help";
            showCurrentSlider();
            sendRouteInfo = new HashMap<String,String>();
            shareIDOfElder = intent.getExtras().getString("Share ID");
            userID = intent.getExtras().getString("userID");
            setInitialInfoForHelp(userID);
            FirebaseDatabase.getInstance().getReference().child("gps-sharing").
                    child(shareIDOfElder).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        setGPSSharing(shareIDOfElder);
                    }else{
                        /////////send name of elderly person
                        makeToast("Elderly Person has Disabled GPS Sharing");
                        helpMode = false;
                        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    makeToast("Tracking has Stopped");
                    helpMode = false;
                    startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                }
            });

        }
        if (intent.hasExtra("elderlyID") && intent.getExtras().containsKey("elderlyID")){
            userID = intent.getExtras().getString("elderlyID");
            elderlyID =userID;
            currentPanel = "volunteer";
            showCurrentSlider();
            volunteerMode = true;
            setInitialInfoForHelp(userID);
            showElderlyLocationForVolunteer(elderlyID);
        }
    }

    //Adds the listener to find when current location of elderly changes.//
    private void setGPSSharing(String shareID){

        // Get a reference to our posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("gps-sharing").
                child(shareID);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double newLatitude = 0;
                double newLongitude = 0;
                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()){
                    if (childSnapshot.getKey().equals("latitude")){
                        newLatitude = Double.parseDouble(childSnapshot.getValue().toString());
                    }
                    if (childSnapshot.getKey().equals("longitude")){
                        newLongitude = Double.parseDouble(childSnapshot.getValue().toString());
                    }
                }

                helpMode = true;
                if (markerOfElderly!=null){
                    markerOfElderly.remove();
                }


                locationOfElderly= new LatLng(newLatitude,newLongitude);

                setMarkerForElderlyPerson();
                map.zoomToLocation(locationOfElderly);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                makeToast("Tracking has Stopped");
                helpMode = false;
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
            }
        });
    }

    //Sets the listeners for the call and chat buttons for all types of sliders.//
    private void setInitialInfoForHelp(final String elderlyID) {

        ImageButton callHelper;
        ImageButton chatHelper;

        ImageButton callHelperRoute;
        ImageButton chatHelperRoute;
        ImageButton callHelperV;
        ImageButton chatHelperV;

        final ArrayList<String> CurrentUserChatIDs = new ArrayList<String>();
        final ArrayList<String> ToUserChatIDs = new ArrayList<String>();

        FirebaseDatabase.getInstance().getReference().child("user").child(elderlyID).child("name").
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nameOfElderly = dataSnapshot.getValue().toString();
                TextView nameTextView = findViewById(R.id.nameHelp);
                nameTextView.setText(nameOfElderly);
                TextView nameTextView1 = findViewById(R.id.nameHelpRoute);
                nameTextView1.setText(nameOfElderly);
                TextView VnameTextView = findViewById(R.id.Vname);
                VnameTextView.setText(nameOfElderly);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        TextView nameTextView = findViewById(R.id.nameHelp);
        nameTextView.setText(nameOfElderly);
        nameTextView = findViewById(R.id.nameHelpRoute);
        nameTextView.setText(nameOfElderly);

        callHelper = findViewById(R.id.callButtonHelp);
        chatHelper = findViewById(R.id.chatButtonHelp);
        callHelperRoute = findViewById(R.id.callButtonHelpRoute);
        chatHelperRoute = findViewById(R.id.chatButtonHelpRoute);
        callHelperV = findViewById(R.id.VcallButtonHelp);
        chatHelperV = findViewById(R.id.VchatButtonHelp);


        FirebaseDatabase.getInstance().getReference().child("user").child(FirebaseAuth.
                getInstance().getCurrentUser().getUid()).child("chat").
                addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot childSnapShot : dataSnapshot.getChildren()) {
                    CurrentUserChatIDs.add(childSnapShot.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        View.OnClickListener callListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call call = getSinchServiceInterface().callUser(elderlyID);
                String callId = call.getCallId();
                Intent callScreen = new Intent(v.getContext(), CallScreenActivity.class);
                callScreen.putExtra(SinchService.CALL_ID, callId);
                startActivity(callScreen);
            }
        };

        callHelper.setOnClickListener(callListener);
        callHelperRoute.setOnClickListener(callListener);

        callHelperV.setOnClickListener(callListener);


        View.OnClickListener chatListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseDatabase.getInstance().getReference().child("user").child(elderlyID).
                        child("chat").addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                String chatIDKey = null;

                                for (DataSnapshot childSnapShot : dataSnapshot.getChildren()) {
                                    ToUserChatIDs.add(childSnapShot.getKey());
                                }

                                Boolean chatExists = false;
                                for(String MyChatIDs : CurrentUserChatIDs) {
                                    for(String ToChatIDs : ToUserChatIDs) {
                                        if(MyChatIDs.equals(ToChatIDs)) {
                                            chatIDKey = MyChatIDs;
                                            chatExists = true;
                                        }
                                    }
                                }

                                if(chatExists.equals(false)) {
                                    String key = FirebaseDatabase.getInstance().getReference().
                                            child("chat").push().getKey();
                                    CurrentUserChatIDs.add(key);
                                    ToUserChatIDs.add(key);
                                    chatIDKey = key;
                                    FirebaseDatabase.getInstance().getReference().child("user").
                                            child(FirebaseAuth.getInstance().getUid()).
                                            child("chat").child(key).setValue(true);
                                    FirebaseDatabase.getInstance().getReference().child("user").
                                            child(elderlyID).child("chat").child(key).
                                            setValue(true);
                                }

                                else {
                                    Toast.makeText(getApplicationContext(),
                                            "Chat Already Exists", Toast.LENGTH_LONG).show();
                                }

                                Intent intent = new Intent(getApplicationContext(),
                                        ChatActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putString("chatID", chatIDKey);
                                intent.putExtras(bundle);
                                getApplicationContext().startActivity(intent);

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        }
                );}};
        chatHelper.setOnClickListener(chatListener);
        chatHelperRoute.setOnClickListener(chatListener);
        chatHelperV.setOnClickListener(chatListener);


    }

    //Creates green marker to show location of the elderly person.//
    private void setMarkerForElderlyPerson(){
        MarkerOptions mo = new MarkerOptions();
        mo.position(locationOfElderly);
        mo.title("Location of Elderly");
        mo.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        markerOfElderly = map.addMarker(mo,locationOfElderly, true);
    }


//////////////////HELPER FUNCTIONS//////////////////

    private void initializingVariables() {
        helpMode = false;
        slidingLayout = (SlidingUpPanelLayout)findViewById(R.id.sliding_layout);
        isEnRoute = false;
        currentPanel = "menu";
        panelHeight = new HashMap<>();
        markers = new HashMap<Marker, String>();
        urlCreator = new URLCreator();
        lastKnownLoc = null;

    }

    //Broadcasting message.//
    private void sendMessageToActivity(String url, LatLng dest) {
        Intent intent = new Intent("New Route");
        // You can also include some extra data.
        intent.putExtra("url", url);
        Bundle b = new Bundle();
        b.putParcelable("dest", dest);
        intent.putExtra("dest", b);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void makeToast(String message){
        Toast.makeText(MapsActivity.this, message, Toast.LENGTH_LONG).show();
    }

    //Setting all button listeners.//
    private void setButtonListeners(){
        menuListeners();
        sosButtonListener();
        ImageButton cancelVolunteer = findViewById(R.id.cancelConnectionButton);
        cancelVolunteer.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View view) {
                Intent mapScreen = new Intent(getApplicationContext(), MapsActivity.class);
                FirebaseDatabase.getInstance().getReference().child("user").child(elderlyID).
                        child("accepted").setValue("false");
                FirebaseDatabase.getInstance().getReference().child("user").
                        child(FirebaseAuth.getInstance().getCurrentUser().getUid()).
                        child("Requested").setValue("False");
                startActivity(mapScreen);
            }
        });
    }

    //Button listeners for buttons on the menu.//
    private void menuListeners() {
        VolunteersButton = findViewById(R.id.volunteersButton);
        ProfileButton = findViewById(R.id.profileButton);
        LogoutButton = findViewById(R.id.logoutButton);
        endCallButton = findViewById(R.id.endCallButton);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });

        VolunteersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaceVolunteerMarkerOnMap();
            }
        });

        ProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MyProfileActivity.class));
            }
        });

        LogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                // make sure the user is who he says he is
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
        });
    }

    //Setting initial the SOS button to make the sliding layout appear/disappear.//
    private void sosButtonListener() {
        slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        SosButton = findViewById(R.id.floatingButton);
        SosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previousPanel.equals("routeInitial")&&currentPanel.equals("menu")){
                    currentPanel = "routeInitial";
                    previousPanel = "menu";
                    showCurrentSlider();
                }else if (currentPanel.equals("routeInitial")){
                    currentPanel = "menu";
                    previousPanel ="routeInitial";
                    showCurrentSlider();
                }else{
                    currentPanel = "menu";
                    showCurrentSlider();
                }
            }
        });
    }


//////////////////SEARCH//////////////////
    //Creates the autocomplete fragment. Sets the sliders accordingly.
    // Takes required actions for autocomplete is closed.//
    private void createAutoCompleteSearch() {
        placeAutocompleteFragment = (PlaceAutocompleteFragment)getFragmentManager().
                findFragmentById(R.id.place_autocomplete_fragment);

        placeAutocompleteFragment.setFilter(new AutocompleteFilter.Builder().setCountry("AU").
                build());

        EditText SearchInput = placeAutocompleteFragment.getView().
                findViewById(R.id.place_autocomplete_search_input);
        SearchInput.setHintTextColor(getResources().getColor(R.color.grey_700));

        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (!helpMode){
                    SosButton.setVisibility(View.VISIBLE);
                }
                currentLocation.hideCurrentLocation();
                final LatLng latLngLoc = place.getLatLng();
                if(marker!=null){
                    marker.remove();
                }
                map.clearMap();
                currentDestination = latLngLoc;

                setDestinationMarker(currentDestination,true);

                modeOfTransport = "walking";

                if (helpMode){
                    currentPanel = "helpRoute";
                    showCurrentSlider();
                }else {
                    currentPanel = "routeInitial";
                    showCurrentSlider();
                }

            }

            @Override
            public void onError(Status status) {
                Toast.makeText(MapsActivity.this, ""+status.toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        placeAutocompleteFragment.getView().findViewById(R.id.place_autocomplete_clear_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //way to access view from PlaceAutoCompleteFragment
                        SosButton.setVisibility(View.GONE);
                        placeAutocompleteFragment.setText("");
                        view.setVisibility(View.GONE);
                        LinearLayout enRouteLayout = findViewById(R.id.enRouteLayout);
                        enRouteLayout.setVisibility(View.GONE);
                        currentPanel = "menu";
                        showCurrentSlider();
                        map.clearMap();
                        currentLocation.showCurrentLocation();
                        map.zoomToLocation(currentLocation.getCurrentLocation());
                        modeOfTransport = "walking";
                        isEnRoute = false;
                    }
                });
    }


//////////////////SETTING UP CONTACT LIST AND CALLS//////////////////

    //Starting one signal services.//
    private void startingOneSignal() {
        OneSignal.startInit(this).setNotificationOpenedHandler(new
                NotificationIsOpened(getApplicationContext())).init();
        OneSignal.setSubscription(true);
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {

                FirebaseDatabase.getInstance().getReference().child("user").child(FirebaseAuth.
                        getInstance().getCurrentUser().getUid()).child("notificationKey").
                        setValue(userId);
            }
        });
        OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);
    }

    private void contactListInMenu() {
        contactList = new ArrayList<>();
        userList = new ArrayList<>();
        //initializeRecyclerView();
        userListView = findViewById(R.id.userList);
        userListView.setNestedScrollingEnabled(false);
        userListView.setHasFixedSize(false);
        userListViewLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayout.
                VERTICAL, false);
        userListView.setLayoutManager(userListViewLayoutManager);

        FirebaseDatabase.getInstance().getReference().child("user").addListenerForSingleValueEvent(
                new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userListViewAdapter = new UserListAdapter(userList, getSinchServiceInterface(),
                        slidingLayout);
                userListView.setAdapter(userListViewAdapter);
                getContactList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getContactList() {
        String isoPrefix = getCountryIso();
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.
                CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()) {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.
                    Phone.DISPLAY_NAME));
            String phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.
                    Phone.NUMBER));

            phone = phone.replace(" ", "");
            phone = phone.replace("-", "");
            phone = phone.replace("(", "");
            phone = phone.replace(")", "");

            if(!String.valueOf(phone.charAt(0)).equals("+")) {
                phone = isoPrefix + phone;
            }
            UserObject mContacts = new UserObject(name, phone, "", "");
            contactList.add(mContacts);
            getUserDetails(mContacts);
        }
    }

    private void getUserDetails(UserObject mContacts) {
        DatabaseReference userDB = FirebaseDatabase.getInstance().getReference().child("user");
        Query query = userDB.orderByChild("phone").equalTo(mContacts.getPhone());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    String phone = "", name = "", myNotificationKey="";
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        if(childSnapshot.child("phone").getValue() != null) {
                            phone = childSnapshot.child("phone").getValue().toString();
                        }
                        if(childSnapshot.child("name").getValue() != null) {
                            name = childSnapshot.child("name").getValue().toString();
                        }
                        if(childSnapshot.child("notificationKey").getValue() != null) {
                            myNotificationKey = childSnapshot.child("notificationKey").getValue().
                                    toString();
                        }

                        UserObject mUser = new UserObject(name, phone, childSnapshot.getKey(),
                                myNotificationKey);
                        for(UserObject mContactIterator : contactList) {
                            if(mContactIterator.getPhone().equals(phone)) {
                                mUser.setName(mContactIterator.getName());

                            }
                        }

                        userList.add(mUser);
                        userListViewAdapter.notifyDataSetChanged();
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getCountryIso() {
        String ISO = null;

        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().
                getSystemService(getApplicationContext().TELEPHONY_SERVICE);

        if(telephonyManager.getNetworkCountryIso() != null) {
            if(telephonyManager.getNetworkCountryIso().toString().equals("")) {
                ISO = telephonyManager.getNetworkCountryIso().toString();
            }
        }
        if (ISO == null) {
            return "";
        }
        return CountryToPhonePrefix.getPhone(ISO);

    }
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }

    }

    @Override
    public void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.addCallListener(new SinchCallListener());
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        endCallButton.setVisibility(View.GONE);
    }


    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {

            CallEndCause cause = call.getDetails().getEndCause();
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = "Call ended: " + call.getDetails().toString();
            Toast.makeText(MapsActivity.this, endMsg, Toast.LENGTH_LONG).show();
            endCall();

        }

        @Override
        public void onCallEstablished(Call call) {
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        @Override
        public void onCallProgressing(Call call) {
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }
    }

//////////////////VOLUNTEERS//////////////////

    private void listenerForVolunteerRequest() {
        FirebaseDatabase.getInstance().getReference().child("user").child(FirebaseAuth.
                getInstance().getCurrentUser().getUid()).child("Requested").addValueEventListener(
                        new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()!=null) {
                    if (dataSnapshot.getValue().toString().equals("True")) {
                        FirebaseDatabase.getInstance().getReference().child("user").child(
                                FirebaseAuth.getInstance().getCurrentUser().getUid()).
                                child("ElderlyIDRequested").addListenerForSingleValueEvent(
                                        new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                elderlyID = dataSnapshot.getValue().toString();
                                Intent intent = new Intent(getApplicationContext(),
                                        NotificationActivity.class);
                                intent.putExtra("elderlyID",elderlyID);
                                java.util.Map map = new HashMap<>();
                                final DatabaseReference userDB = FirebaseDatabase.getInstance().
                                        getReference().child("user").child(FirebaseAuth.
                                        getInstance().getCurrentUser().getUid());
                                map.put("Requested", "False");
                                userDB.updateChildren(map);
                                startActivity(intent);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }

//
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showElderlyLocationForVolunteer(String elderlyID) {

        FirebaseDatabase.getInstance().getReference().child("user").child(elderlyID).
                addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                double newLatitude = 0;
                double newLongitude = 0;
                newLatitude = Double.parseDouble(dataSnapshot.child("latitude").getValue().
                        toString());

                newLongitude = Double.parseDouble(dataSnapshot.child("longitude").getValue().
                        toString());

                if (markerOfElderly!=null){
                    markerOfElderly.remove();
                }

                locationOfElderly= new LatLng(newLatitude,newLongitude);

                setMarkerForElderlyPerson();
                map.zoomToLocation(locationOfElderly);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void createVolunteerChildrenInDB() {
        java.util.Map hmap = new HashMap<>();
        final DatabaseReference userDB = FirebaseDatabase.getInstance().getReference().
                child("user").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        hmap.put("Requested", "False");
        hmap.put("ElderlyIDRequested","");
        hmap.put("accepted","null");
        userDB.updateChildren(hmap);
    }

    private void PlaceVolunteerMarkerOnMap() {
        FirebaseDatabase.getInstance().getReference().child("user").addListenerForSingleValueEvent(
                new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot childsnapshot : dataSnapshot.getChildren()) {
                    for(DataSnapshot volunteersnapshot : childsnapshot.getChildren())
                    {

                        // getting volunteers' coordinates
                        if (volunteersnapshot.getKey().equals("Volunteer") && volunteersnapshot.
                                getValue().toString().equals("Yes")) {
                            for(DataSnapshot volunteersnapshot2 : childsnapshot.getChildren())
                            {
                                if (volunteersnapshot2.getKey().equals("latitude")) {
                                    volLat = Double.parseDouble(volunteersnapshot2.getValue().
                                            toString());
                                }
                                if (volunteersnapshot2.getKey().equals("longitude")) {
                                    volLongi = Double.parseDouble(volunteersnapshot2.getValue().
                                            toString());

                                }
                                if (volunteersnapshot2.getKey().equals("name")) {
                                    currentVolunteerName = volunteersnapshot2.getValue().toString();
                                }


                                if(( volLat!=0 && volLongi!=0 && currentVolunteerName!=""))
                                {
                                    MarkerOptions mo = new MarkerOptions();
                                    LatLng volLatLng = new LatLng(volLat,volLongi);

                                    mo.position(volLatLng);
                                    mo.title(currentVolunteerName);
                                    mo.icon(BitmapDescriptorFactory.
                                            fromResource(R.mipmap.ic_volunteer2));
                                    //mo.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person));


                                    markers.put(map.addMarker(mo,volLatLng,true),
                                            childsnapshot.getKey());
                                    currentVolunteerName = "";
                                }
                            }
                        }
                    }


                }
                map.setListOfVolunteers(markers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}