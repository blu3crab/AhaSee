// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: SEP 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by mat on 9/2/2015.
 */
public class TabSummDetailFragment extends Fragment {
    private final static String TAG = "TabSummDetailFragment";

    private List<String> textMonths = new ArrayList<>(
            Arrays.asList("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"));

    private View mRootView;

    private BroadcastReceiver mBroadcastReceiver;

    ///////////////////////////////////////////////////////////////////////////////////////////
    public TabSummDetailFragment() {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        mRootView = inflater.inflate(R.layout.frag_sum_detail, container, false);

        try {
            mRootView = inflater.inflate(R.layout.frag_sum_detail, container, false);
            if ( getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // set L1 vertical
                LinearLayout ll = (LinearLayout) mRootView.findViewById(R.id.frag_sum_detail);
                ll.setOrientation(LinearLayout.VERTICAL);
            }
            else {
                // set L1 horizontal
                LinearLayout ll = (LinearLayout) mRootView.findViewById(R.id.frag_sum_detail);
                ll.setOrientation(LinearLayout.HORIZONTAL);
            }

        } catch (InflateException e) {
            Log.e(TAG, "inflater exception: " + e.getMessage());
            return null;
        }

        refreshView();

        // create broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(BroadcastUtils.AHA_MESSAGE);
                Log.v(TAG, "BroadcastReceiver: " + s);
                refreshView();
            }
        };
        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver((mBroadcastReceiver),
                new IntentFilter(BroadcastUtils.AHA_REFRESH)
        );
    }
    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean refreshView() {
        int resIdSize = DevUtils.getDevTextSize(getActivity());
        showDetailsText(resIdSize);
        showUsageByTOD();
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showDetailsText(int resIdSize) {
        TextView textView;

        Boolean showDollars = PrefUtils.getPrefsShowDollars(getActivity());

        // update labels for KwH or dollars
        textView = (TextView) mRootView.findViewById(R.id.textViewTotalYearLabel);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(R.string.total_year_label_kwh);
        if (showDollars) textView.setText(R.string.total_year_label_dollar);

        textView = (TextView) mRootView.findViewById(R.id.textViewRollingYearLabel);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(R.string.rolling_year_label_kwh);
        if (showDollars) textView.setText(R.string.rolling_year_label_dollar);

        textView = (TextView) mRootView.findViewById(R.id.textViewTotalAveMoLabel);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(R.string.total_avemo_label_kwh);
        if (showDollars) textView.setText(R.string.total_avemo_label_dollar);
        textView = (TextView) mRootView.findViewById(R.id.textViewRollingAveMoLabel);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(R.string.rolling_avemo_label_kwh);
        if (showDollars) textView.setText(R.string.rolling_avemo_label_dollar);

        textView = (TextView) mRootView.findViewById(R.id.textViewRollingPeakMoLabel);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(R.string.rolling_peakmo_label_kwh);
        if (showDollars) textView.setText(R.string.rolling_peakmo_label_dollar);
        textView = (TextView) mRootView.findViewById(R.id.textViewRollingLowMoLabel);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(R.string.rolling_lowmo_label_kwh);
        if (showDollars) textView.setText(R.string.rolling_lowmo_label_dollar);

        textView = (TextView) mRootView.findViewById(R.id.textViewLastMoLabel);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(R.string.lastmo_label_kwh);
        if (showDollars) textView.setText(R.string.lastmo_label_dollar);

        // total previous year
        long[] tally2014 = PrefUtils.getPrefsOverviewByMonth(getActivity(), 2014);
        Long sumPrevYear = 0L;
        for (long month : tally2014) {
            if (showDollars) month = (long) PrefUtils.convertKwhToDollars((int) month);
            sumPrevYear += month;
        }
        // total current year of rolling 12 months
        long[] tally2015 = PrefUtils.getPrefsOverviewByMonth(getActivity(), 2015);
        Long sumRolling = 0L;
        Long peakRolling = 0L;
        int peakMonthInx = 0;
        Long lowRolling = 1000000L;
        int lowMonthInx = 0;
        int lastMonthInx = 0;
        int monthCount = 0;
        for (long month : tally2015) {
            // sum all non-zero months in 2015
            if (month > 0) {
                if (showDollars) month = (long) PrefUtils.convertKwhToDollars((int) month);

                sumRolling += month;
                lastMonthInx  = monthCount;
                if (month > peakRolling) {
                    peakRolling = month;
                    peakMonthInx = monthCount;
                }
                if (month < lowRolling) {
                    lowRolling = month;
                    lowMonthInx = monthCount;
                }
            }
            ++monthCount;
        }
        // determine last month delta from last year
        long deltaLastMonth = tally2014[lastMonthInx] - tally2015[lastMonthInx];
        if (showDollars) deltaLastMonth = (long) PrefUtils.convertKwhToDollars((int) deltaLastMonth);
        // sum remainder from last portion of 2014 to rolling stats
        for (int i = lastMonthInx+1; i < tally2014.length; i++) {
            if (showDollars) tally2014[i] = (long) PrefUtils.convertKwhToDollars((int) tally2014[i]);

            sumRolling += tally2014[i];
            if (tally2014[i] > peakRolling) {
                peakRolling = tally2014[i];
                peakMonthInx = i;
            }
            if (tally2014[i] < lowRolling) {
                lowRolling = tally2014[i];
                lowMonthInx = i;
            }
        }

        // update details text fields
        Integer aveYear = (int) (sumPrevYear / 12);
        textView = (TextView) mRootView.findViewById(R.id.textViewTotalYear);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(sumPrevYear.toString());
        textView = (TextView) mRootView.findViewById(R.id.textViewTotalAveMo);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(aveYear.toString());
        Integer aveRolling = (int) (sumRolling / 12);
        Integer lastMonth = (int) (deltaLastMonth);
        int color = getActivity().getResources().getColor(R.color.red);
        if (sumPrevYear > sumRolling) {
            color = getActivity().getResources().getColor(R.color.green);
        }
        textView = (TextView) mRootView.findViewById(R.id.textViewRollingYear);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(sumRolling.toString());
        textView.setTextAppearance(getActivity(), resIdSize);
        textView.setTextColor(color);
        textView = (TextView) mRootView.findViewById(R.id.textViewRollingAveMo);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(aveRolling.toString());
        textView.setTextColor(color);
        textView = (TextView) mRootView.findViewById(R.id.textViewRollingPeakMo);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(peakRolling.toString() + " (" + textMonths.get(peakMonthInx) + ")");
        textView = (TextView) mRootView.findViewById(R.id.textViewRollingLowMo);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setText(lowRolling.toString() + " (" + textMonths.get(lowMonthInx) + ")");
        // if delta last month with prev year month is positive turn green, else leave red
        color = getActivity().getResources().getColor(R.color.red);
        if (lastMonth > 0) {
            color = getActivity().getResources().getColor(R.color.green);
        }
        textView = (TextView) mRootView.findViewById(R.id.textViewLastMo);
        textView.setTextAppearance(getActivity(), resIdSize );
        textView.setTextColor(color);
        textView.setText(lastMonth.toString() + " (" + textMonths.get(lastMonthInx) + ")");

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showUsageByTOD() {

        final int CHART_LAYOUT_X = 256;
        final int CHART_LAYOUT_Y = 256;

        final int TOD_PERIOD_COUNT = 6;

        String title = "Usage by Time-of-Day";

        List<String> mColorStrings = new ArrayList<String>(
                Arrays.asList(
                        "#FF202099",    // dark blue
                        "#FF802080",    // purple
                        "#FFaaaa00",    // dark yellow
                        "#FFf2f200",    // bright yellow
                        "#FFf29900",    // redish
                        "#FF2020FF"     // light blue
                ));

        // Pie Chart Section Names
        String[] sliceName = new String[] {
                "12-4", "4-8", "8-12", "12-4", "4-8", "8-12"
        };
        // Pie Chart Section Value - equal across all sections
        double[] distribution = {
                16.666, 16.666, 16.666, 16.666, 16.666, 16.666
        } ;
        // init total consumption & total for each hour
        long totalKwH = 0;
        long[] values = PrefUtils.getPrefsOverviewByTod(getActivity());

        // total for all values for each hour
        for (int period = 0; period < TOD_PERIOD_COUNT; period++) {
            totalKwH += values[period];
        }
        // calculate distribution
        for (int period = 0; period < TOD_PERIOD_COUNT; period++) {
            distribution[period] = ((double) values[period]) / (double) totalKwH;
        }

        // distribution: Instantiating CategorySeries to plot Pie Chart
        CategorySeries distributionSeries = new CategorySeries(title);
        for(int i=0 ;i < distribution.length;i++){
            // Adding a slice with its values and name to the Pie Chart
            distributionSeries.add(sliceName[i], distribution[i]);
        }

        // colors: Instantiating a renderer for the Pie Chart
        DefaultRenderer defaultRenderer  = new DefaultRenderer();
        for(int i = 0 ; i < distribution.length; i++){
            SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
            seriesRenderer.setColor(Color.parseColor(mColorStrings.get(i)));
            // Adding a renderer for a slice
            defaultRenderer.addSeriesRenderer(seriesRenderer);
        }
        defaultRenderer.setStartAngle(270);
        defaultRenderer.setChartTitle(title);
        defaultRenderer.setChartTitleTextSize(20);
        defaultRenderer.setZoomButtonsVisible(true);

        // gather screen dimensions
        Configuration configuration = getActivity().getResources().getConfiguration();
        int screenWidthDp = configuration.screenWidthDp;
        int screenHeightDp = configuration.screenHeightDp;
        Log.v(TAG, "screen w/h dp: " + screenWidthDp + ", " + screenHeightDp);
//        // determine chart dimensions
//        int chartLayoutX;
//        int chartLayoutY;
//        if ( getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
////            chartLayoutX = 256;
////            chartLayoutY = 216;
//            chartLayoutX = (int)(((double)screenWidthDp) * .2);
//            Log.v(TAG, "chartLayoutX: " + chartLayoutX);
//            chartLayoutY = (int)(((double)screenHeightDp) * .27);
//            Log.v(TAG, "chartLayoutY: " + chartLayoutY);
//        }
//        else {
//            chartLayoutX = (int)(((double)screenWidthDp) * .32);
//            Log.v(TAG, "chartLayoutX: " + chartLayoutX);
//            chartLayoutY = (int)(((double)screenHeightDp) * .17);
//            Log.v(TAG, "chartLayoutY: " + chartLayoutY);
//        }
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int pixelWidth = displayMetrics.widthPixels;
        int pixelHeight = displayMetrics.heightPixels;
        float density = displayMetrics.density;
        Log.v(TAG, "screen w/h pixel - density: " + pixelWidth + ", " + pixelHeight + " - " + density);
        // determine chart dimensions
        int chartLayoutX;
        int chartLayoutY;
        if ( getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            chartLayoutX = (int)(((double)pixelWidth) * .2);
            chartLayoutY = (int)(((double)pixelHeight) * .27);
        }
        else {
            chartLayoutX = (int)(((double)pixelWidth) * .32);
            chartLayoutY = (int)(((double)pixelHeight) * .17);
        }
        Log.v(TAG, "chartLayout X, Y: " + chartLayoutX + ", " + chartLayoutY);

        LinearLayout mChartLayout = (LinearLayout) mRootView.findViewById(R.id.chart_minor);

        // get the chart view & add to layout
        View chartView = ChartFactory.getPieChartView(mRootView.getContext(), distributionSeries, defaultRenderer);
        mChartLayout.removeAllViews();
//        mChartLayout.addView(chartView, new LinearLayout.LayoutParams(CHART_LAYOUT_X, CHART_LAYOUT_Y));
        mChartLayout.addView(chartView, chartLayoutX, chartLayoutY);


        return true;
    }
}
///////////////////////////////////////////////////////////////////////////////////////////////
