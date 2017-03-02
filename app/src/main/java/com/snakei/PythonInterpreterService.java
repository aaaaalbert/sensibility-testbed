package com.snakei;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.IBinder;
import android.util.Log;
import android.os.Process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static android.os.Process.myUid;

/**
 *Created by
 * albert.rafetseder@univie.ac.at
 * lukas.puehringer@nyu.edu
 * on 3/28/16.
 *
 * Implements an Android Service that runs in the background and starts
 * a Python interpreter in a separate thread
 *
 * Performs the following tasks:
 *  - Declares a native Java method that is implemented in C and called from
 *    the JVM using JNI
 *  - sets environment variables and passes them to native Python interpreter,
 *    e.g. the directory we want Python to write files to
 *  - Loads shared libraries python2.7 and snakei (sensor extensions)
 *  - Copies files from the assets folder to a place where Python can
 *    access them
 *
 * Note:
 * We have to decide where Python should be allowed to write files to
 * and also keep in mind that external storage can become unavailable
 * during runtime
 *
 *
 */
public class PythonInterpreterService extends Service {


    static String TAG = "PyInterpreterService"; // (sic!) len(TAG) must be < 23

    // Todo: don't hardcode here
    private static String python_home =
            "/data/data/com.sensibility_testbed/files/python";
    private static String python_path =
            "/data/data/com.sensibility_testbed/files/seattle/seattle_repy";

    private static Class[] enabled_services = {
            PythonInterpreterService0.class,
            PythonInterpreterService1.class,
            PythonInterpreterService2.class,
            PythonInterpreterService3.class,
            PythonInterpreterService4.class,
            PythonInterpreterService5.class,
            PythonInterpreterService6.class,
            PythonInterpreterService7.class,
            PythonInterpreterService8.class,
            PythonInterpreterService9.class};


    static {
        System.loadLibrary("python2.7");
        System.loadLibrary("snakei");
    }

    /*
     * Androidesque fork of new Service process
     * actually we adopt a child then spawn a new one
     *
     * Find an idle Service in the set of defined (Manifest) Service processes
     * pack the Python args into an Intent and start theService.
     *
     */
    public static void startService(String[] python_args, Context context) {
        Log.d(TAG, "Entering static startService");

        // Find a Service we can use
        Log.d(TAG, "Start searching for idle services");
        Class idle_service_class = getIdleServiceClass(context);

        // XXX LP: What should we do if there is no idle service around?
        // Try until we find one?
        // Throw an exception?
        // Return not 0 ?
        if (idle_service_class == null) {
            Log.d(TAG, "No idle service process was found (we only have 9)");
            return;
        }
        Log.d(TAG, "Found idle service process");

        Intent intent = new Intent(context, idle_service_class);

        Log.d(TAG, String.format("Starting interpreter service with args: %s", python_args));

        intent.putExtra("python_args", python_args);
        context.startService(intent);
    }

    /* XXX LP: this is a critical section, take care of it!!! */
    public static Class getIdleServiceClass(Context context) {

        ActivityManager manager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);

        // Enabled but idle service
        Class idle_service = null;

        // Currently running services reported by ActivityManager
        List<ActivityManager.RunningServiceInfo> running_services =
                manager.getRunningServices(Integer.MAX_VALUE);

        find_idle_service_loop:
        for (Class enabled_service : enabled_services) {
            for (ActivityManager.RunningServiceInfo running_service : running_services) {

                if (enabled_service.getName().equals(running_service.service.getClassName())) {
                    Log.d(TAG, String.format("Service '%s' is not idle", enabled_service.getName()));

                    // If we find the enabled service in the list of running services
                    // we can continue checking the next enabled service (outer loop)
                    continue find_idle_service_loop;
                }
            }

            // The enabled service was not found in the list of running services
            // ergo it is idle and we can use it
            idle_service = enabled_service;
            break;
        }

        return idle_service;
    }

    public native void runScript(String[] python_args,
                                 String python_home, String python_path, Context context);

    /**
     * Call into native Python Interpreter and run the Python script at the passed path
     *
     * Passes the Application Context to native method so that Python can call back into
     * Java e.g. to start a new Service Process or use Sensors
     */
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        // Log.i(this.getPackageName(), "**** UID is " + Integer.toString(myUid()));
        Log.d(TAG, "Called service onStartCommand");


        // Worker thread for Python Interpreter Process to run Python code
        new Thread(new Runnable() {
            public void run() {

                String[] python_args = intent.getStringArrayExtra("python_args");
                Log.d(TAG, String.format("Calling native runScript method with args: %s", python_args));

                runScript(python_args, python_home, python_path, getApplicationContext());

                // Once the work is done, the service should stop itself (go idle)
                // so that we can reuse it.
                // If the Service process was killed by a natively forked process,
                // it will never get back here, but there is no need to call stopSelf
                // anyway.
                //stopSelf();


                //FIXME: stopSelf appears to be just a friendly request but does not necessarily
                //FIXME: stop the service process, which we want in order to re-use it.
                //FIXME: Moreover, even if the Service is stopped, Android might keep the empty
                //FIXME: process running for performance reasons.

                //QUICKFIX: Harshly kill the process when it is done with its work
                //https://github.com/aaaaalbert/sensibility-testbed/issues/21
                //http://www.revealedtricks4u.com/2015/06/how-to-force-kill-service-programmatically-in-android.html

                Process.killProcess(Process.myPid());

            }
        }).start();

        /*
           From the docs:
           "if this service's process is killed while it is started
           (after returning from onStartCommand(Intent, int, int)),
           and there are no new start intents to deliver to it,
           then take the service out of the started state
           and don't recreate until a future explicit call"

           XXX LP:
           Although above seems to work, after I kill the process in native code the catlog says:
           schedule to restart ...
           If Android indeed tries to restart we maybe want to use flags or startId together with
           some instance variables to make sure onStartCommand is only called by us.
         */
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Don't provide means to bind this service
        return null;
    }

}
