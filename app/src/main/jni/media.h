//
// Created by lukas on 5/27/16.
//

#ifndef SENSIBILITY_TESTBED_MEDIA_H
#define SENSIBILITY_TESTBED_MEDIA_H
#include <Python.h>
#include <jni.h>
#include "snakei.h"
#include "jnihelper.h"

void initmedia();
void media_start_media();
void media_stop_media();
PyObject* media_microphone_record(PyObject *self, PyObject *args);
PyObject* media_is_media_playing(PyObject *self);
//PyObject* media_get_media_play_info(PyObject *self);
PyObject* media_is_tts_speaking(PyObject *self);
PyObject* media_tts_speak(PyObject *self, PyObject *args);
#endif //SENSIBILITY_TESTBED_MEDIA_H
