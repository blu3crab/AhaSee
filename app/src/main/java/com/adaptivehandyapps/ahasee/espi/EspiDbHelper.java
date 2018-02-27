// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: JUL 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee.espi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Created by mat on 4/24/2015.
 */
////////////////////////////////////////////////////////////////////////////////////////////////
public class EspiDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "EspiDbHelper";

    // increment the database version when schema changes
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";

    /* Inner class that defines the table contents */
    public static abstract class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "intervalreading";
        public static final String COLUMN_NAME_DURATION = "duration";
        public static final String COLUMN_NAME_START = "start";
        public static final String COLUMN_NAME_VALUE = "value";
    }

    public static final String UNIQUE = "UNIQUE(";
    public static final String REPLACE = ") ON CONFLICT REPLACE";
    public static final String TEXT_TYPE = " TEXT";
    public static final String REAL_TYPE = " REAL";
    public static final String INTEGER_TYPE = " INTEGER";
    public static final String COMMA_SEP = ",";
//    public static final String SQL_CREATE_ENTRIES =
//            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
//                    FeedEntry._ID + " INTEGER PRIMARY KEY," +
//                    FeedEntry.COLUMN_NAME_DURATION + INTEGER_TYPE + COMMA_SEP +
//                    FeedEntry.COLUMN_NAME_START + INTEGER_TYPE + COMMA_SEP +
//                    FeedEntry.COLUMN_NAME_VALUE + INTEGER_TYPE +
//                    " )";
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedEntry.COLUMN_NAME_DURATION + INTEGER_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_START + INTEGER_TYPE + COMMA_SEP +
                    FeedEntry.COLUMN_NAME_VALUE + INTEGER_TYPE + COMMA_SEP +
                    UNIQUE + FeedEntry.COLUMN_NAME_START + REPLACE +
                    " )";

//    UNIQUE + FeedEntry.COLUMN_NAME_START + INTEGER_TYPE + REPLACE + COMMA_SEP +

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public EspiDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
}
