#include "snakei.h"

/*
 * This function is called when the Java code does `System.loadLibrary()`.
 * See https://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/invocation.html#JNI_OnLoad
 *
 * We use it to cache the native reference to the JVM which will be
 * used by all snakei extensions
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    cached_vm = vm;
    return JNI_VERSION_1_6;

}


