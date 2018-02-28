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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adaptivehandyapps.ahasee.espi.FeedManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mat on 2/24/2015.
 */
public class TabSummarize {

    private static final String TAG = "TabSummarize";

    // parent activity & view references
//    private SeeActivity mContext;
    private Context mContext;
    private View mRootView = null;

    private FeedManager mFeedManager;

    private ProgressBar mProgress;
    private int mProgressStatus = 0;
    private TextView mProgressTextView;
    private TextView mTextViewPleaseRefresh;

    private Integer mFirstYear = 0;
    private Integer mCurrentYear = 0;
    private Integer mLastYear = 0;

    private Button mButtonNext;
    private Button mButtonPrev;

    private Button mButtonRefresh;
    private Button mButtonRemove;

    private Boolean mToastedRefreshGB = false;

    //////////////////////////////////////////////////////////////////////////////////////////
    // getters/setters/helpers
    public View getRootView() {
        return mRootView;
    }

    public Integer getFirstYear() {
        return mFirstYear;
    }
    public void setFirstYear(Integer firstYear) {
        PrefUtils.setPrefsFirstYear(mContext, firstYear);
        this.mFirstYear = firstYear;
    }

    public Integer getCurrentYear() {
        return mCurrentYear;
    }
    public void setCurrentYear(Integer currentYear) {
        PrefUtils.setPrefsCurrentYear(mContext, currentYear);
        this.mCurrentYear = currentYear;
    }

    public Integer getLastYear() {
        return mLastYear;
    }
    public void setLastYear(Integer lastYear) {
        PrefUtils.setPrefsLastYear(mContext, lastYear);
        this.mLastYear = lastYear;
    }

    private FeedManager getFeedManager() { return mFeedManager; }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // progress callback
    private FeedManager.OnProgressCallback mProgressCallback;

    private FeedManager.OnProgressCallback getProgressCallback() {
        // instantiate callback
        FeedManager.OnProgressCallback callback = new FeedManager.OnProgressCallback() {

            @Override
            public void onProgressCallback(int progress) {
                Log.d(TAG, "onProgressCallback progress " + progress);
                setProgressBar(progress);
            }
        };
        //  finish();
        return callback;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    public TabSummarize(Context c, LayoutInflater inflater, ViewGroup container) {

        Log.d(TAG, "inflating layout for RootView.");
        mContext = c;
        // get feed manager instance
        mProgressCallback = getProgressCallback();
        mFeedManager = FeedManager.getInstance(mContext, mProgressCallback);

        try {
            if ( mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mRootView = inflater.inflate(R.layout.frag_sum_land, container, false);
            }
//            else {
//                mRootView = inflater.inflate(R.layout.frag_sum_port, container, false);
//            }
            // init view elements
            initView();

        } catch (InflateException e) {
            Log.e(TAG, "inflater exception: " + e.getMessage());
            return;
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    private void initView() {

        mRootView.setBackgroundColor(mRootView.getResources().getColor(R.color.darkgreen));

        // set current, first, last year markers
        setYearMarkers();

        mButtonPrev = (Button) mRootView.findViewById(R.id.buttonPrevYear);
        mButtonPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setCurrentYear(getCurrentYear()-1);
                refreshView();
                // TODO: refresh details fragment
                BroadcastUtils.broadcastResult(mContext, BroadcastUtils.AHA_REFRESH, "back year");
            }
        });
        mButtonNext = (Button) mRootView.findViewById(R.id.buttonNextYear);
        mButtonNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setCurrentYear(getCurrentYear()+1);
                refreshView();
                // TODO: refresh details fragment
                BroadcastUtils.broadcastResult(mContext, BroadcastUtils.AHA_REFRESH, "next year");
            }
        });

        mButtonRefresh = (Button) mRootView.findViewById(R.id.buttonRefresh);
        mButtonRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // get selected feed paths
                List<String> pathList = getFeedPaths();
                // start read feed
                getFeedManager().readFeed(pathList);
                // refresh view charts, text & controls
                refreshView();
                // disable Please Refresh...
                mTextViewPleaseRefresh = (TextView) mRootView.findViewById(R.id.textViewPleaseRefresh);
                mTextViewPleaseRefresh.setVisibility(TextView.GONE);
                // show progress
                mProgress.setVisibility(View.VISIBLE);
            }
        });

        mButtonRemove = (Button) mRootView.findViewById(R.id.buttonRemove);
        mButtonRemove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start read feed
                getFeedManager().deleteFeed();
                // refresh view charts, text & controls
                refreshView();
                // enable Please Refresh...
                mTextViewPleaseRefresh = (TextView) mRootView.findViewById(R.id.textViewPleaseRefresh);
                mTextViewPleaseRefresh.setTextColor(mContext.getResources().getColor(R.color.red));
                mTextViewPleaseRefresh.setVisibility(TextView.VISIBLE);
            }
        });

        // if DB does not exist, prompt to refresh
        if (!getFeedManager().isValidFeed()) {
            // enable Please Refresh...
            mTextViewPleaseRefresh = (TextView) mRootView.findViewById(R.id.textViewPleaseRefresh);
            mTextViewPleaseRefresh.setVisibility(TextView.VISIBLE);
        }
//        // progress bar text
//        mProgressTextView = (TextView) mRootView.findViewById(R.id.progressTextView);
//        setProgressTextView("Feed Progress");
        // progress bar
        mProgress = (ProgressBar) mRootView.findViewById(R.id.progressBar);
        setProgressBar(mProgressStatus);

        // refresh view charts, text & controls
        refreshView();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private Boolean setYearMarkers() {
        String yearText;

        String from = PrefUtils.getPrefsFeedFromDate(mContext);
        String to = PrefUtils.getPrefsFeedToDate(mContext);
        Log.d(TAG, "setYearMarkers from, to " + from + ", " + to);
        if (!mToastedRefreshGB && from.equals(PrefUtils.PREFS_DEFAULT_FEEDFROMDATE) && to.equals(PrefUtils.PREFS_DEFAULT_FEEDTODATE)) {
            mToastedRefreshGB = true;
            Toast.makeText(mRootView.getContext(), "Please press Refresh GB to build your GreenButton database.", Toast.LENGTH_LONG).show();
        }
        // extract first/last year
        yearText = from.substring(from.lastIndexOf("-") + 1, from.length());
        setFirstYear(Integer.parseInt(yearText));
        yearText = to.substring(to.lastIndexOf("-") + 1, to.length());
        setLastYear(Integer.parseInt(yearText));
        Log.d(TAG, "setYearMarkers first, last " + getFirstYear() + ", " + getLastYear());

        Integer current = PrefUtils.getPrefsCurrentYear(mContext);
        setCurrentYear(current);
        // if current year out of range, reset to within range
        if (getCurrentYear() < getFirstYear() || getCurrentYear() > getLastYear()) {
            if (getLastYear()-getFirstYear() > 1) setCurrentYear(getFirstYear()+1);
        }
        Log.d(TAG, "setYearMarkers current " + getCurrentYear());

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // refresh view charts, text & controls
    public boolean refreshView() {
        // set year markers
        setYearMarkers();
        // enable next/prev buttons
        mButtonPrev.setEnabled(true);
        mButtonNext.setEnabled(true);
        // if current year at boundary, disable next/prev buttons
        if (getCurrentYear().equals(getFirstYear())) mButtonPrev.setEnabled(false);
        if (getCurrentYear().equals(getLastYear())) mButtonNext.setEnabled(false);

        // show by month
        showUsageByMonth();
        // show text
        showDetailsText();
        // show controls
        showFeedFilesSpinner();
        // remove progress bar
        mProgress.setVisibility(View.GONE);

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void setProgressBar(int progressStatus) {
        mProgressStatus = progressStatus;
        mProgress.setProgress(mProgressStatus);
        // Exception: Can't create handler inside thread that has not called Looper.prepare()
//        if (progressStatus == 100) refreshView();
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void setProgressTextView(String text) {
        mProgressTextView.setText(text);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // get selected feed paths
    private List<String> getFeedPaths() {
        // scan directory for list of feed files
        List<String> pathList = FileUtils.getFilesList(FileUtils.DOWNLOAD_DIR, true, FileUtils.FILE_EXTENSION_XML);
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
    private boolean showFeedFilesSpinner() {
        // scan directory for list of feed files
        List<String> pathList = FileUtils.getFilesList(FileUtils.DOWNLOAD_DIR, false, FileUtils.FILE_EXTENSION_XML);
        Log.v(TAG, "readFeed pathList size: " + pathList.size());

        if (pathList.size() > 0) {
            // add feed files to spinner
            Spinner spinner = (Spinner) mRootView.findViewById(R.id.spinnerFeed);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, android.R.id.text1);
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
            Toast.makeText(mRootView.getContext(), "Please copy GreenButton files to " + FileUtils.DOWNLOAD_DIR, Toast.LENGTH_SHORT).show();
        }
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showDetailsText() {
        TextView textView;
        // set location
        textView = (TextView) mRootView.findViewById(R.id.textViewLocation);
        textView.setText(SettingsActivity.getLocation(mContext));
        // set from to dates
        textView = (TextView) mRootView.findViewById(R.id.textViewFromToDate);
        textView.setText(PrefUtils.getPrefsFeedFromDate(mContext) + " to " + PrefUtils.getPrefsFeedToDate(mContext));
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showUsageByMonth() {
//        String title = "Monthly KwH " + FeedManager.YEAR_START.toString() + " through " + FeedManager.YEAR_END.toString();
        String title = "Monthly KwH " + getFirstYear() + " through " + getLastYear();
        String labelX = "Month";
        String labelY = "KwH";

        Boolean showDollars = PrefUtils.getPrefsShowDollars(mContext);

        if (showDollars) {
//            title = "Monthly $$$ " + FeedManager.YEAR_START.toString() + " through " + FeedManager.YEAR_END.toString();
            title = "Monthly $$$ " + getFirstYear() + " through " + getLastYear();
            labelY = "$$$";
        }

        Integer fromTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsFeedFromDate(mContext), true)/1000);
        Integer toTimeSecs = (int) (ChartUtils.dateToMillis(PrefUtils.getPrefsFeedToDate(mContext), true)/1000);

        int stackLimit = 1;
        ChartRender.ChartType chartType = ChartRender.ChartType.BAR;

        // TODO: extract to DevUtils?
        // determine chart dimensions
        int chartLayoutX = 0;
        int chartLayoutY = 0;
        if ( mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            chartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mContext)) * ChartUtils.CHART_LAND_X_PERCENT);
            chartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mContext)) * ChartUtils.CHART_LAND_Y_PERCENT);
        }
//        else {
//            chartLayoutX = (int)(((double)DevUtils.getDisplayWidthPixels(mContext)) * ChartUtils.CHART_PORT_X_PERCENT);
//            chartLayoutY = (int)(((double)DevUtils.getDisplayHeightPixels(mContext)) * ChartUtils.CHART_PORT_Y_PERCENT);
//        }
        Log.v(TAG, "chartLayout X, Y: " + chartLayoutX + ", " + chartLayoutY);

        ChartUtils.ChartControl chartControl = new ChartUtils.ChartControl(
                new ChartRender(mRootView, R.id.chart_major),
                getFeedManager(),
                title, labelX, labelY,
                fromTimeSecs, toTimeSecs, stackLimit, chartType, true, chartLayoutX, chartLayoutY);

        loadLineChart(chartControl, showDollars);

        return true;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean loadLineChart(ChartUtils.ChartControl chartControl, boolean showDollars) {

        Log.v(TAG,"from time: " + chartControl.fromTimeSecs + " to time: " + chartControl.toTimeSecs);
        // for each value set up to stack limit
//        for (Integer y = FeedManager.YEAR_START; y < FeedManager.YEAR_END+1; y++) {
//        for (Integer y = getFirstYear(); y < getLastYear()+1; y++) {
        for (Integer y = getCurrentYear()-1; y < getCurrentYear()+1; y++) {
            List<Integer> values = new ArrayList<>();
            long[] tally = PrefUtils.getPrefsTallyByMonth(mContext, y);
            Log.d(TAG, "loadLineChart gets year " + y + " with tally " + tallyToString(tally));
            // TODO: show year by month summary with no pad
            values.add((int)0);
            for (long month : tally) {
                if (showDollars) month = (long) PrefUtils.convertKwhToDollars((int) month);
//                if (month == 0) month = 1;
                values.add((int)month);
            }
            // TODO: show year by month summary with no pad
            values.add((int)0);

            String valueRange = y.toString();
            // add series
            chartControl.chartRender.addSeriesLabels(valueRange);
            chartControl.chartRender.addSeries(values);
//            // advance from to
//            from = to;
//            to = to + (timeRange);
        }
        // label chart
        chartControl.chartRender.setChartTitle(chartControl.chartTitle);
        chartControl.chartRender.setChartAxisLabelX(chartControl.chartAxisLabelX);
        chartControl.chartRender.setChartAxisLabelY(chartControl.chartAxisLabelY);
        chartControl.chartRender.setXTextLabels(PrefUtils.getPrefsTextMonths(mContext));

        // show full chart
        chartControl.chartRender.showSeries(chartControl.showLabels, chartControl.chartType, chartControl.chartLayoutX, chartControl.chartLayoutY);

        return true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    private String tallyToString(long[] tallyYear) {
        String tallyString = "";
        for (int i = 0; i < tallyYear.length; i++) {
            tallyString = tallyString.concat(Long.toString(tallyYear[i])) + " ";
        }
        return tallyString;
    }
}
//////////////////////////////////////////////////////////////////////////////////////////
