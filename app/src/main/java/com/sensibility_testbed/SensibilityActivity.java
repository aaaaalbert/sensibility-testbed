package com.sensibility_testbed;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.graphics.Color;


import com.snakei.PythonInterpreterService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by lukas.puehringer@nyu.edu on 7/15/16
 *
 * Main activity of Sensibility app, provides
 *
 *
 * - Installation path constants
 *
 * - Installation User Interface to trigger
 *   - Python libs installation
 *     Maybe this can be moved to the actual app installation step, because
 *     we just need to copy over static files from res/raw to internal or external
 *     storage. If not, this should happen in a background Service to not
 *     block the UI
 *
 *   - Seattle download & installation
 *     Seattle needs to be downloaded and installed during app runtime,
 *     because we receive a referrer intent from the app store that
 *     points at the appropriate seattle custom installer
 *     (containing the appropriate keys), in case the user downloaded the app
 *     using the link provided by Sensibility Testbed Custom Installer Builder.
 *     Should also happen in a background service
 *
 * - De-installation interface
 *
 * - Start/Stop Seattle interface
 *
 * - Privacy settings
 *
 *
 * Note:
 * Functionality will be successively be copied over from "ScriptActivity" to
 * finally replace it
 *
 * I think we should Fragments for each of above
 *
 */
public class SensibilityActivity extends FragmentActivity {

    public static final String TAG = "SensibilityActivity";


    // TODO This should point to an "altruistic" installer, not @lukpueh's!
     private String DEFAULT_DOWNLOAD_URL =
            "https://alpha-ch.poly.edu/cib/7e861c52f72c5e16e49b93cf60a84b4273a859d3/installers/android/";


    private String ALPHA_CIB_CERTIFICATE;
    private String FILES_ROOT;
    private String SEATTLE_ZIP;
    private String VESSEL_PATH;  /* use path to check if seattle is installed */
    private int SEATTLE_RAW_RESOURCE_ID;
    private String PYTHON;
    private String PYTHON_LIB;
    private String PYTHON_SCRIPTS;
    private boolean DEV; /* check if been to dev mode before */


    // A code to filter permission requests issued by us in the permission request callback
    public final int SENSIBILITY_RUNTIME_PERMISSIONS = 1;

    /*
     * Downloads zipped Seattle custom installer package from Clearinghouse
     * from passed url to passed destination.
     * Unzips archive to same destination and removes archive.
     *
     * Todo:
     *   Move this out of Activity to InstallerService
     *   Add download function to Utils
     *   Add progress bar and user feedback
     *   Add logging
     *   Error handling
     */


    private void installPython() {
        Log.d(TAG, "Entering installPython");
        Log.d(TAG, String.format("Unpacking python to %s", FILES_ROOT));
        try {
            Utils.unzip(getResources().openRawResource(R.raw.python_lib), FILES_ROOT, true);
        } catch (Exception e) {
            Log.d(TAG, String.format("Couldn't unpack python archive: %s", e.getMessage()));
        }
    }

    private void installSeattle() {
        Log.d(TAG, "Entering installSeattle");

        // Get free disk space
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long availableDiskSpace = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();


        String[] python_args = {
                "seattleinstaller.py", "--percent", "50",
                "--disable-startup-script",
                "--diskused", Long.toString(availableDiskSpace)};
        Log.d(TAG, String.format("Calling PythonInterpreterService.startService with args %s", python_args));

        PythonInterpreterService.startService(python_args, getBaseContext());
    }

    private URL get_download_url() throws MalformedURLException {
        /*
         * Return the download URL for the Sensibility/Repy installer.
         * It is either provided by the user through the `url_edittext`
         * textbox, or (if that is empty) taken from the hardcoded default.
         */

            // Get the user's desired download URL, if any
            EditText editText = (EditText) findViewById(R.id.url_edittext);
            String user_download_url = editText.getText().toString();

            if (user_download_url.isEmpty()) {
                Log.i(TAG, "No URL passed downloading from zip");
                Toast.makeText(getApplicationContext(),"Downloading from zip...",Toast.LENGTH_SHORT);
                return null;
            } else {
                Log.i(TAG, "Downloading from LINK: "+user_download_url);
                Toast.makeText(getApplicationContext(),"Downloading from: "+user_download_url,Toast.LENGTH_SHORT);
                return new URL(user_download_url);
            }
    }

    private void rawResourceInstallSeattle() {
        Log.d(TAG, "Entering rawResourceInstallSeattle");

        if(SEATTLE_RAW_RESOURCE_ID == 0){
            Log.d(TAG,"Could not download from zip");
            Toast.makeText(getApplicationContext(),"Could not download from zip",Toast.LENGTH_SHORT);
            return;
        }

        Log.d(TAG, String.format("Unpacking seattle from raw resources to %s", FILES_ROOT));
        try {
            // The raw resource might not exist
            Utils.unzip(getResources().openRawResource(SEATTLE_RAW_RESOURCE_ID), FILES_ROOT, true);
        } catch (Exception e) {
            Log.d(TAG, String.format("Could not unpack seattle: %s", e.getMessage()));
            Log.d(TAG, "Aborting installation");
            return;
        }
        installSeattle();
    }

    private void downloadAndInstallSeattle(final URL download_url) {
        Log.d(TAG, "Entering downloadAndInstallSeattle");

        Thread t = new Thread() {
            @Override
            public void run() {
                try {

                    if(download_url == null){
                        rawResourceInstallSeattle();
                        return;
                    }

                    Log.d(TAG, String.format("Downloading installer from %s to %s", download_url.toString(), SEATTLE_ZIP));
                    // Download seattle installer package and unpack to internal storage
                    InputStream input;
                    OutputStream output;

                    HttpsURLConnection connection;
                    connection = (HttpsURLConnection) download_url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                        Log.d(TAG, String.format("Connection failed, Code: %d, Message: %s",
                                connection.getResponseCode(), connection.getResponseMessage()));
                        Log.d(TAG, "Aborting installation");
                        Toast.makeText(getApplicationContext(),"Could not download from URL!",Toast.LENGTH_SHORT);
                        return;
                    }

                    input = connection.getInputStream();
                    output = new FileOutputStream(SEATTLE_ZIP);

                    byte data[] = new byte[4096];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                    if (input != null) {
                        input.close();
                    }
                    if (output != null) {
                        output.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                    Log.d(TAG, String.format("Unpacking downloaded seattle to %s", FILES_ROOT));

                    Utils.unzip(new FileInputStream(SEATTLE_ZIP), FILES_ROOT, true);

                } catch (Exception e) {
                    Log.d(TAG, String.format("Download failed: %s", e.getMessage()));
                    Log.d(TAG, "Aborting installation");
                    return;
                }
                installSeattle();
            }
        };
        t.start();
    }

    private void startSeattle() {
        Log.d(TAG, "Entering startSeattle");
        String[] python_args = {"nmmain.py", "--foreground"};
        Log.d(TAG, String.format(
                "Calling PythonInterpreterService.startService with args %s", python_args));
        PythonInterpreterService.startService(python_args, getBaseContext());
    }

    private void killSeattle() {
        Log.d(TAG, "Killing not implemented");
    }


    private void initializeButtons() {
        Log.d(TAG, "Entering initializing buttons");

        if(!DEV){ /* check if been to dev mode before (if not then setup the buttons */
            final Button btn_install_seattle = (Button) findViewById(R.id.install_seattle);
            final Button btn_start = (Button) findViewById(R.id.start);
            final Button btn_kill = (Button) findViewById(R.id.kill);

            // Define listeners for buttons
            Log.d(TAG, "Defining button listeners");

            btn_install_seattle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Clicked 'Install Seattle (download)' button");
                    try {
                        URL downloadFrom = get_download_url();
                        downloadAndInstallSeattle(downloadFrom);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            });

            btn_start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Clicked 'Start' button");
                    startSeattle();
                }
            });
            btn_kill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Clicked 'Stop' button");
                    killSeattle();
                }
            });

        }

    }


    /*
     * Check every required permission and request in case we don't have it yet
     * In API > 23 we have to ask for permissions at runtime and permissions can also be revoked
     * But we cannot (verify this) request permissions from a background service process, therefor
     * I suggest we request permissions when the activity starts and just pass on the exceptions
     * in the services (if a needed permission was not granted or revoked)
     */
    protected void checkRequestPermissions() {
        Log.d(TAG, "Entering checkRequestPermissions");
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        };
        for (String permission : permissions) {
            Log.d(TAG, String.format("Checking permission %s", permission));
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, String.format("Requesting permission %s", permission));
                ActivityCompat.requestPermissions(
                        this, new String[]{permission}, SENSIBILITY_RUNTIME_PERMISSIONS);
            } else {
                Log.d(TAG, String.format("Permission %s already granted", permission));
            }
        }
    }

/*    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Entering onRequestPermissionsResult");
        switch (requestCode) {
            case SENSIBILITY_RUNTIME_PERMISSIONS: {
                Log.d(TAG, "Permission was requested by Sensibility");

                Log.d(TAG, String.format("Requested %d permission(s)", permissions.length));
                Log.d(TAG, String.format("Received %d permission request result(s)", grantResults.length));

                for (int grantResult : grantResults){
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        Log.d(TAG, "Permission denied by user");

                    } else if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Permission granted by user");
                    } else {
                        Log.d(TAG, "Permission result unknown");
                    }
                }
//                if (grantResults.length <= 0 || permissions.length <= 0 ||
//                        grantResults[0] == PackageManager.PERMISSION_DENIED) {
//
//                }
            }
        }
        return;
    }*/

    /*
     * Initialize "constant" global paths which cannot be initialized in class scope because they
     * need a context.
     */
    private void initializePaths() {
        Log.d(TAG, "Entering initializePaths");
        FILES_ROOT = getApplicationContext().getFilesDir().getPath() + "/";
        SEATTLE_ZIP = FILES_ROOT + "seattle_android.zip";
        PYTHON = FILES_ROOT + "python/";
        PYTHON_LIB = FILES_ROOT + "python/lib/python2.7/";
        PYTHON_SCRIPTS = FILES_ROOT + "scripts/";
        VESSEL_PATH = FILES_ROOT + "seattle/seattle_repy/v1/";

        // If the raw resource does not exist the id is 0
        SEATTLE_RAW_RESOURCE_ID = getResources()
                .getIdentifier("seattle_android", "raw", getPackageName());

    }



    /*
     * method to check if user needs to install any required packages and installs it
     *
    */
    private void installRequired(){
        Log.d(TAG,"Checking install requirements");

        final TextView pySet = (TextView) findViewById(R.id.pythonSetup);
        final TextView seattleSet = (TextView) findViewById(R.id.seattleSetup);
        final TextView nodeMan = (TextView) findViewById(R.id.nodeRunning);

        /* setup progress spinner */
        final ProgressDialog progress = new ProgressDialog(this); /* set the context to the app's context */
        progress.setMessage("Checking for requirements...");      /* set a default message */
        progress.setIndeterminate(true);                          /* no total percent just spinning */
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setCancelable(false);                            /* can't close the dialog */
        progress.show();                                          /* show the progress dialog */


        /* thread to  install components and update the progress dialog */
        Thread requireThread = new Thread() {
            @Override
            public void run() {


                /* check if python was installed */
                Log.d(TAG,"Checking if Python installed");
                File pyZip = new File(PYTHON);
                if(! pyZip.isDirectory() || !pyZip.exists()){
                    /* run UI thread for installing component and updating dialog */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Setting up python..."); /* update dialog */
                            installPython();                             /* install python */
                        }
                    });
                    try {
                        Thread.sleep(1000);                          /* set a 1 second sleep */
                    }
                    catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pySet.setText("\u2713 Python Installed");
                        pySet.setTextColor(Color.GREEN);
                    }
                });


                /* check if seattle was installed */
                Log.d(TAG,"Checking if Seattle installed");
                File seattleDir = new File(VESSEL_PATH);
                if(! seattleDir.isDirectory() || !seattleDir.exists()) {
                    /* run UI thread for installing seattle and updating dialog */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Setting up seattle..");   /* update dialog */
                            try {
                                URL def = new URL(DEFAULT_DOWNLOAD_URL);
                                downloadAndInstallSeattle(def);                    /* install seattle from default link */
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    try {
                        Thread.sleep(1000);                             /* set 1 second sleep */
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        seattleSet.setText("\u2713 Seattle Installed");
                        seattleSet.setTextColor(Color.GREEN);
                    }
                });

                /* run UI thread for installing seattle and updating dialog */
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setMessage("Starting Seattle...");   /* update dialog */
                        startSeattle();                    /* start seattle after installing */
                    }
                });
                try {
                    Thread.sleep(1000);                             /* set 1 second sleep */
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        nodeMan.setText("\u2713 Nodemanager running");
                        nodeMan.setTextColor(Color.GREEN);
                    }
                });
                progress.dismiss(); /* close progress dialog at the end */
            }
        };
        requireThread.start(); /* start the thread */


    }

    private void setupTabLayout(){
        Log.d(TAG,"setting up tab layout xml");
        setContentView(R.layout.tabs); /* set the xml file */

        Log.d(TAG,"setting up tab host");
        final TabHost tabhost = (TabHost) findViewById(android.R.id.tabhost); /* setup tab host */
        tabhost.setup();

        Log.d(TAG,"adding tabs to layout");
        TabHost.TabSpec ts = tabhost.newTabSpec("tag1");
        ts.setContent(R.id.home); /* set the home tab */
        ts.setIndicator("Home");
        tabhost.addTab(ts);
        ts = tabhost.newTabSpec("tag2");
        ts.setContent(R.id.manual);
        ts.setIndicator("Manual");
        tabhost.addTab(ts);
        installRequired();

        Log.d(TAG,"add tab listeners");
        tabhost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

            @Override
            public void onTabChanged(String tabId) {

                int i = tabhost.getCurrentTab();
                Log.d("Clicked tab number: ", ": " + i);

                if (i == 0) {
                    installRequired();
                    Log.i("Inside onClick tab 0", "Home Tab");

                }
                else if (i ==1) {
                    initializeButtons();
                    DEV = true;
                    Log.i("Inside onClick tab 1", "Manual tab");

                }

            }
        });

    }



    /*
     * Shows the User Interface
     * Called by system,
     *   - onCreate()
     *   - onRestart()
     */
    @Override
    protected void onStart() {
        Log.d(TAG, "Entering onStart");
        super.onStart();
        Log.d(TAG, "Calling checkRequestPermissions");
        checkRequestPermissions();
        Log.d(TAG, "Calling setupTabLayout");
    }

    /*
     * Sets the user interface
     *
     * Called by system after:
     *  - Activity launched
     *  - App process was killed and user navigates back to the activity
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Entering onCreate");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Calling initializePaths");
        initializePaths();
        DEV = false; /* set dev mode to false to start */
        setupTabLayout();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Entering onResume");
        super.onResume();
    }

}
