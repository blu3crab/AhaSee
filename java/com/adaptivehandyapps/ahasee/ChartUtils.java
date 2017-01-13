// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: FEB 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.util.Log;

import com.adaptivehandyapps.ahasee.espi.FeedManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by mat on 2/24/2015.
 */
public class ChartUtils {

    private static final String TAG = "ChartUtils";
    // test: overlay DB values with list values to visually confirm identical
    private static final boolean OVERLAY_DB_READ_DATA = false;

    public static final String TIMEZONE_EST = "EST";
    public static final String TIMEZONE_UTC = "UTC";

    public static final int DST_START_DAY = 8;
    public static final int DST_START_MONTH = 3;
    public static final int DST_END_DAY = 1;
    public static final int DST_END_MONTH = 11;

    public static class ChartControl {
        public ChartRender chartRender;
        public String chartTitle;
        public String chartAxisLabelX;
        public String chartAxisLabelY;
        public FeedManager feedManager;
        public Integer fromTimeSecs;
        public Integer toTimeSecs;
        public Integer stackLimit;
        public ChartRender.ChartType chartType;
        public Boolean showLabels;
        public Integer chartLayoutX;
        public Integer chartLayoutY;

        public ChartControl(ChartRender chart, FeedManager feed,
                            String chartTitle, String chartAxisLabelX, String chartAxisLabelY,
                            Integer from, Integer to, Integer stack, ChartRender.ChartType type,
                            Boolean showLabels, Integer layoutX, Integer layoutY) {
            chartRender = chart;
            feedManager = feed;
            this.chartTitle = chartTitle;
            this.chartAxisLabelX = chartAxisLabelX;
            this.chartAxisLabelY = chartAxisLabelY;
            fromTimeSecs = from;
            toTimeSecs = to;
            stackLimit = stack;
            chartType = type;
            this.showLabels = showLabels;
            this.chartLayoutX = layoutX;
            this.chartLayoutY = layoutY;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public ChartUtils() {}

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static int getDaylightSavingsTimeOffset(long timeMs) {
        int dstOffset = 5;  // default not daylight savings time

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMs);
        int day = c.get(Calendar.DATE);
        int month = c.get(Calendar.MONTH);
        if((month > DST_START_MONTH && month < DST_END_MONTH) ||
                (month == DST_START_MONTH && day >= DST_START_DAY) ||
                (month == DST_END_MONTH && day <= DST_END_DAY)) {
            dstOffset = 4;
        }
        return dstOffset;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static long shiftUtcMillisToEst(long timeMs) {
        int dstOffset = getDaylightSavingsTimeOffset(timeMs);
        timeMs += (dstOffset * FeedManager.SECS_IN_HOUR * 1000);
//        timeMs += (4 * FeedManager.SECS_IN_HOUR * 1000);
//        Log.v(TAG, "secsToDate GMT+4 timeMS:" + timeMs);
        return timeMs;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static long shiftEstMillisToUtc(long timeMs) {
        int dstOffset = getDaylightSavingsTimeOffset(timeMs);
        timeMs -= (dstOffset * FeedManager.SECS_IN_HOUR * 1000);
//        timeMs -= (4 * FeedManager.SECS_IN_HOUR * 1000);
//        Log.v(TAG, "secsToDate GMT-4 timeMS:" + timeMs);
        return timeMs;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static String secsToDate(Integer timeSecs, boolean shiftEstSecs) {
        long timeMs = (long)timeSecs*1000;
        // TODO: adjust for GMT-4, GMT-5
//        Log.v(TAG, "secsToDate GMT timeMS:" + timeMs);
        if (shiftEstSecs) {
//            timeMs -= (4 * FeedManager.SECS_IN_HOUR * 1000);
            timeMs = shiftEstMillisToUtc(timeMs);
        }
        else {
            timeMs = shiftUtcMillisToEst(timeMs);
        }
        Date date = new Date(timeMs);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy"); // the format of your date
        String dateText = sdf.format(date);
//        Log.v(TAG, "secsToDate date:" + dateText);
//        return sdf.format(date);
        return dateText;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static long dateToMillis(String dateText, boolean shiftEstSecs) {
        long timeMs = 0;
//        dateText = dateText.concat(" GMT-4");
//        dateText = dateText.concat(" GMT");
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy z"); // the format of your date
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy"); // the format of your date
        try {
            Date date = sdf.parse(dateText);
            timeMs = date.getTime();
//            Log.v(TAG, " date: " + dateText + ", ms: " + timeMs);
        } catch (ParseException ex) {
            Log.e(TAG, "dateToMillis exception:" + ex.getMessage());
        }
        // TODO: adjust for GMT-4, GMT-5
        if (shiftEstSecs) {
//            timeMs -= (4 * FeedManager.SECS_IN_HOUR * 1000);
            timeMs = shiftEstMillisToUtc(timeMs);
        }
        else {
            timeMs = shiftUtcMillisToEst(timeMs);
        }
//        Log.v(TAG, "dateToMillis GMT-4 timeMS:" + timeMs);
        return timeMs;
//        long timeMs = 0;
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy"); // the format of your date
//        try {
//            Date date = sdf.parse(dateText);
//            timeMs = date.getTime();
//        } catch (ParseException ex) {
//            Log.e(TAG, "dateToMillis exception:" + ex.getMessage());
//        }
//        return timeMs;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static boolean showFeed(ChartControl chartControl, boolean clearSeries) {
        boolean success = false;

        if (clearSeries) {
            // clear series & series labels
            chartControl.chartRender.clearSeriesLabels();
            chartControl.chartRender.clearSeries();
        }

        switch (chartControl.chartType) {
            case AREA:
            case LINE:
            case BAR:
                loadLineChart(chartControl);
                break;
            case PIE:
                loadPieChart(chartControl);
                break;
            case DONUT:
                loadDonutChart(chartControl);
                break;
            default:
                Log.e(TAG, "OpenChart: Invalid chart type: " + chartControl.chartType.toString());
        }

        // show full chart
        chartControl.chartRender.showSeries(chartControl.showLabels, chartControl.chartType,
                chartControl.chartLayoutX, chartControl.chartLayoutY);
//        // show thumb
//        chartControl.chartRender.showSeries(true, chartControl.chartType);

        success = true;

        return success;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean loadLineChart(ChartControl chartControl) {
        List<Integer> values;
        // init range lablels
        Integer from = chartControl.fromTimeSecs;
        Integer to = chartControl.toTimeSecs;
        Integer timeRange = to - from;

        Log.v(TAG,"from time: " + from + " to time: " + to + ", range " + timeRange);
//        String value1Range = secsToDate(from) + " to " + secsToDate(to);
        String value1Range = secsToDate(from, false);
        String valueRange = "";
        // for each value set up to stack limit
        for (int i = 0; i <= chartControl.stackLimit; i++) {
            // get selected date interval readings
            if (chartControl.feedManager.isValidFeed()) {
                values = chartControl.feedManager.queryWhValues(from, to);
            }
            else {
//                values = chartControl.feedManager.getIntWhValues(from, to);
                return false;
            }
//            String valueRange = secsToDate(from) + " to " + secsToDate(to);
            valueRange = secsToDate(from, false);
            // add series
            chartControl.chartRender.addSeriesLabels(valueRange);
            chartControl.chartRender.addSeries(values);
            // advance from to
            from = to;
            to = to + (timeRange);
        }
        // label chart
//        chartControl.chartRender.setChartTitle(chartControl.chartTitle + " (" + value1Range + " to " + valueRange + ")");
        chartControl.chartRender.setChartTitle(chartControl.chartTitle);
        chartControl.chartRender.setChartAxisLabelX(chartControl.chartAxisLabelX);
        chartControl.chartRender.setChartAxisLabelY(chartControl.chartAxisLabelY);

        return true;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean loadPieChart(ChartControl chartControl) {
        List<Integer> values;
        // init range lablels
        Integer from = chartControl.fromTimeSecs;
        Integer to = chartControl.toTimeSecs;
        Integer timeRange = to - from;
        Log.v(TAG,"from time: " + from + "to time: " + to);
        String value1Range = secsToDate(from, true) + " to " + secsToDate(to, true);
        String valueRange = value1Range;
        // for each value set up to stack limit
        for (int i = 0; i <= chartControl.stackLimit; i++) {
            // get selected date interval readings
            if (chartControl.feedManager.isValidFeed()) {
                values = chartControl.feedManager.queryWhValues(from, to);
            }
            else {
//                values = chartControl.feedManager.getIntWhValues(from, to);
                return false;
            }
            valueRange = secsToDate(from, true) + " to " + secsToDate(to, true);
            // add series
            chartControl.chartRender.addSeriesLabels(valueRange);
            chartControl.chartRender.addSeries(values);
            // advance from to
            from = to;
            to = to + (timeRange);
        }

        // set chart elements
        chartControl.chartRender.setChartTitle(chartControl.chartTitle + value1Range);

        return true;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean loadDonutChart(ChartControl chartControl) {
        List<Integer> values;
        // init range lablels
        Integer from = chartControl.fromTimeSecs;
        Integer to = chartControl.toTimeSecs;
        Integer timeRange = to - from;
        Log.v(TAG,"from time: " + from + "to time: " + to);
        String value1Range = secsToDate(from, true) + " to " + secsToDate(to, true);
        String valueRange = value1Range;
        // for each value set up to stack limit
        for (int i = 0; i <= chartControl.stackLimit; i++) {
            // get selected date interval readings
            if (chartControl.feedManager.isValidFeed()) {
                values = chartControl.feedManager.queryWhValues(from, to);
            }
            else {
//                values = chartControl.feedManager.getIntWhValues(from, to);
                return false;
            }
            valueRange = secsToDate(from, true) + " to " + secsToDate(to, true);
            // add series
            chartControl.chartRender.addSeriesLabels(valueRange);
            chartControl.chartRender.addSeries(values);
            // advance from to
            from = to;
            to = to + (timeRange);
        }
        // label chart
        chartControl.chartRender.setChartTitle(chartControl.chartTitle + value1Range);

        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
////////////////////////////////////////////////////////////////////////////////////////////////////
