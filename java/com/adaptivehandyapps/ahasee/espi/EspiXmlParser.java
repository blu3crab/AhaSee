// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: JUL 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee.espi;

import android.util.Log;
import android.util.Xml;

import com.adaptivehandyapps.ahasee.FileUtils;
import com.adaptivehandyapps.ahasee.SeeActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mat on 4/6/2015.
 */

public class EspiXmlParser {

    private final static String TAG = "EspiXmlParser";

    private SeeActivity mParentActivity;

    // XML parsing constants
    private static final String ns = null;

    private static final String EntryTag = "entry";
    private static final String IdTag = "id";
    private static final String LinkTag = "link";
    private static final String TitleTag = "title";
    private static final String ContentTag = "content";
    private static final String IntervalBlockTag = "IntervalBlock";
    private static final String IntervalTag = "interval";
    private static final String IntervalReadingTag = "IntervalReading";
    private static final String TimePeriodTag = "timePeriod";
    private static final String DurationTag = "duration";
    private static final String StartTag = "start";
    private static final String ValueTag = "value";

    private long mTimeMs = 0;
    private int mPercentComplete = 0;
    private List<IntervalReadingText> mIntervalReadingTextList;
    private List<String> mTitleList;

    // text IntervalReading list element
    public static class IntervalReadingText {
        public final String duration;
        public final String start;
        public final String value;

        private IntervalReadingText(String duration, String start, String value) {
            this.duration = duration;
            this.start = start;
            this.value = value;
        }
    }
    /////////////////////////////////////////////////////////////////////////////////
    // constructor: parent reference enables progress bar updates
    public EspiXmlParser(SeeActivity parentActivity) {
        mParentActivity = parentActivity;
    }
    // parse controller
    public boolean parse(List<String> pathList, int recordCount) throws XmlPullParserException, IOException {
        boolean success = false;
        // text list of reading intervals
        mIntervalReadingTextList = new ArrayList();

        for (String path: pathList) {
            InputStream bis = FileUtils.getFeed(path);
            Log.v(TAG, "Parse " + path);

            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(bis, null);
                parser.nextTag();

                readFeed(parser, mIntervalReadingTextList, recordCount);

                if (mIntervalReadingTextList != null && mIntervalReadingTextList.size() > 0) {
//                    Log.v(TAG, "Parse mIntervalReadingTextList size: " + mIntervalReadingTextList.size());
                    success = true;
                } else {
                    Log.e(TAG, "Empty feed " + path);
                }
            } finally {
                bis.close();
            }
        }
        return success;
    }
    /////////////////////////////////////////////////////////////////////////////////
    // getters
    public long getTimeMs() { return mTimeMs; }
    public int getPercentComplete() { return mPercentComplete; }
    public List<IntervalReadingText> getIntervalReadingList() { return mIntervalReadingTextList; }
    public List<String> getTitleList() { return mTitleList; }
    /////////////////////////////////////////////////////////////////////////////////
    // read feed into text IntervalReading list
    private int readFeed(
            XmlPullParser parser,
            List<IntervalReadingText> intervalReadingTextList,
            int recordCount) throws XmlPullParserException, IOException {

        mTitleList = new ArrayList();
        mTimeMs = System.currentTimeMillis();
        mPercentComplete = 0;
        mParentActivity.getProgressTab().setProgressBar(mPercentComplete);

        String duration = "nada";
        String start = "nada";
        String value = "nada";

        parser.require(XmlPullParser.START_TAG, ns, "feed");

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_DOCUMENT) {
//                System.out.println("Start document");
                continue;
            }
            else if(eventType == XmlPullParser.START_TAG) {
//                System.out.println("Start tag "+parser.getName());
                if (parser.getName().equals(DurationTag)) {
                    eventType = parser.next();
                    if(eventType == XmlPullParser.TEXT) {
//                        System.out.println("Text "+parser.getText());
                        // capture of total duration pushes processing time:
                        //    no check 3090ms
                        //    check    3480ms
                            duration = parser.getText();
                    }
                }
                else if (parser.getName().equals(StartTag)) {
                    eventType = parser.next();
                    if(eventType == XmlPullParser.TEXT) {
//                        System.out.println("Text "+parser.getText());
                        start = parser.getText();
                    }
                }
                else if (parser.getName().equals(ValueTag)) {
                    eventType = parser.next();
                    if(eventType == XmlPullParser.TEXT) {
//                        System.out.println("Text "+parser.getText());
                        value = parser.getText();

                        IntervalReadingText intervalReadingText = new IntervalReadingText(duration, start, value);
                        intervalReadingTextList.add(intervalReadingText);
//                        Log.v(TAG, "IntervalReading(" + intervalReadingList.size() + ")-> duration: " + intervalReading.duration +
//                                ", start: " +intervalReading.start + ", value: " + intervalReading.value);
                        if ((intervalReadingTextList.size()%1000) == 0) {
                            double size = (double) intervalReadingTextList.size();
                            double percent = (size/recordCount)*100.0;
                            mPercentComplete = (int)(percent);
                            Log.v(TAG, "readFeed " + mPercentComplete +"% complete.");
                            mParentActivity.getProgressTab().setProgressBar(mPercentComplete);
                        }
                        if (intervalReadingTextList.size() >= recordCount) {
                            mTimeMs = System.currentTimeMillis() - mTimeMs;
                            Log.v(TAG, "Total (" + intervalReadingTextList.size() + " records) readFeed ms: " + mTimeMs);
                            return intervalReadingTextList.size();
                        }
                    }
                }
                else if (parser.getName().equals(TitleTag)) {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.TEXT) {
//                        System.out.println("Text "+parser.getText());
                        if (mTitleList.size() < 3) {
                            mTitleList.add(parser.getText());
                        }
                    }
                }
            }
//            else if(eventType == XmlPullParser.END_TAG) {
//                System.out.println("End tag "+parser.getName());
//            }
//            else if(eventType == XmlPullParser.TEXT) {
//                System.out.println("Text "+parser.getText());
//            }
            eventType = parser.next();
        }
        // at EOF, remove last entry - aggregate total usage
        Log.v(TAG, "Removing aggregate total: " + intervalReadingTextList.get(intervalReadingTextList.size()-1).value);
        intervalReadingTextList.remove(intervalReadingTextList.size() - 1);
        mTimeMs = System.currentTimeMillis() - mTimeMs;
        Log.v(TAG, "Total (END_DOCUMENT " + intervalReadingTextList.size() + " records) readFeed ms: " + mTimeMs);

        return intervalReadingTextList.size();
    }
    /////////////////////////////////////////////////////////////////////////////////

}