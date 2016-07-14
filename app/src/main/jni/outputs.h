#ifndef _SNAKEI_OUTPUTS_H_
#define _SNAKEI_OUTPUTS_H_

#include <Python.h>
#include <jni.h>
#include "snakei.h"
#include "jnihelper.h"

// Log messages of no more than this many characters
#define MAX_MESSAGE_LENGTH 1024

void initandroidlog();
PyObject* androidlog_log(PyObject *self, PyObject *python_message);

#endif /* defined _SNAKEI_OUTPUTS_H_ */

