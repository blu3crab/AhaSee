// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: FEB 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

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

import java.util.Calendar;
import java.util.List;

/**
 * Created by mat on 2/24/2015.
 */
public class TabCompare {
    // Compare fragment:
    // - show charts of selected days compared to selected baseline
    // - generate & show compare KwH delta details: total, average, peak, low
    // - write/read findings (future)
    // - show findings (future)
    private static final String TAG = "TabCompare";

    // test/debug setting
    private static boolean RESET_PREFS = false;

    // parent activity & view references
    private SeeActivity mParentActivity;
    private View mRootView = null;

    private int mResIdSize = R.style.TextAppearance_AppCompat_Medium;

    private Button mButtonShowBaseline;
    private Button mButtonNext;
    private Button mButtonPrev;

    private Spinner mSpinnerDays;

    private CalendarView mCalendarView;
    private int mCalendarTimeSecs;

    // chart dimensions
    private int mChartLayoutX;
    private int mChartLayoutY;

    // current chart controls
    private String mTitleStub = "Watts from ";
    private String mTitle = "Watts from ";
    private int mFromTimeSecs;
    private int mToTimeSecs;
    private int mStackLimit;    // # days after start date

    private List<Integer> mBaselineValues;

    private int mShowBaselineInx = 0;
    private int mBaselineStartSecs;

    private BaselineDetails mBaselineDetails;
    private BaselineDetails mCompareDetails;

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

    public TabCompare(SeeActivity parentActivity, LayoutInflater inflater, ViewGroup container) {

        mParentActivity = parentActivity;

        try {
            if ( mParentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mRootView = inflater.inflate(R.layout.frag_compare_land, container, false);
            }
            else {
                mRootView = inflater.inflate(R.layout.frag_compare_port, container, false);
            }

            mResIdSize = DevUtils.getDevTextSize(mParentActivity);
            initView(mResIdSize);

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
    public void initView(final int resIdSize) {

        mRootView.setBackgroundColor(mRootView.getResources().getColor(R.color.darkgreen));

        mButtonNext = (Button) mRootView.findViewById(R.id.buttonNext);
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // if advance beyond defined GB data, ignore
                if (0 == 1) {
                    return;
                }
                // advance current date & refresh
                mFromTimeSecs += FeedManager.SECS_IN_DAY;
                mToTimeSecs += FeedManager.SECS_IN_DAY;
                // save from/to times
                PrefUtils.setPrefsFromDate(mParentActivity, ChartUtils.secsToDate(mFromTimeSecs, false));
                PrefUtils.setPrefsToDate(mParentActivity, ChartUtils.secsToDate(mToTimeSecs, false));
                refreshView();
            }
        });

        mButtonPrev = (Button) mRootView.findViewById(R.id.buttonPrev);
        mButtonPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // if reverse beyond defined GB data, ignore
                if (0 == 1) {
                    return;
                }
                // reverse current date & refresh
                mFromTimeSecs -= FeedManager.SECS_IN_DAY;
                mToTimeSecs -= FeedManager.SECS_IN_DAY;
                // save from/to times
                PrefUtils.setPrefsFromDate(mParentActivity, ChartUtils.secsToDate(mFromTimeSecs, false));
                PrefUtils.setPrefsToDate(mParentActivity, ChartUtils.secsToDate(mToTimeSecs, false));
                refreshView();
            }
        });


        mButtonShowBaseline = (Button) mRootView.findViewById(R.id.buttonShowBaseline);
        mButtonShowBaseline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            // bump for next baseline
            ++mShowBaselineInx;
            // save baseline index to prefs
            PrefUtils.setPrefsBaselineInx(mParentActivity,mShowBaselineInx);
            // show next baseline
            refreshView();
            }
        });

        // TODO: extract to DevUtils?
        // size details
        LinearLayout layoutDetails = (LinearLayout) mRootView.findViewById(R.id.details);
        // determine chart dimensions
        if ( mParentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mChartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mParentActivity)) * .65);
            mChartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mParentActivity)) * .65);
            int detailsLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mParentActivity)) * .30);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(detailsLayoutX, mChartLayoutY);
            lp.setMargins(16, 0, 16, 0);
            layoutDetails.setLayoutParams(lp);
            CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);
            calendarView.setLayoutParams(new LinearLayout.LayoutParams(detailsLayoutX, detailsLayoutX-32));
        }
        else {
            mChartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mParentActivity)) * .96);
            mChartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mParentActivity)) * .40);
            int detailsLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mParentActivity)) * .55);
            int detailsLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mParentActivity)) * .30);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mChartLayoutX, detailsLayoutY);
            lp.setMargins(16, 0, 16, 0);
            layoutDetails.setLayoutParams(lp);
            CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);
            calendarView.setLayoutParams(new LinearLayout.LayoutParams(detailsLayoutX, detailsLayoutY));
        }
        Log.v(TAG, "chartLayout X, Y: " + mChartLayoutX + ", " + mChartLayoutY);

        // init days spinner
        mSpinnerDays = (Spinner) mRootView.findViewById(R.id.spinnerDays);
        mStackLimit = PrefUtils.getPrefsStackLimit(mParentActivity);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                mParentActivity, R.array.spinnerDaysItems, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerDays.setAdapter(adapter);

        mSpinnerDays.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        Log.v(TAG, "mSpinnerDays: position=" + position + " id=" + id);
                        mStackLimit = position;
                        Log.v(TAG, "mSpinnerDays.setOnItemSelectedListener days: " + mStackLimit);
                        PrefUtils.setPrefsStackLimit(mParentActivity, mStackLimit);
                        refreshView();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.v(TAG, "mSpinnerDays: unselected");
                    }
                });

        // set spinner selection to trigger refresh view charts, text & controls
        mStackLimit = PrefUtils.getPrefsStackLimit(mParentActivity);

        mSpinnerDays.setSelection(mStackLimit);
        // mSpinnerDays.setSelection should trigger refresh
        refreshView();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // refresh view charts, text & controls
    public boolean refreshView() {

        // reset chart controls
        if (RESET_PREFS) {
            PrefUtils.setPrefsFromDate(mParentActivity, "01-05-2015");
            PrefUtils.setPrefsToDate(mParentActivity, "02-05-2015");
            PrefUtils.setPrefsStackLimit(mParentActivity, 3);
        }
        // set chart controls: from/to times, stacklimit
        mFromTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsFromDate(mParentActivity), true) / 1000);
        mToTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsToDate(mParentActivity), true) / 1000);
        mStackLimit = PrefUtils.getPrefsStackLimit(mParentActivity);

        // restore current baseline index from prefs
        mShowBaselineInx = PrefUtils.getPrefsBaselineInx(mParentActivity);

        //initializes the calendarview
        mCalendarView = initializeCalendar();

        // get current baseline
        getBaseline();

        // show baseline compared to selected days
        showCompare();
        // show text
        showDetailsText(mResIdSize);

        return true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    private CalendarView initializeCalendar() {
        CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);

        try {
            calendarView.setMinDate(ChartUtils.dateToMillis(PrefUtils.getPrefsFeedFromDate(mParentActivity), true));
            calendarView.setMaxDate(ChartUtils.dateToMillis(PrefUtils.getPrefsFeedToDate(mParentActivity), true));
            calendarView.setDate(ChartUtils.dateToMillis(PrefUtils.getPrefsToDate(mParentActivity), true));
//            calendarView.setDate(ChartUtils.dateToMillis("01-05-2015", true));
        }
        catch (Exception e) {
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
//                c.set(year, month, day, 0, 0, 0);
                long selectedTimeMs = calendar.getTimeInMillis();
                Log.v(TAG, "setOnDateChangeListener timeMS:" + selectedTimeMs);
                // set time to calendar selection
                selectedTimeMs = ChartUtils.shiftEstMillisToUtc(selectedTimeMs);
                mFromTimeSecs = (int) (selectedTimeMs / 1000);
                mToTimeSecs = mFromTimeSecs + FeedManager.SECS_IN_DAY;
                Log.v(TAG, "setOnDateChangeListener from-to: " + mFromTimeSecs + " - " + mToTimeSecs);
                // save from/to times
                PrefUtils.setPrefsFromDate(mParentActivity, ChartUtils.secsToDate(mFromTimeSecs, false));
                PrefUtils.setPrefsToDate(mParentActivity, ChartUtils.secsToDate(mToTimeSecs, false));
                // refresh view charts, text & controls
                refreshView();
            }
        });
        return calendarView;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean getBaseline() {

        // scan directory for list of feed files
        List<String> pathList = FileUtils.getFilesList(mParentActivity.getString(R.string.custom_baseline_dir), true);
        Log.v(TAG, "getBaseline pathList size: " + pathList.size());
        // if no baselines, prompt to generate baseline
        if (pathList.size() <= 0) {
            Toast.makeText(mRootView.getContext(), "Please generate a baseline to show.", Toast.LENGTH_SHORT).show();
            return false;
        }
        // if index into pathlist exceeds list size, reset to zero
        if (mShowBaselineInx >= pathList.size()) {
            mShowBaselineInx = 0;
            // save baseline index to prefs
            PrefUtils.setPrefsBaselineInx(mParentActivity,mShowBaselineInx);
        }
        // set baseline file to show
        String path = pathList.get(mShowBaselineInx);
        // get filename
        List<String> nameList = FileUtils.getFilesList(mParentActivity.getString(R.string.custom_baseline_dir), false);
        String filename = nameList.get(mShowBaselineInx);
        // read json
        String json = FileUtils.readFeed(path);

        // extract json into class
        Gson gson = new Gson();
        Baseline b = gson.fromJson(json, Baseline.class);
        Log.v(TAG, "getBaseline json: " + b.toString());

        // set chart controls
//        mTitle = mTitleStub + b.getFilename();
        mTitle = mTitleStub + filename;
//        int stackLimit = b.getDays() - 1;
        mBaselineStartSecs = b.getStartSecs();
//        mToTimeSecs = mFromTimeSecs + (FeedManager.SECS_IN_DAY);
//        String algorithm = b.getAlgorithm();
        mBaselineValues = b.getValues();
//        refreshView();
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showDetailsText(int resIdSize) {
        TextView textView;
        // set location
        textView = (TextView) mRootView.findViewById(R.id.textViewLocation);
        textView.setTextAppearance(mParentActivity, resIdSize);
        textView.setText(SettingsActivity.getLocation(mParentActivity));
        // set overall from to dates
        textView = (TextView) mRootView.findViewById(R.id.textViewFromToDate);
        textView.setTextAppearance(mParentActivity, resIdSize);
        textView.setText(PrefUtils.getPrefsFeedFromDate(mParentActivity) + " to " + PrefUtils.getPrefsFeedToDate(mParentActivity));

        // set baseline details
        if (mBaselineValues != null && mBaselineValues.size() > 0) {
//            BaselineDetails bd = deriveBaselineDetails(mBaselineValues);
            BaselineDetails bd = new BaselineDetails();
            bd = deriveBaselineDetails(mBaselineValues, bd, true);

            // set details
            textView = (TextView) mRootView.findViewById(R.id.textViewTotalDay);
            setVisibleTextAttribs(textView, resIdSize, R.color.black);
            textView.setText(bd.total.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewAveDay);
            setVisibleTextAttribs(textView, resIdSize, R.color.black);
            textView.setText(bd.ave.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewPeakDay);
            setVisibleTextAttribs(textView, resIdSize, R.color.black);
            textView.setText(bd.peak.toString() + "(" + bd.peakHour.toString() + ")");
            textView = (TextView) mRootView.findViewById(R.id.textViewLowDay);
            setVisibleTextAttribs(textView, resIdSize, R.color.black);
            textView.setText(bd.low.toString() + "(" + bd.lowHour.toString() + ")");

            int color;
            textView = (TextView) mRootView.findViewById(R.id.textViewTotalDelta);
            color = compareForColor(bd.total, mCompareDetails.total);
            setVisibleTextAttribs(textView, resIdSize, color);
            textView.setText(mCompareDetails.total.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewAveDelta);
            color = compareForColor(bd.ave, mCompareDetails.ave);
            setVisibleTextAttribs(textView, resIdSize, color);
            textView.setText(mCompareDetails.ave.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewPeakDelta);
            color = compareForColor(bd.peak, mCompareDetails.peak);
            setVisibleTextAttribs(textView, resIdSize, color);
            textView.setText(mCompareDetails.peak.toString() + "(" + mCompareDetails.peakHour.toString() + ")");
            textView = (TextView) mRootView.findViewById(R.id.textViewLowDelta);
            color = compareForColor(bd.low, mCompareDetails.low);
            setVisibleTextAttribs(textView, resIdSize, color);
            textView.setText(mCompareDetails.low.toString() + "(" + mCompareDetails.lowHour.toString() + ")");

        }
        else {
            textView = (TextView) mRootView.findViewById(R.id.textViewTotalDay);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewAveDay);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewPeakDay);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewLowDay);
            textView.setVisibility(View.INVISIBLE);

            textView = (TextView) mRootView.findViewById(R.id.textViewTotalDelta);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewAveDelta);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewPeakDelta);
            textView.setVisibility(View.INVISIBLE);
            textView = (TextView) mRootView.findViewById(R.id.textViewLowDelta);
            textView.setVisibility(View.INVISIBLE);
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private int compareForColor(int baseline, int compare) {
        int color = R.color.black;
        if (baseline < compare) {
            color = R.color.red;
        }
        else if (baseline > compare) {
            color = R.color.green;
        }
        return color;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean setVisibleTextAttribs(TextView textView, int resIdSize, int resIdColor) {
        textView.setTextAppearance(mParentActivity, resIdSize);
        textView.setVisibility(View.VISIBLE);
        textView.setTextColor(mParentActivity.getResources().getColor(resIdColor));
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private BaselineDetails deriveBaselineDetails(List<Integer> baselineValues, BaselineDetails bd, boolean finalize) {
//        BaselineDetails bd = new BaselineDetails();
//        BaselineDetails bd = ibd;
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
        if (finalize) {
            bd.ave = ((bd.total / baselineValues.size()))/1000;
            if (bd.ave.equals(0)) bd.ave = 1;
            bd.total = bd.total / 1000;
            bd.peak = bd.peak / 1000;
            bd.low = bd.low / 1000;
        }
        return bd;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showCompare() {
        String labelX = "Hour of Day";
        String labelY = "Watts";

        ChartRender.ChartType chartType = ChartRender.ChartType.AREA;

        ChartUtils.ChartControl chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_major),
                mParentActivity.getFeedManager(),
                mTitle, labelX, labelY,
                mFromTimeSecs, mToTimeSecs, mStackLimit, chartType, true, mChartLayoutX, mChartLayoutY);

        loadLineChart(chartControl);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean loadLineChart(ChartUtils.ChartControl chartControl) {
        Log.v(TAG,"from time: " + chartControl.fromTimeSecs + " to time: " + chartControl.toTimeSecs);
        String startLabel = ChartUtils.secsToDate(chartControl.fromTimeSecs, false);
        String endLabel = ChartUtils.secsToDate(chartControl.toTimeSecs + (mStackLimit*FeedManager.SECS_IN_DAY), true);
        String baselineStart = ChartUtils.secsToDate(mBaselineStartSecs, false);

        if (mBaselineValues != null && mBaselineValues.size() > 0) {
            // load baseline values
            chartControl.chartRender.addSeriesLabels(baselineStart);
            chartControl.chartRender.addSeries(mBaselineValues);
        }

        // load selected days values
        Integer from = chartControl.fromTimeSecs;
        Integer to = chartControl.toTimeSecs;
        Integer timeRange = to - from;

        List<Integer> values;
        String seriesLabel = "";
        boolean finalize = false;
        // create new compare details
        mCompareDetails = new BaselineDetails();

        // for each value set up to stack limit, set series & series label
        for (int i = 0; i <= chartControl.stackLimit; i++) {
            // get selected date interval readings
            if (chartControl.feedManager.isValidFeed()) {
                values = chartControl.feedManager.queryWhValues(from, to);
                if (values.size() > 0) {
                    seriesLabel = ChartUtils.secsToDate(from, false);
                    // add series
                    chartControl.chartRender.addSeriesLabels(seriesLabel);
                    chartControl.chartRender.addSeries(values);
                    // TODO: if last value set is empty, finalize will NOT occur!
                    // finalize on the last value set by triggering wats to kwh conversion
                    if (i == chartControl.stackLimit) {
                        finalize = true;
                    }
                    // aggregate compare details
                    mCompareDetails = deriveBaselineDetails(values, mCompareDetails, finalize);
                }
            }
            else {
                return false;
            }
            // advance from to
            from = to;
            to = to + (timeRange);
        }
        // label chart
        chartControl.chartRender.setChartTitle(chartControl.chartTitle + " (" + startLabel + " to " + endLabel + ")");
        chartControl.chartRender.setChartAxisLabelX(chartControl.chartAxisLabelX);
        chartControl.chartRender.setChartAxisLabelY(chartControl.chartAxisLabelY);

        // show full chart
        chartControl.chartRender.showSeries(chartControl.showLabels, chartControl.chartType, chartControl.chartLayoutX, chartControl.chartLayoutY);

        // if multiple days, adjust total & average by # days
        if (mStackLimit > 0) {
            mCompareDetails.total = mCompareDetails.total / (mStackLimit+1);
            mCompareDetails.ave = mCompareDetails.ave / (mStackLimit+1);
        }
        return true;
    }

//    ////////////////////////////////////////////////////////////////////////////////////////////////
//    private List<Integer> deriveBaselineValues(ChartUtils.ChartControl chartControl, String algorithm) {
//
//        List<Integer> baseline = new ArrayList<>();
//
//        if (!chartControl.feedManager.isValidFeed()) return baseline;
//
//        List<Integer> values;
//        List<Integer> sum = new ArrayList<>();
//        List<Integer> max = new ArrayList<>();
//        List<Integer> min = new ArrayList<>();
//
//        // init range lablels
//        Integer from = chartControl.fromTimeSecs;
//        Integer to = chartControl.toTimeSecs;
//        Integer timeRange = to - from;
//
//        // for each value set up to stack limit
//        for (int i = 0; i <= chartControl.stackLimit; i++) {
//            // get selected date interval readings
//            values = chartControl.feedManager.queryWhValues(from, to);
//            int iv = 0;
//            for (Integer v : values) {
//                // if intermediate list entry defined
//                if (iv < sum.size()) {
//                    // add value to running sum
//                    sum.set(iv, sum.get(iv) + v);
//                    // if value highest, set as max
//                    if (max.get(iv) < v ) max.set(iv, v);
//                    // if value lowest, set as min
//                    if (min.get(iv) > v ) min.set(iv, v);
//                }
//                else {
//                    // create entry
//                    sum.add(v);
//                    max.add(v);
//                    min.add(v);
//                }
//                ++iv;
//            }
//            // advance from to
//            from = to;
//            to = to + (timeRange);
//        }
//        // set baseline values based on analysis of intermediate sum, max, min
//        if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_max))) {
//            baseline = max;
//        }
//        else if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_min))) {
//            baseline = min;
//        }
//        else if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_average)) ||
//                ((algorithm.equals(mParentActivity.getString(R.string.baseline_algo_mid)) &&
//                        mStackLimit < 3))) {
//            for (Integer v : sum) {
//                baseline.add(v/(mStackLimit+1));
//            }
//        }
//        else if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_mid))) {
//            int iv = 0;
//            for (Integer v : sum) {
//                v -= max.get(iv) + min.get(iv);
//                baseline.add(v/((mStackLimit+1)-2));
//                ++iv;
//            }
//        }
//        return baseline;
//    }
    //////////////////////////////////////////////////////////////////////////////////////////
}
//////////////////////////////////////////////////////////////////////////////////////////
