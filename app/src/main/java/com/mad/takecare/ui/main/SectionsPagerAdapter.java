package com.mad.takecare.ui.main;

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.mad.takecare.R;
import com.mad.takecare.ui.DashboardFragment;
import com.mad.takecare.ui.EnvironmentFragment;
import com.mad.takecare.ui.UserProfileFragment;
import com.parse.ParseUser;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.umgebung, R.string.dashboard, R.string.konto};
    private final Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
//        return PlaceholderFragment.newInstance(position + 1);
        Fragment fragment = null;

        if (ParseUser.getCurrentUser() != null) {
            switch (position) {
                case 0:
                    fragment = EnvironmentFragment.newInstance("1", "2"); //TODO GIVE VALUES TO WORK WITH
                    break;
                case 1:
                    fragment = DashboardFragment.newInstance("1", "2"); ////TODO GIVE VALUES TO WORK WITH
                    break;
                case 2:
                    fragment = UserProfileFragment.newInstance("1", "2"); ////TODO GIVE VALUES TO WORK WITH
                    break;
            }
        } else {
            fragment = EnvironmentFragment.newInstance("1", "2");
        }
        return fragment;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        if (ParseUser.getCurrentUser() != null) {
            return 3;
        } else {
            return 1;
        }
    }
}