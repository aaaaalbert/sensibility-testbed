package com.snakei;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lukas.puehringer@nyu.edu
 * on  6/3/16.
 *
 * A pseudo Service that facades miscellaneous Android Info calls
 *
 * Provides info methods for
 *   - WiFi
 *   - Cellular network
 *   - Bluetooth
 *   - Battery
 *   - Device settings
 *
 * Complex info objects are returned as serialized JSON
 * Primitive data types are returned as they are
 *
 * This class is a Singleton using the thread safe
 * Java Initialization on Demand Holder pattern
 * (cf. SensorService.java for more info )
 *
 * Exceptions should be thrown, save for SecurityExceptions which we
 * catch and present to the sandbox by simply not passing down the
 * disallowed sensor value.
 *
 * For example, if the sandbox calls `get_foo()` but the user didn't
 * give their permission for the app to access Foo, the sandbox will
 * see a return value of `None`. For more complex sensors, single values
 * will be missing from the returned list or dict.
 *
 */
public class MiscInfoService {
    static final String TAG = "MiscInfoService";

    Context cached_context;

    // CRUD query tool used for display and mode settings
    // which are stored in content providers
    ContentResolver content_resolver;

    // Used for cellular network info
    ConnectivityManager connectivity_manager;
    TelephonyManager telephony_manager;

    // Used for wifi scanning and info
    WifiManager wifi_manager;
    BroadcastReceiver wifi_broadcast_receiver;
    Object wifi_sync;

    // Used for audio info
    AudioManager audio_manager;
    // Used for display info
    DisplayManager display_manager;

    // Used for bluetooth scanning and info
    BluetoothManager bluetooth_manager;
    BluetoothAdapter bluetooth_adapter;
    Object bluetooth_sync;
    BroadcastReceiver bluetooth_broadcast_receiver;
    ArrayList<BluetoothDevice> scanned_bluetooth_devices;


    /* See Initialization on Demand Holder pattern */
    private static class MiscInfoServiceHolder {
        private static final MiscInfoService instance = new MiscInfoService();
    }


    /* Classic Singleton Instance Getter */

    public static MiscInfoService getInstance(){
        return MiscInfoServiceHolder.instance;
    }

    /*
     * Initializer
     *
     * Caches passed app context
     * Initializes required managers
     * Initializes BroadCastReceiver used for WiFi scanning
     * Initializes BroadCastReceiver used for Bluetooth scanning
     *
     * XXX LP:
     * We maybe want to make this thread
     * We maybe want to add a "initialized" getter where we throw an exception if the singleton
     * has not been initialized yet, or we simply initialize there on the spot
     */
    public void init(Context context) {
        Log.d(TAG, "Entering init");


        cached_context = context;
        Log.d(TAG, "Requesting android connectivity system service");
        content_resolver = cached_context.getContentResolver();
        connectivity_manager = (ConnectivityManager) cached_context.getSystemService(
                cached_context.CONNECTIVITY_SERVICE);
        Log.d(TAG, "Requesting android telephony system service");
        telephony_manager = (TelephonyManager) cached_context.getSystemService(
                cached_context.TELEPHONY_SERVICE);
        Log.d(TAG, "Requesting android audio system service");
        audio_manager = (AudioManager)cached_context.getSystemService(
                cached_context.AUDIO_SERVICE);
        Log.d(TAG, "Requesting android display system service");
        display_manager = (DisplayManager)cached_context.getSystemService(
                cached_context.DISPLAY_SERVICE);
        Log.d(TAG, "Requesting android wifi system service");
        wifi_manager = (WifiManager) cached_context.getSystemService(
                cached_context.WIFI_SERVICE);
        Log.d(TAG, "Requesting android bluetooth system service");
        bluetooth_manager = (BluetoothManager)cached_context.getSystemService(
                cached_context.BLUETOOTH_SERVICE);

        Log.d(TAG, "Registering wifi broadcast receiver");
        wifi_sync = new Object();
        wifi_broadcast_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Called wifi broadcast receiver onReceive");
                synchronized(wifi_sync) {
                    // Discovery has finished, unregister receiver
                    cached_context.unregisterReceiver(wifi_broadcast_receiver);
                    wifi_sync.notify();
                }
            }
        };

        Log.d(TAG, "Registering bluetooth broadcast receiver");
        bluetooth_adapter = bluetooth_manager.getAdapter();
        bluetooth_sync = new Object();
        bluetooth_broadcast_receiver = new BroadcastReceiver() {
            // This BroadcastReceiver receives bluetooth discovery actions
            // for page requests (ACTION_FOUND) and when discovery and all page
            // requests have finished (ACTION_DISCOVERY_FINISHED)
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Called bluetooth broadcast receiver onReceive");
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Bluetooth discovery has received info
                    // from a paged remote device
                    BluetoothDevice remote_device = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE);
                    scanned_bluetooth_devices.add(remote_device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(
                        action)) {
                    // Bluetooth discovery has finished, there won't be any
                    // more page requests, unregister receiver and notify
                    // scan info function to stop waiting and return values
                    synchronized (bluetooth_sync){
                        cached_context.unregisterReceiver(
                                bluetooth_broadcast_receiver);
                        bluetooth_sync.notify();
                    }
                }
            }
        };
    }


    /*
     * Returns network info (WiFi, GPRS, UMTS, etc.)
     * "is_connected" identifies networks the device is currently connected to
     *
     * @return  String serialized JSON Array
     * e.g.:
     * [
     *   {
     *     'is_failover':False,
     *     'is_connected':True,
     *     'type_name':'WIFI',
     *     'detailed_state':'CONNECTED',
     *     'is_roaming':False,
     *     'subtype_name':'',
     *     'subtype':0,
     *     'state':'CONNECTED',
     *     'is_available':True,
     *     'is_connected_or_connecting':True,
     *     'type':1, # cf. developer.android.com/reference/android/net/ConnectivityManager.html
     *     'extra_info':'"eduroam"'
     *   }, ...
     * ]
     *
     */
    public String getNetworkInfo() throws JSONException {
        Network[] networks;
        Log.d(TAG, "Entering getNetworkInfo");

        try {
            networks = connectivity_manager.getAllNetworks();
        } catch (SecurityException e)
                // We are lacking the ACCESS_NETWORK_STATE permission.
        {
            return null;
        }

        if (networks.length > 0) {
            JSONArray network_info_json_array = new JSONArray();
            for (Network network : networks) {
                JSONObject network_info_json = new JSONObject();
                NetworkInfo network_info = connectivity_manager
                        .getNetworkInfo(network);
                network_info_json.put("detailed_state",
                        network_info.getDetailedState().name());
                network_info_json.put("extra_info",
                        network_info.getExtraInfo());
                network_info_json.put("reason",
                        network_info.getReason());
                network_info_json.put("state",
                        network_info.getState().name());
                network_info_json.put("subtype", network_info.getSubtype());
                network_info_json.put("subtype_name",
                        network_info.getSubtypeName());
                network_info_json.put("type", network_info.getType());
                network_info_json.put("type_name", network_info.getTypeName());
                network_info_json.put("is_connected",
                        network_info.isConnected());
                network_info_json.put("is_available",
                        network_info.isAvailable());
                network_info_json.put("is_connected_or_connecting",
                        network_info.isConnectedOrConnecting());
                network_info_json.put("is_failover",
                        network_info.isFailover());
                network_info_json.put("is_roaming", network_info.isRoaming());

                String type =  network_info.getTypeName();
                type.replace("_"," ");
                network_info_json.put("show_name",type);

                network_info_json_array.put(network_info_json);
            }
            return network_info_json_array.toString();
        }
        return null;
    }

    /*
     * Returns cellular provider info
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {
     * 'network_operator_name': 'AT&T',
     * 'network_operator': '310410'
     * }
     */
    public String getCellularProviderInfo() throws JSONException {
        Log.d(TAG, "Entering getCellularProviderInfo");
        JSONObject provider_info_json = new JSONObject();

        provider_info_json.put("network_operator",
                telephony_manager.getNetworkOperator());
        provider_info_json.put("network_operator_name",
                telephony_manager.getNetworkOperatorName());

        return provider_info_json.toString();
    }


    /*
     * Returns all observed cell information from all radios on
     * the device including the primary and neighboring cells.
     *
     * Attributes differ from network to network (CDMA, LTE, GSM, WCDMA)
     *
     * @return  String serialized JSON Array
     * e.g. (LTE)
     * [{
     *   'ci':28435727,
     *   'asu_level':97,
     *   'dbm':101,
     *   'level':2,
     *   'mcc':310,
     *   'mnc':410,
     *   'pci':144,
     *   'is_registered':True,
     *   'tac':2313,
     *   'timing_advance':4
     * }, ...]
     *
     */
    public String getCellInfo() throws JSONException {
        Log.d(TAG, "Entering getCellInfo");

        List<CellInfo> cell_infos;

        try {
            cell_infos = telephony_manager.getAllCellInfo();
            // Older devices may return null
            // Android refs suggest to use getCellLocation as alternative
            // I think we should not add this here but rather add an additional extension if we
            // want to provide that call.
            if (cell_infos != null && cell_infos.size() > 0) {
                JSONArray cell_info_json_array = new JSONArray();
                for (CellInfo cell_info : cell_infos) {
                    JSONObject cell_info_json = new JSONObject();
                    cell_info_json.put("is_registered", cell_info.isRegistered());
                    if (cell_info instanceof CellInfoCdma) {

                        // CDMA Signal Strength
                        CellSignalStrengthCdma signal_strength = ((CellInfoCdma)
                                cell_info).getCellSignalStrength();
                        cell_info_json.put("asu_level",
                                signal_strength.getAsuLevel());
                        cell_info_json.put("cdma_dbm",
                                signal_strength.getCdmaDbm());
                        cell_info_json.put("cdma_level",
                                signal_strength.getCdmaLevel());
                        cell_info_json.put("dbm", signal_strength.getDbm());
                        cell_info_json.put("evdo_dbm",
                                signal_strength.getEvdoDbm());
                        cell_info_json.put("evdo_ecio",
                                signal_strength.getEvdoEcio());
                        cell_info_json.put("evdo_level",
                                signal_strength.getEvdoLevel());
                        cell_info_json.put("evdo_snr",
                                signal_strength.getEvdoSnr());
                        cell_info_json.put("level", signal_strength.getLevel());

                        //CDMA Cell Identity
                        CellIdentityCdma cell_id = ((CellInfoCdma)
                                cell_info).getCellIdentity();

                        cell_info_json.put("base_station_id",
                                cell_id.getBasestationId());
                        cell_info_json.put("base_station_latitude",
                                cell_id.getLatitude());
                        cell_info_json.put("base_station_longitude",
                                cell_id.getLongitude());
                        cell_info_json.put("network_id",
                                cell_id.getNetworkId());
                        cell_info_json.put("system_id",
                                cell_id.getSystemId());

                    } else if (cell_info instanceof CellInfoLte) {
                        // LTE Signal Strength
                        CellSignalStrengthLte signal_strength = ((CellInfoLte)
                                cell_info).getCellSignalStrength();
                        cell_info_json.put("dbm",
                                signal_strength.getDbm());
                        cell_info_json.put("asu_level",
                                signal_strength.getAsuLevel());
                        cell_info_json.put("level",
                                signal_strength.getLevel());
                        cell_info_json.put("timing_advance",
                                signal_strength.getTimingAdvance());

                        //LTE Cell Identity
                        CellIdentityLte cell_id = ((CellInfoLte)
                                cell_info).getCellIdentity();
                        cell_info_json.put("ci", cell_id.getCi());
                        cell_info_json.put("mcc", cell_id.getMcc());
                        cell_info_json.put("mnc", cell_id.getMnc());
                        cell_info_json.put("pci", cell_id.getPci());
                        cell_info_json.put("tac", cell_id.getTac());

                    } else if (cell_info instanceof CellInfoGsm) {
                        // GSM Signal Strength
                        CellSignalStrengthGsm signal_strength = ((CellInfoGsm
                                ) cell_info).getCellSignalStrength();
                        cell_info_json.put("asu_level",
                                signal_strength.getAsuLevel());
                        cell_info_json.put("dbm", signal_strength.getDbm());
                        cell_info_json.put("level", signal_strength.getLevel());

                        //GSM Cell Identity
                        CellIdentityGsm cell_id = ((CellInfoGsm)
                                cell_info).getCellIdentity();
                        cell_info_json.put("mnc", cell_id.getMnc());
                        cell_info_json.put("mcc", cell_id.getMcc());
                        cell_info_json.put("cid", cell_id.getCid());
                        cell_info_json.put("lac", cell_id.getLac());

                    } else if (cell_info instanceof CellInfoWcdma) {
                        // WCDMA Signal Strength
                        CellSignalStrengthWcdma signal_strength = ((CellInfoWcdma
                                ) cell_info).getCellSignalStrength();
                        cell_info_json.put("asu_level", signal_strength.getAsuLevel());
                        cell_info_json.put("dbm", signal_strength.getDbm());
                        cell_info_json.put("level", signal_strength.getLevel());

                        //WCDMA Cell Identity
                        CellIdentityWcdma cell_id = ((CellInfoWcdma)
                                cell_info).getCellIdentity();
                        cell_info_json.put("psc", cell_id.getPsc());
                        cell_info_json.put("cid", cell_id.getCid());
                        cell_info_json.put("lac", cell_id.getLac());
                        cell_info_json.put("mcc", cell_id.getMcc());
                        cell_info_json.put("mnc", cell_id.getMnc());

                    } else {
                        // XXX Throw an exception?
                        Log.wtf(TAG, "Cell info of unknown Type");
                        continue;
                    }
                    cell_info_json_array.put(cell_info_json);
                }
                return cell_info_json_array.toString();
            }
        } catch (SecurityException e) {
            // We are missing the ACCESS_COARSE_LOCATION permission.
        }
        return null;

    }


    /*
     * Returns info about Subscriber Identity Module (SIM)
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {
     * 'SIM_country_code': 'us',
     * 'SIM_operator_name': '',
     * 'SIM_operator': '310410',
     * 'SIM_serial_number':
     * '89014103278902860330',
     * 'SIM_state': 5
     * }
     *
     */
    public String getSimInfo() throws JSONException {
        Log.d(TAG, "Entering getSimInfo");

        JSONObject sim_info_json = new JSONObject();

        sim_info_json.put("SIM_operator", telephony_manager.getSimOperator());
        sim_info_json.put("SIM_country_code",
                telephony_manager.getSimCountryIso());
        sim_info_json.put("SIM_operator_name",
                telephony_manager.getSimOperatorName());
        sim_info_json.put("SIM_state", telephony_manager.getSimState());

    // This is the only permissions-secured telephony_manager call in here,
    // but it's so privacy invasive that I remove it altogether.
    //        try {
    //            sim_info_json.put("SIM_serial_number",
    //                   telephony_manager.getSimSerialNumber());
    //
    //        } catch (SecurityException e) {
    //            // We are missing the READ_PHONE_STATE permission.
    //        }

        return sim_info_json.toString();
    }


    /*
     * Returns info about the phone
     *
     * @return  String serialized JSON Object
     * e.g.
     * {
     * 'phone_type': 1,
     * 'subscriber_id': '310410890286033',
     * 'data_activity': 0,
     * 'call_state': 0,
     * 'data_state': 0,
     * 'device_software_version': '04',
     * 'network_type': 13,
     * 'device_id': '356266070625857'
     * }
     *
     */
    public String getPhoneInfo() throws JSONException {
        Log.d(TAG, "Entering getPhoneInfo");

        JSONObject phone_info_json = new JSONObject();

        phone_info_json.put("call_state",
                telephony_manager.getCallState());
        phone_info_json.put("data_activity",
                telephony_manager.getDataActivity());
        phone_info_json.put("data_state", telephony_manager.getDataState());
        phone_info_json.put("network_type", telephony_manager.getNetworkType());
        phone_info_json.put("phone_type", telephony_manager.getPhoneType());

        // The subscriber and device ID are too privacy invasive. Don't expose.
        // phone_info_json.put("subscriber_id",
        //        telephony_manager.getSubscriberId());
        // phone_info_json.put("device_id", telephony_manager.getDeviceId());

        try {
            phone_info_json.put("device_software_version",
                    telephony_manager.getDeviceSoftwareVersion());
        } catch (SecurityException e) {
            // We are missing the READ_PHONE_STATE permission.
        }

        return phone_info_json.toString();
    }


    /*
     * Returns whether WiFi is currently enabled on the phone
     *
     * @return  Is WiFi enabled (bool)
     */
    public boolean isWifiEnabled() {
        Log.d(TAG, "Entering isWifiEnabled");

        return wifi_manager.isWifiEnabled();
    }

    /*
     * Returns current WiFi state
     * c.f. developer.android.com/reference/android/net/wifi/WifiManager.html
     * for all states
     *
     * @return  WiFi state (int)
     */
    public int getWifiState() {
        Log.d(TAG, "Entering getWifiState");

        return wifi_manager.getWifiState();
    }


    /*
     * Returns info about the WiFi network the device is currently connected to
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {
     * 'ssid': '"eduroam"',
     * 'bssid': '6c:99:89:76:7f:64',
     * 'network_id': 1,
     * 'supplicant_state': 'COMPLETED',
     * 'link_speed': 72,
     * 'frequency': 2412,
     * 'mac_address': '02:00:00:00:00:00',
     * 'rssi': -54,
     * 'ip_address': -1260512340,
     * 'hidden_ssid': False
     * }
     *
     */
    public String getWifiConnectionInfo() throws JSONException {
        Log.d(TAG, "Entering getWifiConnectionInfo");
        JSONObject wifi_info_json = new JSONObject();
        WifiInfo wifi_info = wifi_manager.getConnectionInfo();

        wifi_info_json.put("ssid",  wifi_info.getSSID());
        wifi_info_json.put("hidden_ssid", wifi_info.getHiddenSSID());
        wifi_info_json.put("bssid", wifi_info.getBSSID());
        wifi_info_json.put("rssi", wifi_info.getRssi());
        wifi_info_json.put("supplicant_state",
                wifi_info.getSupplicantState().name());
        wifi_info_json.put("link_speed", wifi_info.getLinkSpeed());
        wifi_info_json.put("mac_address", wifi_info.getMacAddress());
        wifi_info_json.put("ip_address", wifi_info.getIpAddress());
        wifi_info_json.put("network_id", wifi_info.getNetworkId());
        wifi_info_json.put("frequency", wifi_info.getFrequency());

        // Dump JSON to string and return
        return wifi_info_json.toString();
    }


    /*
     * Starts WiFi scan, waits until gets notified that scan is finished
     * and returns info about all scanned WiFis
     *
     * @return  String serialized JSON Array or null
     * e.g.:
     * [{
     *   'rssi':-56,
     *   'capabilities':'[WPA-EAP-TKIP][WPA2-EAP-CCMP][ESS][BLE]',
     *   'frequency':2412,
     *   'ssid':'nyu',
     *   'bssid':'6c:99:89:76:7f:60'
     * }, ... ]
     *
     */
    public String getWifiScanInfo() throws InterruptedException, JSONException {
        Log.d(TAG, "Entering getWifiScanInfo");

        // Register "onReceive" for scan results (gets called from main thread)
        cached_context.registerReceiver(wifi_broadcast_receiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // If scan was started wait until onReceive notifies us via
        // the wifi_sync object that scan results are available
        Log.d(TAG, "Starting wifi scan");
        if (wifi_manager.startScan()) {
            synchronized (wifi_sync) {
                wifi_sync.wait();
                JSONArray wifi_json_array = new JSONArray();

                // Scan results are available now
                // Let's call them and convert them to JSON
                for (ScanResult result : wifi_manager.getScanResults()) {
                    JSONObject wifi_json = new JSONObject();
                    wifi_json.put("bssid", result.BSSID);
                    wifi_json.put("ssid", result.SSID);
                    wifi_json.put("capabilities", result.capabilities);
                    wifi_json.put("frequency", result.frequency);
                    wifi_json.put("rssi", result.level);

                    wifi_json_array.put(wifi_json);
                }
                return wifi_json_array.toString();
            }
        } else {
            Log.d(TAG, "Wifi Scan could not be started");
            // XXX raise couldn't start scan exception ?
        }
        return null;
    }


    /*
     * Returns info about the bluetooth interface in the device
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {
     * 'local_address': '02:00:00:00:00:00',
     * 'state': 12,
     * 'scan_mode': 21,
     * 'local_name':
     * 'SAMSUNG-SM-J120A'
     * }
     */
    public String getBluetoothInfo() throws JSONException {
        Log.d(TAG, "Entering getBluetoothInfo");

        JSONObject bluetooth_info_json = new JSONObject();
        BluetoothAdapter bluetooth_adapter = bluetooth_manager.getAdapter();

        bluetooth_info_json.put("state", bluetooth_adapter.getState());
        bluetooth_info_json.put("scan_mode", bluetooth_adapter.getScanMode());
        bluetooth_info_json.put("local_name", bluetooth_adapter.getName());
        bluetooth_info_json.put("local_address",
                bluetooth_adapter.getAddress());

        bluetooth_adapter.getClass();

        return bluetooth_info_json.toString();
    }


    /*
     * Starts bluetooth discovery, wait gets notified that discovery
     * has finished and returns info about all remote bluetooth devices
     *
     * Discovery usually involves an inquiry scan of about 12 seconds,
     * followed by a page scan for each found device.
     *
     * @return  String serialized JSON Array
     * e.g.:
     * [{'bond_state': 10,
     * # DEVICE_TYPE_CLASSIC=1 | DEVICE_TYPE_LE=2 |
     * #   DEVICE_TYPE_DUAL=3 | DEVICE_TYPE_UNKNOWN=0
     * 'type': 2,
     * 'name': 'UP2',
     * 'address': 'FB:A0:85:FF:AE:1E'
     * }, ...]
     *
     */
    public String getBluetoothScanInfo()
            throws JSONException, InterruptedException {
        Log.d(TAG, "Entering getBluetoothScanInfo");

        // Register receiver for page scan result
        // and discovery finished
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(BluetoothDevice.ACTION_FOUND);
        ifilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        cached_context.registerReceiver(bluetooth_broadcast_receiver, ifilter);

        // Initialize list to which the broadcast receiver will append
        // paged bluetooth devices
        scanned_bluetooth_devices = new ArrayList<BluetoothDevice>();

        Log.d(TAG, "Entering start bluetooth discovery");
        // Start discovery and wait until it is finished
        if (bluetooth_adapter.startDiscovery()) {
            synchronized(bluetooth_sync) {
                bluetooth_sync.wait();
                // If we have discovered some devices
                // transform infos to JSON and return as String
                if (scanned_bluetooth_devices.size() > 0) {
                    JSONArray bluetooth_json_array = new JSONArray();
                    for (BluetoothDevice remote_device :
                            scanned_bluetooth_devices) {
                        JSONObject bluetooth_json = new JSONObject();
                        bluetooth_json.put("address",
                                remote_device.getAddress());
                        //bluetooth_json.put("name", remote_device.getName());
                        bluetooth_json.put("bond_state",
                                remote_device.getBondState());
                        bluetooth_json.put("type", remote_device.getType());

                        bluetooth_json_array.put(bluetooth_json);
                    }
                    return bluetooth_json_array.toString();
                }
            }
        } else {
            Log.d(TAG, "Bluetooth discovery got not be started");
            // XXX raise couldn't start discovery exception ?
        }
        return null;
    }



    /*
     * Returns info about device battery
     *
     * Constants can be found at
     * https://developer.android.com/reference/android/os/BatteryManager.html
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {
     * 'status': 3,
     * 'temperature': 257,
     * 'level': 99,
     * 'battery_present': True,
     * 'plugged': 2,
     * 'health': 2,
     * 'voltage': 4186,
     * 'technology': 'Li-ion'
     * }
     */
    public String getBatteryInfo() throws JSONException {
        Log.d(TAG, "Entering getBatteryInfo");

        JSONObject battery_info_json = new JSONObject();

        // Register a null receiver which immediately returns Intent
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery_info = cached_context.registerReceiver(null, ifilter);

        // Retrieve values
        int status = battery_info.getIntExtra(
                BatteryManager.EXTRA_STATUS, -1);
        int temperature = battery_info.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE, -1);
        int level = battery_info.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        boolean present = battery_info.getExtras().getBoolean(
                BatteryManager.EXTRA_PRESENT, false);
        int plugged = battery_info.getIntExtra(
                BatteryManager.EXTRA_PLUGGED, -1);
        int health = battery_info.getIntExtra(
                BatteryManager.EXTRA_HEALTH, -1);
        int voltage = battery_info.getIntExtra(
                BatteryManager.EXTRA_VOLTAGE, -1);
        String technology = battery_info.getExtras().getString(
                BatteryManager.EXTRA_TECHNOLOGY, "N/A");

        // Add values to JSON dict
        battery_info_json.put("status", status);
        battery_info_json.put("temperature", temperature);
        battery_info_json.put("level", level);
        battery_info_json.put("battery_present", present);
        battery_info_json.put("plugged", plugged);
        battery_info_json.put("health", health);
        battery_info_json.put("voltage", voltage);
        battery_info_json.put("technology", technology);

        // Dump JSON to string and return
        return battery_info_json.toString();
    }


    /*
     * Returns the current ringer mode and whether airplane mode is on or off
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {'airplane_mode': False, 'ringer_mode': 1}
     *
     *
     */
    public String getModeSettings()
            throws Settings.SettingNotFoundException, JSONException {
        Log.d(TAG, "Entering getModeSettings");

        JSONObject mode_settings_json = new JSONObject();
        boolean airplane_mode = (android.provider.Settings.System
                .getString(content_resolver,
                        Settings.Global.AIRPLANE_MODE_ON)  ==
                Settings.Global.AIRPLANE_MODE_ON);
        int ringer_mode = audio_manager.getRingerMode();

        mode_settings_json.put("airplane_mode", airplane_mode);
        mode_settings_json.put("ringer_mode", ringer_mode);

        return mode_settings_json.toString();
    }

    
    /*
     * Returns info about the device display
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {
     * 'name': 'Built-in Screen',
     * 'brightness_mode': '0',
     * 'brightness': '255',
     * 'state': 2,
     * 'size_x': 480,
     * 'size_y': 800,
     * 'timeout': '30000',
     * 'rotation': 0
     * }
     *
     */
    public String getDisplayInfo() throws JSONException {
        Log.d(TAG, "Entering getDisplayInfo");

        JSONObject screen_settings_json = new JSONObject();
        Display display = display_manager.getDisplay(Display.DEFAULT_DISPLAY);
        Point size = new Point();

        String name = display.getName();
        int state = display.getState();
        int rotation = display.getRotation();
        display.getSize(size);

        String brightness = android.provider.Settings.System.getString(
                content_resolver,Settings.System.SCREEN_BRIGHTNESS);
        String brightness_mode = android.provider.Settings.System.getString(
                content_resolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
        String timeout = android.provider.Settings.System.getString(
                content_resolver, Settings.System.SCREEN_OFF_TIMEOUT);

        screen_settings_json.put("name", name);
        screen_settings_json.put("state", state);
        screen_settings_json.put("rotation", rotation);
        screen_settings_json.put("size_x", size.x);
        screen_settings_json.put("size_y",  size.y);

        screen_settings_json.put("brightness", brightness);
        screen_settings_json.put("brightness_mode", brightness_mode);
        screen_settings_json.put("timeout", timeout);

        return screen_settings_json.toString();
    }


    /*
     * Returns info about the media and about the ringer volume
     *
     * @return  String serialized JSON Object
     * e.g.:
     * {
     * 'max_media_volume': 15,
     * 'media_volume': 0,
     * 'max_ringer_volume': 15,
     * 'ringer_volume': 0
     * }
     */
    public String getVolumeInfo() throws JSONException {
        Log.d(TAG, "Entering getVolumeInfo");

        JSONObject volume_json = new JSONObject();

        volume_json.put("media_volume",
                audio_manager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volume_json.put("max_media_volume",
                audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume_json.put("ringer_volume",
                audio_manager.getStreamVolume(AudioManager.STREAM_RING));
        volume_json.put("max_ringer_volume",
                audio_manager.getStreamMaxVolume(AudioManager.STREAM_RING));

        return volume_json.toString();
    }
}
