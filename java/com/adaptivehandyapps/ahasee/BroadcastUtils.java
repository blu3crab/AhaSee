// Project: AHA Smart Energy Explorer
// Contributor(s): M.A.Tucker
// Origination: SEP 2015
// Copyright 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.ahasee;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by mat on 9/3/2015.
 */
public class BroadcastUtils {

    static final public String AHA_REFRESH = "com.adaptivehandyapps.ahasee.REQUEST_REFRESH";

    static final public String AHA_MESSAGE = "com.adaptivehandyapps.ahasee.MESSAGE";

    //////////////////////////////////////////////////////////////////////////////////////////
    // notify listeners of result
    public static void broadcastResult(Context context, String request, String message) {
        // instantiate broadcaster
        LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(context);
        Intent intent = new Intent(request);
        if(message != null)
            intent.putExtra(AHA_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

}
