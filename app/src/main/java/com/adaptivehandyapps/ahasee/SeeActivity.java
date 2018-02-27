// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: FEB 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.adaptivehandyapps.ahasee.espi.FeedManager;

import java.util.Locale;

public class SeeActivity extends ActionBarActivity implements ActionBar.TabListener {

    private static final String TAG = "SeeActivity";

    // permissions
    public final static String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    public final static int EXTERNAL_REQUEST = 138;

    private static SeeActivity mParentActivity;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ViewPager controls
    private static final int TAB_TOTAL_PAGES = 3;
    private static final int TAB_SUMMARIZE = 1;
    private static final int TAB_BASELINE = 2;
    private static final int TAB_COMPARE = 3;
    // classes managing each tabbed page
    private static TabSummarize mTabSummarize = null;
    private static TabBaseline mTabBaseline = null;
    private static TabCompare mTabCompare = null;

    private static boolean mIsSaveInstanceState = false;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    // feed manager maintains espi data feed
    private FeedManager mFeedManager;

    private BroadcastReceiver mBroadcastReceiver;

//    ////////////////////////////////////////////////////////////////////////////////////////////////
//    // progress callback
//    private FeedManager.OnProgressCallback mProgressCallback;
//
//    private FeedManager.OnProgressCallback getProgressCallback() {
//        // instantiate callback
//        FeedManager.OnProgressCallback callback = new FeedManager.OnProgressCallback() {
//
//            @Override
//            public void onProgressCallback(int progress) {
//                Log.d(TAG, "onProgressCallback progress " + progress);
//                getProgressTab().setProgressBar(progress);
//            }
//        };
//        //  finish();
//        return callback;
//    }
//    ////////////////////////////////////////////////////////////////////////////////////////////////
//    // getters
//    private FeedManager getFeedManager() {return mFeedManager;}
//    private TabSummarize getProgressTab() {return mTabSummarize;}

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate...");

        if (requestForPermission(this)) {
            Log.d(TAG, "onCreate permissions granted...");
            init();
        }

    }

    private Boolean init() {
        Log.d(TAG, "init...");
        // force landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_see);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // retain SeeActivity
        mParentActivity = this;

//        // instantiate feed manager
//        mProgressCallback = getProgressCallback();
////        mFeedManager = new FeedManager(this, mProgressCallback);
//        mFeedManager = FeedManager.getInstance(this, mProgressCallback);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");

//        String location = SettingsActivity.getLocation(this);
//        Log.v(TAG, "onResume location: " + location);
//        String period = SettingsActivity.getPeriod(this);
//        Log.v(TAG, "onResume exploration period: " + period);
//        String dups = SettingsActivity.getDupsTreatment(this);
//        Log.v(TAG, "onResume dups treatment: " + dups);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mBroadcastReceiver == null) {
            // create broadcast receiver
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String s = intent.getStringExtra(BroadcastUtils.AHA_MESSAGE);
                    Log.v(TAG, "BroadcastReceiver: " + s);
                    if (mTabSummarize != null) mTabSummarize.refreshView();
                    if (mTabBaseline != null) mTabBaseline.refreshView();
                    if (mTabCompare != null) mTabCompare.refreshView();
                }
            };
        }
        if (mBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).registerReceiver((mBroadcastReceiver),
                    new IntentFilter(BroadcastUtils.AHA_REFRESH)
            );
        }
        else {
            Log.e(TAG, "Oops! onStart finds mBroadcastReceiver NULL!");
        }
    }
    @Override
    protected void onStop() {
        if (mBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        }
        else {
            Log.e(TAG, "Oops! onStart finds mBroadcastReceiver NULL!");
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        mIsSaveInstanceState = true;
        Log.v(TAG, "onSaveInstanceState");
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        mIsSaveInstanceState = false;
        Log.v(TAG, "onRestoreInstanceState");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_see, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // create intent & start activity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            Toast.makeText(this, R.string.start_activity, Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
//            return PlaceholderFragment.newInstance(position + 1);

            Fragment f = PlaceholderFragment.newInstance(position + 1);
            return f;
        }

        @Override
        public int getCount() {
            // Show total pages.
            return TAB_TOTAL_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position+1) {
                case TAB_SUMMARIZE:
                    return getString(R.string.title_section1).toUpperCase(l);
                case TAB_BASELINE:
                    return getString(R.string.title_section2).toUpperCase(l);
                case TAB_COMPARE:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            View rootView = null;

            switch (sectionNumber) {
                case TAB_SUMMARIZE:
                    mTabSummarize = new TabSummarize(mParentActivity, inflater, container);
                    rootView = mTabSummarize.getRootView();
                    break;
                case TAB_BASELINE:
                    mTabBaseline = new TabBaseline(mParentActivity, inflater, container);
                    rootView = mTabBaseline.getRootView();
                    break;
                case TAB_COMPARE:
                    mTabCompare = new TabCompare(mParentActivity, inflater, container);
                    rootView = mTabCompare.getRootView();
                    break;
                default:
                    Log.e(TAG, "Invalid sectionNumber: " + sectionNumber);
            }
            return rootView;
        }
        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (!mIsSaveInstanceState) {
                getFragmentManager().beginTransaction().remove(this).commit();
            }
            Log.v(TAG, "onDestroyView: destroying fragment.");
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // request write permission (read permission implicit)
    //	requestForPermission();
    public static boolean requestForPermission(Context context) {

        boolean isPermissionOn = true;
        final int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            if (!canAccessExternalSd(context)) {
                isPermissionOn = false;
                ActivityCompat.requestPermissions((Activity)context, EXTERNAL_PERMS, EXTERNAL_REQUEST);
            }
        }
        return isPermissionOn;
    }

    public static boolean canAccessExternalSd(Context context) {
        return (hasPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private static boolean hasPermission(Context context, String perm) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, perm));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "EXTERNAL_REQUEST code = " + requestCode);
        if (requestCode == EXTERNAL_REQUEST) {
            if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "EXTERNAL_REQUEST granted - re-launching view");
                init();
            }
            else {
                // denied
                Log.e(TAG, "onCreate permissions not granted - finish.");
                finishAndRemoveTask();
            }
        }
    }
}
////////////////////////////////////////////////////////////////////////////////////////////////
