//
// Created by lukas on 5/4/16.
//

#ifndef _SNAKEI_PYHELPER_H_
#define _SNAKEI_PYHELPER_H_

#include <Python.h>

#include <jni.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include "snakei.h"

int Verbose_PyRun_SimpleString(const char *code);
int Verbose_PyRun_SimpleFile(const char *filename);

#endif //_SNAKEI_PYHELPER_H_
