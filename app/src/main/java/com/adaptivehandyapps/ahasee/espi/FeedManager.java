// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: JUL 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee.espi;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.adaptivehandyapps.ahasee.BroadcastUtils;
import com.adaptivehandyapps.ahasee.FileUtils;
import com.adaptivehandyapps.ahasee.PrefUtils;
import com.adaptivehandyapps.ahasee.R;
import com.adaptivehandyapps.ahasee.ChartUtils;
import com.adaptivehandyapps.ahasee.SettingsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    // disallow prior to date
    public static final Integer JAN_01_2013_SECS = 1356998400;
    public static final Integer JAN_01_2014_SECS = 1388552400;
    public static final Integer VALID_EPOCH_START_SECS = JAN_01_2013_SECS;
    // public time units constants
    public static final Integer SECS_IN_HOUR = 60 * 60;
    public static final Integer SECS_IN_DAY = 60 * 60 * 24;
    public static final Integer SECS_IN_WEEK = 60 * 60 * 24 * 7;
    public static final Integer SECS_IN_YEAR = 60 * 60 * 24 * 365;
    public static final Integer YEAR_START = 2016;
    public static final Integer YEAR_END = 2017;
    public static final Integer YEAR_SPAN = 10; // decade
    // interval reading limits
    private static final int INTERVALREADING_DEFAULT_TEST_COUNT = 32;
    private static final int INTERVALREADING_YEAR_HOURLY_COUNT = 8760;
    // test flags: replace duplicate entries & enable DB construction
    private static final boolean REPLACE_DUPS  = true;
    private static final boolean DB_ENABLED  = true;
    private static final boolean DB_FLUSH  = false;

    // parent used as context & notify parent of progress
    private Context mContext;
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
    // elasped time to process feed
    private long mTotalElapsedMs;

    ///////////////////////////////////////////////////////////////////////////
    // interface for a callback invoked when asynctask completes
    private OnProgressCallback mCallback = null; //call back interface

    public interface OnProgressCallback {
        void onProgressCallback(int progress);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    private static volatile FeedManager instance;

    public synchronized static FeedManager getInstance(Context c, OnProgressCallback callback)
    {
        if (instance == null){
            synchronized (FeedManager.class) {   //Check for the second time.
                //if there is no instance available... create new one
                if (instance == null){
                    instance = new FeedManager(c, callback);
                }
            }
        }

        return instance;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    public FeedManager(Context c, OnProgressCallback callback) {

        mContext = c;
        mCallback = callback;

        // create ESPI database
        mEspiDbDal = new EspiDbDal(mContext, DB_FLUSH);
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
        mEspiDbDal = new EspiDbDal(mContext, true);
        // clear yearly overview
        resetYearByMonthTally();
        // clear feed intermediaries
        mEspiReadingList = new ArrayList();
        mEspiReadingMap = new LinkedHashMap<Integer, EspiIntervalReading>();

        return true;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // read new feed
    public boolean readFeed(final List<String> pathList) {
        boolean success = true;

        mEspiReadingList = new ArrayList();
        mEspiReadingMap = new LinkedHashMap<Integer, EspiIntervalReading>();

        Log.v(TAG, "readFeed pathList size: " + pathList.size());
        for(int i = 0; i < pathList.size(); i++) {
            Log.v(TAG, "readFeed pathList(" + i + "): " + pathList.get(i));
        }
        if (pathList.size() <= 0) {
            Toast.makeText(mContext, "Oops, no files found in " + FileUtils.DOWNLOAD_DIR + "...", Toast.LENGTH_SHORT).show();
            return false;
        }

        // instantiate XML parser
        mXmlParser = new EspiXmlParser(mCallback);
        // zero progress bar
        if (mCallback != null) mCallback.onProgressCallback(mXmlParser.getPercentComplete());
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
                        if (mCallback != null) mCallback.onProgressCallback(mPercentComplete);
                        // notify listeners
                        BroadcastUtils.broadcastResult(mContext, BroadcastUtils.AHA_REFRESH, "100% complete");
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
            String dups = SettingsActivity.getDupsTreatment(mContext);
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
                else if (!dups.equals(mContext.getString(R.string.pref_ignore_value))){
                    ++dupCount;
                    Log.v(TAG, "Dup( " + dupCount + ") at " + espiIntervalReading.getStartSecs());
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

            Collections.sort(mEspiReadingList, new Comparator<EspiIntervalReading>() {
                @Override
                public int compare(EspiIntervalReading o1, EspiIntervalReading o2) {
                    return o1.getStartSecs().compareTo(o2.getStartSecs());
                }
            });

            // tally by year, month, season, TOD
            setOverview(mEspiReadingList);
            tallyYearByMonth(mEspiReadingList);

            elapsedMs = System.currentTimeMillis();
            // create ESPI DB handler
            mEspiDbDal = new EspiDbDal(mContext, false);
            // create content values array for bulk insert
            int i = 0;
            int progressIndicator = mEspiReadingList.size()/5;
            int percentComplete = 50;
            int percentIncrement = 10;
            List<ContentValues> valuesArray = new ArrayList();
            for (EspiIntervalReading espiIntervalReading : mEspiReadingList) {
                // if entry prior to 2014, ignore
                if (espiIntervalReading.getStartSecs().compareTo(VALID_EPOCH_START_SECS) >= 0) {
                    // create array of content values for bulk insert
                    ContentValues values = mEspiDbDal.setValues(
                            espiIntervalReading.getDurationSecs(),
                            espiIntervalReading.getStartSecs(),
                            espiIntervalReading.getValueWh());
                    valuesArray.add(values);
                    // indicate progress
                    ++i;
                    if (i >= progressIndicator) {
                        i = 0;
                        percentComplete += percentIncrement;
                        if (mCallback != null) mCallback.onProgressCallback(percentComplete);
                    }
                }
            }

            // bulk insert into ESPI database
            mEspiDbDal.bulkInsert(valuesArray);
            // individual inserts - 46 secs for 10k inserts
            elapsedMs = System.currentTimeMillis() - elapsedMs;
            Log.v(TAG, "Total elapsed MS: " + elapsedMs);
            mTotalElapsedMs = System.currentTimeMillis() - mTotalElapsedMs;

            // fromTime is first reading
            ContentValues cv = valuesArray.get(0);
            mFromTimeSecs = (Integer)cv.get(EspiDbHelper.FeedEntry.COLUMN_NAME_START);
            String fromDate = ChartUtils.secsToDate(mFromTimeSecs, false);
            PrefUtils.setPrefsFeedFromDate(mContext, fromDate);
            // toTime is last reading
            cv = valuesArray.get(valuesArray.size()-1);
            mToTimeSecs = (Integer)cv.get(EspiDbHelper.FeedEntry.COLUMN_NAME_START);
            String toDate = ChartUtils.secsToDate(mToTimeSecs, false);
            PrefUtils.setPrefsFeedToDate(mContext, toDate);
            Log.d(TAG, "loadEspiReadingIntervals from-to date " + PrefUtils.getPrefsFeedFromDate(mContext) + "-" + PrefUtils.getPrefsFeedToDate(mContext));

            // TODO: if current to/from display dates are not valid (or defaults)
            // set display from/to date to most recent date (to)
            PrefUtils.setPrefsFromDate(mContext, toDate);
            PrefUtils.setPrefsToDate(mContext, toDate);
        }

        return isValidFeed();
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // getters
    public boolean isValidFeed() {return mValidFeed;}

    public EspiXmlParser getEspiXmlParser() { return mXmlParser; }

    public long getTotalElapsedMs() { return mTotalElapsedMs; }

    //////////////////////////////////////////////////////////////////////////////////////////
    public List queryWhValues(Integer fromTimeSecs, Integer toTimeSecs) {
        List<Integer> values = new ArrayList();
        values = mEspiDbDal.queryValues(fromTimeSecs, toTimeSecs);
        return values;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // tally by year, month, season, TOD
    private void resetYearByMonthTally() {

        // overview tallies
        long[] tallyYear = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        long[] tallyTod = new long[]{0, 0, 0, 0, 0, 0};

        // reset yearly tallies
        for (Integer year = YEAR_START; year < YEAR_START+YEAR_SPAN; year++) {
            PrefUtils.setPrefsOverviewByMonth(mContext, year, tallyYear);
            PrefUtils.setPrefsOverviewByTod(mContext, year, tallyTod);
            Log.d(TAG, "tallyYearByMonth resets year " + year.toString());
        }
        // reset start/end dates
        PrefUtils.setPrefsFeedFromDate(mContext, PrefUtils.PREFS_DEFAULT_FEEDFROMDATE);
        PrefUtils.setPrefsFeedToDate(mContext, PrefUtils.PREFS_DEFAULT_FEEDTODATE);
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // tally by year, month, season, TOD
    private int tallyYearByMonth(List<EspiIntervalReading> espiReadingList) {

        // overview tallies
        long[] tallyYear = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};;
        long[] tallyTod = new long[]{0, 0, 0, 0, 0, 0};

        Date date;
        int month;
        int year = YEAR_SPAN;
        Integer currentYear = 0;
        int todInx = 0;
        int todCount = 0;
        int todInterval = 24/tallyTod.length; // each 6 intervals sum 4 intervals
        for (EspiIntervalReading espiIntervalReading : mEspiReadingList) {
            // tally by month
            date = new Date(((long)espiIntervalReading.getStartSecs())*1000);
            month = date.getMonth(); // 0 for january
            year = date.getYear() + 1900;   // 114 for 2014 (year - 1900)

            if (year >= YEAR_START) {
                if (year != currentYear) {
                    // if not first pass
                    if (currentYear != 0) {
                        Log.d(TAG, "tallyYearByMonth sets year " + currentYear + " with tally " + tallyToString(tallyYear));
                        // save current year tally
                        PrefUtils.setPrefsOverviewByMonth(mContext, currentYear, tallyYear);
                        PrefUtils.setPrefsOverviewByTod(mContext, currentYear, tallyTod);
                    }
                    currentYear = year;
                    Log.d(TAG, "tallyYearByMonth adds year " + currentYear.toString());
                    // fetch new year tally (placeholder if initial fetch)
                    tallyYear = PrefUtils.getPrefsTallyByMonth(mContext, currentYear);
                    tallyTod = PrefUtils.getPrefsOverviewByTod(mContext, currentYear);
                }

                // tally by month
                tallyYear[month] += espiIntervalReading.getValueWh();
                // TODO: assumes full day of data - midnight to 4, 4-8, etc. confirm?
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
            else {
                Log.e(TAG,"tallyYearByMonth finds year " + year);
            }
        }
        if (currentYear != 0) {
            Log.d(TAG, "tallyYearByMonth sets year " + currentYear + " with tally " + tallyToString(tallyYear));
            // save final year
            PrefUtils.setPrefsOverviewByMonth(mContext, currentYear, tallyYear);
            PrefUtils.setPrefsOverviewByTod(mContext, currentYear, tallyTod);
        }
        return espiReadingList.size();
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    private String tallyToString(long[] tallyYear) {
        String tallyString = "";
        for (int i = 0; i < tallyYear.length; i++) {
            tallyString = tallyString.concat(Long.toString(tallyYear[i])) + " ";
        }
        return tallyString;
    }
//    //////////////////////////////////////////////////////////////////////////////////////////
//    // tally by year, month, season, TOD
//    private void resetYearByMonthTally() {
//
//        // overview tallies
//        long[] tallyYearStartbyMonth = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        long[] tallyYearEndbyMonth = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        long[] tallyTod = new long[]{0, 0, 0, 0, 0, 0};
//
//        PrefUtils.setPrefsOverviewByMonth(mContext, FeedManager.YEAR_START, tallyYearStartbyMonth);
//        PrefUtils.setPrefsOverviewByMonth(mContext, FeedManager.YEAR_END, tallyYearEndbyMonth);
//        PrefUtils.setPrefsOverviewByTod(mContext, tallyTod);
//
//        PrefUtils.setPrefsFeedFromDate(mContext, PrefUtils.PREFS_DEFAULT_FEEDFROMDATE);
//        PrefUtils.setPrefsFeedToDate(mContext, PrefUtils.PREFS_DEFAULT_FEEDTODATE);
//
//    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // tally by year, month, season, TOD
    private int setOverview(List<EspiIntervalReading> espiReadingList) {

        // overview tallies
        long[] tallyYearStartbyMonth = PrefUtils.getPrefsTallyByMonth(mContext, FeedManager.YEAR_START);
        long[] tallyYearEndbyMonth = PrefUtils.getPrefsTallyByMonth(mContext, FeedManager.YEAR_END);
//        long[] tallyTod = PrefUtils.getPrefsOverviewByTod(mContext);

        Date date;
        int month;
        int year = 0;
        int todInx = 0;
        int todCount = 0;
//        int todInterval = 24/tallyTod.length; // each 6 intervals sum 4 intervals
        for (EspiIntervalReading espiIntervalReading : mEspiReadingList) {
            // tally by month
            date = new Date(((long)espiIntervalReading.getStartSecs())*1000);
            month = date.getMonth(); // 0 for january
            year = date.getYear() + 1900;   // 114 for 2014 (year - 1900)

            if (year == FeedManager.YEAR_START || year == FeedManager.YEAR_END) {
                if (year == FeedManager.YEAR_START) {
                    tallyYearStartbyMonth[month] += espiIntervalReading.getValueWh();
                }
                else if (year == FeedManager.YEAR_END) {
                    tallyYearEndbyMonth[month] += espiIntervalReading.getValueWh();
                }
//                // tally by TOD
//                tallyTod[todInx] += espiIntervalReading.getValueWh();
//                // bump count within interval
//                ++todCount;
//                // when count with interval reaches tod interval max
//                if (todCount >= todInterval) {
//                    // bump tod index & clear count
//                    ++todInx;
//                    todCount = 0;
//                }
//                // at end of time periods, reset tod index to zero
//                if (todInx >= tallyTod.length) todInx = 0;
            }
        }
        Log.d(TAG, "setOverview finds year " + FeedManager.YEAR_START + " with tally " + tallyToString(tallyYearStartbyMonth));
        Log.d(TAG, "setOverview finds year " + FeedManager.YEAR_END + " with tally " + tallyToString(tallyYearEndbyMonth));

//        PrefUtils.setPrefsOverviewByMonth(mContext, FeedManager.YEAR_START, tallyYearStartbyMonth);
//        PrefUtils.setPrefsOverviewByMonth(mContext, FeedManager.YEAR_END, tallyYearEndbyMonth);
//        PrefUtils.setPrefsOverviewByTod(mContext, tallyTod);

        return espiReadingList.size();
    }
}
//////////////////////////////////////////////////////////////////////////////////////////
