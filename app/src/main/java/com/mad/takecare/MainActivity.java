package com.mad.takecare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.mad.takecare.data.SharedPrefencesManager;
import com.mad.takecare.ui.EnvironmentFragment;
import com.mad.takecare.ui.main.SectionsPagerAdapter;
import com.parse.ParseUser;

import java.util.List;
import java.util.Objects;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity {
    private static final int[] TAB_ICONS = {R.drawable.ic_baseline_location_city_24, R.drawable.ic_baseline_dashboard_24, R.drawable.ic_baseline_account_circle_24};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        Intent intent = getIntent();
        intent.getIntExtra("fuckthis", 2); //This is needed to force the Intent to un-parcel
        if(intent.hasExtra(getString(R.string.fragmentPositionParameter))) {
            viewPager.setCurrentItem((intent.getIntExtra(getString(R.string.fragmentPositionParameter), 2)));
        }

        if (ParseUser.getCurrentUser() != null) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.getTabAt(i).setIcon(TAB_ICONS[i]);
                tabs.getTabAt(i).setTabLabelVisibility(TabLayout.TAB_LABEL_VISIBILITY_UNLABELED);
            }
            tabs.setVisibility(View.VISIBLE);
        } else {
            tabs.setVisibility(View.GONE);
        }
    }
}