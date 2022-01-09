package com.mad.takecare.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created / Edited by Eve Emily Sophie Schade on 24.07.20.
 * build from example of: https://stackoverflow.com/a/50856971/9824424
 */
public class SharedPrefencesManager {
    private static final String APP_PREFS = "AppPrefsFile";
    private static final String SHARED_LOC_LAT = "sharedLocLat";
    private static final String SHARED_LOC_LONG = "sharedLocLong";
    private static final String SHARED_LOC_RADIUS = "sharedLocRadius";

    private SharedPreferences sharedPrefs;
    private static SharedPrefencesManager instance;


    private SharedPrefencesManager(Context context) {
        sharedPrefs = context.getApplicationContext().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
    }


    public static synchronized SharedPrefencesManager getInstance(Context context) {
        if (instance == null)
            instance = new SharedPrefencesManager(context);

        return instance;
    }

    public void setRecentLocArray(String[] recentLocation) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SHARED_LOC_LAT, recentLocation[0]);
        editor.putString(SHARED_LOC_LONG, recentLocation[1]);
        editor.apply();
    }

    public void setRecentLocRadius(String radius) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(SHARED_LOC_RADIUS, radius);
        editor.apply();
    }

    public String[] getRecentLocArray() {
        String[] recentLocationArray = {
                sharedPrefs.getString(SHARED_LOC_LAT, null),
                sharedPrefs.getString(SHARED_LOC_LONG, null),
        };
        return recentLocationArray;
    }

    public String getRecentLocRadius() {
        return sharedPrefs.getString(SHARED_LOC_RADIUS, "10000");
    }

}
