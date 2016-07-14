/*
 * Created by
 * albert.rafetseder@univie.ac.at
 * lukas.puehringer@nyu.edu
 * on 5/29/16
 *
 * C Python module uses SensorService.java via JNI to return sensor
 * values
 *
 * Usage:
 * Module initialization - call initsensor() from C
 *  - Initializes Python module (sensor)
 *  - Caches native reference to Java Singleton Class SensorService.java
 *  - Caches native reference to Singleton getter and Java Methods
 *
 * Sensor initialization - call sensor_start_sensing from C 
 *  - Calls start_sensing Java Method for a specific Sensor to register Sensor 
 *    update listener
 *  - Cf. SensorService.java constants CUSTOM_TYPE_* for a list of all sensors
 *
 * Get Sensor values - call sensor.get_* from Python
 *  - Calls the according get* Method in Java
 *  - Return values are documented in SensorService.java
 *
 * Sensor de-initialization - call sensor_stop_sensing from C 
 *  - Calls stop_sensing Java Method for specific Sensor to unregister 
 *    Sensor update listener in order to free resources
 *
 * Note:
 * We need to think of
 *  - acquiring and releasing resources
 *  - dealing with multiple sensors for one type
 *  - dealing with multiple threads concurrently accessing a sensor
 *
 * Possible other approaches:
 *  - One function for all sensors, like in current Repy Sensor API [1]
 *  - Pass callback functions (cool but harder) that receive the sensor values 
 *    from Java on update
 *    e.g.:
 *      start_sensing(TYPE_ACCELEROMETER, callback_fn_receives_sensor_values)
 *      stop_sensing(TYPE_ACCELEROMETER)
 *  - Go OO, like Yocto Python API[2] (not really pythonic, is it?)
 *    e.g.:
 *      sensor = find_sensor(TYPE_ACCELEROMETER)
 *      sensor.start()
 *      sensor.getValue()
 *      sensor.stop()
 *
 * [1] https://sensibilitytestbed.com/projects/project/wiki/sensors#Sensors
 * [2] http://www.yoctopuce.com/EN/products/yocto-meteo/doc/
 *              METEOMK1.usermanual.html#CHAP14
 *
 */
#include "sensors.h"
 

/*
 * Caches native references of used Java Class and Java Methods
 */
static struct sensor_cache {
    jclass class;
    jmethodID get_instance;
    jmethodID start_sensing;
    jmethodID stop_sensing;
    jmethodID get_sensor_list;
    jmethodID get_acceleration;
    jmethodID get_ambient_temperature;
    jmethodID get_game_rotation_vector;
    jmethodID get_geomagnetic_rotation_vector;
    jmethodID get_gravity;
    jmethodID get_gyroscope;
    jmethodID get_gyroscope_uncalibrated;
    jmethodID get_heart_rate;
    jmethodID get_light;
    jmethodID get_linear_acceleration;
    jmethodID get_magnetic_field;
    jmethodID get_magnetic_field_uncalibrated;
    jmethodID get_pressure;
    jmethodID get_proximity;
    jmethodID get_relative_humidity;
    jmethodID get_rotation_vector;
    jmethodID get_step_counter;
} m_cached;


/*
 * Calls Java to register a SensorEventListener for a specific
 * sensor to receive Sensor updates
 * Cf. SensorService.java constants CUSTOM_TYPE_* for a list of all sensors
 *
 * Not in Python module - needs to be called from C!
 *
 */
void sensor_start_sensing(int sensor_type) {
    jh_call(m_cached.class, m_cached.get_instance, jh_callVoidMethod,
                   m_cached.start_sensing, (jint) sensor_type);
}


/*
 * Calls Java to unregister the SensorEventListener of a specific
 * sensor to stop receiving Sensor updates
 * Cf. SensorService.java constants CUSTOM_TYPE_* for a list of all sensors
 *
 * Not in Python module - needs to be called from C!
 */
void sensor_stop_sensing(int sensor_type) {
    jh_call(m_cached.class, m_cached.get_instance, jh_callVoidMethod,
                   m_cached.stop_sensing, (jint) sensor_type);
}


/*
 * Cf. getSensorList() in SensorService.java for details
 */
PyObject* sensor_get_sensor_list(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_sensor_list);
}


/*
 * Cf. getAcceleration() in SensorService.java for details
 */
PyObject* sensor_get_acceleration(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_acceleration);
}


/*
 * Cf. getAmbientTemperature() in SensorService.java for details
 */
PyObject* sensor_get_ambient_temperature(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_ambient_temperature);
}


/*
 * Cf. getGameRotationVector() in SensorService.java for details
 */
PyObject* sensor_get_game_rotation_vector(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_game_rotation_vector);
}


/*
 * Cf. getGeomagneticRotationVector() in SensorService.java for details
 */
PyObject* sensor_get_geomagnetic_rotation_vector(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_geomagnetic_rotation_vector);
}


/*
 * Cf. getGravity() in SensorService.java for details
 */
PyObject* sensor_get_gravity(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_gravity);
}


/*
 * Cf. getGyroscope() in SensorService.java for details
 */
PyObject* sensor_get_gyroscope(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_gyroscope);
}


/*
 * Cf. getGyroscopeUncalibrated() in SensorService.java for details
 */
PyObject* sensor_get_gyroscope_uncalibrated(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_gyroscope_uncalibrated);
}


/*
 * Cf. getHeartRate() in SensorService.java for details
 */
PyObject* sensor_get_heart_rate(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_heart_rate);
}


/*
 * Cf. getLight() in SensorService.java for details
 */
PyObject* sensor_get_light(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_light);
}


/*
 * Cf. getLinearAcceleration() in SensorService.java for details
 */
PyObject* sensor_get_linear_acceleration(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_linear_acceleration);
}


/*
 * Cf. getMagneticField() in SensorService.java for details
 */
PyObject* sensor_get_magnetic_field(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_magnetic_field);
}


/*
 * Cf. getMagneticFieldUncalibrated() in SensorService.java for details
 */
PyObject* sensor_get_magnetic_field_uncalibrated(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_magnetic_field_uncalibrated);
}


/*
 * Cf. getPressure() in SensorService.java for details
 */
PyObject* sensor_get_pressure(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_pressure);
}


/*
 * Cf. getProximity() in SensorService.java for details
 */
PyObject* sensor_get_proximity(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_proximity);
}


/*
 * Cf. getRelativeHumidity() in SensorService.java for details
 */
PyObject* sensor_get_relative_humidity(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_relative_humidity);
}


/*
 * Cf. getRotationVector() in SensorService.java for details
 */
PyObject* sensor_get_rotation_vector(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_rotation_vector);
}


/*
 * Cf. getStepCounter() in SensorService.java for details
 */
PyObject* sensor_get_step_counter(PyObject *self) {
    return jh_call(m_cached.class, m_cached.get_instance, 
            jh_callJsonStringMethod, m_cached.get_step_counter);
}


/*
 * Maps C functions to Python module methods
 */
static PyMethodDef AndroidSensorMethods[] = {
        {"get_sensor_list", (PyCFunction) sensor_get_sensor_list, METH_NOARGS,
         "Returns list of sensor info dictionaries"},
        {"get_acceleration", 
        (PyCFunction) sensor_get_acceleration, METH_NOARGS,
         "Returns list of acceleration values"},
        {"get_ambient_temperature", 
        (PyCFunction) sensor_get_ambient_temperature, METH_NOARGS,
         "Returns list of ambient temperature values"},
        {"get_game_rotation_vector", 
        (PyCFunction) sensor_get_game_rotation_vector, METH_NOARGS,
         "Returns list of rotation values"},
        {"get_geomagnetic_rotation_vector", 
        (PyCFunction) sensor_get_geomagnetic_rotation_vector, METH_NOARGS,
         "Returns list of geomagnetic rotation vector values"},
        {"get_gravity", (PyCFunction) sensor_get_gravity, METH_NOARGS,
         "Returns list of gravity values"},
        {"get_gyroscope", (PyCFunction) sensor_get_gyroscope, METH_NOARGS,
         "Return list of gyroscope values"},
        {"get_gyroscope_uncalibrated", 
        (PyCFunction) sensor_get_gyroscope_uncalibrated, METH_NOARGS,
         "Return list of uncalibrated gyroscope values"},
        {"get_heart_rate", (PyCFunction) sensor_get_heart_rate, METH_NOARGS,
         "Return list of heart rate values"},
        {"get_light", (PyCFunction) sensor_get_light, METH_NOARGS,
         "Return list of light values"},
        {"get_linear_acceleration", 
        (PyCFunction) sensor_get_linear_acceleration, METH_NOARGS,
         "Return list of linear acceleration values"},
        {"get_magnetic_field", 
        (PyCFunction) sensor_get_magnetic_field, METH_NOARGS,
         "Return list of magnetic field values"},
        {"get_magnetic_field_uncalibrated", 
        (PyCFunction) sensor_get_magnetic_field_uncalibrated, METH_NOARGS,
         "Return list of uncalibrated magnetic field values"},
        {"get_pressure", (PyCFunction) sensor_get_pressure, METH_NOARGS,
         "Return list of pressure values"},
        {"get_proximity", (PyCFunction) sensor_get_proximity, METH_NOARGS,
         "Return list of proximity values"},
        {"get_relative_humidity", 
        (PyCFunction) sensor_get_relative_humidity, METH_NOARGS,
         "Return list of relative humidity values"},
        {"get_rotation_vector", 
        (PyCFunction) sensor_get_rotation_vector, METH_NOARGS,
         "Return list of rotation vector values"},
        {"get_step_counter", 
        (PyCFunction) sensor_get_step_counter, METH_NOARGS,
         "Return list of step counter values"},
        {NULL, NULL, 0, NULL} // This is the end-of-array marker
};


/*
 * Initializes Python module (sensor), looks up Java class and Java Methods
 * used to poll sensor values and stores them to cache
 *
 * Note:
 * If we wanted to build the module as .so or .dll we could
 * would have to change the signature to
 * PyMODINIT_FUNC initsensor(void)
 *
 */
void initsensor() {
    Py_InitModule("sensor", AndroidSensorMethods);
    jclass class = jh_getClass("com/snakei/SensorService");

    m_cached = (struct sensor_cache){
            .class = class,
            .get_instance = jh_getGetter(class, 
                    "()Lcom/snakei/SensorService;"),
            .start_sensing = jh_getMethod(class, 
                    "start_sensing", "(I)V"),
            .stop_sensing = jh_getMethod(class, 
                    "stop_sensing", "(I)V"),
            .get_sensor_list = jh_getMethod(class, 
                    "getSensorList", "()Ljava/lang/String;"),
            .get_acceleration = jh_getMethod(class, 
                    "getAcceleration", "()Ljava/lang/String;"),
            .get_ambient_temperature = jh_getMethod(class,
                    "getAmbientTemperature", "()Ljava/lang/String;"),
            .get_game_rotation_vector = jh_getMethod(class, 
                    "getGameRotationVector", "()Ljava/lang/String;"),
            .get_geomagnetic_rotation_vector = jh_getMethod(class, 
                    "getGeomagneticRotationVector", "()Ljava/lang/String;"),
            .get_gravity = jh_getMethod(class, "getGravity", 
                    "()Ljava/lang/String;"),
            .get_gyroscope = jh_getMethod(class, "getGyroscope", 
                    "()Ljava/lang/String;"),
            .get_gyroscope_uncalibrated = jh_getMethod(class, 
                    "getGyroscopeUncalibrated", "()Ljava/lang/String;"),
            .get_heart_rate = jh_getMethod(class, "getHeartRate", 
                    "()Ljava/lang/String;"),
            .get_light = jh_getMethod(class, "getLight", 
                    "()Ljava/lang/String;"),
            .get_linear_acceleration = jh_getMethod(class, 
                    "getLinearAcceleration", "()Ljava/lang/String;"),
            .get_magnetic_field = jh_getMethod(class, "getMagneticField",
                    "()Ljava/lang/String;"),
            .get_magnetic_field_uncalibrated = jh_getMethod(class, 
                    "getMagneticFieldUncalibrated", "()Ljava/lang/String;"),
            .get_pressure = jh_getMethod(class, "getPressure", 
                    "()Ljava/lang/String;"),
            .get_proximity = jh_getMethod(class, "getProximity", 
                    "()Ljava/lang/String;"),
            .get_relative_humidity = jh_getMethod(class, "getRelativeHumidity",
                    "()Ljava/lang/String;"),
            .get_rotation_vector = jh_getMethod(class, "getRotationVector",
                    "()Ljava/lang/String;"),
            .get_step_counter = jh_getMethod(class, "getStepCounter", 
                    "()Ljava/lang/String;")};
}
