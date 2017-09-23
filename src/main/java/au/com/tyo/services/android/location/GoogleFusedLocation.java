package au.com.tyo.services.android.location;

import android.app.Activity;
import android.location.*;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

import au.com.tyo.android.CommonPermission;

public class GoogleFusedLocation {

    private static final String TAG = GoogleFusedLocation.class.getSimpleName();

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    public interface OnLocationChangedListener {
        void onChange(SimpleLocation location);
    }

    private LocationListener locationListener;

    public static class SimpleLocation {

        private BigDecimal lat, lon;
        private Date date;
        private String lastUpdate;

        public SimpleLocation(Location location) {
            this(location.getLatitude(), location.getLongitude());
        }

        public SimpleLocation(BigDecimal latitude, BigDecimal longitude) {
            this.lat = latitude;
            this.lon = longitude;
            this.date = new Date();
            this.lastUpdate = DateFormat.getTimeInstance().format(this.date);
        }

        public SimpleLocation(Double latitude, Double longitude) {
            this.lat = BigDecimal.valueOf(latitude);
            this.lon = BigDecimal.valueOf(longitude);
        }

        public BigDecimal getLatitude() {
            return lat;
        }

        public Date getDate() {
            return date;
        }

        public String getLastUpdate() {
            return lastUpdate;
        }

        public BigDecimal getLongitude() {

            return lon;
        }

        @Override
        public String toString() {
            return "(lat: " + lat + ", lon: " + lon + ")";
        }
    }

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    public GoogleFusedLocation() {

        this.locationRequest = new LocationRequest();
        this.locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(this.locationRequest);
        this.locationSettingsRequest = builder.build();

        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                android.location.Location currentLocation = locationResult.getLastLocation();

                if (null != locationListener)
                    locationListener.onLocationChanged(currentLocation);
            }
        };
    }

    public void setLocationListener(LocationListener locationListener) {
        this.locationListener = locationListener;
    }

    public void start(Activity context) {

        // this cant be associated with UI / Page
        CommonPermission.checkLocationPermissions(context);

        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        this.fusedLocationProviderClient.requestLocationUpdates(this.locationRequest,
                this.locationCallback, Looper.myLooper());
    }

    public LocationSettingsRequest getLocationSettingsRequest() {
        return this.locationSettingsRequest;
    }

    public void stop() {
        Log.i(TAG, "stop() Stopping location tracking");
        this.fusedLocationProviderClient.removeLocationUpdates(this.locationCallback);
    }
}

