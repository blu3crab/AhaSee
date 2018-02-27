// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: FEB 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adaptivehandyapps.ahasee.espi.FeedManager;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by mat on 2/24/2015.
 */
public class TabBaseline {
    // Baseline fragment:
    // - generate energy usage baseline based on algorithm & raw # days
    // - show raw energy or baseline usage charts
    // - generate & show baseline KwH details: total, average, peak, low
    // - write/read baseline KwH values as JSON files
    private static final String TAG = "TabBaseline";

    private static boolean RESET_PREFS = false;

    // parent activity & view references
//    private SeeActivity mContext;
    private Context mContext;
    private View mRootView = null;

    private FeedManager mFeedManager;

    private String mCustomBaselinesDirectory;

    private Spinner mSpinnerAlgo;
    private Spinner mSpinnerDays;
    private Spinner mSpinnerBaselineFiles;

    private CalendarView mCalendarView;

    private Button mButtonPrevBaseline;
    private Button mButtonNextBaseline;

    private Button mButtonSaveBaseline;
    private Button mButtonRemoveBaseline;

    private Boolean mNewBaselineInProgress;

    // chart dimensions
    private int mChartLayoutX;
    private int mChartLayoutY;

    // current chart controls
    private String mTitleStub = "Watts from ";
    private String mTitle = "Watts from ";
    private int mFromTimeSecs;
    private int mToTimeSecs;
    private int mStackLimit;    // # days after start date
    private String mAlgorithm;

    private List<Integer> mBaselineValues;

    // baseline details
    private class BaselineDetails {
        Integer total;
        Integer ave;
        Integer peak;
        Integer peakHour;
        Integer low;
        Integer lowHour;

        public BaselineDetails() {
            this.total = 0;
            this.ave = 0;
            this.peak = 0;
            this.peakHour = 0;
            this.low = 999999;
            this.lowHour = 0;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    private Boolean mBaselineFilesSpinnerInitialLoad = true;

    private List<String> mBaselineFilenameList;

    private List<String> getBaselineFilenameList() {
        return mBaselineFilenameList;
    }

    private List<String> setBaselineFilenameList(List<String> baselineFilenameList) {
        mBaselineFilenameList = baselineFilenameList;
        return mBaselineFilenameList;
    }

    private Integer mBaselineIndex = 0;

    private Integer getBaselineIndex() {
        mBaselineIndex = PrefUtils.getPrefsBaselineInx(mContext);
        return mBaselineIndex;
    }

    private Integer setBaselineIndex(Integer baselineIndex) {
        mBaselineIndex = baselineIndex;
        PrefUtils.setPrefsBaselineInx(mContext, baselineIndex);
        return mBaselineIndex;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    private FeedManager getFeedManager() {
        return mFeedManager;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    public TabBaseline(Context c, LayoutInflater inflater, ViewGroup container) {

        // get feed manager instance
        mContext = c;
        mFeedManager = FeedManager.getInstance(mContext, null);
        // set custom baseline directory path
        mCustomBaselinesDirectory = FileUtils.APP_DIR + mContext.getString(R.string.custom_baseline_dir);

        try {
            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mRootView = inflater.inflate(R.layout.frag_base_land, container, false);
            }
//            else {
//                mRootView = inflater.inflate(R.layout.frag_base_port, container, false);
//            }
            // init view elements
            initView();

        } catch (InflateException e) {
            Log.e(TAG, "inflater exception: " + e.getMessage());
            return;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // getters/setters/helpers
    public View getRootView() {
        return mRootView;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void initView() {

        mRootView.setBackgroundColor(mRootView.getResources().getColor(R.color.darkgreen));

        mButtonPrevBaseline = (Button) mRootView.findViewById(R.id.buttonPrevBaseline);
        mButtonPrevBaseline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // set selected baseline to previous instance
                Integer inx = getBaselineIndex();
                if (inx > 0) setBaselineIndex(getBaselineIndex() - 1);
                else if (inx <= 0) setBaselineIndex(getBaselineFilenameList().size() - 1);
                // update baseline files spinner to refresh view
                mSpinnerBaselineFiles.setSelection(getBaselineIndex());
//                // refresh view
//                refreshView();
            }
        });
        mButtonNextBaseline = (Button) mRootView.findViewById(R.id.buttonNextBaseline);
        mButtonNextBaseline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // set selected baseline to previous instance
                Integer inx = getBaselineIndex();
                if (inx < getBaselineFilenameList().size() - 1)
                    setBaselineIndex(getBaselineIndex() + 1);
                else if (inx >= getBaselineFilenameList().size() - 1) setBaselineIndex(0);
                // update baseline files spinner to refresh view
                mSpinnerBaselineFiles.setSelection(getBaselineIndex());
            }
        });

        mButtonSaveBaseline = (Button) mRootView.findViewById(R.id.buttonSaveBaseline);
        mButtonSaveBaseline.setText(R.string.baseline_button_new_baseline);
        mNewBaselineInProgress = false;
        mButtonSaveBaseline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // create baseline button press
                if (!mNewBaselineInProgress) {
                    mNewBaselineInProgress = true;
                    mButtonSaveBaseline.setText(R.string.baseline_button_save_baseline);
                    mButtonRemoveBaseline.setText(R.string.baseline_button_cancel_baseline);
                    // show baseline navigation elements
                    showBaselineNavigation();
//
//                    mButtonNextBaseline.setEnabled(false);
//                    mButtonPrevBaseline.setEnabled(false);
//                    if (mSpinnerBaselineFiles != null) mSpinnerBaselineFiles.setEnabled(false);
                } else {
                    // save baseline button press
                    saveBaseline();
                    mNewBaselineInProgress = false;
                    mButtonSaveBaseline.setText(R.string.baseline_button_new_baseline);
                    mButtonRemoveBaseline.setText(R.string.baseline_button_remove_baseline);
//                    mButtonNextBaseline.setEnabled(true);
//                    mButtonPrevBaseline.setEnabled(true);
//                    if (mSpinnerBaselineFiles != null) mSpinnerBaselineFiles.setEnabled(true);
                    // show baseline navigation elements
                    showBaselineNavigation();
                    // init baseline spinner
                    initBaselineFilesSpinner();
                }
            }
        });

        mButtonRemoveBaseline = (Button) mRootView.findViewById(R.id.buttonRemoveBaseline);
        mButtonRemoveBaseline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // remove selected baseline file
                removeBaseline();
                // init baseline spinner
                initBaselineFilesSpinner();
                // show current baseline
                refreshView();
            }
        });

        // TODO: extract to DevUtils?
        // size details
        LinearLayout layoutDetails = (LinearLayout) mRootView.findViewById(R.id.details);
        // determine chart dimensions
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mChartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mContext)) * .65);
//            mChartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mContext)) * .65);
            mChartLayoutX = (int) (((double) DevUtils.getDisplayWidthPixels(mContext)) * ChartUtils.CHART_LAND_X_PERCENT);
            mChartLayoutY = (int) (((double) DevUtils.getDisplayHeightPixels(mContext)) * ChartUtils.CHART_LAND_Y_PERCENT);
            int detailsLayoutX = (int) (((double) DevUtils.getDisplayWidthPixels(mContext)) * ChartUtils.DETAILS_LAND_X_PERCENT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(detailsLayoutX, mChartLayoutY);
            lp.setMargins(16, 0, 16, 0);
            layoutDetails.setLayoutParams(lp);
            CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);
            calendarView.setLayoutParams(new LinearLayout.LayoutParams(detailsLayoutX, detailsLayoutX - 128));
        }
//        else {
//            mChartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mContext)) * ChartUtils.CHART_PORT_X_PERCENT);
//            mChartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mContext)) * ChartUtils.CHART_PORT_Y_PERCENT);
//            int detailsLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mContext)) * .60);
//            int detailsLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mContext)) * .30);
//            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mChartLayoutX, detailsLayoutY);
//            lp.setMargins(16, 0, 16, 0);
//            layoutDetails.setLayoutParams(lp);
//            CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);
//            calendarView.setLayoutParams(new LinearLayout.LayoutParams(detailsLayoutX, detailsLayoutY));
//        }
        Log.v(TAG, "chartLayout X, Y: " + mChartLayoutX + ", " + mChartLayoutY);

        // set chart controls
        if (RESET_PREFS) {
            PrefUtils.setPrefsFromDate(mContext, PrefUtils.PREFS_DEFAULT_FROMDATE);
            PrefUtils.setPrefsToDate(mContext, PrefUtils.PREFS_DEFAULT_TODATE);
            PrefUtils.setPrefsStackLimit(mContext, 3);
        }
        mFromTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsFromDate(mContext), true) / 1000);
        mToTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsToDate(mContext), true) / 1000);

//        // restore current baseline index from prefs
//        mShowBaselineInx = PrefUtils.getPrefsBaselineInx(mContext);

        // init spinners
        mAlgorithm = PrefUtils.getPrefsAlgorithm(mContext);
        mSpinnerAlgo = (Spinner) mRootView.findViewById(R.id.spinnerAlgo);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterAlgo = ArrayAdapter.createFromResource(
                mContext, R.array.spinnerAlgoItems, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterAlgo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerAlgo.setAdapter(adapterAlgo);

        mSpinnerAlgo.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        Log.v(TAG, "mSpinnerAlgo: position=" + position + " id=" + id);
                        mAlgorithm = mSpinnerAlgo.getItemAtPosition(mSpinnerAlgo.getSelectedItemPosition()).toString();
                        Log.v(TAG, "mSpinnerAlgo.setOnItemSelectedListener algorithm: " + mAlgorithm);
                        PrefUtils.setPrefsAlgorithm(mContext, mAlgorithm);
                        refreshView();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.v(TAG, "mSpinnerAlgo: unselected");
                    }
                });

        // set spinner selection to trigger refresh view charts, text & controls
        mSpinnerAlgo.setSelection(((ArrayAdapter<String>) mSpinnerAlgo.getAdapter()).getPosition(mAlgorithm));

        // init spinners
        mStackLimit = PrefUtils.getPrefsStackLimit(mContext);
        mSpinnerDays = (Spinner) mRootView.findViewById(R.id.spinnerDays);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterDays = ArrayAdapter.createFromResource(
                mContext, R.array.spinnerDaysItems, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterDays.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerDays.setAdapter(adapterDays);

        mSpinnerDays.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        Log.v(TAG, "mSpinnerDays: position=" + position + " id=" + id);
                        mStackLimit = position;
                        Log.v(TAG, "mSpinnerDays.setOnItemSelectedListener days: " + mStackLimit);
                        PrefUtils.setPrefsStackLimit(mContext, mStackLimit);
                        refreshView();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.v(TAG, "mSpinnerDays: unselected");
                    }
                });

        // set spinner selection to trigger refresh view charts, text & controls
        mSpinnerDays.setSelection(mStackLimit);

        //initializes the calendarview
        mCalendarView = initializeCalendar();

        // init baseline spinner
        initBaselineFilesSpinner();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    private CalendarView initializeCalendar() {
        CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);

        try {
            calendarView.setMinDate(ChartUtils.dateToMillis(PrefUtils.getPrefsFeedFromDate(mContext), true));
            calendarView.setMaxDate(ChartUtils.dateToMillis(PrefUtils.getPrefsFeedToDate(mContext), true));
            calendarView.setDate(ChartUtils.dateToMillis(PrefUtils.getPrefsToDate(mContext), true));
        } catch (Exception e) {
            Log.e(TAG, "initializeCalendar exception: " + e.getMessage());
        }

        //sets the listener to be notified upon selected date change.
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            //show the selected date as a toast
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                Toast.makeText(mRootView.getContext(), day + "/" + month + "/" + year, Toast.LENGTH_LONG).show();
                Log.v(TAG, "onSelectedDayChange " + day + "/" + month + "/" + year);
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                calendar.set(Calendar.HOUR_OF_DAY, 00);
                calendar.set(Calendar.MINUTE, 00);
                calendar.set(Calendar.SECOND, 00);
                long selectedTimeMs = calendar.getTimeInMillis();
                Log.v(TAG, "setOnDateChangeListener timeMS:" + selectedTimeMs);
                // set time to calendar selection
                selectedTimeMs = ChartUtils.shiftEstMillisToUtc(selectedTimeMs);
                mFromTimeSecs = (int) (selectedTimeMs / 1000);
                mToTimeSecs = mFromTimeSecs + FeedManager.SECS_IN_DAY;
                Log.v(TAG, "setOnDateChangeListener from-to: " + mFromTimeSecs + " - " + mToTimeSecs);
                // save from/to times
                PrefUtils.setPrefsFromDate(mContext, ChartUtils.secsToDate(mFromTimeSecs, false));
                PrefUtils.setPrefsToDate(mContext, ChartUtils.secsToDate(mToTimeSecs, false));
                // refresh view charts, text & controls
                refreshView();
            }
        });
        return calendarView;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private Boolean showBaselineNavigation() {
        if (mSpinnerBaselineFiles != null) {
            if (getBaselineFilenameList().size() > 0) {
                mSpinnerBaselineFiles.setVisibility(View.VISIBLE);
            }
            else {
                mSpinnerBaselineFiles.setVisibility(View.INVISIBLE);
            }
        }
        if (getBaselineFilenameList().size() > 1) {
            mButtonNextBaseline.setEnabled(true);
            mButtonPrevBaseline.setEnabled(true);
        }
        else {
            mButtonNextBaseline.setEnabled(false);
            mButtonPrevBaseline.setEnabled(false);
        }

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private Boolean initBaselineFilesSpinner() {
        // scan directory for list of feed files
        setBaselineFilenameList(FileUtils.getFilesList(mCustomBaselinesDirectory, false, ""));
        Log.v(TAG, "initBaselineFilesSpinner pathList size: " + getBaselineFilenameList().size());

        if (getBaselineFilenameList().size() > 0) {
            mButtonRemoveBaseline.setEnabled(true);
//            if (getBaselineFilenameList().size() > 0) {
//                mButtonNextBaseline.setVisibility(View.VISIBLE);
//                mButtonPrevBaseline.setVisibility(View.VISIBLE);
//            }
            // add feed files to spinner
            mSpinnerBaselineFiles = (Spinner) mRootView.findViewById(R.id.spinnerBaseline);
//            mSpinnerBaselineFiles.setVisibility(View.VISIBLE);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, android.R.id.text1);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinnerBaselineFiles.setAdapter(spinnerAdapter);
            for (int i = 0; i < getBaselineFilenameList().size(); i++) {
//                Log.v(TAG, "Baseline pathList(" + i + "): " + pathList.get(i));
                spinnerAdapter.add(getBaselineFilenameList().get(i));
            }
            spinnerAdapter.notifyDataSetChanged();

            // if baseline index invalid
            if (getBaselineIndex() >= getBaselineFilenameList().size() || getBaselineIndex() < 0) {
                // reset & save baseline index to prefs
                setBaselineIndex(0);
            }
            // show baseline navigation elements
            showBaselineNavigation();
            // set current selection
            mSpinnerBaselineFiles.setSelection(getBaselineIndex());

            mBaselineFilesSpinnerInitialLoad = true;
            mSpinnerBaselineFiles.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        public void onItemSelected(
                                AdapterView<?> parent, View view, int position, long id) {
                            Log.v(TAG, "initBaselineFilesSpinner: position=" + position + " id=" + id);
                            // set baseline index to selected position
                            setBaselineIndex(position);
                            // if not initial load, refresh view
                            if (!mBaselineFilesSpinnerInitialLoad) refreshView();
                            mBaselineFilesSpinnerInitialLoad = false;
                        }

                        public void onNothingSelected(AdapterView<?> parent) {
                            Log.v(TAG, "initBaselineFilesSpinner: unselected");
                        }
                    });
        } else {
//            Toast.makeText(mRootView.getContext(), "Please generate a baseline to show.", Toast.LENGTH_SHORT).show();
//            if (mSpinnerBaselineFiles != null) mSpinnerBaselineFiles.setVisibility(View.INVISIBLE);
//            mButtonNextBaseline.setVisibility(View.INVISIBLE);
//            mButtonPrevBaseline.setVisibility(View.INVISIBLE);
            mButtonRemoveBaseline.setEnabled(false);
            // show baseline navigation elements
            showBaselineNavigation();
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // refresh view charts, text & controls
    public boolean refreshView() {
//        // load baseline chart
//        loadBaseline();

//        showBaselineUsage(mAlgorithm);
        // init the calendar
        initializeCalendar();

        if (mNewBaselineInProgress) {
//            if (mAlgorithm.equals(mContext.getString(R.string.baseline_algo_none))) {
            // init chart title
            String date = ChartUtils.secsToDate(mFromTimeSecs, false);
            mTitle = mTitleStub + date;
            // show by month
            showRawUsage();
        } else {
            // load baseline chart
            loadBaseline();
            showBaselineUsage(mAlgorithm);
        }
        // show text
        showDetailsText();

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean loadBaseline() {
//        // get custom baselines directory
//        String mCustomBaselinesDirectory = FileUtils.APP_DIR + mContext.getString(R.string.custom_baseline_dir);
        // scan directory for list of feed files
        List<String> pathList = FileUtils.getFilesList(mCustomBaselinesDirectory, true, "");
        Log.v(TAG, "loadBaseline pathList size: " + pathList.size());
        // if no baselines, prompt to generate baseline
        if (pathList.size() <= 0) {
//            Toast.makeText(mRootView.getContext(), "Please generate a baseline to show.", Toast.LENGTH_SHORT).show();
            return false;
        }
//        // if index into pathlist exceeds list size, reset to zero
//        if (mShowBaselineInx >= pathList.size()) mShowBaselineInx = 0;
//        // save baseline index to prefs
//        PrefUtils.setPrefsBaselineInx(mContext, mShowBaselineInx);

        // set baseline file to show
        String path = pathList.get(getBaselineIndex());
        // read json
        String json = FileUtils.readFeed(path);

        // extract json into class
        Gson gson = new Gson();
        Baseline b = gson.fromJson(json, Baseline.class);
        Log.v(TAG, "loadBaseline json: " + b.toString());

        // set chart controls
        List<String> nameList = FileUtils.getFilesList(mCustomBaselinesDirectory, false, "");
        String filename = nameList.get(getBaselineIndex());
        mTitle = mTitleStub + filename;
        mStackLimit = b.getDays() - 1;
        mFromTimeSecs = b.getStartSecs();
        mToTimeSecs = mFromTimeSecs + (FeedManager.SECS_IN_DAY);
        Log.d(TAG, "loadBaseline from date(secs) " + ChartUtils.secsToDate(mFromTimeSecs, false) + "(" + mFromTimeSecs + ")\n" +
                "to date(secs) " + ChartUtils.secsToDate(mToTimeSecs, false) + "(" + mToTimeSecs + ")");
//        Log.d(TAG,"loadBaseline from date(secs) " + ChartUtils.secsToDate(mFromTimeSecs,true) + "(" + mFromTimeSecs + ")\n" +
//                "to date(secs) " + ChartUtils.secsToDate(mToTimeSecs,true) + "(" + mToTimeSecs + ")");
        mAlgorithm = b.getAlgorithm();
        mBaselineValues = b.getValues();
        Log.v(TAG, "loadBaseline mBaselineValues: " + mBaselineValues);
        // TODO: introduce flag to prevent multiple refreshes on selection change
        // set spinner selection to trigger refresh view charts, text & controls
        mSpinnerDays.setSelection(mStackLimit);
        mSpinnerAlgo.setSelection(((ArrayAdapter<String>) mSpinnerAlgo.getAdapter()).getPosition(mAlgorithm));
        long millis = (long) mFromTimeSecs * 1000;
        try {
            // shift UTC time to EST for calendar
            millis = ChartUtils.shiftUtcMillisToEst(millis);
            mCalendarView.setDate(millis);
        } catch (Exception ex) {
            Log.e(TAG, "loadBaseline exception: set time to " + millis + "EX: " + ex.getMessage());
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showDetailsText() {
        TextView textView;
        // set location
        textView = (TextView) mRootView.findViewById(R.id.textViewLocation);
        textView.setText(SettingsActivity.getLocation(mContext));
        // set overall from to dates
        textView = (TextView) mRootView.findViewById(R.id.textViewFromToDate);
        textView.setText(PrefUtils.getPrefsFeedFromDate(mContext) + " to " + PrefUtils.getPrefsFeedToDate(mContext));

        // set baseline details
        if (mBaselineValues != null && mBaselineValues.size() > 0) {
            BaselineDetails bd = deriveBaselineDetails(mBaselineValues);
            // set details
            textView = (TextView) mRootView.findViewById(R.id.textViewTotalDay);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.total.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewAveDay);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.ave.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewPeakDay);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.peak.toString() + "(" + bd.peakHour.toString() + ")");
            textView = (TextView) mRootView.findViewById(R.id.textViewLowDay);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.low.toString() + "(" + bd.lowHour.toString() + ")");
        } else {
            textView = (TextView) mRootView.findViewById(R.id.textViewTotalDay);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewAveDay);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewPeakDay);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewLowDay);
            textView.setVisibility(View.INVISIBLE);
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showRawUsage() {
        String labelX = "Hour of Day";
        String labelY = "Watts";
        ChartRender.ChartType chartType = ChartRender.ChartType.AREA;

        ChartUtils.ChartControl chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_major),
                getFeedManager(),
                mTitle, labelX, labelY,
                mFromTimeSecs, mToTimeSecs, mStackLimit, chartType, true, mChartLayoutX, mChartLayoutY);

        ChartUtils.showFeed(chartControl, true);

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showBaselineUsage(String algorithm) {
        String labelX = "Hour of Day";
        String labelY = "Watts";

        ChartRender.ChartType chartType = ChartRender.ChartType.AREA;

        ChartUtils.ChartControl chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_major),
                getFeedManager(),
                mTitle, labelX, labelY,
                mFromTimeSecs, mToTimeSecs, mStackLimit, chartType, true, mChartLayoutX, mChartLayoutY);

        loadLineChart(chartControl, algorithm);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean loadLineChart(ChartUtils.ChartControl chartControl, String algorithm) {
        Log.v(TAG, "loadLineChart from time: " + chartControl.fromTimeSecs + " to time: " + chartControl.toTimeSecs);
        String value1Range = ChartUtils.secsToDate(chartControl.fromTimeSecs, true);
        String valueRange = ChartUtils.secsToDate(chartControl.toTimeSecs, true);

        // derive baseline values
        mBaselineValues = deriveBaselineValues(chartControl, algorithm);
        Log.v(TAG, "loadLineChart mBaselineValues: " + mBaselineValues);
        chartControl.chartRender.addSeriesLabels(valueRange);
        chartControl.chartRender.addSeries(mBaselineValues);
        // label chart
        chartControl.chartRender.setChartTitle(chartControl.chartTitle + " (" + value1Range + " to " + valueRange + ")");
        chartControl.chartRender.setChartAxisLabelX(chartControl.chartAxisLabelX);
        chartControl.chartRender.setChartAxisLabelY(chartControl.chartAxisLabelY);

        // show full chart
        chartControl.chartRender.showSeries(chartControl.showLabels, chartControl.chartType, chartControl.chartLayoutX, chartControl.chartLayoutY);

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private BaselineDetails deriveBaselineDetails(List<Integer> baselineValues) {
        BaselineDetails bd = new BaselineDetails();
        for (Integer v : baselineValues) {
            bd.total += v;
            if (v.compareTo(bd.peak) > 0) {
                bd.peak = v;
                bd.peakHour = baselineValues.indexOf(v);
            }
            if (v.compareTo(bd.low) < 0) {
                bd.low = v;
                bd.lowHour = baselineValues.indexOf(v);
            }
        }
        bd.ave = ((bd.total / baselineValues.size())) / 1000;
        if (bd.ave.equals(0)) bd.ave = 1;
        bd.total = bd.total / 1000;
        bd.peak = bd.peak / 1000;
        bd.low = bd.low / 1000;
        return bd;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private List<Integer> deriveBaselineValues(ChartUtils.ChartControl chartControl, String algorithm) {

        List<Integer> baseline = new ArrayList<>();

        if (!chartControl.feedManager.isValidFeed()) return baseline;

        List<Integer> values = new ArrayList<>();
        List<Integer> sum = new ArrayList<>();
        List<Integer> max = new ArrayList<>();
        List<Integer> min = new ArrayList<>();

        // init range lablels
        Integer from = chartControl.fromTimeSecs;
        Integer to = chartControl.toTimeSecs;
        Integer timeRange = to - from;

        // for each value set up to stack limit
        for (int i = 0; i <= chartControl.stackLimit; i++) {
            // get selected date interval readings
            values = chartControl.feedManager.queryWhValues(from, to);
            int iv = 0;
            for (Integer v : values) {
                // if intermediate list entry defined
                if (iv < sum.size()) {
                    // add value to running sum
                    sum.set(iv, sum.get(iv) + v);
                    // if value highest, set as max
                    if (max.get(iv) < v) max.set(iv, v);
                    // if value lowest, set as min
                    if (min.get(iv) > v) min.set(iv, v);
                } else {
                    // create entry
                    sum.add(v);
                    max.add(v);
                    min.add(v);
                }
                ++iv;
            }
            // advance from to
            from = to;
            to = to + (timeRange);
        }
        // set baseline values
        if (mStackLimit < 2) {
            // if 1 day sample, use raw values
            baseline = values;
        }
        else if (algorithm.equals(mContext.getString(R.string.baseline_algo_average)) ||
                algorithm.equals(mContext.getString(R.string.baseline_algo_none)) ||
                ((algorithm.equals(mContext.getString(R.string.baseline_algo_mid)) &&
                        mStackLimit < 3))) {
            // if raw or average, or mean with 2 samples (identical to average), calculate average
            for (Integer v : sum) {
                baseline.add(v / (mStackLimit + 1));
            }
        }
        else if (algorithm.equals(mContext.getString(R.string.baseline_algo_max))) {
            // if multiple samples, set max
            baseline = max;
        }
        else if (algorithm.equals(mContext.getString(R.string.baseline_algo_min))) {
            // if multiple samples, set min
            baseline = min;
        }
        else if (algorithm.equals(mContext.getString(R.string.baseline_algo_mid))) {
            // if multiple samples, calculate mean
            int iv = 0;
            for (Integer v : sum) {
                v -= max.get(iv) + min.get(iv);
                baseline.add(v / ((mStackLimit + 1) - 2));
                ++iv;
            }
        }
//        // set baseline values based on analysis of intermediate sum, max, min
//        if (algorithm.equals(mContext.getString(R.string.baseline_algo_none))) {
//            baseline = values;
//        } else if (algorithm.equals(mContext.getString(R.string.baseline_algo_max))) {
//            baseline = max;
//        } else if (algorithm.equals(mContext.getString(R.string.baseline_algo_min))) {
//            baseline = min;
//        } else if (algorithm.equals(mContext.getString(R.string.baseline_algo_average)) ||
//                ((algorithm.equals(mContext.getString(R.string.baseline_algo_mid)) &&
//                        mStackLimit < 3))) {
//            for (Integer v : sum) {
//                baseline.add(v / (mStackLimit + 1));
//            }
//        } else if (algorithm.equals(mContext.getString(R.string.baseline_algo_mid))) {
//            int iv = 0;
//            for (Integer v : sum) {
//                v -= max.get(iv) + min.get(iv);
//                baseline.add(v / ((mStackLimit + 1) - 2));
//                ++iv;
//            }
//        }
        return baseline;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // save button
    private Boolean saveBaseline() {
        // if no baseline, prompt to generate baseline
        if (mBaselineValues != null && mBaselineValues.size() <= 0) {
            Toast.makeText(mRootView.getContext(), "Please generate a baseline to save.", Toast.LENGTH_SHORT).show();
        }
        // reset stack limit
        mStackLimit = 0;
        PrefUtils.setPrefsStackLimit(mContext, mStackLimit);
        // show baseline to derive values according to algorithm
        showBaselineUsage(mAlgorithm);

        // build filename: baseline + algo + days + date
        String date = ChartUtils.secsToDate(mFromTimeSecs, false);
        String filename = "baseline_" + mAlgorithm + "_" + (mStackLimit + 1) + "_days_" + date + ".json";
        Log.v(TAG, "saveBaseline: filename " + filename);

        // init GSON object
        Baseline b = new Baseline();
        b.setFilename(filename);
        b.setAlgorithm(mAlgorithm);
        b.setDays(mStackLimit + 1);
        b.setStartSecs(mFromTimeSecs);
        b.setValues(mBaselineValues);

        Gson gson = new Gson();
        String json = gson.toJson(b);
        Log.v(TAG, "saveBaseline: json " + json);

        // get custom baselines directory
//        String customBaselinesDirectory = FileUtils.APP_DIR + mContext.getString(R.string.custom_baseline_dir);
        File folder = FileUtils.getTargetDir(mCustomBaselinesDirectory);
        String path = folder.getAbsolutePath();
        Log.v(TAG, "saveBaseline: path " + path);

        path = path.concat("/" + filename);
        Log.v(TAG, "saveBaseline: fullpath " + path);

        // write baseline
        try {
            File outFile = new File(path);
            if (outFile.exists()) {
                if (!outFile.delete()) Log.e(TAG, "saveBaseline failed to delete existing file.");
                if (!outFile.createNewFile()) Log.e(TAG, "saveBaseline failed to create new file.");
            }
            FileOutputStream fOut = new FileOutputStream(outFile);
            OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
            outWriter.append(json);
            outWriter.close();
            fOut.close();
        } catch (Exception e) {
            Log.e(TAG, "saveBaseline exception: " + e.getMessage());
            Toast.makeText(mRootView.getContext(), "Unable to save baseline.", Toast.LENGTH_SHORT).show();
            return false;
        }
        // confirm baseline created
        Toast.makeText(mRootView.getContext(), "Baseline saved to " + filename, Toast.LENGTH_SHORT).show();

        return true;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // TODO: remove button
    private Boolean removeBaseline() {

        File folder = FileUtils.getTargetDir(mCustomBaselinesDirectory);
        String path = folder.getAbsolutePath();
        Log.v(TAG, "removeBaseline: path " + path);

        String filename = getBaselineFilenameList().get(getBaselineIndex());
        path = path.concat("/" + filename);
        Log.v(TAG, "removeBaseline: fullpath " + path);

        File fdelete = new File(path);
        if (fdelete.exists())
        {
            if (fdelete.delete()) {
                System.out.println("removeBaseline file Deleted :" + path);
            }
            else {
                System.out.println("removeBaseline file not Deleted :" + path);
            }
        }
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
//////////////////////////////////////////////////////////////////////////////////////////
