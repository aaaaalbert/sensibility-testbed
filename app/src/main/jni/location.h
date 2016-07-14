//
// Created by lukas on 5/4/16.
//

#ifndef _SNAKEI_location_H_
#define _SNAKEI_location_H_

#include <Python.h>
#include <jni.h>
#include <assert.h>
#include "snakei.h"
#include "jnihelper.h"

void initlocation();
void location_start_location();
void location_stop_location();
PyObject* location_get_location();
PyObject* location_get_lastknown_location();
PyObject* location_get_geolocation(PyObject *self, PyObject *args);

#endif //_SNAKEI_location_H_
