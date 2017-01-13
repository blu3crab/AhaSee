// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: JUL 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee.espi;

import android.content.ContentValues;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.adaptivehandyapps.ahasee.BroadcastUtils;
import com.adaptivehandyapps.ahasee.PrefUtils;
import com.adaptivehandyapps.ahasee.R;
import com.adaptivehandyapps.ahasee.SeeActivity;
import com.adaptivehandyapps.ahasee.ChartUtils;
import com.adaptivehandyapps.ahasee.SettingsActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//////////////////////////////////////////////////////////////////////////////////////////

/**
 * Created by mat on 4/9/2015.
 */
public class FeedManager {
    private static final String TAG = "FeedManager";

//    static final public String AHA_REFRESH = "com.adaptivehandyapps.ahasee.REQUEST_REFRESH";
//
//    static final public String AHA_MESSAGE = "com.adaptivehandyapps.ahasee.FEED_MSG";
//
    public static final Integer JAN_01_2014_SECS = 1388552400;
    // public time units constants
    public static final Integer SECS_IN_HOUR = 60 * 60;
    public static final Integer SECS_IN_DAY = 60 * 60 * 24;
    public static final Integer SECS_IN_WEEK = 60 * 60 * 24 * 7;
    public static final Integer SECS_IN_YEAR = 60 * 60 * 24 * 365;
    // interval reading limits
    private static final int INTERVALREADING_DEFAULT_TEST_COUNT = 32;
    private static final int INTERVALREADING_YEAR_HOURLY_COUNT = 8760;
    // test flags: replace duplicate entries & enable DB construction
    private static final boolean REPLACE_DUPS  = true;
    private static final boolean DB_ENABLED  = true;
    private static final boolean DB_FLUSH  = false;

    // parent & broadcaster to notify parent
    private SeeActivity mParentActivity;
//    private LocalBroadcastManager mBroadcaster;
    // ESPI XML parser - parse XML feed
    private EspiXmlParser mXmlParser;

    // list & map of ESPI ReadingInterval values
    private List<EspiIntervalReading> mEspiReadingList = new ArrayList();
    private Map<Integer, EspiIntervalReading> mEspiReadingMap = new LinkedHashMap<Integer, EspiIntervalReading>();
    // ESPI database
    private EspiDbDal mEspiDbDal;

    // access via public getters/setters:
    // from/to time of feed
    private Integer mFromTimeSecs;
    private Integer mToTimeSecs;
    // valid set true after read feed completes successfully
    private boolean mValidFeed = false;
//    private boolean mValidDb = DB_ENABLED;
    // elasped time to process feed
    private long mTotalElapsedMs;
    //////////////////////////////////////////////////////////////////////////////////////////
    public FeedManager(SeeActivity parentActivity) {

        mParentActivity = parentActivity;
//        // create feed directory if not present
//        final List<String> pathList = FileUtils.getFilesList(mParentActivity.getString(R.string.espi_feed_dir), true);
//        Log.v(TAG, "readFeed pathList size: " + pathList.size());
//        for(int i = 0; i < pathList.size(); i++) {
//            Log.v(TAG, "readFeed pathList(" + i + "): " + pathList.get(i));
//        }

        // create ESPI database
        mEspiDbDal = new EspiDbDal(mParentActivity, DB_FLUSH);
        // test database
        String path = mEspiDbDal.getDbPath();
        if (path != null) {
            Log.v(TAG, "DB path: " + path);
            mValidFeed = true;
        }
        else {
            mValidFeed = false;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    // delete new feed
    public boolean deleteFeed() {

        // create ESPI database w/ flush flag true
        mEspiDbDal = new EspiDbDal(mParentActivity, true);
        // clear yearly overview
        clearOverview();
        // clear feed intermediaries
        mEspiReadingList = new ArrayList();
        mEspiReadingMap = new LinkedHashMap<Integer, EspiIntervalReading>();

        return true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // read new feed
    public boolean readFeed(final List<String> pathList) {
        boolean success = true;

        Log.v(TAG, "readFeed pathList size: " + pathList.size());
        for(int i = 0; i < pathList.size(); i++) {
            Log.v(TAG, "readFeed pathList(" + i + "): " + pathList.get(i));
        }
        if (pathList.size() <= 0) {
            Toast.makeText(mParentActivity, "Oops, no files found in " + mParentActivity.getString(R.string.espi_feed_dir) + "...", Toast.LENGTH_SHORT).show();
            return false;
        }

        // instantiate XML parser
        mXmlParser = new EspiXmlParser(mParentActivity);
        // zero progress bar
        mParentActivity.getProgressTab().setProgressBar(mXmlParser.getPercentComplete());
        Log.v(TAG, "readFeed init % complete: " + mXmlParser.getPercentComplete());

        // Start lengthy operation in a background thread
        final Thread t = new Thread(new Runnable() {
            public void run() {
                // parse XML feed
                try {
                    mXmlParser.parse(pathList, INTERVALREADING_YEAR_HOURLY_COUNT*4);
                    List<EspiXmlParser.IntervalReadingText> espiList = mXmlParser.getIntervalReadingList();
                    if (loadEspiReadingIntervals(espiList)) {
                        int mPercentComplete = 100;
                        mParentActivity.getProgressTab().setProgressBar(mPercentComplete);
                        // notify listeners
                        BroadcastUtils.broadcastResult(mParentActivity, BroadcastUtils.AHA_REFRESH, "100% complete");
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG, "Exception: " + ex.getMessage());
                }
            }
        });
        t.start();

        return success;
    }


    public boolean loadEspiReadingIntervals(List<EspiXmlParser.IntervalReadingText> espiList) {
        long elapsedMs;
        int hourCount = 0;
        int dupCount = 0;
        int alertCount = 0;
        if (espiList != null) {
            elapsedMs = System.currentTimeMillis();
            mTotalElapsedMs = elapsedMs;
            // get duplicate entry treatment setting
            String dups = SettingsActivity.getDupsTreatment(mParentActivity);
            Log.v(TAG, "loadEspiReadingIntervals: dups treatment = " + dups);

            for (EspiXmlParser.IntervalReadingText espi : espiList) {
                // convert text ReadingInterval to values
                EspiIntervalReading espiIntervalReading = new EspiIntervalReading(espi.duration,espi.start,espi.value);
                // if interval reading not already present
                if (mEspiReadingMap.get(espiIntervalReading.getStartSecs())== null) {
                    // add to list & put in map
                    mEspiReadingList.add(espiIntervalReading);
                    mEspiReadingMap.put(espiIntervalReading.getStartSecs(), espiIntervalReading);
                    ++hourCount;
                }
                else if (!dups.equals(mParentActivity.getString(R.string.pref_ignore_value))){
                    ++dupCount;
//                    Log.v(TAG, "Dup( " + dupCount + ") at " + espiIntervalReading.getStartSecs());
                    EspiIntervalReading espiExist = mEspiReadingMap.get(espiIntervalReading.getStartSecs());
                    // alert if entries differ
                    if ( !espiExist.getStartSecs().equals(espiIntervalReading.getStartSecs()) ||
                            !espiExist.getValueWh().equals(espiIntervalReading.getValueWh()) ) {
                        ++alertCount;
                        Log.e(TAG, "Alert( " + alertCount + ") of (" + dupCount + ") existing-replace time: " +
                                + espiExist.getStartSecs() + "-" + espiIntervalReading.getStartSecs() +
                                ", values: " + espiExist.getValueWh() + "-" + espiIntervalReading.getValueWh());
                        // replace existing entry
                        int espiExistInx = mEspiReadingList.indexOf(espiExist);
                        mEspiReadingList.set(espiExistInx, espiIntervalReading);
                        // replace existing entry
                        mEspiReadingMap.put(espiIntervalReading.getStartSecs(), espiIntervalReading);
                    }
                }
            }
            elapsedMs = System.currentTimeMillis() - elapsedMs;

            // if feed is not empty, clear empty flag
            if (mEspiReadingList.size() > 0) {
                mValidFeed = true;
                Log.v(TAG, "mEspiReadingList (size " + mEspiReadingList.size() + ") elapsed MS: " + elapsedMs);
                Log.v(TAG, "Total Hours: " + hourCount + ", Dups: " + dupCount + ", Total Alerts " + alertCount);
            }
            else {
                // flag empty feed
                return isValidFeed();
            }

            // tally by year, month, season, TOD
            setOverview(mEspiReadingList);

            elapsedMs = System.currentTimeMillis();
            // create ESPI DB handler
            mEspiDbDal = new EspiDbDal(mParentActivity, false);
            // create content values array for bulk insert
            dupCount = 0;
            alertCount = 0;
            int i = 0;
            int progressIndicator = mEspiReadingList.size()/5;
            int percentComplete = 50;
            int percentIncrement = 10;
//            ContentValues[] valuesArray = new ContentValues[mEspiReadingList.size()];
            List<ContentValues> valuesArray = new ArrayList();
            for (EspiIntervalReading espiIntervalReading : mEspiReadingList) {
                // if entry prior to 2014, ignore
                if (espiIntervalReading.getStartSecs().compareTo(JAN_01_2014_SECS) >= 0) {
                    // create array of content values for bulk insert
                    ContentValues values = mEspiDbDal.setValues(
                            espiIntervalReading.getDurationSecs(),
                            espiIntervalReading.getStartSecs(),
                            espiIntervalReading.getValueWh());
//                    valuesArray[i++] = values;
                    valuesArray.add(values);
                    // indicate progress
                    ++i;
                    if (i >= progressIndicator) {
                        i = 0;
                        percentComplete += percentIncrement;
                        mParentActivity.getProgressTab().setProgressBar(percentComplete);
                    }
                }
            }

            // bulk insert into ESPI database
            mEspiDbDal.bulkInsert(valuesArray);
            // individual inserts - 46 secs for 10k inserts
            elapsedMs = System.currentTimeMillis() - elapsedMs;
            Log.v(TAG, "mEspiDbDal bulk insert elapsed MS: " + elapsedMs);
//            }
            Log.v(TAG, "Total elapsed MS: " + elapsedMs);
            mTotalElapsedMs = System.currentTimeMillis() - mTotalElapsedMs;

            // fromTime is first readings
//            mFromTimeSecs = mEspiReadingList.get(0).getStartSecs();
            ContentValues cv = valuesArray.get(0);
            mFromTimeSecs = (Integer)cv.get(EspiDbHelper.FeedEntry.COLUMN_NAME_START);
            String settingFromDate = PrefUtils.getPrefsFeedFromDate(mParentActivity);
            // if incoming fromDate earlier than setting fromDate, update preferences
            if ((long)mFromTimeSecs < (ChartUtils.dateToMillis(settingFromDate, true)/1000)) {
                String fromDate = ChartUtils.secsToDate(mFromTimeSecs, false);
                PrefUtils.setPrefsFeedFromDate(mParentActivity, fromDate);
            }
            // toTime is last reading
//            mToTimeSecs = mEspiReadingList.get(mEspiReadingList.size()-1).getStartSecs();
            cv = valuesArray.get(valuesArray.size()-1);
            mToTimeSecs = (Integer)cv.get(EspiDbHelper.FeedEntry.COLUMN_NAME_START);
            String settingToDate = PrefUtils.getPrefsFeedToDate(mParentActivity);
            // if incoming toDate later than setting toDate, update preferences
            if ((long)mToTimeSecs > (ChartUtils.dateToMillis(settingToDate, true)/1000)) {
                String toDate = ChartUtils.secsToDate(mToTimeSecs, false);
                PrefUtils.setPrefsFeedToDate(mParentActivity, toDate);
            }
        }

        return isValidFeed();
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // getters
    public boolean isValidFeed() {return mValidFeed;}
//    public boolean isValidDb() {return mValidDb;}

    public EspiXmlParser getEspiXmlParser() { return mXmlParser; }

    public long getTotalElapsedMs() { return mTotalElapsedMs; }

//    //////////////////////////////////////////////////////////////////////////////////////////
//    public List getIntWhValues(Integer fromTimeSecs, Integer toTimeSecs) {
//        long elapsedMs;
//        elapsedMs = System.currentTimeMillis();
//        List<Integer> values = new ArrayList();
//        Log.v(TAG, "getWhValues from " + fromTimeSecs + " to " + toTimeSecs + " secs.");
//
//        try {
//            EspiIntervalReading espi;
//            Integer time = fromTimeSecs;
//            if ((espi = mEspiReadingMap.get(time)) != null) {
//                // mapping exists for fromTime, add to values & get next until toTime reached
//                values.add(espi.getValueWh());
////                Log.v(TAG, "getWhValues: adding first value " + espi.getValueWh());
//                while (espi != null && espi.getStartSecs() < toTimeSecs) {
//                    // bump time to next hour
//                    time += SECS_IN_HOUR;
//                    if ((espi = mEspiReadingMap.get(time)) != null) {
//                        // mapping exists for fromTime, add to values & get next until toTime reached
//                        values.add(espi.getValueWh());
////                        Log.v(TAG, "ESPI IntervalReading Map (" + values.size() + ")-> " + (int) espi.getValueWh());
//                    }
//
//                }
//                elapsedMs = System.currentTimeMillis() - elapsedMs;
//                Log.v(TAG, "mEspiReadingList getIntWhValues elapsed (map) MS: " + elapsedMs);
//                // success!
//                return values;
//            }
//            else {
//                Log.v(TAG, "getWhValues: no espi reading at " + fromTimeSecs);
//            }
//        }
//        catch (Exception ex) {
//            Log.e(TAG, "Exception: " + ex.getMessage());
//        }
//
//        // fallback to list scan
//        for (EspiIntervalReading espi : mEspiReadingList) {
//            // if espi time after specified time
//            if (espi.getStartSecs() >= fromTimeSecs && espi.getStartSecs() <= toTimeSecs) {
//                // add value & return if forCount reached
//                try {
//                    values.add((int) espi.getValueWh());
//                    Log.v(TAG, "ESPI IntervalReading List(" + values.size() + ")-> " + (int) espi.getValueWh());
//                }
//                catch (Exception ex) {
//                    Log.e(TAG, "Exception: " + ex.getMessage());
//                }
//            }
//            else if (espi.getStartSecs() > toTimeSecs) {
//                elapsedMs = System.currentTimeMillis() - elapsedMs;
//                Log.v(TAG, "mEspiReadingList getIntWhValues elapsed (list) MS: " + elapsedMs);
//                return values;
//            }
//        }
//        elapsedMs = System.currentTimeMillis() - elapsedMs;
//        Log.v(TAG, "mEspiReadingList getIntWhValues elapsed (list) MS: " + elapsedMs);
//        Log.v(TAG, values.size() + " values...");
//        return values;
//    }
    //////////////////////////////////////////////////////////////////////////////////////////
    public List queryWhValues(Integer fromTimeSecs, Integer toTimeSecs) {
//        long elapsedMs;
//        elapsedMs = System.currentTimeMillis();
//        Log.v(TAG, "queryWhValues from " + fromTimeSecs + " to " + toTimeSecs + " secs.");

        List<Integer> values = new ArrayList();
        values = mEspiDbDal.queryValues(fromTimeSecs, toTimeSecs);
//        elapsedMs = System.currentTimeMillis() - elapsedMs;
//        Log.v(TAG, "mEspiDbHelper queryWhValues elapsed MS: " + elapsedMs);
//        Log.v(TAG, values.size() + " values...");
        return values;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // tally by year, month, season, TOD
    private void clearOverview() {

        // overview tallies
        long[] tally2014byMonth = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        long[] tally2015byMonth = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        long[] tallyTod = new long[]{0, 0, 0, 0, 0, 0};

        PrefUtils.setPrefsOverviewByMonth(mParentActivity, 2014, tally2014byMonth);
        PrefUtils.setPrefsOverviewByMonth(mParentActivity, 2015, tally2015byMonth);
        PrefUtils.setPrefsOverviewByTod(mParentActivity, tallyTod);

        PrefUtils.setPrefsFeedFromDate(mParentActivity, PrefUtils.PREFS_DEFAULT_FEEDFROMDATE);
        PrefUtils.setPrefsFeedToDate(mParentActivity, PrefUtils.PREFS_DEFAULT_FEEDTODATE);

    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // tally by year, month, season, TOD
    private int setOverview(List<EspiIntervalReading> espiReadingList) {

        // overview tallies
//        long[] tally2014byMonth = new long[] {0,0,0,0,0,0,0,0,0,0,0,0};
//        long[] tally2015byMonth = new long[] {0,0,0,0,0,0,0,0,0,0,0,0};
//        long[] tallyTod = new long[] {0,0,0,0,0,0};

        long[] tally2014byMonth = PrefUtils.getPrefsOverviewByMonth(mParentActivity, 2014);
        long[] tally2015byMonth = PrefUtils.getPrefsOverviewByMonth(mParentActivity, 2015);
        long[] tallyTod = PrefUtils.getPrefsOverviewByTod(mParentActivity);

        Date date;
        int month;
        int year = 0;
        int todInx = 0;
        int todCount = 0;
        int todInterval = 24/tallyTod.length; // each 6 intervals sum 4 intervals
        for (EspiIntervalReading espiIntervalReading : mEspiReadingList) {
            // tally by month
            date = new Date(((long)espiIntervalReading.getStartSecs())*1000);
            month = date.getMonth(); // 0 for january
            year = date.getYear() + 1900;   // 114 for 2014 (year - 1900)

            if (year == 2014 || year == 2015) {
                if (year == 2014) {
                    tally2014byMonth[month] += espiIntervalReading.getValueWh();
                }
                else if (year == 2015) {
                    tally2015byMonth[month] += espiIntervalReading.getValueWh();
                }
//                else {
//                    Log.e(TAG, "setOverview unknown year: " + year);
//                }
                // tally by TOD
                tallyTod[todInx] += espiIntervalReading.getValueWh();
                // bump count within interval
                ++todCount;
                // when count with interval reaches tod interval max
                if (todCount >= todInterval) {
                    // bump tod index & clear count
                    ++todInx;
                    todCount = 0;
                }
                // at end of time periods, reset tod index to zero
                if (todInx >= tallyTod.length) todInx = 0;
            }
        }

//        int i = 0;
//        for (long m : tally2014byMonth) {
//            Log.v(TAG, " 2014 month(" + i++ + ") tally: " + m);
//        }
//        i = 0;
//        for (long m : tally2015byMonth) {
//            Log.v(TAG, " 2015 month(" + i++ + ") tally: " + m);
//        }

        PrefUtils.setPrefsOverviewByMonth(mParentActivity, 2014, tally2014byMonth);
        PrefUtils.setPrefsOverviewByMonth(mParentActivity, 2015, tally2015byMonth);
        PrefUtils.setPrefsOverviewByTod(mParentActivity, tallyTod);

        return espiReadingList.size();
    }
}
//////////////////////////////////////////////////////////////////////////////////////////
