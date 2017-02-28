package com.sensibility_testbed;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.snakei.PythonInterpreterService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
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
public class SensibilityActivity extends Activity {

    public static final String TAG = "SensibilityActivity";


    // TODO This should point to an "altruistic" installer, not @lukpueh's!
    private String DEFAULT_DOWNLOAD_URL =
            "https://alpha-ch.poly.edu/cib/278d4df96687a8e1c57283fc396d5c77b6787398/installers/android";


    private String ALPHA_CIB_CERTIFICATE;
    private String FILES_ROOT;
    private String SEATTLE_ZIP;
    private int SEATTLE_RAW_RESOURCE_ID;
    private String PYTHON;
    private String PYTHON_LIB;
    private String PYTHON_SCRIPTS;

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

        URL download_url;

        // Get the user's desired download URL, if any
        EditText editText = (EditText) findViewById(R.id.url_edittext);
        String user_download_url = editText.getText().toString();

       if (user_download_url.isEmpty()) {
           Log.i(TAG, "Empty user download URL; using DEFAULT_DOWNLOAD_URL '" +
                   DEFAULT_DOWNLOAD_URL + "' instead.");
           return new URL(DEFAULT_DOWNLOAD_URL);
       } else {
           return new URL(user_download_url);
       }
    }

    private void rawResourceInstallSeattle() {
        Log.d(TAG, "Entering rawResourceInstallSeattle");

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

    private void downloadAndInstallSeattle() {
        Log.d(TAG, "Entering downloadAndInstallSeattle");

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    URL download_url = get_download_url();

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

    private void initializeSimpleLayout() {
        Log.d(TAG, "Entering initializeSimpleLayout");

        Log.d(TAG, "Setting content view to dev_layout");
        setContentView(R.layout.dev_layout);


        //Initialize buttons
        Log.d(TAG, "Initializing buttons");
        final Button btn_install_python = (Button) findViewById(R.id.install_python);
        final Button btn_install_seattle_ref = (Button) findViewById(R.id.install_seattle_referrer);
        final Button btn_install_seattle_zip = (Button) findViewById(R.id.install_seattle_zip);
        final Button btn_start = (Button) findViewById(R.id.start);
        final Button btn_kill = (Button) findViewById(R.id.kill);

        // Define listeners for buttons
        Log.d(TAG, "Defining button listeners");

        btn_install_python.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked 'Install Python' button");
                installPython();
            }
        });
        btn_install_seattle_ref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked 'Install Seattle (download)' button");
                downloadAndInstallSeattle();
            }
        });
        btn_install_seattle_zip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked 'Install Seattle (from zip)' button");
                rawResourceInstallSeattle();
            }
        });
        // Disable the button if the resource does not exist
        if (SEATTLE_RAW_RESOURCE_ID == 0) {
            btn_install_seattle_zip.setEnabled(false);
        }

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

        // If the raw resource does not exist the id is 0
        SEATTLE_RAW_RESOURCE_ID = getResources()
                .getIdentifier("seattle_android", "raw", getPackageName());

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
        Log.d(TAG, "Calling initializeSimpleLayout");
        initializeSimpleLayout();
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
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Entering onResume");
        super.onResume();
    }

}
