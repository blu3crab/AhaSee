// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: SEP 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by mat on 9/10/2015.
 */
public class DevUtils {
    private static final String TAG = "DevUtils";

    // GT-P5210 Samsung Galaxy Tablet landscape 1280/1280 x 800/775 pixel/dp, density 1.0
    // GT-P5210 Samsung Galaxy Tablet portrait 800/800 x 1280/1255 pixel/dp, density 1.0
    // SM-N900V Samsung Note Phablet landscape 1920/640 x 1080/335 pixel/dp, density 3.0
    // SM-N900V Samsung Note Phablet portrait 1080/360 x 1920/615 pixel/dp, density 3.0
    // Piranha tablet landscape 1024/1024x552/552 pixel/dp, density 1.0
    // Piranha tablet portrait 600/600x976/976 pixel/dp, density 1.0

    public static int getDevTextSize(Context context) {
        // gather screen dimensions - dp & pixels
        Configuration configuration = context.getResources().getConfiguration();
        int screenWidthDp = configuration.screenWidthDp;
        int screenHeightDp = configuration.screenHeightDp;
        Log.v(TAG, "screen w/h dp: " + screenWidthDp + ", " + screenHeightDp);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int pixelWidth = displayMetrics.widthPixels;
        int pixelHeight = displayMetrics.heightPixels;
        float density = displayMetrics.density;
        Log.v(TAG, "Pixel w,h - density: " + pixelWidth + ", " + pixelHeight + " - " + density);
        // if pixel or dp dimensions less than 600, set text size to small
        int resIdSize = R.style.TextAppearance_AppCompat_Medium;
        if (pixelWidth < 600 || pixelHeight < 600 ||
                screenWidthDp < 600 || screenHeightDp < 600) {
            resIdSize = R.style.TextAppearance_AppCompat_Small;
            Log.v(TAG, "Low pixel dimensions, setting resIdSize to small: " + resIdSize);
        }
//        Toast.makeText(context, "W x H (pixel-dp), density: " + pixelWidth + "-" + screenWidthDp + " x " +
//                pixelHeight + "-" + screenHeightDp + ", " + density, Toast.LENGTH_LONG).show();
        return resIdSize;
    }

    public static int getScreenWidthDp(Context context) {
        // gather screen dimensions
        Configuration configuration = context.getResources().getConfiguration();
        return configuration.screenWidthDp;
    }
    public static int getScreenHeightDp(Context context) {
        // gather screen dimensions
        Configuration configuration = context.getResources().getConfiguration();
        return configuration.screenHeightDp;
    }

    public static int getDisplayWidthPixels(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }
    public static int getDisplayHeightPixels(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.heightPixels;
    }
    public static float getDisplayDensity(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.density;
    }

}
