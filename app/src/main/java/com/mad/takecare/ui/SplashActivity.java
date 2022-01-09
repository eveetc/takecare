package com.mad.takecare.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mad.takecare.MainActivity;
import com.mad.takecare.R;
import com.mad.takecare.data.SharedPrefencesManager;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class SplashActivity extends AppCompatActivity {
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 30 * 1000;  /* 25 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private int SMALLEST_DISPLACEMENT_FOR_UPDATE = 10; //10 meters
    private boolean firstStart;
    TextView errorText;
    Intent intent;

    Animation frombottom;
    Animation fromtop;
    ImageView logo;
    ImageView logoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        firstStart = true;
        errorText = findViewById(R.id.errorText);
        errorText.setVisibility(View.INVISIBLE);

        //Do the following once, in the first activity
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("takecare")
                .server("http://194.55.13.214:1337/parse/") // '/' important after 'parse'
                //.server("http://10.0.2.2:1337/parse/") // '/' important after 'parse'
                .build()
        );
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    ParseQuery<ParseUser> userQuery = ParseQuery.getQuery("User");
                    userQuery.findInBackground(new FindCallback<ParseUser>() {
                        public void done(List<ParseUser> results, ParseException e) {
                            if (e == null) {
                                //Connection to server works
                                intent = new Intent(SplashActivity.this, MainActivity.class);
                                startLocationUpdates(); //location manager should start, after permissions are set, otherwise tasks cant be loaded

                            } else {
                                errorText.setVisibility(View.VISIBLE);
                                errorText.setText(e.toString());
                            }
                        }
                    });
                } else {
                    Intent i = new Intent();
                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    i.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    getApplicationContext().startActivity(i);
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();

        frombottom = AnimationUtils.loadAnimation(this, R.anim.frombottom);
        fromtop = AnimationUtils.loadAnimation(this, R.anim.fromtop);
        logo = (ImageView) findViewById(R.id.logo_without_text);
        logoText = (ImageView) findViewById(R.id.logo_text);

        logo.setAnimation(fromtop);
        logoText.setAnimation(frombottom);

    }

    /**
     * Trigger new location updates at interval
     */
    protected void startLocationUpdates() {
        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT_FOR_UPDATE);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        //TODO GLOBAL LOCATION LISTENER

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    public void onLocationChanged(Location location) {
        if (location.getLatitude() > 0.0) {
            String[] sharedRecentLocation = {String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude())};
            SharedPrefencesManager.getInstance(this.getApplicationContext()).setRecentLocArray(sharedRecentLocation);

            //keep looper for location running in background, but dont restart intent on location update
            if (firstStart) {
                startActivity(intent);
                finish();
                firstStart = false;
            }
        }
    }
}