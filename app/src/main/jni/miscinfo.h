//
// Created by lukas on 6/3/16.
//

#ifndef SENSIBILITY_TESTBED_MISCINFO_H
#define SENSIBILITY_TESTBED_MISCINFO_H
#include <Python.h>
#include <jni.h>
#include "snakei.h"
#include "cjson.h"
#include "jnihelper.h"
#include "python2.7/object.h"

void initmiscinfo();

PyObject* miscinfo_is_wifi_enabled(PyObject *self);
PyObject* miscinfo_get_wifi_state(PyObject *self);
PyObject* miscinfo_get_wifi_connection_info(PyObject *self);
PyObject* miscinfo_get_wifi_scan_info(PyObject *self);
PyObject* miscinfo_get_bluetooth_info(PyObject *self);
PyObject* miscinfo_get_bluetooth_scan_info(PyObject *self);
PyObject* miscinfo_get_network_info(PyObject *self);
PyObject* miscinfo_get_cellular_provider_info(PyObject *self);
PyObject* miscinfo_get_cell_info(PyObject *self);
PyObject* miscinfo_get_sim_info(PyObject *self);
PyObject* miscinfo_get_phone_info(PyObject *self);
PyObject* miscinfo_get_mode_settings(PyObject *self);
PyObject* miscinfo_get_display_info(PyObject *self);
PyObject* miscinfo_get_volume_info(PyObject *self);
PyObject* miscinfo_get_battery_info(PyObject *self);




#endif //SENSIBILITY_TESTBED_MISCINFO_H
