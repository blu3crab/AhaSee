// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: JUL 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee.espi;

import android.util.Log;

//////////////////////////////////////////////////////////////////////////////////////////

/**
 * Created by mat on 4/10/2015.
 */
public class EspiIntervalReading {

    private static final String TAG = "EspiIntervalReading";

    private Integer durationSecs = -1;
    private Integer startSecs = -1;    // secs not ms!
    private Integer valueWh = -1;

    public EspiIntervalReading(String durationSecs, String startSecs, String valueWh) {
        try {
            this.durationSecs = Integer.valueOf(durationSecs);
            this.startSecs = Integer.valueOf(startSecs);
            this.valueWh = Integer.valueOf(valueWh);
        }
        catch (NumberFormatException ex) {
            this.durationSecs = -1;
            this.startSecs = -1;
            this.valueWh = -1;
            Log.e(TAG, "Exception: " + ex.getMessage());
        }
    }

    public String toString()
    {
        return "durationSecs: " + getDurationSecs() + ", start: " + getStartSecs() + ", value: " + getValueWh();
    }
    // getters/setters
    public Integer getDurationSecs() {
        return durationSecs;
    }

    public void setDurationSecs(Integer duration) {
        this.durationSecs = duration;
    }

    public Integer getStartSecs() {
        return startSecs;
    }

    public void setStartSecs(Integer start) {
        this.startSecs = startSecs;
    }

    public Integer getValueWh() {
        return valueWh;
    }

    public void setValueWh(Integer value) {
        this.valueWh = value;
    }
}
//////////////////////////////////////////////////////////////////////////////////////////
