// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: SEP 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mat on 9/2/2015.
 */
public class PrefUtils {
    private static final String TAG = "PrefUtils";

    // shared preferences keys
    private static final String PREFS_KEY_FEEDFROMDATE = "FEED_FROM_DATE";
    private static final String PREFS_KEY_FEEDTODATE = "FEED_TO_DATE";
    private static final String PREFS_KEY_SHOWDOLLARS = "SHOW_DOLLARS";
    private static final String PREFS_KEY_CHARTTYPE = "CHART_TYPE";
    private static final String PREFS_KEY_FROMDATE = "FROM_DATE";
    private static final String PREFS_KEY_TODATE = "TO_DATE";
    private static final String PREFS_KEY_ALGORITHM = "ALGORITHM";
    private static final String PREFS_KEY_STACKLIMIT = "STACK_LIMIT";
    private static final String PREFS_KEY_BASELINEINX = "BASELINE_INX";

    private static final String PREFS_KEY_TOD = "TOD";

    // shared preferences defaults
    private static final String PREFS_DEFAULT_CHARTTYPE = "Area";
    public static final String PREFS_DEFAULT_FEEDFROMDATE = "01-01-2016";
    public static final String PREFS_DEFAULT_FEEDTODATE = "01-01-2013";
    private static final String PREFS_DEFAULT_FROMDATE = "01-05-2015";
    private static final String PREFS_DEFAULT_TODATE = "02-05-2015";

    private static final Boolean PREFS_DEFAULT_SHOWDOLLARS = false;

    private static final String PREFS_DEFAULT_ALGORITHM = "None";
    private static final Integer PREFS_DEFAULT_STACKLIMIT = 0;
    private static final Integer PREFS_DEFAULT_BASELINEINX = 0;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // final preferences
    private static final List<String> textMonths = new ArrayList<>(
            Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));

    public static List<String> getPrefsTextMonths(Context context) {
        return textMonths;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // shared preferences
    public static String getPrefsFeedFromDate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREFS_KEY_FEEDFROMDATE, PREFS_DEFAULT_FEEDFROMDATE);
    }

    public static void setPrefsFeedFromDate(Context context, String prefsFromTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREFS_KEY_FEEDFROMDATE, prefsFromTime).apply();
    }

    public static String getPrefsFeedToDate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREFS_KEY_FEEDTODATE, PREFS_DEFAULT_FEEDTODATE);
    }

    public static void setPrefsFeedToDate(Context context, String prefsToTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREFS_KEY_FEEDTODATE, prefsToTime).apply();
    }

    public static Boolean getPrefsShowDollars(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_KEY_SHOWDOLLARS, PREFS_DEFAULT_SHOWDOLLARS);
    }

    public static void setPrefsShowDollars(Context context, Boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREFS_KEY_SHOWDOLLARS, flag).apply();
    }

    public static String getPrefsAlgorithm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREFS_KEY_ALGORITHM, PREFS_DEFAULT_ALGORITHM);
    }

    public static void setPrefsAlgorithm(Context context, String prefsAlgorithm) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREFS_KEY_ALGORITHM, prefsAlgorithm).apply();
    }

    public static Integer getPrefsStackLimit(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Integer prefsStackLimit = prefs.getInt(PREFS_KEY_STACKLIMIT, PREFS_DEFAULT_STACKLIMIT);
        return prefsStackLimit;
    }

    public static void setPrefsStackLimit(Context context, Integer prefsStackLimit) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREFS_KEY_STACKLIMIT, prefsStackLimit).apply();
    }

    public static Integer getPrefsBaselineInx(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Integer prefsBaselineInx = prefs.getInt(PREFS_KEY_BASELINEINX, PREFS_DEFAULT_BASELINEINX);
        return prefsBaselineInx;
    }

    public static void setPrefsBaselineInx(Context context, Integer prefsBaselineInx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(PREFS_KEY_BASELINEINX, prefsBaselineInx).apply();
    }

    public static String getPrefsChartType(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREFS_KEY_CHARTTYPE, PREFS_DEFAULT_CHARTTYPE);
    }

    public static void setPrefsChartType(Context context, String prefsChartType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREFS_KEY_CHARTTYPE, prefsChartType).apply();
    }

    public static String getPrefsFromDate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREFS_KEY_FROMDATE, PREFS_DEFAULT_FROMDATE);
    }

    public static void setPrefsFromDate(Context context, String prefsFromTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREFS_KEY_FROMDATE, prefsFromTime).apply();
    }

    public static String getPrefsToDate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREFS_KEY_TODATE, PREFS_DEFAULT_TODATE);
    }

    public static void setPrefsToDate(Context context, String prefsToTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREFS_KEY_TODATE, prefsToTime).apply();
    }

    public static long[] getPrefsOverviewByMonth(Context context, Integer year) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long[] tally = new long[12];
        for (Integer i = 0; i < 12; i++) {
            String key = i.toString() + year.toString();
            tally[i] = prefs.getLong(key, 0);
        }
        return tally;
    }

    public static void setPrefsOverviewByMonth(Context context, Integer year, long[] tally) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (Integer i = 0; i < 12; i++) {
            String key = i.toString() + year.toString();
            prefs.edit().putLong(key, tally[i]/1000).apply();
        }
    }

    public static long[] getPrefsOverviewByTod(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long[] tally = new long[6];
        for (Integer i = 0; i < 6; i++) {
            String key = PREFS_KEY_TOD + i.toString();
            tally[i] = prefs.getLong(key, 0);
        }
        return tally;
    }

    public static void setPrefsOverviewByTod(Context context, long[] tally) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        for (Integer i = 0; i < tally.length; i++) {
            String key = PREFS_KEY_TOD + i.toString();
            prefs.edit().putLong(key, tally[i]/1000).apply();
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // helpers
    public static ChartRender.ChartType getChartType(Context context, String chartTypeText) {
        ChartRender.ChartType chartType = ChartRender.ChartType.AREA;
        if (chartTypeText.equals(context.getString(R.string.chart_type_area)))
            chartType = ChartRender.ChartType.AREA;
        else if (chartTypeText.equals(context.getString(R.string.chart_type_line)))
            chartType = ChartRender.ChartType.LINE;
        else if (chartTypeText.equals(context.getString(R.string.chart_type_bar)))
            chartType = ChartRender.ChartType.BAR;
        else if (chartTypeText.equals(context.getString(R.string.chart_type_pie)))
            chartType = ChartRender.ChartType.PIE;
        else if (chartTypeText.equals(context.getString(R.string.chart_type_donut)))
            chartType = ChartRender.ChartType.DONUT;
        return chartType;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    public static int convertKwhToDollars (int kwh) {
        double KWH_TO_DOLLAR_FACTOR = .136;
        return (int)((double)kwh * KWH_TO_DOLLAR_FACTOR);
    }
    //////////////////////////////////////////////////////////////////////////////////////////
}
