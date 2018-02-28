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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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
public class TabSumDetailFragment extends Fragment {
    private final static String TAG = "TabSumDetailFragment";

    private List<String> textMonths = new ArrayList<>(
            Arrays.asList("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"));

    private View mRootView;

    private BroadcastReceiver mBroadcastReceiver;

    private Integer mCurrentYear;

    ///////////////////////////////////////////////////////////////////////////////////////////
    public TabSumDetailFragment() {
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

        Log.d(TAG, "inflating layout for RootView.");

        try {
            mRootView = inflater.inflate(R.layout.frag_sum_detail, container, false);
            if ( getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // set L1 vertical
                LinearLayout ll = (LinearLayout) mRootView.findViewById(R.id.frag_sum_detail);
                ll.setOrientation(LinearLayout.VERTICAL);
            }
//            else {
//                // set L1 horizontal
//                LinearLayout ll = (LinearLayout) mRootView.findViewById(R.id.frag_sum_detail);
//                ll.setOrientation(LinearLayout.HORIZONTAL);
//            }

        } catch (InflateException e) {
            Log.e(TAG, "inflater exception: " + e.getMessage());
            return null;
        }
        // init kwh/dollar radio
        initKwhDollarRadio();
        // draw view
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
    private boolean initKwhDollarRadio() {
        // initialize Radio Group and attach click handler
        RadioGroup radioGroup = (RadioGroup) mRootView.findViewById(R.id.radioKwhDollar);
        // default check to kwh then check settings & check dollars if true
        radioGroup.check(R.id.radioKwh);
        if (PrefUtils.getPrefsShowDollars(mRootView.getContext())) {
            radioGroup.check(R.id.radioDollar);
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioDollar:
                        Toast.makeText(mRootView.getContext(), "Show $$$", Toast.LENGTH_SHORT).show();
                        PrefUtils.setPrefsShowDollars(mRootView.getContext(), true);
                        // notify listeners
                        BroadcastUtils.broadcastResult(mRootView.getContext(), BroadcastUtils.AHA_REFRESH, "Show Dollars");
                        break;
                    case R.id.radioKwh:
                        Toast.makeText(mRootView.getContext(), "Show KwH", Toast.LENGTH_SHORT).show();
                        PrefUtils.setPrefsShowDollars(mRootView.getContext(), false);
                        // notify listeners
                        BroadcastUtils.broadcastResult(mRootView.getContext(), BroadcastUtils.AHA_REFRESH, "Show Kwh");
                        break;
                }
            }
        });

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean refreshView() {
        // fetch current selected year
        mCurrentYear = PrefUtils.getPrefsCurrentYear(mRootView.getContext());
        Log.d(TAG, "refreshView current year " + mCurrentYear);
        showDetailsText();
        showUsageByTOD();
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showDetailsText() {
        TextView textView;

        Boolean showDollars = PrefUtils.getPrefsShowDollars(getActivity());

        String thisYear = mCurrentYear.toString();
        String lastYear = String.valueOf(mCurrentYear-1);
        Log.d(TAG, "showDetailsText current year " + thisYear + ", last year " + lastYear);

        // update labels for KwH or dollars
        textView = (TextView) mRootView.findViewById(R.id.textViewLastYearLabel);
        textView.setText(getResources().getString(R.string.total_kwh_label) + lastYear);
        if (showDollars) textView.setText(getResources().getString(R.string.total_dollar_label) + lastYear);

        textView = (TextView) mRootView.findViewById(R.id.textViewThisYearLabel);
        textView.setText(getResources().getString(R.string.total_kwh_label) + thisYear);
        if (showDollars) textView.setText(getResources().getString(R.string.total_dollar_label) + thisYear);

        textView = (TextView) mRootView.findViewById(R.id.textViewLastYearAveMoLabel);
        textView.setText(getResources().getString(R.string.avemo_label_kwh) + lastYear);
        if (showDollars) textView.setText(R.string.avemo_label_dollar + lastYear);
        textView = (TextView) mRootView.findViewById(R.id.textViewThisYearAveMoLabel);
        textView.setText(getResources().getString(R.string.avemo_label_kwh) + thisYear);
        if (showDollars) textView.setText(getResources().getString(R.string.avemo_label_dollar) + thisYear);

        textView = (TextView) mRootView.findViewById(R.id.textViewPeakMoLabel);
        textView.setText(getResources().getString(R.string.peakmo_label_kwh) + thisYear);
        if (showDollars) textView.setText(getResources().getString(R.string.peakmo_label_dollar) + thisYear);
        textView = (TextView) mRootView.findViewById(R.id.textViewLowMoLabel);
        textView.setText(getResources().getString(R.string.lowmo_label_kwh) + thisYear);
        if (showDollars) textView.setText(getResources().getString(R.string.lowmo_label_dollar) + thisYear);

        textView = (TextView) mRootView.findViewById(R.id.textViewDeltaLabel);
        textView.setText(R.string.delta_label_kwh);
        if (showDollars) textView.setText(R.string.delta_label_dollar);

        // total previous year
//        long[] tallyYearStart = PrefUtils.getPrefsTallyByMonth(getActivity(), FeedManager.YEAR_START);
        long[] tallyYearStart = PrefUtils.getPrefsTallyByMonth(getActivity(), mCurrentYear-1);
        Long sumPrevYear = 0L;
        for (long month : tallyYearStart) {
            if (showDollars) month = (long) PrefUtils.convertKwhToDollars((int) month);
            sumPrevYear += month;
        }
        // total current year of rolling 12 months
//        long[] tallyYearEnd = PrefUtils.getPrefsTallyByMonth(getActivity(), FeedManager.YEAR_END);
        long[] tallyYearEnd = PrefUtils.getPrefsTallyByMonth(getActivity(), mCurrentYear);
        Long sumRolling = 0L;
        Long peakRolling = 0L;
        int peakMonthInx = 0;
        Long lowRolling = 1000000L;
        int lowMonthInx = 0;
        int lastMonthInx = 0;
        int monthCount = 0;
        for (long month : tallyYearEnd) {
            // sum all non-zero months
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
        long deltaLastMonth = tallyYearStart[lastMonthInx] - tallyYearEnd[lastMonthInx];
        if (showDollars) deltaLastMonth = (long) PrefUtils.convertKwhToDollars((int) deltaLastMonth);
//        // sum remainder from last portion of 2014 to rolling stats
//        for (int i = lastMonthInx+1; i < tallyYearStart.length; i++) {
//            if (showDollars) tallyYearStart[i] = (long) PrefUtils.convertKwhToDollars((int) tallyYearStart[i]);
//
//            sumRolling += tallyYearStart[i];
//            if (tallyYearStart[i] > peakRolling) {
//                peakRolling = tallyYearStart[i];
//                peakMonthInx = i;
//            }
//            if (tallyYearStart[i] < lowRolling) {
//                lowRolling = tallyYearStart[i];
//                lowMonthInx = i;
//            }
//        }

        // update details text fields
        Integer aveYear = (int) (sumPrevYear / 12);
        textView = (TextView) mRootView.findViewById(R.id.textViewTotalLastYear);
        textView.setText(sumPrevYear.toString());
        textView = (TextView) mRootView.findViewById(R.id.textViewLastYearAveMo);
        textView.setText(aveYear.toString());
        Integer aveRolling = (int) (sumRolling / 12);
        Integer lastMonth = (int) (deltaLastMonth);
        int color = getActivity().getResources().getColor(R.color.red);
        if (sumPrevYear > sumRolling) {
            color = getActivity().getResources().getColor(R.color.green);
        }
        textView = (TextView) mRootView.findViewById(R.id.textViewTotalThisYear);
        textView.setText(sumRolling.toString());
        textView.setTextColor(color);
        textView = (TextView) mRootView.findViewById(R.id.textViewThisYearAveMo);
        textView.setText(aveRolling.toString());
        textView.setTextColor(color);
        textView = (TextView) mRootView.findViewById(R.id.textViewThisYearPeakMo);
        textView.setText(peakRolling.toString() + " (" + textMonths.get(peakMonthInx) + ")");
        textView = (TextView) mRootView.findViewById(R.id.textViewThisYearLowMo);
        textView.setText(lowRolling.toString() + " (" + textMonths.get(lowMonthInx) + ")");
        // if delta last month with prev year month is positive turn green, else leave red
        color = getActivity().getResources().getColor(R.color.red);
        if (lastMonth > 0) {
            color = getActivity().getResources().getColor(R.color.green);
        }
        textView = (TextView) mRootView.findViewById(R.id.textViewDelta);
        textView.setTextColor(color);
        textView.setText(lastMonth.toString() + " (" + textMonths.get(lastMonthInx) + ")");

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean showUsageByTOD() {

        final int CHART_LAYOUT_X = 256;
        final int CHART_LAYOUT_Y = 256;

//        Integer year = FeedManager.YEAR_END;;
        Integer year = mCurrentYear;;
//        String yearText = PrefUtils.getPrefsToDate(getActivity());
//        Log.d(TAG, "showUsageByTOD for " + yearText);
//        try {
//            year = Integer.parseInt(yearText);
//        }
//        catch (Exception ex) {
//            Log.e(TAG, "showUsageByTOD finds invalid getPrefsToDate " + yearText);
//        }

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
                " 12-4 ", " 4-8 ", " 8-12 ", " 12-4 ", " 4-8 ", " 8-12 "
        };
        // Pie Chart Section Value - equal across all sections
        double[] distribution = {
                16.666, 16.666, 16.666, 16.666, 16.666, 16.666
        } ;
        // init total consumption & total for each hour
        long totalKwH = 0;
        long[] values = PrefUtils.getPrefsOverviewByTod(getActivity(), year);

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
        defaultRenderer.setZoomButtonsVisible(ChartRender.SHOW_ZOOM_BUTTONS);
        defaultRenderer.setLabelsTextSize(ChartRender.LABEL_TEXT_SIZE);
        defaultRenderer.setLegendTextSize(ChartRender.LEGEND_TEXT_SIZE);

        // gather screen dimensions
        Configuration configuration = getActivity().getResources().getConfiguration();
        int screenWidthDp = configuration.screenWidthDp;
        int screenHeightDp = configuration.screenHeightDp;
        Log.v(TAG, "screen w/h dp: " + screenWidthDp + ", " + screenHeightDp);

        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int pixelWidth = displayMetrics.widthPixels;
        int pixelHeight = displayMetrics.heightPixels;
        float density = displayMetrics.density;
        Log.v(TAG, "screen w/h pixel - density: " + pixelWidth + ", " + pixelHeight + " - " + density);
        // determine chart dimensions
        int chartLayoutX = 0;
        int chartLayoutY = 0;
        if ( getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            chartLayoutX = (int)(((double)pixelWidth) * .2);
//            chartLayoutY = (int)(((double)pixelHeight) * .27);
            chartLayoutX = (int)(((double)pixelWidth) * ChartUtils.CHART_MINOR_X_PERCENT);
            chartLayoutY = (int)(((double)pixelHeight) * ChartUtils.CHART_MINOR_Y_PERCENT);
        }
//        else {
//            chartLayoutX = (int)(((double)pixelWidth) * .32);
//            chartLayoutY = (int)(((double)pixelHeight) * .17);
//        }
        Log.v(TAG, "chartLayout X, Y: " + chartLayoutX + ", " + chartLayoutY);

        LinearLayout mChartLayout = (LinearLayout) mRootView.findViewById(R.id.chart_minor);

        // get the chart view & add to layout
        View chartView = ChartFactory.getPieChartView(mRootView.getContext(), distributionSeries, defaultRenderer);
        mChartLayout.removeAllViews();
        mChartLayout.addView(chartView, chartLayoutX, chartLayoutY);

        return true;
    }
}
///////////////////////////////////////////////////////////////////////////////////////////////
