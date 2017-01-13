// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: FEB 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.adaptivehandyapps.ahasee.espi.FeedManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by mat on 2/24/2015.
 */
public class TabAlert {

    private static final String TAG = "TabAlert";

    // parent activity & view references
    private SeeActivity mParentActivity;
    private View mRootView;

    // controls
    private final Button mButtonNext;
    private final Button mButtonPrev;
    private final SeekBar  mSeekBarSensitivity;
    private CalendarView mCalendarView;

    private List<ChartUtils.ChartControl> mChartControlList;
//    private int mChartListIndex;

    private int mTypicalAwayChartListIndex;
    private int mTypicalHomeChartListIndex;

//    private ChartRender.ChartType mChartType;

    private enum AlertType {
        EMPTY_MATCH (0),
        EMPTY_MISMATCH (1),
        OCCUPIED_MATCH (2),
        OCCUPIED_MISMATCH (3);

        private int _value;
        AlertType(int Value) { this._value = Value; }
        public int getValue() { return _value; }
    }
    private AlertType mAlertType = AlertType.EMPTY_MATCH;

    private int mDiffTimeSecs;

    //////////////////////////////////////////////////////////////////////////////////////////
    public TabAlert(SeeActivity parentActivity, LayoutInflater inflater, ViewGroup container) {

        mParentActivity = parentActivity;
        View rootView = inflater.inflate(R.layout.fragment_alert, container, false);
        mRootView = rootView;

        mRootView.setBackgroundColor(Color.RED);

        // init chart controls (typical empty & occupied)
        String chartTypeText = PrefUtils.getPrefsChartType(mParentActivity);
        Log.v(TAG, "showFeed chart type text = " + chartTypeText);
        ChartRender.ChartType mChartType = PrefUtils.getChartType(mParentActivity, chartTypeText);
        initChartControlList(mChartType);

        // show view controls
        showViewControls();

        // initialize diff time
        mDiffTimeSecs = (int) (ChartUtils.dateToMillis("02-05-2015", true) / 1000);

        // show alert
        showAlert();

        // initialize Radio Group and attach click handler
        RadioGroup radioGroup = (RadioGroup) mRootView.findViewById(R.id.radioAlert);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mAlertType = AlertType.EMPTY_MATCH;
                switch (checkedId) {
                    case R.id.radioAwayMatch:
//                        Toast.makeText(mRootView.getContext(), "Show Matching Empty", Toast.LENGTH_SHORT).show();
                        mAlertType = AlertType.EMPTY_MATCH;
                        break;
                    case R.id.radioAwayMisMatch:
//                        Toast.makeText(mRootView.getContext(), "Show Mis-Matching Empty", Toast.LENGTH_SHORT).show();
                        mAlertType = AlertType.EMPTY_MISMATCH;
                        break;
                    case R.id.radioHomeMatch:
//                        Toast.makeText(mRootView.getContext(), "Show Matching Occupied", Toast.LENGTH_SHORT).show();
                        mAlertType = AlertType.OCCUPIED_MATCH;
                        break;
                    case R.id.radioHomeMisMatch:
//                        Toast.makeText(mRootView.getContext(), "Show Mis-Matching Occupied", Toast.LENGTH_SHORT).show();
                        mAlertType = AlertType.OCCUPIED_MISMATCH;
                        break;
                }
                // show alert
                showAlert();
            }
        });

        mButtonNext = (Button) mRootView.findViewById(R.id.buttonStart);
//        mButtonNext.setBackgroundColor(Color.GRAY);
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(mRootView.getContext(), R.string.view_next_button_text, Toast.LENGTH_SHORT).show();
                // bump diff time to next day
                mDiffTimeSecs += FeedManager.SECS_IN_DAY;
                Integer timeSecs = mDiffTimeSecs;
                long ms = timeSecs.longValue() * 1000;
                mCalendarView.setDate(ms);

                // show alert
                showAlert();
            }
        });

        mButtonPrev = (Button) mRootView.findViewById(R.id.buttonStop);
        mButtonPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(mRootView.getContext(), R.string.view_prev_button_text, Toast.LENGTH_SHORT).show();
                // bump diff time to prev day
                mDiffTimeSecs -= FeedManager.SECS_IN_DAY;
                Integer timeSecs = mDiffTimeSecs;
                long ms = timeSecs.longValue() * 1000;
                mCalendarView.setDate(ms);
                // show alert
                showAlert();
            }
        });

        mSeekBarSensitivity = (SeekBar) mRootView.findViewById(R.id.seekBarSensitivity);
        mSeekBarSensitivity.setProgress(75);
        mSeekBarSensitivity.setBackgroundColor(Color.DKGRAY);

        //initializes the calendarview
        mCalendarView = initializeCalendar();

    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // getters/setters/helpers
    public View getRootView() {
        return mRootView;
    }
    private int getTypicalChartIndex(AlertType alertType) {
        switch (alertType) {
            case EMPTY_MATCH:
            case EMPTY_MISMATCH:
                return mTypicalAwayChartListIndex;
            case OCCUPIED_MATCH:
            case OCCUPIED_MISMATCH:
                return mTypicalHomeChartListIndex;
        }
        return mTypicalAwayChartListIndex;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean initChartControlList(ChartRender.ChartType chartType) {
        String title;
        String labelX = "Hour of Day";
        String labelY = "Watts";
        int fromTimeSecs;
        int toTimeSecs;
        int stackLimit;
        ChartUtils.ChartControl chartControl;
        // create chart control list
        mChartControlList = new ArrayList<>();

        // add empty house leaving & coming
//        title = "Alert for Empty Watts";
        title = mParentActivity.getString(R.string.alert_away_title);
        fromTimeSecs = (int) (ChartUtils.dateToMillis("02-05-2015", true) / 1000);
        toTimeSecs = (int) (ChartUtils.dateToMillis("03-05-2015", true) / 1000);

        stackLimit = 0;

        chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_layout),
                mParentActivity.getFeedManager(),
                title, labelX, labelY,
                fromTimeSecs, toTimeSecs, stackLimit, chartType, true, 832, 512);
        mTypicalAwayChartListIndex = mChartControlList.size();
        mChartControlList.add(chartControl);

        // typical weekday house
//        title = "Alert for Occupied Watts";
        title = mParentActivity.getString(R.string.alert_home_title);
        fromTimeSecs = (int) (ChartUtils.dateToMillis("21-04-2015", true) / 1000);
        toTimeSecs = (int) (ChartUtils.dateToMillis("22-04-2015", true) / 1000);

        stackLimit = 0;

        chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_layout),
                mParentActivity.getFeedManager(),
                title, labelX, labelY,
                fromTimeSecs, toTimeSecs, stackLimit, chartType, true, 832, 512);
        mTypicalHomeChartListIndex = mChartControlList.size();
        mChartControlList.add(chartControl);

        return true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    private CalendarView initializeCalendar() {
        CalendarView calendarView = (CalendarView) mRootView.findViewById(R.id.calendarView);

        try {
            calendarView.setMinDate(ChartUtils.dateToMillis(PrefUtils.getPrefsFeedFromDate(mParentActivity), true));
            calendarView.setMaxDate(ChartUtils.dateToMillis(PrefUtils.getPrefsFeedToDate(mParentActivity), true));
            calendarView.setDate(ChartUtils.dateToMillis(PrefUtils.getPrefsFeedToDate(mParentActivity), true));
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
                Log.v(TAG, "onSelectedDayChange timeMS:" + selectedTimeMs);
//                // TODO: adjust for GMT-4, GMT-5
//                selectedTimeMs -= (4 * FeedManager.SECS_IN_HOUR * 1000) ;
                selectedTimeMs = ChartUtils.shiftUtcMillisToEst(selectedTimeMs);
//                Log.v(TAG, "onSelectedDayChange GMT-4 timeMS:" + selectedTimeMs);
                // set diff time to calendar selection
                mDiffTimeSecs = (int) (selectedTimeMs / 1000);
                Log.v(TAG, "onSelectedDayChange mDiffTimeSecs:" + mDiffTimeSecs);
                showAlert();
            }
        });
        return calendarView;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showAlert() {

        ChartRender.ChartType chartType = ChartRender.ChartType.AREA;

        int chartListIndex = getTypicalChartIndex(mAlertType);
        // copy current typical empty or occupied to new chart control
        ChartUtils.ChartControl chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_layout),
                mParentActivity.getFeedManager(),
                mChartControlList.get(chartListIndex).chartTitle,
                mChartControlList.get(chartListIndex).chartAxisLabelX,
                mChartControlList.get(chartListIndex).chartAxisLabelY,
                mChartControlList.get(chartListIndex).fromTimeSecs,
                mChartControlList.get(chartListIndex).toTimeSecs,
                mChartControlList.get(chartListIndex).stackLimit,
                mChartControlList.get(chartListIndex).chartType,
                mChartControlList.get(chartListIndex).showLabels,
                832, 512);
//            mChartControlList.get(chartListIndex);

        if (chartType != ChartRender.ChartType.DIFF) {
            // create base typical series
            ChartUtils.showFeed(chartControl, true);
            // add current selection
//        chartControl.mFromTimeSecs = (int) (ChartUtils.dateToMillis("21-04-2015") / 1000);
//        chartControl.mToTimeSecs = (int) (ChartUtils.dateToMillis("22-04-2015") / 1000);
            chartControl.fromTimeSecs = mDiffTimeSecs;
            chartControl.toTimeSecs = mDiffTimeSecs + FeedManager.SECS_IN_DAY;
            ChartUtils.showFeed(chartControl, true);
        }
        else {
//
//            ChartRender chartRender = new ChartRender(mRootView, R.id.chart_layout, 0);
            loadLineChart(chartControl);
            chartControl.chartRender.showSeries(false, chartType, chartControl.chartLayoutX, chartControl.chartLayoutY);
        }
        return true;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean loadLineChart(ChartUtils.ChartControl chartControl) {
        List<Integer> values;
        // init range lablels
        Integer from = chartControl.fromTimeSecs;
        Integer to = chartControl.toTimeSecs;
        Integer timeRange = to - from;

        Log.v(TAG, "from time: " + from + "to time: " + to + ", range " + timeRange);

        String valueRange = "";
        // for each value set up to stack limit
        for (int i = 0; i <= chartControl.stackLimit + 1; i++) {
            // get selected date interval readings
            if (chartControl.feedManager.isValidFeed()) {
                values = chartControl.feedManager.queryWhValues(from, to);
            }
            else {
//                values = chartControl.feedManager.getIntWhValues(from, to);
                return false;
            }
            valueRange = ChartUtils.secsToDate(from, false);
            // add series
            chartControl.chartRender.addSeriesLabels(valueRange);
            chartControl.chartRender.addSeries(values);
            // advance from to
            from = mDiffTimeSecs;
            to = mDiffTimeSecs + (timeRange);
        }
        // label chart
        chartControl.chartRender.setChartTitle(chartControl.chartTitle + " for " + ChartUtils.secsToDate(mDiffTimeSecs, false));
        chartControl.chartRender.setChartAxisLabelX(chartControl.chartAxisLabelX);
        chartControl.chartRender.setChartAxisLabelY(chartControl.chartAxisLabelY);

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showViewControls() {
        TextView textView;
        // set from to dates
        textView = (TextView) mRootView.findViewById(R.id.textViewFromDate);
        textView.setText(PrefUtils.getPrefsFeedFromDate(mParentActivity));
        textView = (TextView) mRootView.findViewById(R.id.textViewToDate);
        textView.setText(PrefUtils.getPrefsFeedToDate(mParentActivity));

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
//////////////////////////////////////////////////////////////////////////////////////////
