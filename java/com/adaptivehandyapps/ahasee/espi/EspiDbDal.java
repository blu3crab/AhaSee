// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: JUL 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee.espi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.InflateException;

import com.adaptivehandyapps.ahasee.PrefUtils;
import com.adaptivehandyapps.ahasee.R;
import com.adaptivehandyapps.ahasee.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mat on 4/24/2015.
 */
////////////////////////////////////////////////////////////////////////////////////////////////
public class EspiDbDal {
    private static final String TAG = "EspiDbDal";

    // parent context
    private Context mParentContext;
    // ESPI database
    private EspiDbHelper mEspiDbHelper;

    private SQLiteDatabase mDb;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public EspiDbDal(Context context, boolean flush) {
        mParentContext = context;
        mEspiDbHelper = new EspiDbHelper(mParentContext);
        if (flush) {
            mDb = openDb();
            deleteDb(mDb);
            createDb(mDb);
            closeDb(mDb);
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private SQLiteDatabase openDb()
    {
        return mEspiDbHelper.getWritableDatabase();
    }
    private void deleteDb(SQLiteDatabase db)
    {
        Log.v(TAG, "deleteDb: " + EspiDbHelper.SQL_DELETE_ENTRIES);
        db.execSQL(EspiDbHelper.SQL_DELETE_ENTRIES);
    }
    private void createDb(SQLiteDatabase db)
    {
        Log.v(TAG, "createDb: " + EspiDbHelper.SQL_CREATE_ENTRIES);
        db.execSQL(EspiDbHelper.SQL_CREATE_ENTRIES);
    }
    private void closeDb(SQLiteDatabase db)
    {
        db.close();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public String getDbPath() {
        mDb = openDb();
        String path = mDb.getPath();
        closeDb(mDb);
        return path;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public long insert(Integer duration, Integer start, Integer value) {
        // Gets the data repository in write mode
        SQLiteDatabase db = mEspiDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(EspiDbHelper.FeedEntry.COLUMN_NAME_DURATION, duration);
        values.put(EspiDbHelper.FeedEntry.COLUMN_NAME_START, start);
        values.put(EspiDbHelper.FeedEntry.COLUMN_NAME_VALUE, value);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = 0;
        newRowId = db.insert(EspiDbHelper.FeedEntry.TABLE_NAME, "null", values);
        return newRowId;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
//    public int bulkInsert(ContentValues[] valuesArray) {
    public int bulkInsert(List<ContentValues> valuesArray) {
        // Gets the data repository in write mode
        SQLiteDatabase db = mEspiDbHelper.getWritableDatabase();
        // check ignore/replace setting, default to replace
        int conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE;
        if (SettingsActivity.getDupsTreatment(mParentContext).equals(mParentContext.getString(R.string.pref_ignore_value))) {
            conflictAlgorithm = SQLiteDatabase.CONFLICT_IGNORE;
            Log.v(TAG, "bulkInsert ignoring dups.");
        }
        // start db transaction
        db.beginTransaction();
        try {
            // insert each row
            for (ContentValues values : valuesArray) {
                // prevent duplicate entries
//                db.insert(EspiDbHelper.FeedEntry.TABLE_NAME, "null", values);
//                db.insertWithOnConflict(EspiDbHelper.FeedEntry.TABLE_NAME, "null", values, SQLiteDatabase.CONFLICT_REPLACE);
                db.insertWithOnConflict(EspiDbHelper.FeedEntry.TABLE_NAME, "null", values, conflictAlgorithm);
            }
            db.setTransactionSuccessful();
        }
        catch (Exception e) {
            Log.e(TAG, "bulkInsert exception: " + e.getMessage());
        }
        finally {
            db.endTransaction();
            db.close();
        }
//        return valuesArray.length;
        return valuesArray.size();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public List<Integer> queryValues(Integer fromTimeSecs, Integer toTimeSecs) {

//        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteDatabase db = mEspiDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                EspiDbHelper.FeedEntry._ID,
                EspiDbHelper.FeedEntry.COLUMN_NAME_DURATION,
                EspiDbHelper.FeedEntry.COLUMN_NAME_START,
                EspiDbHelper.FeedEntry.COLUMN_NAME_VALUE
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                EspiDbHelper.FeedEntry.COLUMN_NAME_START + " ASC";

//        String whereClause = null;
        String whereClause = EspiDbHelper.FeedEntry.COLUMN_NAME_START + ">=" + fromTimeSecs.toString() + " AND " +
                EspiDbHelper.FeedEntry.COLUMN_NAME_START + "<=" + toTimeSecs.toString();
        String [] whereArgs = null;
//        String whereClause = FeedEntry.COLUMN_NAME_START+">=?";
//        String [] whereArgs = {fromTimeSecs.toString()};

        Cursor cursor = db.query(
                EspiDbHelper.FeedEntry.TABLE_NAME,  // table to query
                projection,            // columns to return
                whereClause,           // columns for the WHERE clause
                whereArgs,             // values for the WHERE clause
                null,                  // don't group the rows
                null,                  // don't filter by row groups
                sortOrder              // sort order
        );

        List<Integer> values = new ArrayList();

        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            values.add(cursor.getInt(3));
            cursor.moveToNext();
        }

        cursor.close();
        db.close();

        return values;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public ContentValues setValues(Integer duration, Integer start, Integer value) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(EspiDbHelper.FeedEntry.COLUMN_NAME_DURATION, duration);
        values.put(EspiDbHelper.FeedEntry.COLUMN_NAME_START, start);
        values.put(EspiDbHelper.FeedEntry.COLUMN_NAME_VALUE, value);
        return values;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
}
