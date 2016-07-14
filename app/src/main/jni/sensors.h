#ifndef _SNAKEI_SENSORS_H_
#define _SNAKEI_SENSORS_H_

#include <Python.h>
#include <jni.h>
#include "snakei.h"

void initsensor();
void sensor_start_sensing(int sensor_type);
void sensor_stop_sensing(int sensor_type);

PyObject* sensor_get_sensor_list(PyObject *self);
PyObject* sensor_get_acceleration(PyObject *self);
PyObject* sensor_get_ambient_temperature(PyObject *self);
PyObject* sensor_get_game_rotation_vector(PyObject *self);
PyObject* sensor_get_geomagnetic_rotation_vector(PyObject *self);
PyObject* sensor_get_gravity(PyObject *self);
PyObject* sensor_get_gyroscope(PyObject *self);
PyObject* sensor_get_gyroscope_uncalibrated(PyObject *self);
PyObject* sensor_get_heart_rate(PyObject *self);
PyObject* sensor_get_light(PyObject *self);
PyObject* sensor_get_linear_acceleration(PyObject *self);
PyObject* sensor_get_magnetic_field(PyObject *self);
PyObject* sensor_get_magnetic_field_uncalibrated(PyObject *self);
PyObject* sensor_get_pressure(PyObject *self);
PyObject* sensor_get_proximity(PyObject *self);
PyObject* sensor_get_relative_humidity(PyObject *self);
PyObject* sensor_get_rotation_vector(PyObject *self);
PyObject* sensor_get_step_counter(PyObject *self);

#endif /* defined _SNAKEI_SENSORS_H_ */

