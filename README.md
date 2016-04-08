This is the source repo for SensibilityTestbed (Android app).

See https://sensibilitytestbed.com/ for details

-----
We are currently moving away from SL4A towards an embedded Python 
interpreter with native-looking sensor interfaces (i.e. Python 
functions that you can call from Python code in order to get 
sensor values).

How this works approximately:
* The required sensor event listening stuff is implemented in Java
* The Python interpreter is [embedded](https://docs.python.org/2/extending/embedding.html) 
  into C code, where we use
 * the Java Native interface to call into Java, and 
 * the proper Python bindings to make these JNI calls available 
  to Python code.

Depending on what values need to be transmitted, lots of type 
conversions are required. (See this [example](https://github.com/aaaaalbert/sensibility-testbed/blob/use-android-studio/app/src/main/jni/outputs.c#L9-L38)).


# Build

* Clone the project
* Check out the `use-android-studio` branch
* Use `ndk-build` in `sensibility-testbed/app/src/main/jni` to build the 
  native module, `snakei`.
* Copy the shared objects for Snakei and Python from 
  `sensibility-testbed/app/src/main/libs/armeabi` into `sensibility-testbed/app/jniLibs` 
  where [the Gradle config currently expects them](https://github.com/aaaaalbert/sensibility-testbed/blob/use-android-studio/app/build.gradle#L39).
* Import the `sensibility-testbed` project into Android Studio.
* Android Studio should (TM) download all of the Gradle etc. stuff it 
  needs to build the APK.
* Build the APK.
* `adb install` it on your device
* Look at `adb logcat` to see interesting debug messages scroll by.

While we are working on [getting file access to work](6db3908b97e0bc411bcdd6f0ebf42ab09bd5d658),
there [still is a way](https://github.com/aaaaalbert/sensibility-testbed/blob/use-android-studio/app/src/main/jni/interpreter.c#L62) to test-drive [any calls you add to the embedded Python interpreter](https://github.com/aaaaalbert/sensibility-testbed/blob/use-android-studio/app/src/main/jni/interpreter.c#L15-L21).
