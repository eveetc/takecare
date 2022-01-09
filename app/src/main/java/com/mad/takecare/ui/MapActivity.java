package com.mad.takecare.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mad.takecare.R;
import com.mad.takecare.data.SharedPrefencesManager;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Integer.parseInt;

/**
 * Created / Edited by Eve Emily Sophie Schade on 03.07.20.
 */
public class MapActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    MapView map = null;


    private Location recentLocation = new Location("recentLocation");
    private IMapController mapController;
    private Marker locationMarker;
    private Marker taskAdressMarker;
    private ArrayList<Marker> allTasksMarkers = new ArrayList<>();
    private Context ctx;

    private Boolean radiusMap;
    private Boolean taskLocationImported;
    private Boolean allTasksImported;
    private ArrayList<Location> allTasks;

    private int radius;
    private Polygon radiusMarker;
    private TextView radiusText;
    private ArrayList<GeoPoint> geoRadiusPoints;

    private Intent mapResultsIntent;

    private Location taskLocation;
    private boolean accessPermitsEdit;

    private int textSpaceAmountOld;
    ProgressBar pgsBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getApplicationContext();
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("recentRadius")) {
                radiusMap = true;
                radius = extras.getInt("recentRadius"); //keep before selected radius
            } else {
                radiusMap = false;
            }
            if (extras.containsKey("location")) {
                Location loc = null;
                if ((loc = extras.getParcelable("location")) != null)
                    recentLocation = loc; //set already located position of user. TODO not needed if central locationamanger
            }
            if (extras.containsKey("access")) {
                switch (extras.getString("access")) {
                    case "FREE":
                    case "ACCEPTED":
                        accessPermitsEdit = false;
                        break;
                    case "OWNED":
                    case "NEW":
                        accessPermitsEdit = true;
                        break;
                }
            } else {
                accessPermitsEdit = false;
            }
            if (extras.containsKey("allTasks")) {
                ArrayList<Location> allTasksTmp = null;
                if ((allTasksTmp = extras.getParcelableArrayList("allTasks")) != null) {
                    allTasksImported = true;
                    allTasks = allTasksTmp;
                } else {
                    allTasksImported = false;
                }
            } else {
                allTasksImported = false;
            }
            if (extras.containsKey("taskLocation")) {
                Location taskLoc;
                if ((taskLoc = extras.getParcelable("taskLocation")) != null) {
                    taskLocation = taskLoc;
                    taskLocationImported = true;
                } else {
                    taskLocationImported = false;
                }
            } else {
                taskLocationImported = false;
            }
        } else {
            if (!radiusMap) {
                accessPermitsEdit = false;
            }
        }


        setContentView(R.layout.activity_map);

        radiusText = findViewById(R.id.environment_radius_value);
        //PERMISSIONS
        //especially internet, read and write access needed for flawless map visibility
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
            public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();


        //get recent data from sharedpreferences manager for location (provided by mainactivity looper) and radius
        String[] sharedRecentLocation = SharedPrefencesManager.getInstance(ctx).getRecentLocArray();
        if (sharedRecentLocation[0] != null) {
            recentLocation.setLatitude(Double.parseDouble(sharedRecentLocation[0]));
            recentLocation.setLongitude(Double.parseDouble(sharedRecentLocation[1]));
        }

        String sharedRecentRadius = SharedPrefencesManager.getInstance(ctx).getRecentLocRadius();
        if (sharedRecentRadius != null) {
            radius = parseInt(sharedRecentRadius);
            if (radius == 0) {
                radius = 10000;
            }
        } else {
            radius = 10000;
        }

        mapResultsIntent = new Intent();

        //load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string


        map = findViewById(R.id.map);

        final String[] tileURLs = {"http://194.55.13.214:8090/styles/basic-preview/"};
        final ITileSource tileSource = new XYTileSource("Eve's Tile Server",
                6,
                18,
                256,
                ".png",
                tileURLs,
                "from OpenMapTiles Map Server");
        map.setTileSource(tileSource);

        map.getOverlayManager().getTilesOverlay().setLoadingBackgroundColor(android.R.color.white);
        map.getOverlays().removeAll(allTasksMarkers);

        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        mapController = map.getController();
//        map.setScrollableAreaLimitDouble(map.getBoundingBox());
        map.setMinZoomLevel(8.0);
        mapController.setZoom(12.5);
        map.setHasTransientState(false);
        //TODO call setHasTransientState(true) on the recycler's item view that contains the mapView.
        // Doing this will prevent the RecyclerView from attempting to reuse the view

        //Setting location marker
        locationMarker = new Marker(map);
        locationMarker.setIcon(new IconicsDrawable(this).icon(FontAwesome.Icon.faw_map_marker_alt)
                .color(IconicsColor.colorInt(R.color.mapLocation)).size(IconicsSize.dp(20)));//TODO Change icon to gmaps position icon

        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        locationMarker.setInfoWindow(null);
        GeoPoint locationGeoPoint = new GeoPoint(recentLocation.getLatitude(), recentLocation.getLongitude());
        locationMarker.setPosition(locationGeoPoint);
        mapController.setCenter(locationGeoPoint);
        map.getOverlays().add(locationMarker);

        //Setting radius polygon around users location
        if (radiusMap) {
            findViewById(R.id.search_card).setVisibility(View.GONE);
            findViewById(R.id.radius_card).setVisibility(View.VISIBLE);
            TextView mapTitle = (TextView) findViewById(R.id.map_title);
            mapTitle.setText(getString(R.string.set_location_radius));

            setMapRadiusResult(radius);
            radiusText.setText(String.valueOf(radius / 1000) + " km");
            radiusMarker = new Polygon(map);
            geoRadiusPoints = getRadiusPointOfPosition(recentLocation, radius);
            radiusMarker.getOutlinePaint().setColor(ContextCompat.getColor(ctx, R.color.mapRadius));
            radiusMarker.getOutlinePaint().setStrokeWidth(2);
            radiusMarker.getFillPaint().setColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(ctx, R.color.mapRadius), Math.round(10f / 100 * 255)));
            radiusMarker.setPoints(geoRadiusPoints);
            radiusMarker.setInfoWindow(null);
            map.getOverlays().add(radiusMarker);
            mapController.setCenter(locationGeoPoint);


            //If other tasks are imported from mapactivity in radiusmode
            if (allTasksImported) {
                for (Location t : allTasks) {
                    Marker marker = new Marker(map);
                    marker.setIcon(new IconicsDrawable(this).icon(FontAwesome.Icon.faw_map_pin)
                            .color(IconicsColor.colorInt(getColor(R.color.taskLocation))).size(IconicsSize.dp(20)));//TODO Change icon to gmaps position icon
                    marker.setPosition(new GeoPoint(t.getLatitude(), t.getLongitude()));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(marker);
                    marker.setInfoWindow(null);
                    map.invalidate();
                    allTasksMarkers.add(marker);
                }
            }

            map.invalidate();

            // ugly hack fix. Since this activity got rid of the locationmanager
            // which starts zooming to users location and the radius boundingbox with a locationupdate
            // osmdroid does zoom OUT to min zoom .. which is fixed with waiting 50ms to start the zoom
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setRadiusOfExercises(recentLocation);
                }
            }, 50);

            //Setting up Seekbar for radius
            SeekBar radiusVal = (SeekBar) findViewById(R.id.radiusVal);
            radiusVal.setProgress(radius); //setting loaded value for seekbar
            radiusVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    //TODO CHANGE SEEKBAR to Slider with costum steps
                    radius = progress;
                    setMapRadiusResult(progress);
                    setRadiusOfExercises(recentLocation);
                    radiusText.setText(String.valueOf(progress / 1000) + " km");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        } else {
            findViewById(R.id.search_card).setVisibility(View.VISIBLE);
            findViewById(R.id.radius_card).setVisibility(View.GONE); //hide in this mode
            TextView mapTitle = (TextView) findViewById(R.id.map_title);
            mapTitle.setText(getString(R.string.setAddress));

            //initialize Search view for query search
            SearchView searchView = (SearchView) findViewById(R.id.search_bar);
            searchView.setOnQueryTextListener(this);
            //==>region to enable search after click in input-text(not only at magnifier icon)
            searchView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchView.onActionViewExpanded();
                }
            });

            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b) {
                        if (searchView.getQuery().toString().length() < 1) {
                            searchView.setIconified(true);
                        }
                        searchView.clearFocus();
                    }
                }
            });

            taskAdressMarker = new Marker(map);
            taskAdressMarker.setIcon(new IconicsDrawable(this).icon(FontAwesome.Icon.faw_map_pin)
                    .color(IconicsColor.colorInt(getColor(R.color.taskLocation))).size(IconicsSize.dp(20)));

            taskAdressMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            taskAdressMarker.setInfoWindow(null);

            //If tasklocation is available and  imported from from taskview
            if (taskLocationImported) {
                GeoPoint taskGeopoint = new GeoPoint(taskLocation.getLatitude(), taskLocation.getLongitude());
                mapController.setCenter(taskGeopoint);
                taskAdressMarker.setPosition(taskGeopoint);
//                mapController.animateTo(taskGeopoint);
            }
            map.getOverlays().add(taskAdressMarker);

            pgsBar = findViewById(R.id.progressBar);
            Sprite doubleBounce = new DoubleBounce();
            pgsBar.setIndeterminateDrawable(doubleBounce);

            //click event on map to pull that location and set locationmarker
            final MapEventsReceiver mReceive = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    taskAdressMarker.setPosition(p);
                    map.invalidate();
                    new GeoRequestAsync(p, "false").execute();
                    //askGeoCodingServer("false", p);
                    return false;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            };
            map.getOverlays().add(new MapEventsOverlay(mReceive));

            if (accessPermitsEdit) {
                textSpaceAmountOld = 0;
            }


            //TODO
            /*
            initilize search
            map marker manipulate search
            alert when trying to finish without setting result
            setting searched location as result
             */
        }
        map.invalidate(); //needed to update map

        FloatingActionButton centerPosition = (FloatingActionButton) findViewById(R.id.centerPosition);
        centerPosition.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                centerMapToUsersPosition(recentLocation);
            }
        });

        FloatingActionButton map_done = (FloatingActionButton) findViewById(R.id.edit_location_radius);
        map_done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }

        });

        ua = new WebView(ctx).getSettings().getUserAgentString();
        geocoderNominatim = new GeocoderNominatim(Locale.getDefault(), ua);
    }

    String ua;
    GeocoderNominatim geocoderNominatim;

    /**
     * Function called to set the extra for radius result.
     * When function is finished this resulted is forwarded to the EnvironmentActivity
     *
     * @param radius
     */
    private void setMapRadiusResult(int radius) {
        mapResultsIntent.putExtra(getString(R.string.radiusParameter), radius);
        setResult(RESULT_OK, mapResultsIntent);
    }

    /**
     * Function called to set the extra for task location.
     * When function is finished this result is forwarded to aufgabenansichtactivitiy
     *
     * @param location
     */
    private void setTaskLocation(Location location, String locationName) {
        if (accessPermitsEdit) {
            mapResultsIntent.putExtra(getString(R.string.taskLocationParameter), location);
            mapResultsIntent.putExtra(getString(R.string.locationNameParameter), locationName);
            setResult(RESULT_OK, mapResultsIntent);
        }
    }

    /**
     * Calling this function the map zooms and shifts to the position of the users location animated
     *
     * @param location
     */
    public void centerMapToUsersPosition(Location location) {
        GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (radiusMap) {
            BoundingBox bb = BoundingBox.fromGeoPoints(geoRadiusPoints);
            map.zoomToBoundingBox(bb, true);
        }
        mapController.animateTo(startPoint);
        mapController.setCenter(startPoint);
    }


    /**
     * Function to create a list of points around the users location regarding a radius for creating a polyline
     *
     * @param location
     * @param radius
     * @return
     */
    public ArrayList<GeoPoint> getRadiusPointOfPosition(Location location, int radius) {
        ArrayList<GeoPoint> circlePoints = new ArrayList<GeoPoint>();
        for (float f = 0; f < 360; f += 1) {
            circlePoints.add(new GeoPoint(location.getLatitude(), location.getLongitude()).destinationPoint(radius, f));
        }
        return circlePoints;
    }

    /**
     * Setting Polyline around users location with given radius and zoom to the bounding box of this radius polyline
     *
     * @param location
     */
    public void setRadiusOfExercises(Location location) {
        if (location != null) {//TODO if exception still persists, rethink code -> central locationmanager will fix it
            geoRadiusPoints = getRadiusPointOfPosition(location, radius);
            if (geoRadiusPoints != null) {
                radiusMarker.setPoints(geoRadiusPoints);
                map.invalidate();
                BoundingBox bb = BoundingBox.fromGeoPoints(geoRadiusPoints);
                map.zoomToBoundingBox(bb, true);
            }
        }
    }

    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    /**
     * Search on Query listener
     *
     * @param query
     * @return
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        textSpaceAmountOld = 0;
//        Toast.makeText(this, query, Toast.LENGTH_SHORT).show();
        pgsBar.setVisibility(View.VISIBLE);
        new GeoRequestAsync(null, query).execute();
        //askGeoCodingServer(query, null);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        int textSpaceAmountNew = 0;
        for (char c : newText.toCharArray()) {
            if (c == ' ') {
                textSpaceAmountNew++;
            }
        }
        if (textSpaceAmountNew > textSpaceAmountOld) {
//            Toast.makeText(ctx, newText, Toast.LENGTH_SHORT).show();
//            List<Address> addresslist = getQuerySuggestions(newText);
            textSpaceAmountOld = textSpaceAmountNew;
        }

        return false;
    }


    //    public List<Address> getQuerySuggestions(String query) {
//        if (android.os.Build.VERSION.SDK_INT > 9) {
//            StrictMode.ThreadPolicy policy =
//                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }
//
//        String ua = new WebView(ctx).getSettings().getUserAgentString();
//        GeocoderNominatim coderNominatim = new GeocoderNominatim(Locale.getDefault(), ua);
//        try {
//            coderNominatim.setService("http://194.55.13.214:7070/");
//            List<Address> geoResults = coderNominatim.getFromLocationName(query, 10);
//            if (geoResults.size() == 0) {
//                Toast.makeText(ctx, "Country not found.", Toast.LENGTH_SHORT).show();
//            } else {
//                return geoResults;
////                Address address = geoResults.get(0);
//            }
//        } catch (IOException e) {
//            Toast.makeText(ctx, "Geocoding error", Toast.LENGTH_SHORT).show();
//        }
//        return null;
//    }
    private class GeoRequestAsync extends AsyncTask<Void, Void, Void> {
        GeoPoint locationIfAvailable;
        String queryIfAvailable;

        public GeoRequestAsync(GeoPoint ploc, String pQuer) {
            locationIfAvailable = ploc;
            queryIfAvailable = pQuer;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            latestGeoResults = new ArrayList<Address>();
        }

        @Override
        protected Void doInBackground(Void... param) {
            publishProgress(param);

            askGeoCodingServer(queryIfAvailable, locationIfAvailable);

            return null;
        }

    }

    List<Address> latestGeoResults;

    public void askGeoCodingServer(String queryIfAvailable, GeoPoint locationIfAvailable) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        try {
            geocoderNominatim.setService("http://194.55.13.214:7070/");

            if (!queryIfAvailable.equals("false")) {
                latestGeoResults = geocoderNominatim.getFromLocationName(queryIfAvailable, 1);
                done(queryIfAvailable);

            } else {
                latestGeoResults = geocoderNominatim.getFromLocation(locationIfAvailable.getLatitude(), locationIfAvailable.getLongitude(), 1);
                done2(locationIfAvailable);
            }
        } catch (
                IOException e) {
            Toast.makeText(ctx, "Geocoding error", Toast.LENGTH_SHORT).show();
        }
    }

    public void done(String pQueryIfAvailable) {
        String queryIfAvailable;
        queryIfAvailable = pQueryIfAvailable;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (latestGeoResults.size() == 0) {
                    Toast.makeText(ctx, "Country not found.", Toast.LENGTH_SHORT).show();
                } else {
                    Address address = latestGeoResults.get(0);
                    GeoPoint addressGeopoint = new GeoPoint(address.getLatitude(), address.getLongitude());
                    Location adressLocation = new Location("");
                    adressLocation.setLatitude(address.getLatitude());
                    adressLocation.setLongitude(address.getLongitude());

                    map.getOverlays().add(taskAdressMarker);
                    taskAdressMarker.setPosition(addressGeopoint);

                    setTaskLocation(adressLocation, queryIfAvailable); //TODO DONT SAFE QUERY AS LOCATIONNAME
                    map.invalidate();
                    mapController.setZoom(17.0);

                    mapController.animateTo(addressGeopoint);
                    mapController.setCenter(addressGeopoint);
                    if (pgsBar.isShown()) {
                        pgsBar.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });

    }

    public void done2(GeoPoint locationIfAvailable) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (latestGeoResults.size() == 0) {
                    Toast.makeText(ctx, "Country not found.", Toast.LENGTH_SHORT).show();
                } else {
                    String address = latestGeoResults.get(0).getAddressLine(0);
                    Location taskLocation = new Location("");
                    taskLocation.setLatitude(locationIfAvailable.getLatitude());
                    taskLocation.setLongitude(locationIfAvailable.getLongitude());

                    setTaskLocation(taskLocation, address);
                }
            }
        });

    }

}
