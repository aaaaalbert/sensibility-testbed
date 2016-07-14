package com.snakei;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.sensibility_testbed.SensibilityApplication;

/**
 * Created by lukas.puehringer@nyu.edu
 * on 5/4/16.
 *
 * A pseudo Service class that facades Android location services
 *
 * Provides methods to start and stop location services, poll updated
 * location information for different providers, poll last known location
 * information for different providers and perform reverse geocoding
 *
 * Currently provides three location providers
 * GPS and Network (Android Location API) and Fused (Google Play Services)
 *
 * Revers geocoding uses Google Play Services and requires an internet connection
 *
 * Location and address objects are returned as serialized JSON
 *
 * This class is a Singleton using the thread safe
 * Java Initialization on Demand Holder pattern
 * (cf. SensorService.java for more info )
 *
 * Todo:
 *   Maybe we can generalize some common aspects of all the facades
 *     (Real sensors, location, media, miscinfo,...)
 *   Decide how to handle resource allocation and release in multi-threading
 *   environments, keep battery drain in mind
 *
 */


public class LocationService implements android.location.LocationListener,
        com.google.android.gms.location.LocationListener {

    static final String TAG = "LocationService";

    // Used to start/stop listener on network and gps location provider
    private LocationManager location_manager;

    // Used to connect to Google Play Service
    private GoogleApiClient google_api_client;
    // Used to define accuracy and frequency of Google Play Services
    // location updates
    private LocationRequest google_location_request;

    // We need to make those class members to be able to
    // unregister them in their own callback functions
    private ConnectionCallbacks google_api_connection_callbacks;
    private OnConnectionFailedListener google_api_connection_failed_callbacks;

    // Used to transform lat/long to addresses or vice-versa
    // Requires Google Play Service
    private Geocoder geocoder;

    // Location JSON object for each provider
    private JSONObject location_gps_json;
    private JSONObject location_network_json;
    private JSONObject location_fused_json;


    /* See Initialization on Demand Holder pattern */
    private static class LocationServiceHolder {
        private static final LocationService instance = new LocationService();
    }


    /* Singleton Instance Getter */
    public static LocationService getInstance(){
        return LocationServiceHolder.instance;
    }


    /*
     * Singleton Constructor
     *
     * Fetches context from static application function
     * Initializes Android location manager and Google Play Service objects
     *
     */
    private LocationService() {
        Context app_context = SensibilityApplication.getAppContext();
        location_manager = (LocationManager)app_context.getSystemService(
                app_context.LOCATION_SERVICE);

        // Set Google Play Service QoS parameters to "real-time"
        google_location_request = new LocationRequest();
        google_location_request.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        google_location_request.setInterval(5);

        geocoder = new Geocoder(app_context, Locale.getDefault());
    }


    /*
     * Registers location update listeners for GPS and network provider and 
     * connects to Google API client, upon connection it will also register
     * a location update listener
     *
     */
    public void start_location() {
        // There is no use in listening for PASSIVE_PROVIDER if one of the
        // other two is registered
        // It only retrieves values if any other app is listening to a gps or
        // network provider
        // If they can we can too, furthermore we need the same permissions
        // for passive as for gps and network.
        // Current Sensibility API returns values from all three providers
        location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0, 0, this, Looper.getMainLooper());
        location_manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0,
                this, Looper.getMainLooper());

        // Store a copy of this to use in
        // inner classes ConnectionCallbacks, OnConnectionFailedListener
        final LocationService _this = this;

        // Initialize here and pass to google_api_client builder later
        google_api_connection_callbacks = new ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                LocationServices.FusedLocationApi
                        .requestLocationUpdates(_this.google_api_client,
                        _this.google_location_request, _this,
                                Looper.getMainLooper());

                // Once connected we can unregister the connection listener
                // and connection failed listener
                _this.google_api_client.unregisterConnectionCallbacks(this);

                // Only gets here after call to google_api_client.connect()
                // which happens after google_api_connection_failed_callbacks
                // is initialized but the compiler doesn't know this and
                // wants us to check
                if (_this.google_api_connection_failed_callbacks != null) {
                    _this.google_api_client.unregisterConnectionFailedListener(
                            google_api_connection_failed_callbacks);
                }
            }
            @Override
            public void onConnectionSuspended(int i) {
            }
        };

        // Initialize here and pass to google_api_client builder later
        google_api_connection_failed_callbacks = new OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull
                                           ConnectionResult connectionResult) {

                // XXX: This happens asynchronously, so we won't be able
                // to tell the user but s/he will figure out sooner or later
                // when s/he wants to use the Google API
                Log.wtf(TAG, String.format("Connection failed with code: %d. " +
                        "Check com.google.android.gms.common.ConnectionResult" +
                        " constants",
                        connectionResult.getErrorCode()));

                // Once failed we can unregister the connection listener
                // and connection failed listener
                _this.google_api_client.unregisterConnectionCallbacks(
                        google_api_connection_callbacks);
                _this.google_api_client.unregisterConnectionFailedListener(this);
            }
        };


        // Create Google Play Service API client using its builder and pass
        // it the above created connection/failure listener ...
        google_api_client = new GoogleApiClient.Builder(
                SensibilityApplication.getAppContext())
                .addConnectionCallbacks(google_api_connection_callbacks)
                .addOnConnectionFailedListener(google_api_connection_failed_callbacks)
                .addApi(LocationServices.API)
                .build();

        // ... and finally try connect to the API
        google_api_client.connect();
    }


    /*
     * Unregisters Android location update listeners
     * Unregisters Google Play Service location update listeners
     * Disconnects from Google Play Service
     *
     */
    public void stop_location() {

        location_manager.removeUpdates(this);

        // If in the process of connecting wait with disconnect until
        // being connected
        if (google_api_client.isConnecting()) {
            google_api_client.blockingConnect();
            google_api_client.disconnect();
        }

        // Disconnect from Google Api Only if we are connected
        if(google_api_client.isConnected()) {
            // If listener is not registered this has no effects
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    google_api_client, this);
            google_api_client.disconnect();
        }
    }


    /*
     * Returns last location update received by LocationService for
     * each available provider: fused, network and gps
     *
     * @return  String serialized JSON Object or null
     * e.g.:
     * {
     * 'fused':{
     *   'bearing':0,
     *   'altitude':0,
     *   'longitude':-73.987226800000002,
     *   'time_sample':1467211033628,
     *   'time_polled':1467211037064,
     *   'latitude':40.691905499999997,
     *   'speed':0,
     *   'accuracy':699.9990234375
     * },
     * 'network':{
     *   'bearing':0,
     *   'altitude':0,
     *   'extras':{
     *     'travelState':'stationary',
     *     'nlpVersion':2021,
     *     'networkLocationType':'cell'
     *   },
     *   'longitude':-73.987226800000002,
     *   'time_sample':1467211033628,
     *   'time_polled':1467211037030,
     *   'latitude':40.691905499999997,
     *   'speed':0,
     *   'accuracy':699.9990234375
     * },
     * 'gps':{
     *   'bearing':0,
     *   'altitude':129,
     *   'extras':{
     *     'satellites':8
     *   },
     *   'longitude':-73.985975449999998,
     *   'time_sample':1466541236000,
     *   'time_polled':1467211037029,
     *   'latitude':40.692894889999998,
     *   'speed':0,
     *   'accuracy':44
     * }
     *
     */
    public String getLocation() throws JSONException {
        JSONObject locations_json = new JSONObject();
        if (location_gps_json != null) {
            locations_json.put("gps", location_gps_json);
        }
        if (location_network_json != null) {
            locations_json.put("network", location_network_json);
        }
        if (location_fused_json != null) {
            locations_json.put("fused", location_fused_json);
        }

        if (locations_json.length() > 0) {
            return locations_json.toString();
        }
        return null;
    }


   /*
    * Returns last location update received by the device (e.g. by another app)
    * foreach available provider: fused, network and gps
    *
    * @return  String serialized JSON Object or null
    * e.g.: same as getLocation
    */
    public String getLastKnownLocation() throws JSONException {

        JSONObject locations_json = new JSONObject();
        Location location_gps = null;
        Location location_network = null;
        Location location_fused = null;

        location_gps = location_manager.getLastKnownLocation("gps");
        if (location_gps != null) {
            locations_json.put("gps", jsonify_location(location_gps));
        }

        location_network = location_manager.getLastKnownLocation("network");
        if (location_network != null) {
            locations_json.put("network", jsonify_location(location_network));
        }

        if (google_api_client.isConnected()) {
            location_fused = LocationServices.FusedLocationApi.getLastLocation(
                    google_api_client);
            if (location_fused != null) {
                locations_json.put("fused", jsonify_location(location_fused));
            }
        }

        if (locations_json.length() > 0) {
            return locations_json.toString();
        }
        return null;
    }

    /*
     * Returns at max a specified amount of address information for a given
     * Latitude and Longitude. This process is called reverse geo coding and
     * requires an internet connection and a connection to Google
     * Play Service API
     *
     * @param   Latitude (double)
     * @param   Longitude (double)
     * @param   Max results (int)
     * @return  String serialized JSON Object or null
     * e.g.:
     * [
     *   {
     *     'thoroughfare':'Jay St',
     *     'lines':[
     *       '375 Jay St',
     *       'Brooklyn, NY 11201'
     *     ],
     *     'admin_area':'New York',
     *     'feature_name':'375',
     *     'country_code':'US',
     *     'country_name':'United States',
     *     'postal_code':'11201',
     *     'sub_locality':'Brooklyn',
     *     'sub_thoroughfare':'375'
     *   },
     *   {
     *     'locality':'Brooklyn',
     *     'lines':[
     *       'Downtown Brooklyn',
     *       'Brooklyn, NY'
     *     ],
     *     'sub_admin_area':'Kings County',
     *     'admin_area':'New York',
     *     'feature_name':'Downtown Brooklyn',
     *     'country_code':'US',
     *     'country_name':'United States',
     *     'sub_locality':'Downtown Brooklyn'
     *   }
     * ]
     *
     */
    public String getGeoLocation(double latitude, double longitude,
                                 int max_results) throws
        IOException, IllegalArgumentException, JSONException,
            GooglePlayServicesNotAvailableException {

        List<Address> addresses = null;
        if (google_api_client.isConnected() &&
                geocoder.isPresent()) {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    max_results);
        } else {
            throw new GooglePlayServicesNotAvailableException(1);
        }

        if (addresses != null) {
            JSONArray addresses_json = new JSONArray();
            for (Address address: addresses  ) {
                JSONObject address_json = new JSONObject();
                address_json.put("admin_area", address.getAdminArea());
                address_json.put("country_code", address.getCountryCode());
                address_json.put("country_name", address.getCountryName());
                address_json.put("feature_name", address.getFeatureName());
                address_json.put("locality", address.getLocality());
                address_json.put("phone", address.getPhone());
                address_json.put("postal_code", address.getPostalCode());
                address_json.put("premises", address.getPremises());
                address_json.put("sub_admin_area", address.getSubAdminArea());
                address_json.put("sub_locality", address.getSubLocality());
                address_json.put("sub_thoroughfare",
                        address.getSubThoroughfare());
                address_json.put("thoroughfare", address.getThoroughfare());
                address_json.put("url", address.getUrl());
                int address_line_cnt = address.getMaxAddressLineIndex();
                if (address_line_cnt > 0) {
                    JSONArray address_lines_json = new JSONArray();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++){
                        address_lines_json.put(address.getAddressLine(i));
                    }
                    address_json.put("lines", address_lines_json);
                }
                addresses_json.put(address_json);
            }
            return addresses_json.toString();
        }
       return null;
    }


    /*
     * Internal helper function that takes a location object of any provider,
     * extracts its attributes and creates a JSONObject.
     * Also adds the current time in milliseconds as "time_polled" attribute,
     * i.e. the time when the user called getLocation or getLastKnownLocation.
      * Whereas "time_sample" is the time when LocationService received
      * the location update
     *
     * @return  String serialized JSON Object or null
     * e.g.: see getLocation or getLastKnownLocation
     *
     */
    private JSONObject jsonify_location(Location location)
            throws JSONException {
        JSONObject location_json = new JSONObject();

        location_json.put("time_polled", System.currentTimeMillis());
        location_json.put("time_sample", location.getTime());
        location_json.put("accuracy", location.getAccuracy());
        location_json.put("altitude", location.getAltitude());
        location_json.put("bearing", location.getBearing());
        location_json.put("latitude", location.getLatitude());
        location_json.put("longitude", location.getLongitude());
        location_json.put("speed", location.getSpeed());
        Bundle extras = location.getExtras();

        // Provider specific extra information,
        // e.g. satellites (gps), travelState (network)
        if (extras != null) {
            JSONObject extras_json = new JSONObject();
            for (String key : extras.keySet()) {
                // Use wrap to also stringify in case the value
                // is an unexpected Object
                extras_json.put(key, JSONObject.wrap(extras.get(key)));
            }
            if (extras_json.length() > 0) {
                location_json.put("extras", extras_json);
            }
        }
        return location_json;
    }

    /*
     * ###################################################
     * Required LocationListener implementations
     * (Android AND Google Play Service)
     * ###################################################
     */

    /*
     * Callback that receives gps, network and fused locations
     * Stores location object the according provider's class members
     *
     * CAUTION:
     * This method implements `onLocationChanged` of two different
     * interfaces:
     *     - android.location.LocationListener
     *     - com.google.android.gms.location.LocationListener
     * Both callbacks receive an Android Location object but
     * from different Location Providers
     *
     * Android's Interface expects Locations of provider type:
     *      - LocationManager.GPS_PROVIDER
     *      - LocationManager.NETWORK_PROVIDER
     * Google Play Services's Interface expects Locations of provider type:
     *      "fused" (XXX: this is a hardcoded string, prone to change)
     *
     */
    @Override
    public void onLocationChanged(Location location) {
        JSONObject location_json = null;

        // We catch the exception here because the callback is asynchronous
        // so we can't pass the exception on to the user
        try {
            location_json = jsonify_location(location);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            location_gps_json = location_json;
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            location_network_json = location_json;
        } else if (location.getProvider().equals("fused")) {
            location_fused_json = location_json;
        } else {
            // WHAT THE terrible failure?!!
            Log.wtf(TAG, String.format(
                    "Received location from unknown Provider: %s",
                    location.getProvider()));
        }
    }

    /*
     * ###################################################
     * Required  android.location.LocationListener implementations
     * but we don't really need them
     * ###################################################
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    @Override
    public void onProviderEnabled(String provider) {
    }
    @Override
    public void onProviderDisabled(String provider) {
    }
}
