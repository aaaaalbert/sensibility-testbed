#ifndef _SNAKEI_INTERPRETER_H_
#define _SNAKEI_INTERPRETER_H_

#include <Python.h>
#include <jni.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include "snakei.h"
#include "outputs.h"
#include "sensors.h"
#include "location.h"
#include "media.h"
#include "pyhelper.h"
#include "miscinfo.h"

void Java_com_snakei_PythonInterpreterService_startNativePythonInterpreter(
        JNIEnv* env, jobject instance, jstring python_files);
#endif /* defined _SNAKEI_INTERPRETER_H_ */

