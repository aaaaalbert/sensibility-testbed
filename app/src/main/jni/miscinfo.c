/*
 * Created by lukas.puehringer@nyu.edu
 * on 6/3/16.
 *
 * C Python module uses MiscInfoService.java via JNI to return miscellaneous
 * information about the Android device
 *
 * Usage:
 * Module initialization - call initmiscinfo() from C
 *  - Initializes Python module (miscinfo)
 *  - Caches native reference to Java Singleton Class MiscInfoService.java
 *  - Caches native reference to Singleton getter and Java Methods
 *
 * Get miscinfo values - call miscinfo.get_* from Python
 *  - Calls the according get* method in Java
 *  - Return values are documented in MiscInfoService.java
 *
 *  Note:
 *  Cf. sensors.c notes
 *
 */

#include "miscinfo.h"


/*
 * Caches native references of used Java Class and Java Methods
 */
static struct miscinfo_cache {
    jclass class;
    jmethodID get_instance;
    jmethodID is_wifi_enabled;
    jmethodID get_wifi_state;
    jmethodID get_wifi_connection_info;
    jmethodID get_battery_info;
    jmethodID get_wifi_scan_info;
    jmethodID get_bluetooth_info;
    jmethodID get_bluetooth_scan_info;
    jmethodID get_network_info;
    jmethodID get_cellular_provider_info;
    jmethodID get_cell_info;
    jmethodID get_sim_info;
    jmethodID get_phone_info;
    jmethodID get_mode_settings;
    jmethodID get_display_info;
    jmethodID get_volume_info;
} m_cached;


/*
 * Cf. getBluetoothInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_bluetooth_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_bluetooth_info);
}


/*
 * Cf. getBluetoothScanInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_bluetooth_scan_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_bluetooth_scan_info);
}


/*
 * Cf. isWifiEnabled() in MiscInfoService.java for details
 */
PyObject* miscinfo_is_wifi_enabled(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callBooleanMethod, m_cached.is_wifi_enabled);
}


/*
 * Cf. getWifiState() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_wifi_state(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callIntMethod, m_cached.get_wifi_state);
}


/*
 * Cf. getWifiConnectionInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_wifi_connection_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_wifi_connection_info);
}


/*
 * Cf. getWifiScanInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_wifi_scan_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_wifi_scan_info);
}


/*
 * Cf. getNetworkInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_network_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_network_info);
}


/*
 * Cf. getCellularProviderInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_cellular_provider_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod,
                   m_cached.get_cellular_provider_info);
}


/*
 * Cf. getCellInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_cell_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_cell_info);
}


/*
 * Cf. getSimInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_sim_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_sim_info);
}


/*
 * Cf. getPhoneInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_phone_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_phone_info);
}


/*
 * Cf. getModeSettings() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_mode_settings(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_mode_settings);
}


/*
 * Cf. getDisplayInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_display_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_display_info);
}


/*
 * Cf. getVolumeInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_volume_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_volume_info);
}


/*
 * Cf. getBatteryInfo() in MiscInfoService.java for details
 */
PyObject* miscinfo_get_battery_info(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance,
                   jh_callJsonStringMethod, m_cached.get_battery_info);
}


/*
 * Maps C functions to Python module methods
 */
static PyMethodDef AndroidMiscinfoMethods[] = {
        {"is_wifi_enabled",
         (PyCFunction) miscinfo_is_wifi_enabled, METH_NOARGS,
         "True if WiFi is enabled, False otherwise"},
        {"get_wifi_state", (PyCFunction) miscinfo_get_wifi_state, METH_NOARGS,
         "Returns state of WiFi"},
        {"get_wifi_connection_info",
         (PyCFunction) miscinfo_get_wifi_connection_info, METH_NOARGS,
         "Returns info about WiFi currently connected WiFi "},
        {"get_wifi_scan_info",
         (PyCFunction) miscinfo_get_wifi_scan_info, METH_NOARGS,
         "Performs WiFi scan and returns info about scanned WiFis"},
        {"get_bluetooth_info",
         (PyCFunction) miscinfo_get_bluetooth_info, METH_NOARGS,
         "Returns info about bluetooth interface on the device"},
        {"get_bluetooth_scan_info",
         (PyCFunction) miscinfo_get_bluetooth_scan_info, METH_NOARGS,
         "Performs BT discovery and returns info about discovered devices"},
        {"get_network_info",
         (PyCFunction) miscinfo_get_network_info, METH_NOARGS,
         "Returns network info (WiFi, GPRS, UMTS, etc.)"},
        {"get_cellular_provider_info",
         (PyCFunction) miscinfo_get_cellular_provider_info, METH_NOARGS,
         "Returns cellular provider info"},
        {"get_cell_info", (PyCFunction) miscinfo_get_cell_info, METH_NOARGS,
         "Returns observed cell information from all radios on the device"},
        {"get_sim_info", (PyCFunction) miscinfo_get_sim_info, METH_NOARGS,
         "Returns Subscriber Identity Module (SIM)"},
        {"get_phone_info", (PyCFunction) miscinfo_get_phone_info, METH_NOARGS,
         "Returns info about the phone"},
        {"get_mode_settings",
         (PyCFunction) miscinfo_get_mode_settings, METH_NOARGS,
         "Returns ringer mode and whether airplane mode is on or off"},
        {"get_display_info",
         (PyCFunction) miscinfo_get_display_info, METH_NOARGS,
         "Returns info about the device display"},
        {"get_volume_info",
         (PyCFunction) miscinfo_get_volume_info, METH_NOARGS,
         "Returns info about media and ringer volume"},
        {"get_battery_info",
         (PyCFunction) miscinfo_get_battery_info, METH_NOARGS,
         "Returns info about device battery"},
        {NULL, NULL, 0, NULL} // This is the end-of-array marker
};


/*
 * Initializes Python module (miscinfo), looks up Java class and Java Methods
 * used retrieve miscellaneous information about the Android device
 *
 * Note:
 * If we wanted to build the module as .so or .dll we could
 * would have to change the signature to
 * PyMODINIT_FUNC initmiscinfo(void)
 *
 */
void initmiscinfo() {
    Py_InitModule("miscinfo", AndroidMiscinfoMethods);

    jclass class = jh_getClass("com/snakei/MiscInfoService");
    m_cached = (struct miscinfo_cache){
            .class = class,
            .get_instance = jh_getGetter(class,
                                         "()Lcom/snakei/MiscInfoService;"),
            .is_wifi_enabled = jh_getMethod(class,
                                            "isWifiEnabled",
                                            "()Z"),
            .get_wifi_state = jh_getMethod(class,
                                           "getWifiState",
                                           "()I"),
            .get_wifi_connection_info = jh_getMethod(class,
                                                     "getWifiConnectionInfo",
                                                     "()Ljava/lang/String;"),
            .get_wifi_scan_info = jh_getMethod(class,
                                               "getWifiScanInfo",
                                               "()Ljava/lang/String;"),
            .get_bluetooth_scan_info = jh_getMethod(class,
                                                    "getBluetoothScanInfo",
                                                    "()Ljava/lang/String;"),
            .get_bluetooth_info = jh_getMethod(class,
                                               "getBluetoothInfo",
                                               "()Ljava/lang/String;"),
            .get_battery_info = jh_getMethod(class,
                                             "getBatteryInfo",
                                             "()Ljava/lang/String;"),
            .get_network_info = jh_getMethod(class,
                                             "getNetworkInfo",
                                             "()Ljava/lang/String;"),
            .get_cellular_provider_info = jh_getMethod(class,
                                             "getCellularProviderInfo",
                                             "()Ljava/lang/String;"),
            .get_cell_info = jh_getMethod(class,
                                          "getCellInfo",
                                          "()Ljava/lang/String;"),
            .get_sim_info = jh_getMethod(class,
                                         "getSimInfo",
                                         "()Ljava/lang/String;"),
            .get_phone_info = jh_getMethod(class,
                                           "getPhoneInfo",
                                           "()Ljava/lang/String;"),
            .get_mode_settings = jh_getMethod(class,
                                              "getModeSettings",
                                              "()Ljava/lang/String;"),
            .get_display_info = jh_getMethod(class,
                                             "getDisplayInfo",
                                             "()Ljava/lang/String;"),
            .get_volume_info = jh_getMethod(class,
                                            "getVolumeInfo",
                                            "()Ljava/lang/String;")
    };
}