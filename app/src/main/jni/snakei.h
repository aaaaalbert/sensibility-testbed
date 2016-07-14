#ifndef _SNAKEI_H_
#define _SNAKEI_H_

#include <Python.h>
#include <jni.h>
#include <stdio.h>
#include <unistd.h>
#include <android/log.h>

#include "interpreter.h"
#include "outputs.h"
#include "sensors.h"
#include "pyhelper.h"
#include "miscinfo.h"



// Define a "convenience" macro for simple logging (see `adb logcat`)
// (taken from the Android NDK samples)
# define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, __FILE__, __VA_ARGS__))

/* Following the suggestions in
 * https://developer.android.com/training/articles/perf-jni.html#native_libraries,
 * we pre-cache the `JNIEnv` and classes that we will use in C code.
 * First, declare a few references we will populate soon and then reuse 
 * in the course of our runtime.
 *
 * XXX:
 *   we need to cache the JNIEnv per thread, i.e. we shouldn't cache
 *   it here, because this gets called once by the parent thread that will
 *   spawn off other threads
 *
 *   The vm pointer on the other hand can be cached shared by all threads
 *   and therefor cached here
 *
 *   Java classes and methods are currently cached in the each extension module
 *   maybe this can be done here
 */
JavaVM* cached_vm;

#endif /* defined _SNAKEI_H_ */

