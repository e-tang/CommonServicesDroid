package au.com.tyo.services.android.location;

import android.app.Activity;
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


/**
 * Uses Google Play API for obtaining device locations
 * Created by alejandro.tkachuk
 * alejandro@calculistik.com
 * www.calculistik.com Mobile Development
 */

public class GoogleFusedLocation {

    private static final String TAG = GoogleFusedLocation.class.getSimpleName();

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;

    public static class GPSPoint {

        private BigDecimal lat, lon;
        private Date date;
        private String lastUpdate;

        public GPSPoint(BigDecimal latitude, BigDecimal longitude) {
            this.lat = latitude;
            this.lon = longitude;
            this.date = new Date();
            this.lastUpdate = DateFormat.getTimeInstance().format(this.date);
        }

        public GPSPoint(Double latitude, Double longitude) {
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
            return "(" + lat + ", " + lon + ")";
        }
    }

    public interface Workable<T> {
        void work(T t);
    }

    private Workable<GPSPoint> workable;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    public GoogleFusedLocation(Activity context) {
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
                super.onLocationResult(locationResult); // why? this. is. retarded. Android.
                Location currentLocation = locationResult.getLastLocation();

                GPSPoint gpsPoint = new GPSPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
                Log.i(TAG, "Location Callback results: " + gpsPoint);
                if (null != workable)
                    workable.work(gpsPoint);
            }
        };

        this.mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        CommonPermission.checkLocationPermissions(context);
        this.mFusedLocationClient.requestLocationUpdates(this.locationRequest,
                this.locationCallback, Looper.myLooper());
    }

    public void onChange(Workable<GPSPoint> workable) {
        this.workable = workable;
    }

    public LocationSettingsRequest getLocationSettingsRequest() {
        return this.locationSettingsRequest;
    }

    public void stop() {
        Log.i(TAG, "stop() Stopping location tracking");
        this.mFusedLocationClient.removeLocationUpdates(this.locationCallback);
    }
}

