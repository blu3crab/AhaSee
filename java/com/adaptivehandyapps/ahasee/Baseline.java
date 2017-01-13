// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: SEP 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Created by mat on 9/21/2015.
 */
public class Baseline implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String JSON_CONTAINER = "baseline";

    // properties
    @SerializedName("filename")
    private String filename;	    // baseline filename
    @SerializedName("algorithm")
    private String algorithm;		// algorithm used to generate baseline
    @SerializedName("days")
    private Integer days;		    // # days used to generate baseline
    @SerializedName("startSecs")
    private Integer startSecs;		    // # days used to generate baseline

    // values
    @SerializedName("values")
    private List<Integer> values;	// baseline values for each hour

    // constructor
    public Baseline() {
        this.filename = "nada";
        this.days = 0;
        this.startSecs = 0;
        this.algorithm = "nada";

        this.values = new ArrayList<Integer>();
    }

//    public String toString() {
//        return filename + ", " + algorithm + ", " + days + ", " + startSecs + ": " + values;
//    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getStartSecs() {
        return startSecs;
    }

    public void setStartSecs(Integer startSecs) {
        this.startSecs = startSecs;
    }

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }
}
