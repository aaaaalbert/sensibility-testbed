package com.snakei;
// TODO Should we make this a proper Library Module? https://developer.android.com/tools/projects/index.html#LibraryModules

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.sensibility_testbed.SensibilityApplication;


/**
 * Created by
 * albert.rafetseder@univie.ac.at
 * lukas.puehringer@nyu.edu
 * on 5/4/16.
 *
 * A pseudo Service class that provides a static method to write a String
 * to the Android log and also as a Toast to the device's UI
 *
 * Note:
 * This and all but one of the other snakei.*Service.java classes are not real
 * Android Services:
 * They are never started as a Servicec in the app
 * But, PythonInterpreterService is a real Android Service started by the app
 * which executes this via python->c->jni
 *
 */
public class OutputService {
    static final String TAG = "OutputService";
    public static Toast toast;

    public static void logMessage(final String message) throws Exception {
        // This logs into the debug log...
        Log.i(TAG, message);

        //Also log to UI
        //XXX useful for e.g. GPS debugging where I have to carry the phone around
        final Context app_context = SensibilityApplication.getAppContext();
        // UI activity needs to run on the UI Thread (MainLooper)
        final Handler handler = new Handler(app_context.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (toast != null)
                    toast.cancel();
                toast = Toast.makeText(app_context, message, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }
}
