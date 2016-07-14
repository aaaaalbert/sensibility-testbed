## Native Sensor Extensions

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
 * the Java Native Interface (JNI) to call into Java, and 
 * the proper Python bindings to make these JNI calls available 
  to Python code.

Read the **docstrings** and **comments** for detailed information about the different components!

## Installation and Usage

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
* Add Python scripts - `*.py` - to the [assets directory](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/assets) and modify [interpreter.c](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/jni/interpreter.c#L55-L73) to execute them.
* Build the APK
* `adb install` it on your device
* Look at `adb logcat` to see interesting debug messages


## Detailed System Overview

![Sensibility Implementation Overview](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/docs/sensibility_overview.png "Sensibility Implementation Overview")

### JVM
 - **Activity** - `com.sensibility_testbed.ScriptActivity`
   - `onCreate` is the entry point to the app
   - sets up UI and handles user interaction 
   - starts `PythonInterpreterService`
   - *Seattle installation is currently commented out*
 - **Python Interpreter Service** - `com.snakei.PythonInterpreterService` 
   - runs in background
   - loads native libraries (Python) and native modules (Snakei)
   - starts new thread (Android `Service`s don't run in their own thread a priori)
   - calls its native method which is defined in `interpreter.c` 
 - **Application** - `com.sensibility_testbed.SensibilityApplication`
   - workaround to provide a static reference of the application context to Sensor Service Facades
 - **Android Manifest** - `AndroidManifest.xml`
   - defines above components and required permissions
 - **Sensor Service Facades** -  `com.snakei.SensorService`, `com.snakei.LocationService`, `com.snakei.MediaService`,  `com.snakei.MiscInfoService`, `com.snakei.OutputService`
   - provide fFacades for Android resource access, i.e. sensors, location providers, media components and miscellaneous device information
   - made available to native code via JNI calls to functions that return either simple data types or String serialized JSON Objects
   - *currently access the application context using a static call to `SensibilityApplication`*
   - *these aren't Android Services, as they do not implement `android.app.Service`, but we call them Service because they run in the background*


### Native Code
 - **Snakei** - `snakei.c`
   - receives and caches a reference to JVM when loaded via `System.loadLibrary()`
   - the cached reference is used by all native extension modules
 - **Python Interpreter** - `interpreter.c`
   - defines native function, declared in `PythonInterpreterService.java`, from where it gets called
   - currently used to initialize Python modules - `init*()`, acquire resources in Java - `*_start_*()`, run some Python scripts that use the new modules, and eventually release the resources - `*_stop_*()`
 - **Sensor Init/Deint Function /  Python Extensions** - `sensors.c`, `location.c`, `media.c`, `miscinfo.c`, `outputs.c`
   - initialize Python module
   - acquire Android resources
   - provide Python extensions to access Android resources
   - release Android resources
 - **JNI glue** - `jnihelper.c`
   - helper functions that can be used by extension to call into the Android Java Virtual Machine (JVM) using the Java Native Interface (JNI)
   - some of the functions convert Java's return values to Python objects
 - **CJSON** - `cjson.c`
   - we use [Python CJSON](https://pypi.python.org/pypi/python-cjson) to decode String serialized JSON objects received from Java and convert them directly to Python objects


## Accelerometer Sensor Extension Example

![Sensibility Sequence Diagram for Acceleration Extension](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/docs/sensibility_sequence.png "Sequence Diagram for Acceleration Extension")

1. The native method `Java_com_snakei_PythonInterpreterService_startNativePythonInterpreter()` is declared in [PythonInterpreterService.java](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/java/com/snakei/PythonInterpreterService.java) and defined in [interpreter.c](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/jni/interpreter.c) and gets called from the JVM
1. This method calls [sensor.c's](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/jni/sensors.c#L340)`initsensor()` which initializes the sensor extensions in a Python module (not shown in sequence diagram) and
1. uses the custom JNI glue - [jnihelper.c](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/jni/jnihelper.c) - to lookup the required Java class and methods defined in [SensorService.java](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/java/com/snakei/SensorService.java) and caches them for later use.
1. The interpreter then calls sensor.c's `sensor_start_sensing()`, which
1. passes the previously cached Java class and methods to the the JNI glue, in order to
1. retrieve a Singleton instance of `SensorService` and
1. call `start_sensing()` on the instance which registers a listener on the main thread (not the thread the method was called from). The listener henceforth receives regular updates from the device's Accelerometer until it gets unregistered
1. The interpreter then runs a Python script which simply imports the previously initialized Python module `sensor` and calls its `get_acceleration()` function. The script is executed using a custom C wrapper defined in [pyhelper.c](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/jni/pyhelper.c)/
1. The Python function `get_acceleration()` is defined as C-Python extension in `sensors.c`, where it again 
1. uses `jnihelper.c` to get a Singleton instance of `SensorService.java` and call `getAcceleration()` on that instance, which 
1. returns the last Sensor update, received in the previously registered Listener, as String serialized JSON Object
1. `jnihelper.c` then uses [cjson.c](https://github.com/aaaaalbert/sensibility-testbed/blob/native-sensors-jni/app/src/main/jni/cjson.c) to decode the returned String as Python Object which gets returned to the calling Python script
1. Eventually, the interpreter calls `sensor_stop_sensing()` to unregister the listener in Java

## Notes on Java Native Interface (JNI)
In general the JNI can be used to call native methods from Java and vice versa. This application uses the JNI in both ways. It starts a Python interpreter - a native method that runs Python scripts - from Java and it calls into the JVM from native code, to access Android resources and make them available to Python via C-Python extensions. Most of the JNI specific calls are hidden from the extensions by `jnihelper.c`.

### Passing data from Java to native Code
Given a list of Java objects, where each object has getters that return different primitive data types, the native code would have to iterate the list
and call back into the JVM to first find each object's class and all the getters and finally call each getter. Also, Java would probably have to give back a Java List object with the capability of containing objects of different types to the native code, which can't be simply iterated but would require the native code to find the class and methods of the List's iterator using JNI and then iterate through the List also using the JNI.

To avoid this, all Java functions that are called from native code either directly return primitive data types that don't require reaching back into the JVM or convert the returned data to JSON and give back a serialized String that gets decoded in native code.

### Caching
To avoid redundant calls into the JVM, classes and methods that get repeatedly called are cached in the init function of the Python extension modules

### Helpful resources
 - [Android JNI Tips](https://developer.android.com/training/articles/perf-jni.html)
 - [JNI Specification](http://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/jniTOC.html)
 - [JNI best practices](http://www.ibm.com/developerworks/java/library/j-jni/index.html)


## API modules
The following modules are available in Python. Detailed information about each method can be found in the source code comments. 

### sensor
`sensors.c`

 - get_sensor_list()
 - get_acceleration()
 - get_ambient_temperature()
 - get_game_rotation_vector()
 - get_geomagnetic_rotation_vector()
 - get_gravity()
 - get_gyroscope()
 - get_gyroscope_uncalibrated()
 - get_heart_rate()
 - get_light()
 - get_linear_acceleration()
 - get_magnetic_field()
 - get_magnetic_field_uncalibrated()
 - get_pressure()
 - get_proximity()
 - get_relative_humidity()
 - get_rotation_vector()
 - get_step_counter()

### location
`location.c`

 - get_location()
 - get_lastknown_location()
 - get_geolocation(latitude, longitude, max_results)

### media
`media.c`

 - tts_speak(message)
 - microphone_record(file_name, duration)
 - is_media_playing()
 - is_tts_speaking()

### miscinfo
`miscinfo.c`

 - get_bluetooth_info()
 - get_bluetooth_scan_info()
 - is_wifi_enabled()
 - get_wifi_state()
 - get_wifi_connection_info()
 - get_wifi_scan_info()
 - get_network_info()
 - get_cellular_provider_info()
 - get_cell_info()
 - get_sim_info()
 - get_phone_info()
 - get_mode_settings()
 - get_display_info()
 - get_volume_info()
 - get_battery_info()

### androidlog
`outputs.c`

 - log(message)

 ## Caveats, pitfalls and notes
 *Most of them are already mentioned in the source code comments but they should also be mentioned here.*


-----
## Todo
 - Clean up build and config scripts (I think there are redundancies in gradle, AndroidManifest and Android.mk)
 - Take care of `thread-from-JVM` removal
 - Take care of save file system access
 - Install and start Seattle using JNI (don't forget about vessel management)