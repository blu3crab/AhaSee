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
    private SeeActivity mParentActivity;
    private View mRootView = null;

    private int mResIdSize = R.style.TextAppearance_AppCompat_Medium;

    private Button mButtonShowAnalysis;
    private Button mButtonSaveBaseline;
    private Button mButtonShowBaseline;

    private Spinner mSpinnerAlgo;
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
    private String mAlgorithm;

    private List<Integer> mBaselineValues;

    private int mShowBaselineInx = 0;

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

    public TabBaseline(SeeActivity parentActivity, LayoutInflater inflater, ViewGroup container) {

        mParentActivity = parentActivity;

        try {
            if ( mParentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mRootView = inflater.inflate(R.layout.frag_base_land, container, false);
            }
            else {
                mRootView = inflater.inflate(R.layout.frag_base_port, container, false);
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

        mButtonShowAnalysis = (Button) mRootView.findViewById(R.id.buttonShowAnalysis);
        mButtonShowAnalysis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // save spinner selection to prefs
                mAlgorithm = mSpinnerAlgo.getItemAtPosition(mSpinnerAlgo.getSelectedItemPosition()).toString();
                Log.v(TAG, "mButtonShowAnalysis.setOnClickListener algorithm: " + mAlgorithm);
                PrefUtils.setPrefsAlgorithm(mParentActivity, mAlgorithm);
                mStackLimit = mSpinnerDays.getSelectedItemPosition();
                Log.v(TAG, "mButtonShowAnalysis.setOnClickListener days: " + mStackLimit);
                PrefUtils.setPrefsStackLimit(mParentActivity, mStackLimit);
                // refresh view charts, text & controls
                refreshView();
            }
        });

        mButtonSaveBaseline = (Button) mRootView.findViewById(R.id.buttonSaveBaseline);
        mButtonSaveBaseline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // save baseline
                saveBaseline();
            }
        });

        mButtonShowBaseline = (Button) mRootView.findViewById(R.id.buttonShowBaseline);
        mButtonShowBaseline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // show next baseline
                showBaseline();
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
            int detailsLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mParentActivity)) * .60);
            int detailsLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mParentActivity)) * .30);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mChartLayoutX, detailsLayoutY);
            lp.setMargins(16, 0, 16, 0);
            layoutDetails.setLayoutParams(lp);
            CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);
            calendarView.setLayoutParams(new LinearLayout.LayoutParams(detailsLayoutX, detailsLayoutY));
        }
        Log.v(TAG, "chartLayout X, Y: " + mChartLayoutX + ", " + mChartLayoutY);

        // set chart controls
        if (RESET_PREFS) {
            PrefUtils.setPrefsFromDate(mParentActivity, "01-05-2015");
            PrefUtils.setPrefsToDate(mParentActivity, "02-05-2015");
            PrefUtils.setPrefsStackLimit(mParentActivity, 3);
        }
        mFromTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsFromDate(mParentActivity), true) / 1000);
        mToTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsToDate(mParentActivity), true) / 1000);

        // restore current baseline index from prefs
        mShowBaselineInx = PrefUtils.getPrefsBaselineInx(mParentActivity);

        // init spinners
        mAlgorithm = PrefUtils.getPrefsAlgorithm(mParentActivity);
        mSpinnerAlgo = (Spinner) mRootView.findViewById(R.id.spinnerAlgo);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterAlgo = ArrayAdapter.createFromResource(
                mParentActivity, R.array.spinnerAlgoItems, android.R.layout.simple_spinner_item);
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
                        PrefUtils.setPrefsAlgorithm(mParentActivity, mAlgorithm);
                        refreshView();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.v(TAG, "mSpinnerAlgo: unselected");
                    }
                });

        // set spinner selection to trigger refresh view charts, text & controls
        mSpinnerAlgo.setSelection(((ArrayAdapter<String>)mSpinnerAlgo.getAdapter()).getPosition(mAlgorithm));

        // init spinners
        mStackLimit = PrefUtils.getPrefsStackLimit(mParentActivity);
        mSpinnerDays = (Spinner) mRootView.findViewById(R.id.spinnerDays);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterDays = ArrayAdapter.createFromResource(
                mParentActivity, R.array.spinnerDaysItems, android.R.layout.simple_spinner_item);
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
                        PrefUtils.setPrefsStackLimit(mParentActivity, mStackLimit);
                        refreshView();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.v(TAG, "mSpinnerDays: unselected");
                    }
                });

        // set spinner selection to trigger refresh view charts, text & controls
        mSpinnerDays.setSelection(mStackLimit);

        // refresh view charts, text & controls
//        refreshView();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showBaseline() {

        // scan directory for list of feed files
        List<String> pathList = FileUtils.getFilesList(mParentActivity.getString(R.string.custom_baseline_dir), true);
        Log.v(TAG, "showBaseline pathList size: " + pathList.size());
        // if no baselines, prompt to generate baseline
        if (pathList.size() <= 0) {
            Toast.makeText(mRootView.getContext(), "Please generate a baseline to show.", Toast.LENGTH_SHORT).show();
            return false;
        }
        // if index into pathlist exceeds list size, reset to zero
        if (mShowBaselineInx >= pathList.size()) mShowBaselineInx = 0;
        // set baseline file to show then increment for next
        String path = pathList.get(mShowBaselineInx);
        // save baseline index to prefs
        PrefUtils.setPrefsBaselineInx(mParentActivity, mShowBaselineInx);
        // get filename
        List<String> nameList = FileUtils.getFilesList(mParentActivity.getString(R.string.custom_baseline_dir), false);
        String filename = nameList.get(mShowBaselineInx);
        // bump for next baseline
        ++mShowBaselineInx;

        // read json
        String json = FileUtils.readFeed(path);

        // extract json into class
        Gson gson = new Gson();
        Baseline b = gson.fromJson(json, Baseline.class);
        Log.v(TAG, "showBaseline json: " + b.toString());

        // set chart controls
//        mTitle = mTitleStub + b.getFilename();
        mTitle = mTitleStub + filename;
        mStackLimit = b.getDays() - 1;
        mFromTimeSecs = b.getStartSecs();
        mToTimeSecs = mFromTimeSecs + (FeedManager.SECS_IN_DAY);
        mAlgorithm = b.getAlgorithm();
        mBaselineValues = b.getValues();
        // TODO: introduce flag to prevent multiple refreshes on selection change
        // set spinner selection to trigger refresh view charts, text & controls
        mSpinnerDays.setSelection(mStackLimit);
        mSpinnerAlgo.setSelection(((ArrayAdapter<String>)mSpinnerAlgo.getAdapter()).getPosition(mAlgorithm));
        long millis = (long)mFromTimeSecs * 1000;
        try {
            mCalendarView.setDate(millis);
        }
        catch (Exception ex) {
            Log.e(TAG, "showBaseline exception: set time to " + millis + "EX: " + ex.getMessage());
        }
        refreshView();
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean saveBaseline() {
        // if no baseline, prompt to generate baseline
        // TODO: refresh view to generate baseline
        if (mBaselineValues != null && mBaselineValues.size() <= 0) {
            Toast.makeText(mRootView.getContext(), "Please generate a baseline to save.", Toast.LENGTH_SHORT).show();
        }
        // build filename: baseline + algo + days + date
        String date = ChartUtils.secsToDate(mFromTimeSecs, false);
        String filename = "baseline_" + mAlgorithm + "_" + (mStackLimit+1) + "_days_" + date + ".json";
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

        // get storage dir
        File folder = FileUtils.getTargetDir(mParentActivity.getString(R.string.custom_baseline_dir));
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
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // refresh view charts, text & controls
    public boolean refreshView() {

        //initializes the calendarview
        mCalendarView = initializeCalendar();

        if (mAlgorithm.equals(mParentActivity.getString(R.string.baseline_algo_none))) {
            // init chart title
            String date = ChartUtils.secsToDate(mFromTimeSecs, false);
            mTitle = mTitleStub + date;
            // show by month
            showRawUsage();
        }
        else {
            showBaselineUsage(mAlgorithm);
        }
        // show text
        showDetailsText(mResIdSize);
        // show controls
//        showControlsText();

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
            BaselineDetails bd = deriveBaselineDetails(mBaselineValues);
            // set details
            textView = (TextView) mRootView.findViewById(R.id.textViewTotalDay);
            textView.setTextAppearance(mParentActivity, resIdSize);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.total.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewAveDay);
            textView.setTextAppearance(mParentActivity, resIdSize);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.ave.toString());
            textView = (TextView) mRootView.findViewById(R.id.textViewPeakDay);
            textView.setTextAppearance(mParentActivity, resIdSize);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.peak.toString() + "(" + bd.peakHour.toString() + ")");
            textView = (TextView) mRootView.findViewById(R.id.textViewLowDay);
            textView.setTextAppearance(mParentActivity, resIdSize);
            textView.setVisibility(View.VISIBLE);
            textView.setText(bd.low.toString() + "(" + bd.lowHour.toString() + ")");
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
        }
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
        bd.ave = ((bd.total / baselineValues.size()))/1000;
        if (bd.ave.equals(0)) bd.ave = 1;
        bd.total = bd.total/1000;
        bd.peak = bd.peak / 1000;
        bd.low = bd.low / 1000;
        return bd;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showRawUsage() {
        String labelX = "Hour of Day";
        String labelY = "Watts";
        ChartRender.ChartType chartType = ChartRender.ChartType.AREA;

        ChartUtils.ChartControl chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_major),
                mParentActivity.getFeedManager(),
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
                mParentActivity.getFeedManager(),
                mTitle, labelX, labelY,
                mFromTimeSecs, mToTimeSecs, mStackLimit, chartType, true, mChartLayoutX, mChartLayoutY);

        loadLineChart(chartControl, algorithm);

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean loadLineChart(ChartUtils.ChartControl chartControl, String algorithm) {
        Log.v(TAG,"from time: " + chartControl.fromTimeSecs + " to time: " + chartControl.toTimeSecs);
        String value1Range = ChartUtils.secsToDate(chartControl.fromTimeSecs, true);
        String valueRange = ChartUtils.secsToDate(chartControl.toTimeSecs, true);

        // derive baseline values
        mBaselineValues = deriveBaselineValues(chartControl, algorithm);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private List<Integer> deriveBaselineValues(ChartUtils.ChartControl chartControl, String algorithm) {

        List<Integer> baseline = new ArrayList<>();

        if (!chartControl.feedManager.isValidFeed()) return baseline;

        List<Integer> values;
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
                    if (max.get(iv) < v ) max.set(iv, v);
                    // if value lowest, set as min
                    if (min.get(iv) > v ) min.set(iv, v);
                }
                else {
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
        // set baseline values based on analysis of intermediate sum, max, min
        if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_max))) {
            baseline = max;
        }
        else if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_min))) {
            baseline = min;
        }
        else if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_average)) ||
                ((algorithm.equals(mParentActivity.getString(R.string.baseline_algo_mid)) &&
                        mStackLimit < 3))) {
            for (Integer v : sum) {
                baseline.add(v/(mStackLimit+1));
            }
        }
        else if (algorithm.equals(mParentActivity.getString(R.string.baseline_algo_mid))) {
            int iv = 0;
            for (Integer v : sum) {
                v -= max.get(iv) + min.get(iv);
                baseline.add(v/((mStackLimit+1)-2));
                ++iv;
            }
        }
        return baseline;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
}
//////////////////////////////////////////////////////////////////////////////////////////
