// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: FEB 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mat on 2/24/2015.
 */
public class TabSummarize {

    private static final String TAG = "TabSummarize";

    // parent activity & view references
    private SeeActivity mParentActivity;
    private View mRootView = null;

    private ProgressBar mProgress;
    private int mProgressStatus = 0;
    private TextView mProgressTextView;
    private TextView mTextViewPleaseRefresh;

    private Button mButtonRefresh;
    private Button mButtonRemove;

    private int mResIdSize = R.style.TextAppearance_AppCompat_Medium;

    //////////////////////////////////////////////////////////////////////////////////////////

    public TabSummarize(SeeActivity parentActivity, LayoutInflater inflater, ViewGroup container) {

        mParentActivity = parentActivity;
        try {
            if ( mParentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mRootView = inflater.inflate(R.layout.frag_sum_land, container, false);
            }
            else {
                mRootView = inflater.inflate(R.layout.frag_sum_port, container, false);
            }

            mResIdSize = DevUtils.getDevTextSize(mParentActivity);
            initView(mResIdSize);

        } catch (InflateException e) {
            Log.e(TAG, "inflater exception: " + e.getMessage());
            return;
        }
    }

    public void initView(final int resIdSize) {

        mRootView.setBackgroundColor(mRootView.getResources().getColor(R.color.darkgreen));

        mButtonRefresh = (Button) mRootView.findViewById(R.id.buttonRefresh);
        mButtonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // get selected feed paths
                List<String> pathList = getFeedPaths();
                // start read feed
                mParentActivity.getFeedManager().readFeed(pathList);
                // refresh view charts, text & controls
                refreshView();
                // disable Please Refresh...
                mTextViewPleaseRefresh = (TextView) mRootView.findViewById(R.id.textViewPleaseRefresh);
                mTextViewPleaseRefresh.setTextAppearance(mParentActivity, resIdSize );
                mTextViewPleaseRefresh.setVisibility(TextView.GONE);
                // show progress
                mProgress.setVisibility(View.VISIBLE);
            }
        });

        mButtonRemove = (Button) mRootView.findViewById(R.id.buttonRemove);
        mButtonRemove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start read feed
                mParentActivity.getFeedManager().deleteFeed();
                // refresh view charts, text & controls
                refreshView();
                // enable Please Refresh...
                mTextViewPleaseRefresh = (TextView) mRootView.findViewById(R.id.textViewPleaseRefresh);
                mTextViewPleaseRefresh.setTextAppearance(mParentActivity, resIdSize );
                mTextViewPleaseRefresh.setTextColor(mParentActivity.getResources().getColor(R.color.red));
                mTextViewPleaseRefresh.setVisibility(TextView.VISIBLE);
            }
        });

        // initialize Radio Group and attach click handler
        RadioGroup radioGroup = (RadioGroup) mRootView.findViewById(R.id.radioKwhDollar);
        // default check to kwh then check settings & check dollars if true
        radioGroup.check(R.id.radioKwh);
        if (PrefUtils.getPrefsShowDollars(mParentActivity)) {
            radioGroup.check(R.id.radioDollar);
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioDollar:
                        Toast.makeText(mRootView.getContext(), "Show $$$", Toast.LENGTH_SHORT).show();
                        PrefUtils.setPrefsShowDollars(mParentActivity, true);
                        // notify listeners
                        BroadcastUtils.broadcastResult(mParentActivity, BroadcastUtils.AHA_REFRESH, "Show Dollars");
                        break;
                    case R.id.radioKwh:
                        Toast.makeText(mRootView.getContext(), "Show KwH", Toast.LENGTH_SHORT).show();
                        PrefUtils.setPrefsShowDollars(mParentActivity, false);
                        // notify listeners
                        BroadcastUtils.broadcastResult(mParentActivity, BroadcastUtils.AHA_REFRESH, "Show Kwh");
                        break;
                }
                // refresh view charts, text & controls
//                refreshView();
            }
        });

        // if DB does not exist, prompt to refresh
        if (!mParentActivity.getFeedManager().isValidFeed()) {
            // enable Please Refresh...
            mTextViewPleaseRefresh = (TextView) mRootView.findViewById(R.id.textViewPleaseRefresh);
            mTextViewPleaseRefresh.setTextAppearance(mParentActivity, resIdSize);
            mTextViewPleaseRefresh.setVisibility(TextView.VISIBLE);
        }
        // progress bar text
        mProgressTextView = (TextView) mRootView.findViewById(R.id.progressTextView);
        mProgressTextView.setTextAppearance(mParentActivity, resIdSize);
        setProgressTextView("Feed Progress");
        // progress bar
        mProgress = (ProgressBar) mRootView.findViewById(R.id.progressBar);
        setProgressBar(mProgressStatus);

        // refresh view charts, text & controls
        refreshView();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // getters/setters/helpers
    public View getRootView() {
        return mRootView;
    }

    // refresh view charts, text & controls
    public boolean refreshView() {
        // show by month
        showUsageByMonth();
        // show text
        showDetailsText(mResIdSize);
        // show controls
        showControlsText();
        // remove progress bar
        mProgress.setVisibility(View.GONE);

        return true;
    }
    public void setProgressBar(int progressStatus) {
        mProgressStatus = progressStatus;
        mProgress.setProgress(mProgressStatus);
        // Exception: Can't create handler inside thread that has not called Looper.prepare()
//        if (progressStatus == 100) refreshView();
    }

    public void setProgressTextView(String text) {
        mProgressTextView.setText(text);
    }

    // get selected feed paths
    private List<String> getFeedPaths() {
        // scan directory for list of feed files
        List<String> pathList = FileUtils.getFilesList(mParentActivity.getString(R.string.espi_feed_dir), true);
        Log.v(TAG, "readFeed pathList size: " + pathList.size());

        List<String> selectionList = new ArrayList<>();
        if (pathList.size() > 0) {
            // add feed files to spinner
            Spinner spinner = (Spinner) mRootView.findViewById(R.id.spinnerFeed);
            int position = spinner.getSelectedItemPosition();
            // if all selected
            if (position == 0) {
                selectionList = pathList;
            }
            else {
                selectionList.add(pathList.get(position-1));
            }
        }
        return selectionList;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showControlsText() {
        // scan directory for list of feed files
        List<String> pathList = FileUtils.getFilesList(mParentActivity.getString(R.string.espi_feed_dir), false);
        Log.v(TAG, "readFeed pathList size: " + pathList.size());

        if (pathList.size() > 0) {
            // add feed files to spinner
            Spinner spinner = (Spinner) mRootView.findViewById(R.id.spinnerFeed);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mParentActivity, android.R.layout.simple_spinner_item, android.R.id.text1);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);
            spinnerAdapter.add("All");
            for (int i = 0; i < pathList.size(); i++) {
//                Log.v(TAG, "Feed pathList(" + i + "): " + pathList.get(i));
                spinnerAdapter.add(pathList.get(i));
            }
            spinnerAdapter.notifyDataSetChanged();
        }
        else {
            Toast.makeText(mRootView.getContext(), "Please copy files to " + mParentActivity.getString(R.string.espi_feed_dir), Toast.LENGTH_SHORT).show();
        }
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showDetailsText(int resIdSize) {
        TextView textView;
        long[] tally;
//        // set labels
//        textView = (TextView) mRootView.findViewById(R.id.textViewTitle1);
//        textView.setTextAppearance(mParentActivity, resIdSize);
//        textView = (TextView) mRootView.findViewById(R.id.textViewTitle3);
//        textView.setTextAppearance(mParentActivity, resIdSize);
        // set location
        textView = (TextView) mRootView.findViewById(R.id.textViewLocation);
        textView.setTextAppearance(mParentActivity, resIdSize);
        textView.setText(SettingsActivity.getLocation(mParentActivity));
        // set from to dates
        textView = (TextView) mRootView.findViewById(R.id.textViewFromToDate);
        textView.setTextAppearance(mParentActivity, resIdSize );
        textView.setText(PrefUtils.getPrefsFeedFromDate(mParentActivity) + " to " + PrefUtils.getPrefsFeedToDate(mParentActivity));
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showUsageByMonth() {
        String title = "Monthly KwH 2014 through 2015";
        String labelX = "Month";
        String labelY = "KwH";

        Boolean showDollars = PrefUtils.getPrefsShowDollars(mParentActivity);

        if (showDollars) {
            title = "Monthly $$$ 2014 through 2015";
            labelY = "$$$";
        }

        Integer fromTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsFeedFromDate(mParentActivity), true)/1000);
        Integer toTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsFeedToDate(mParentActivity), true)/1000);

        int stackLimit = 1;
        ChartRender.ChartType chartType = ChartRender.ChartType.BAR;

        // TODO: extract to DevUtils?
        // determine chart dimensions
        int chartLayoutX;
        int chartLayoutY;
        if ( mParentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            chartLayoutX = 832;
//            chartLayoutY = 512;
//            chartLayoutX = (int)(((double)screenWidthDp) * .65);
//            chartLayoutY = (int)(((double)screenHeightDp) * .65);
            chartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mParentActivity)) * .65);
            chartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mParentActivity)) * .65);
        }
        else {
//            chartLayoutX = 772;
//            chartLayoutY = 512;
//            chartLayoutX = (int)(((double)screenWidthDp) * .96);
//            chartLayoutY = (int)(((double)screenHeightDp) * .40);
            chartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mParentActivity)) * .96);
            chartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mParentActivity)) * .40);
        }
        Log.v(TAG, "chartLayout X, Y: " + chartLayoutX + ", " + chartLayoutY);

        ChartUtils.ChartControl chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_major),
                mParentActivity.getFeedManager(),
                title, labelX, labelY,
                fromTimeSecs, toTimeSecs, stackLimit, chartType, true, chartLayoutX, chartLayoutY);

        loadLineChart(chartControl, showDollars);

        return true;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean loadLineChart(ChartUtils.ChartControl chartControl, boolean showDollars) {

        // init range lablels
        Integer from = chartControl.fromTimeSecs;
        Integer to = chartControl.toTimeSecs;
        Integer timeRange = to - from;

        Log.v(TAG,"from time: " + from + "to time: " + to + ", range " + timeRange);
        // for each value set up to stack limit
        for (Integer y = 2014; y < 2016; y++) {
            List<Integer> values = new ArrayList<>();
            long[] tally = PrefUtils.getPrefsOverviewByMonth(mParentActivity, y);
            int i = 0;
            for (long month : tally) {
                if (showDollars) month = (long) PrefUtils.convertKwhToDollars((int) month);
                values.add((int)month);
//                Log.v(TAG, y.toString() + " month(" + i++ + ") tally: " + month);
            }

            String valueRange = y.toString();
            // add series
            chartControl.chartRender.addSeriesLabels(valueRange);
            chartControl.chartRender.addSeries(values);
            // advance from to
            from = to;
            to = to + (timeRange);
        }
        // label chart
        chartControl.chartRender.setChartTitle(chartControl.chartTitle);
        chartControl.chartRender.setChartAxisLabelX(chartControl.chartAxisLabelX);
        chartControl.chartRender.setChartAxisLabelY(chartControl.chartAxisLabelY);
        chartControl.chartRender.setXTextLabels(PrefUtils.getPrefsTextMonths(mParentActivity));

        // show full chart
        chartControl.chartRender.showSeries(chartControl.showLabels, chartControl.chartType, chartControl.chartLayoutX, chartControl.chartLayoutY);

        return true;
    }
}
//////////////////////////////////////////////////////////////////////////////////////////
