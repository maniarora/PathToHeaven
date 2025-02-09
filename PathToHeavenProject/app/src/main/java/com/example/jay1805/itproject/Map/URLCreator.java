//Class is used to find the URL for API requests.//
package com.example.jay1805.itproject.Map;

import android.util.Log;

public class URLCreator {

    private static final int PROXIMITY_RADIUS = 10000;

    //This creates the URL for the api to find out the directions from one location to another.//
    public String getDirectionsUrl(double latitude, double longitude, double endLatitude,double endLongitude,String mode){
        StringBuilder googleDirectionsUrl = new
                StringBuilder("https://maps.googleapis.com/maps/api/directions/json?");
        googleDirectionsUrl.append("origin="+latitude+","+longitude);
        googleDirectionsUrl.append("&destination="+endLatitude+","+endLongitude);
        googleDirectionsUrl.append("&mode="+mode);
        googleDirectionsUrl.append("&key="+"AIzaSyAIhjebUkrK_zx9sXyjv6ryUVuZV9VsEVk");

        return googleDirectionsUrl.toString();
    }

    //This function would be used if nearby places was implemented, this is a simple addition and
    // extension to our project.//
    public String getUrl(double latitude, double longitude, String nearbyPlace){
        StringBuilder googlePlaceURL = new
                StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceURL.append("location="+latitude+","+longitude);
        googlePlaceURL.append("&radius="+PROXIMITY_RADIUS);
        googlePlaceURL.append("&type="+nearbyPlace);
        googlePlaceURL.append("&sensor=true");
        googlePlaceURL.append("&key="+"AIzaSyD41lfBGXbDzxAWQiB1TKLabumoY2GHOJk");

        Log.d("MapsActivity", "url = "+googlePlaceURL.toString());

        return googlePlaceURL.toString();
    }


}
