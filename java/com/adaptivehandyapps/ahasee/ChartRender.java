// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: APR 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.adaptivehandyapps.ahasee.achartdemo.chart.AbstractDemoChart;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mat on 4/10/2015.
 */
public class ChartRender extends AbstractDemoChart {

    private static final String TAG = "ChartRender";

    private View mRootView;
    private LinearLayout mChartLayout;
//    private LinearLayout mChartLayoutThumb;

    // chart types
    public enum ChartType {BAR, LINE, AREA, PIE, DONUT, DIFF};

    // chart elements
    private String mChartTitle = "Value1 vs Value2 Chart";
    private String mChartAxisLabelX = "Hour";
    private String mChartAxisLabelY = "Watts Per Hour";

    private List<String> mXTextLabels = new ArrayList();
    private List<String> mSeriesLabels = new ArrayList();
    private List<List> mSeries = new ArrayList();

    private int mValuesSize = 8760; // year of hourly data

    private List<String> mColorStrings = new ArrayList<String>(
            Arrays.asList(
                    "#FFf22020",
                    "#88f22020",
                    "#FF20f220",
                    "#8820f220",
                    "#FF2020f2",
                    "#882099f2",
                    "#FFf2f220",
                    "#88f2f220",
                    "#FFf220f2",
                    "#88f220f2",
                    "#FF20f2f2",
                    "#8820f2f2",
                    "#FF992020",
                    "#88992020",
                    "#FF209920",
                    "#88209920",
                    "#FF202099",
                    "#88202099",
                    "#FF999920",
                    "#88999920",
                    "#FF992099",
                    "#88992099",
                    "#FF209999",
                    "#88209999"
            ));
    //////////////////////////////////////////////////////////////////////////////////////////
//    public ChartRender(View rootView) {
//    public ChartRender(View rootView, int chartResId, int thumbResId) {
    public ChartRender(View rootView, int chartResId) {
        mRootView = rootView;
        mChartLayout = (LinearLayout) mRootView.findViewById(chartResId);
//        if (thumbResId > 0) mChartLayoutThumb = (LinearLayout) mRootView.findViewById(thumbResId);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // AbstractDemoChart abstract class methods
    public String getName() {
        return "Meter Charts";
    }
    public String getDesc() {
        return "Meter Charts show various chart types";
    }
    public Intent execute(Context context) {
        return null;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // chart element getters/setters
    public String getChartTitle() {return mChartTitle;}
    public void setChartTitle(String chartTitle) {mChartTitle = chartTitle;}
    public String getChartAxisLabelX() {return mChartAxisLabelX;}
    public void setChartAxisLabelX(String chartAxisLabelX) {mChartAxisLabelX = chartAxisLabelX;}
    public String getChartAxisLabelY() {return mChartAxisLabelY;}
    public void setChartAxisLabelY(String chartAxisLabelY) {mChartAxisLabelY = chartAxisLabelY;}

    public void setXTextLabels ( List<String> textLabels) {
        mXTextLabels = textLabels;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // chart series helpers
    public List<List> getSeries() { return mSeries; }

    public void clearSeriesLabels() {
        mSeriesLabels = new ArrayList();
    }
    public void clearSeries() {
        mSeries = new ArrayList();
    }

    public int addSeriesLabels(String label) {
        mSeriesLabels.add(label);
        return mSeriesLabels.size();
    }
    public int addSeries(List<Integer> values) {
        mSeries.add(values);
        if (values.size() < mValuesSize) mValuesSize = values.size();
        return mSeries.size();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // chart & thumb layout params
//    private final static int CHART_LAYOUT_X = 832;
//    private final static int CHART_LAYOUT_Y = 512;
////    private final static int CHART_LAYOUT_X = LinearLayout.LayoutParams.FILL_PARENT;
////    private final static int CHART_LAYOUT_Y = LinearLayout.LayoutParams.WRAP_CONTENT;
//    private final static int THUMB_LAYOUT_X = 128;
//    private final static int THUMB_LAYOUT_Y = 96;

    public void showSeries(boolean showLabels, ChartType chartType, int chartLayoutX, int chartLayoutY) {

//        int chartLayoutX = 832;
//        int chartLayoutY = 512;
        // ensure layouts are defined
        if (mChartLayout == null) {
            return;
        }
        switch (chartType) {
            case AREA:
            case LINE:
            case BAR:
                openChart(showLabels, chartType, chartLayoutX, chartLayoutY);
                break;
            case PIE:
                openChartPie(showLabels, chartLayoutX, chartLayoutY);
                break;
            case DONUT:
                openChartDonut(showLabels, chartLayoutX, chartLayoutY);
                break;
            case DIFF:
                openChartDiff(chartLayoutX, chartLayoutY);
                break;
            default:
                Log.e(TAG, "OpenChart: Invalid chart type: " + chartType.toString());
        }
        return;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void openChart(boolean showLabels, ChartType chartType, int chartLayoutX, int chartLayoutY) {
        BarChart.Type barType = BarChart.Type.DEFAULT;
        // list of renders for each series
        List<XYSeriesRenderer> rendererList = new ArrayList();
        // Creating a dataset to hold all series
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        // for each list of values in the series list
        int labelInx = 0;
        int colorInx = 0;
        for (List<Integer> values : mSeries) {
            // creating an  XYSeries for values
            // TODO: list of labels
            XYSeries valueSeries = new XYSeries(mSeriesLabels.get(labelInx++));
            // Adding data to Series
            for(int i = 0; i < mValuesSize; i++){
                valueSeries.add(i, values.get(i));
            }
            // add values to dataset
            dataset.addSeries(valueSeries);

            // create XYSeriesRenderer to customize Series
            XYSeriesRenderer renderer = new XYSeriesRenderer();
            // TODO: list of colors
            renderer.setColor(Color.parseColor(mColorStrings.get(colorInx++)));
            renderer.setFillPoints(true);
            renderer.setLineWidth(2);
            renderer.setDisplayChartValues(true);
            if (chartType == ChartType.AREA) {
                if (!showLabels) {
                    renderer.setPointStyle(PointStyle.CIRCLE);
                }
                renderer.setFillBelowLine(true);
                // TODO: list of colors
                renderer.setFillBelowLineColor(Color.parseColor(mColorStrings.get(colorInx++)));
            }
            else {
                colorInx++;
            }
            if (colorInx >= mColorStrings.size()) colorInx = 0;
            rendererList.add(renderer);
        }

        // Creating a XYMultipleSeriesRenderer to customize the whole chart
        XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
        multiRenderer.setXLabels(0);
        multiRenderer.setChartTitle(getChartTitle());
        multiRenderer.setXTitle(getChartAxisLabelX());
        multiRenderer.setYTitle(getChartAxisLabelY());
        multiRenderer.setZoomButtonsVisible(true);
        multiRenderer.setBackgroundColor(Color.rgb(220, 220, 220));
        multiRenderer.setApplyBackgroundColor(true);
//        int roll = 4; // GMT-4
        int roll = 0;
        for(int i = 0; i < mValuesSize; i++){
            if (mXTextLabels.size() == mValuesSize) {
                multiRenderer.addXTextLabel(i, mXTextLabels.get(i));
            }
            else {
                multiRenderer.addXTextLabel(i, String.valueOf(roll));
            }

            if (roll < 23) {
                ++roll;
            }
            else {
                roll = 0;
            }
        }
        // add renderers to multipleRenderer
        // Note: use same order of dataseries in dataset and renderers to multipleRenderer
        for (XYSeriesRenderer renderer : rendererList) {
            multiRenderer.addSeriesRenderer(renderer);
        }

        if (chartType == ChartType.BAR) {
            // full chart
            if (!showLabels) {
                // hide Labels
                for (XYSeriesRenderer renderer : rendererList) {
                    renderer.setDisplayChartValues(false);
                }
                multiRenderer.setZoomButtonsVisible(false);
                multiRenderer.setChartTitle("");
                multiRenderer.setXTitle("");
                multiRenderer.setYTitle("");
                multiRenderer.clearXTextLabels();
                multiRenderer.setYLabels(0);
            }
            // get the chart view & add to layout
            View chartView = ChartFactory.getBarChartView(mRootView.getContext(), dataset, multiRenderer, barType);
            mChartLayout.removeAllViews();
//            mChartLayout.addView(chartView, new LinearLayout.LayoutParams(CHART_LAYOUT_X, CHART_LAYOUT_Y));
            mChartLayout.addView(chartView, new LinearLayout.LayoutParams(chartLayoutX, chartLayoutY));
        }
        else if (chartType == ChartType.LINE || chartType == ChartType.AREA) {
            // full chart
            if (!showLabels) {
                // hide Labels
                for (XYSeriesRenderer renderer : rendererList) {
                    renderer.setDisplayChartValues(false);
                }
                multiRenderer.setZoomButtonsVisible(false);
                multiRenderer.setChartTitle("");
                multiRenderer.setXTitle("");
                multiRenderer.setYTitle("");
                multiRenderer.clearXTextLabels();
                multiRenderer.setYLabels(0);
            }
            // get the chart view & add to layout
            View chartView = ChartFactory.getLineChartView(mRootView.getContext(), dataset, multiRenderer);
            mChartLayout.removeAllViews();
//            mChartLayout.addView(chartView, new LinearLayout.LayoutParams(CHART_LAYOUT_X, CHART_LAYOUT_Y));
            mChartLayout.addView(chartView, new LinearLayout.LayoutParams(chartLayoutX, chartLayoutY));
        }
        else {
            Log.e(TAG, "OpenChart: Invalid chart type: " + chartType.toString());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void openChartPie(boolean showLabels, int chartLayoutX, int chartLayoutY) {
        // Pie Chart Section Names
        String[] sliceName = new String[] {
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
                "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"
        };
        // Pie Chart Section Value
        double[] distribution = {
                4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666,
                4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666,
                4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666, 4.1666
        } ;
        // init total consumption & total for each hour
        int totalWh = 0;
        List<Integer> allValues = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            allValues.add(hour, 0);
        }
        // tally total consumption for 24 hour period & total for all values for each hour
        for (List<Integer> values : mSeries) {
//            List<Integer> values = mSeries.get(0);
            for (int hour = 0; hour < 24; hour++) {
                totalWh += values.get(hour);
                allValues.set(hour, allValues.get(hour)+values.get(hour));
            }
        }
        // calculate distribution
        for (int hour = 0; hour < 24; hour++) {
//                distribution[hour] = ((double) values.get(hour)) / (double) totalWh;
            sliceName[hour] = sliceName[hour].concat(":" + allValues.get(hour).toString());
            distribution[hour] = ((double) allValues.get(hour)) / (double) totalWh;
        }

        // distribution: Instantiating CategorySeries to plot Pie Chart
        CategorySeries distributionSeries = new CategorySeries(getChartTitle());
        for(int i=0 ;i < distribution.length;i++){
            // Adding a slice with its values and name to the Pie Chart
            distributionSeries.add(sliceName[i], distribution[i]);
        }

        // colors: Instantiating a renderer for the Pie Chart
        DefaultRenderer defaultRenderer  = new DefaultRenderer();
        for(int i = 0 ; i < distribution.length; i++){
            SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
//            seriesRenderer.setColor(colors[i]);
            seriesRenderer.setColor(Color.parseColor(mColorStrings.get(i)));
            // Adding a renderer for a slice
            defaultRenderer.addSeriesRenderer(seriesRenderer);
        }
        defaultRenderer.setStartAngle(270);
        defaultRenderer.setChartTitle(getChartTitle());
        defaultRenderer.setChartTitleTextSize(20);
        defaultRenderer.setZoomButtonsVisible(true);

        // hide labels
       if (!showLabels) {
            defaultRenderer.setZoomButtonsVisible(false);
            defaultRenderer.setChartTitle("");
        }
        // get the chart view & add to layout
        View chartView = ChartFactory.getPieChartView(mRootView.getContext(), distributionSeries, defaultRenderer);
        mChartLayout.removeAllViews();
//        mChartLayout.addView(chartView, new LinearLayout.LayoutParams(CHART_LAYOUT_X, CHART_LAYOUT_Y));
        mChartLayout.addView(chartView, new LinearLayout.LayoutParams(chartLayoutX, chartLayoutY));
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void openChartDonut(boolean showLabels, int chartLayoutX, int chartLayoutY) {
        // set titles
        List<String[]> titles = new ArrayList<String[]>();
        // set distribution values
        List<double[]> values = new ArrayList<double[]>();

        for (List<Integer> s : mSeries) {
            // add titles
            titles.add(new String[] {
                    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
                    "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"
            });
            // init total consumption
            int totalWh = 0;
            for (int hour = 0; hour < 24; hour++) {
                totalWh += s.get(hour);
            }
            // calculate distribution
            double[] distribution = new double[24];
            for (int hour = 0; hour < 24; hour++) {
                distribution[hour] = ((double) s.get(hour)) / (double) totalWh;
            }
            values.add(distribution);
        }
        // set colors
        int[] colors = new int[24];
        for (int hour = 0; hour < 24; hour++) {
            colors[hour] = Color.parseColor(mColorStrings.get(hour));
        }

        DefaultRenderer defaultRenderer = buildCategoryRenderer(colors);
        defaultRenderer.setStartAngle(270);
        defaultRenderer.setApplyBackgroundColor(true);
        defaultRenderer.setBackgroundColor(Color.rgb(222, 222, 200));
        defaultRenderer.setLabelsColor(Color.GRAY);

        // if hide labels
        if (!showLabels) {
            defaultRenderer.setZoomButtonsVisible(false);
            defaultRenderer.setChartTitle("");
        }
        // get the chart view & add to layout
        View chartView = ChartFactory.getDoughnutChartView(mRootView.getContext(),
                buildMultipleCategoryDataset(getChartTitle(), titles, values), defaultRenderer);
        mChartLayout.removeAllViews();
//        mChartLayout.addView(chartView, new LinearLayout.LayoutParams(CHART_LAYOUT_X, CHART_LAYOUT_Y));
        mChartLayout.addView(chartView, new LinearLayout.LayoutParams(chartLayoutX, chartLayoutY));
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private boolean openChartDiff(int chartLayoutX, int chartLayoutY) {
        String[] titles = new String[] { "Typical", "Finding",
                "Difference between Typical/Finding" };
        List<double[]> values = new ArrayList<double[]>();

        for (List<Integer> s : mSeries) {
            double[] dvalues = new double[s.size()];
            int di = 0;
            for (Integer ivalue : s) {
                dvalues[di] = ivalue;
                ++di;
            }
            values.add(dvalues);
        }

        int length = values.get(0).length;
        double[] diff = new double[length];
        for (int i = 0; i < length; i++) {
            diff[i] = values.get(0)[i] - values.get(1)[i];
        }
        values.add(diff);
        int[] colors = new int[] { Color.BLUE, Color.CYAN, Color.GREEN };
        PointStyle[] styles = new PointStyle[] { PointStyle.POINT, PointStyle.POINT, PointStyle.POINT };
        XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
        setChartSettings(renderer, getChartTitle(), getChartAxisLabelX(), getChartAxisLabelY(), 0.75,
                25.5, -5000, 19000, Color.GRAY, Color.LTGRAY);
        renderer.setXLabels(24);
        renderer.setYLabels(10);
        renderer.setChartTitleTextSize(20);
        renderer.setTextTypeface("sans_serif", Typeface.BOLD);
        renderer.setLabelsTextSize(14f);
        renderer.setAxisTitleTextSize(15);
        renderer.setLegendTextSize(15);
        length = renderer.getSeriesRendererCount();

        for (int i = 0; i < length; i++) {
            XYSeriesRenderer seriesRenderer = (XYSeriesRenderer) renderer.getSeriesRendererAt(i);
            if (i == length - 1) {
                XYSeriesRenderer.FillOutsideLine fill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                fill.setColor(Color.GREEN);
                seriesRenderer.addFillOutsideLine(fill);
            }
            seriesRenderer.setLineWidth(2.5f);
            seriesRenderer.setDisplayChartValues(true);
            seriesRenderer.setChartValuesTextSize(10f);
        }

        View chartView = ChartFactory.getCubeLineChartView(mRootView.getContext(), buildBarDataset(titles, values), renderer,
                0.5f);
        mChartLayout.removeAllViews();
//        mChartLayout.addView(chartView, new LinearLayout.LayoutParams(CHART_LAYOUT_X, CHART_LAYOUT_Y));
        mChartLayout.addView(chartView, new LinearLayout.LayoutParams(chartLayoutX, chartLayoutY));

        return true;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

}
////////////////////////////////////////////////////////////////////////////////////////////////////
