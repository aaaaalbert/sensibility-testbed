/*
 * Created by lukas.puehringer@nyu.edu
 * on 5/4/16.
 *
 * Provides wrappers for C-Python run methods
 * that write Exception messages to the Android Log
 * instead of the stderr
 *
 * Note:
 * This is helpful in development but not necessarily necessary
 * in production
 *
 */

#include "pyhelper.h"

/*
 * Verbosely execute Python code.
 * Exceptions are print to Android log using LOGI macro.
 *
 * Arguments
 *   code - Python code (const char*)
 *
 * Returns
 *   0 on success or -1 if an exception was raised
 */
int Verbose_PyRun_SimpleString(const char *code) {
    // Used for globals and locals
    PyObject *module = PyImport_AddModule("__main__");
    PyObject *d = PyDict_New();
    if (module == NULL)
        return -1;
    d = PyModule_GetDict(module);
    PyRun_StringFlags(code, Py_file_input, d, d, NULL);
    if (PyErr_Occurred()) {
        PyObject *errtype, *errvalue, *traceback;
        PyObject *errstring = NULL;

        PyErr_Fetch(&errtype, &errvalue, &traceback);

        if(errtype != NULL) {
            errstring = PyObject_Str(errtype);
            LOGI("Errtype: %s\n", PyString_AS_STRING(errstring));
        }
        if(errvalue != NULL) {
            errstring = PyObject_Str(errvalue);
            LOGI("Errvalue: %s\n", PyString_AS_STRING(errstring));
        }

        Py_XDECREF(errvalue);
        Py_XDECREF(errtype);
        Py_XDECREF(traceback);
        Py_XDECREF(errstring);
    }
    return 0;
}

/*
 * Verbosely open a python file and run it
 * Exceptions are print to Android log using LOGI macro.
 *
 * Arguments
 *   filename - full path and file name to Python script (const char*)
 *
 * Returns
 *   0 on success or -1 if an exception was raised.
 *
 * Note:
 * This is basically a copy of PyRun_SimpleFileExFlags with
 * the following changes
 *  - only needs the filename (fopen is done here)
 *  - expects the file to be python source code
 *    (does not handle *.pyc, or *.pyo)
 *  - replaces PyErr_Print() part with custom LOGI calls
 */

int Verbose_PyRun_SimpleFile(const char *filename) {
    FILE *fp;
    int closeit = 0;
    PyCompilerFlags *flags = NULL;

    fp = fopen(filename, "r");
    if (fp == NULL) {
        LOGI("NULL file pointer for '%s' because errno %i '%s'",
             filename, errno, strerror(errno));
        return -1;
    }

    PyObject *m, *d, *v;
    const char *ext;
    int set_file_name = 0, len, ret = -1;

    m = PyImport_AddModule("__main__");
    if (m == NULL)
        return -1;
    Py_INCREF(m);
    d = PyModule_GetDict(m);
    if (PyDict_GetItemString(d, "__file__") == NULL) {
        PyObject *f = PyString_FromString(filename);
        if (f == NULL)
            goto done;
        if (PyDict_SetItemString(d, "__file__", f) < 0) {
            Py_DECREF(f);
            goto done;
        }
        set_file_name = 1;
        Py_DECREF(f);
    }
    len = strlen(filename);
    ext = filename + len - (len > 4 ? 4 : 0);

    v = PyRun_FileExFlags(fp, filename, Py_file_input, d, d,
                              closeit, flags);

    if (v == NULL) {
        PyObject *errtype, *errvalue, *traceback;
        PyObject *errstring = NULL;

        PyErr_Fetch(&errtype, &errvalue, &traceback);

        if(errtype != NULL) {
            errstring = PyObject_Str(errtype);
            LOGI("Errtype: %s\n", PyString_AS_STRING(errstring));
        }
        if(errvalue != NULL) {
            errstring = PyObject_Str(errvalue);
            LOGI("Errvalue: %s\n", PyString_AS_STRING(errstring));
        }

        Py_XDECREF(errvalue);
        Py_XDECREF(errtype);
        Py_XDECREF(traceback);
        Py_XDECREF(errstring);
        goto done;
    }
    Py_DECREF(v);
    if (Py_FlushLine())
        PyErr_Clear();
    ret = 0;
    done:
    if (set_file_name && PyDict_DelItemString(d, "__file__"))
        PyErr_Clear();
    Py_DECREF(m);
    return ret;
}