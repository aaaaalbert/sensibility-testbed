/*
 * Created by
 * albert.rafetseder@univie.ac.at
 * lukas.puehringer@nyu.edu
 * on 3/30/16
 *
 * The native implementation of startNativePythonInterpreter declared in
 * PythonInterpreterService.java
 *
 * Currently the interpreter is used to initialize "sensor" functions as Python
 * modules - init*(), acquire resources in Java - *_start_*(), run Python
 * tests scripts that use the new modules, and eventually release the
 * resources - *_stop_*().
 *
 *
 * Note:
 * Btw. this is the local JNI scope, all local references that get created
 * in here, e.g. by running a Python script that uses C-Python Extension, which
 * create native references to Java objects will not be automatically released
 * until this function returns
 * They can (and should be) released explicitly, though
 *
 *
 */

#include "interpreter.h"

void Java_com_snakei_PythonInterpreterService_startNativePythonInterpreter(
        JNIEnv* env, jobject instance, jstring python_files) {

  char* files = (char*) (*env)->GetStringUTFChars(env, python_files, NULL);

  LOGI("Before Py_Initialize...");
  Py_Initialize();

//    PySys_SetPath(path);
//    Py_SetPythonHome(home);
//    Py_SetProgramName("/sdcard/mypython/python");


    // Print stats about the environment
  LOGI("ProgramName %s", (char*) Py_GetProgramName());
  LOGI("Prefix %s", Py_GetPrefix());
  LOGI("ExecPrefix %s", Py_GetExecPrefix());
  LOGI("ProgramFullName %s", Py_GetProgramFullPath());
  LOGI("Path %s", Py_GetPath());
  LOGI("Platform %s", Py_GetPlatform());
  LOGI("PythonHome %s", Py_GetPythonHome());

  initandroidlog();

  char *filename;
  char *full_filename;

  LOGI("Start Sensing IN C!!!!");
  initsensor();
  int i;
  for (i = 1;  i <= 17; i++) {
     sensor_start_sensing(i);
  }

  // Och, memory...
  filename = "test_sensors.py";
  full_filename = (char *) malloc(1 + strlen(files) + strlen(filename));
  strcpy(full_filename, files);
  strcat(full_filename, filename);

  LOGI("PyRun returns %i", Verbose_PyRun_SimpleFile(full_filename));
  LOGI("Stop Sensing IN C!!!!");
  int j;
  for (j = 1;  j <= 17; j++) {
    sensor_stop_sensing(j);
   }

  LOGI("Start Locating IN C!!!!");
  initlocation();
  location_start_location();
  filename = "test_location.py";
  full_filename = (char *) malloc(1 + strlen(files) + strlen(filename));
  strcpy(full_filename, files);
  strcat(full_filename, filename);
  LOGI("PyRun File: %s", full_filename);
  LOGI("PyRun returns %i for %s", Verbose_PyRun_SimpleFile(full_filename),
       filename);
  LOGI("Stop Locating IN C!!!!");
  location_stop_location();

  LOGI("Start Media-ing IN C!!!!");
  initmedia();
  media_start_media();
  filename = "test_tts.py";
  full_filename = (char *) malloc(1 + strlen(files) + strlen(filename));
  strcpy(full_filename, files);
  strcat(full_filename, filename);
  LOGI("PyRun File: %s", full_filename);
  LOGI("PyRun returns %i for %s", Verbose_PyRun_SimpleFile(full_filename),
       filename);
  LOGI("Stop Media-ing IN C!!!!");
  media_stop_media();

  LOGI("Init and start MiscInfo-ing IN C!!!!");
  initmiscinfo();
  filename = "test_miscinfo.py";
  full_filename = (char *) malloc(1 + strlen(files) + strlen(filename));
  strcpy(full_filename, files);
  strcat(full_filename, filename);
  LOGI("PyRun File: %s", full_filename);
  LOGI("PyRun returns %i for %s", Verbose_PyRun_SimpleFile(full_filename),
       filename);

  LOGI("Before Py_Finalize...");
  Py_Finalize();

  LOGI("Done. Bye!");
};

