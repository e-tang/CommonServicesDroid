package au.com.tyo.services.android.location;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import java.text.DateFormat;
import java.util.Date;

import au.com.tyo.android.BuildConfig;
import au.com.tyo.android.CommonLocationService;
import au.com.tyo.android.Constants;
import au.com.tyo.utils.LocationUtils;

public class GoogleFusedLocation extends CommonLocationService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = GoogleFusedLocation.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationListener locationListener;

    public static class SimpleLocation implements LocationUtils.LocationPoint {

        private double lat, lon;
        private Date date;
        private String lastUpdate;

        public SimpleLocation(Location location) {
            this(location.getLatitude(), location.getLongitude());
        }

        public SimpleLocation(double latitude, double longitude) {
            this.lat = latitude;
            this.lon = longitude;
            this.date = new Date();
            this.lastUpdate = DateFormat.getTimeInstance().format(this.date);
        }

        public double getLatitude() {
            return lat;
        }

        public Date getDate() {
            return date;
        }

        public String getLastUpdate() {
            return lastUpdate;
        }

        public double getLongitude() {
            return lon;
        }

        @Override
        public String toString() {
            return "(lat: " + lat + ", lon: " + lon + ")";
        }
    }

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = BuildConfig.DEBUG ? 0 : 1 * 60 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = BuildConfig.DEBUG ? 0: 1000;

    public GoogleFusedLocation() {
        super();

        this.locationRequest = new LocationRequest();
        this.locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(this.locationRequest);
        this.locationSettingsRequest = builder.build();
    }

    public boolean isConnected() {
        return (null != googleApiClient && googleApiClient.isConnected());
    }

    public void setLocationListener(LocationListener locationListener) {
        this.locationListener = locationListener;
    }

    public void start(Context context) {
        if (null == googleApiClient)
            this.googleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        connect();
    }

    public LocationSettingsRequest getLocationSettingsRequest() {
        return this.locationSettingsRequest;
    }

    public void stop() {
        Log.i(TAG, "stop() Stopping location tracking");
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected to Google location service.");

        if (hasLocationPermission())
            startLocationUpdate();
    }

    @Override
    protected boolean handleClientMessage(Message m) {
        if (m.what == Constants.MESSAGE_SERVICE_PERMISSION_GRANTED) {
            onPermissionMessageReceived();
        }
        return super.handleClientMessage(m);
    }

    protected void onPermissionMessageReceived() {
         if (hasLocationPermission())
             onHasLocationPermission();
    }

    protected void onHasLocationPermission() {
        startLocationUpdate();
    }

    /**
     * Make sure we have the permission
     */
    protected void startLocationUpdate() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        setStartLocation(location);

        LocationServices.FusedLocationApi.requestLocationUpdates(this.googleApiClient, this.locationRequest, locationListener == null ? this : locationListener);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection to Google location service suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection to Google location service failed.");
    }

    @Override
    public void onLocationChanged(Location location) {
        setLastKnownLocation(location);
    }

    public void connect() {
        if (!isConnected())
            googleApiClient.connect();
    }

    public void disconnect() {
        if (isConnected())
            googleApiClient.disconnect();
    }


    /**
     * some useful references
     * 1. https://stackoverflow.com/questions/39226392/android-google-fused-location-doesnt-work-while-location-is-turned-off
     */
}

