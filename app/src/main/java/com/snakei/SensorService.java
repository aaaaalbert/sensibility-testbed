package com.snakei;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Created by lukas.puehringer@nyu.edu
 *  on 4/20/16.
 *
 * A pseudo Service class that facades Android sensors
 *
 * Provides methods for starting and stopping sensors,
 * polling sensor values and getting sensor information
 *
 * Sensor values and information is returned as serialized JSON
 *
 * This class is a Singleton using the thread safe
 * Java Initialization on Demand Holder pattern
 * A Singleton is chosen to provide a single point of access
 * to all Sensors for all calling concurrent threads.
 * This has the advantage that once a sensor is started it can
 * be re-used without having to store the reference,
 * which is useful when calling from native code
 *
 * Todo:
 *   - Consider refactor name, this is not an actual Android "Service"
 *   - Find out if we need to worry about memory leaks
 *   - Make sure sensors are wake-up sensors
 *   - Consider better readability vs. redundancy
 */

public class SensorService implements SensorEventListener  {
    static final String TAG = "SensorService";

    Context cached_context;

    // XXX: These values will be hardcoded in calls from native code, if we 
    // change them here, we have to change them in the calling function too
    // e.g. by looking for functions that get passed integers and compare them
    // with below constants
    static final int CUSTOM_TYPE_ACCELEROMETER = 1;
    static final int CUSTOM_TYPE_AMBIENT_TEMPERATURE = 2;
    static final int CUSTOM_TYPE_GAME_ROTATION_VECTOR = 3;
    static final int CUSTOM_TYPE_GEOMAGNETIC_ROTATION_VECTOR = 4;
    static final int CUSTOM_TYPE_GRAVITY = 5;
    static final int CUSTOM_TYPE_GYROSCOPE = 6;
    static final int CUSTOM_TYPE_GYROSCOPE_UNCALIBRATED = 7;
    static final int CUSTOM_TYPE_HEART_RATE = 8;
    static final int CUSTOM_TYPE_LIGHT = 9;
    static final int CUSTOM_TYPE_LINEAR_ACCELERATION = 10;
    static final int CUSTOM_TYPE_MAGNETIC_FIELD = 11;
    static final int CUSTOM_TYPE_MAGNETIC_FIELD_UNCALIBRATED = 12;
    static final int CUSTOM_TYPE_PRESSURE = 13;
    static final int CUSTOM_TYPE_PROXIMITY = 14;
    static final int CUSTOM_TYPE_RELATIVE_HUMIDITY = 15;
    static final int CUSTOM_TYPE_ROTATION_VECTOR = 16;
    static final int CUSTOM_TYPE_STEP_COUNTER = 17;

    // Used to initialize all the sensors
    private SensorManager sensor_manager;

    // We need all of those sensors to register listener on them
    // Most of the devices won't support most of the sensors
    // But hey, if Android does, so do we
    // Note:
    // Using sensors that only fire an event when something interesting happened
    // are omitted: TYPE_SIGNIFICANT_MOTION, TYPE_STEP_DETECTOR
    private Sensor accelerometer;
    private Sensor ambient_temperature;
    private Sensor game_rotation_vector;
    private Sensor geomagnetic_rotation_vector;
    private Sensor gravity;
    private Sensor gyroscope;
    private Sensor gyroscope_uncalibrated;
    private Sensor heart_rate;
    private Sensor light;
    private Sensor linear_acceleration;
    private Sensor magnetic_field;
    private Sensor magnetic_field_uncalibrated;
    private Sensor pressure;
    private Sensor proximity;
    private Sensor relative_humidity;
    private Sensor rotation_vector;
    private Sensor step_counter;

    // For each Sensor we store the data (timestamp + values) that we received 
    // upon a previously registered ``onSensorChange'' as JSONArray
    // New values overwrite previous values
    private JSONArray accelerometer_json;
    private JSONArray ambient_temperature_json;
    private JSONArray game_rotation_vector_json;
    private JSONArray geomagnetic_rotation_vector_json;
    private JSONArray gravity_json;
    private JSONArray gyroscope_json;
    private JSONArray gyroscope_uncalibrated_json;
    private JSONArray heart_rate_json;
    private JSONArray light_json;
    private JSONArray linear_acceleration_json;
    private JSONArray magnetic_field_json;
    private JSONArray magnetic_field_uncalibrated_json;
    private JSONArray pressure_json;
    private JSONArray proximity_json;
    private JSONArray relative_humidity_json;
    private JSONArray rotation_vector_json;
    private JSONArray step_counter_json;

    /* See Initialization on Demand Holder pattern */
    private static class SensorServiceHolder {
        private static final SensorService instance = new SensorService();
    }


    /* Singleton Instance Getter */
    public static SensorService getInstance(){
        return SensorServiceHolder.instance;
    }


    /*
     * Initializes default sensors for each sensor type
     *
     */
    public void init(Context context) {
        Log.d(TAG, "Entering init");

        cached_context = context;
        Log.d(TAG, "Requesting android sensor system service");
        sensor_manager = (SensorManager)cached_context.getSystemService(
            cached_context.SENSOR_SERVICE);

        // I guess we can get all the sensors right away
        accelerometer = sensor_manager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER);
        ambient_temperature = sensor_manager.getDefaultSensor(
            Sensor.TYPE_AMBIENT_TEMPERATURE);
        game_rotation_vector = sensor_manager.getDefaultSensor(
            Sensor.TYPE_GAME_ROTATION_VECTOR);
        geomagnetic_rotation_vector = sensor_manager.getDefaultSensor(
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        gravity = sensor_manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gyroscope = sensor_manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyroscope_uncalibrated = sensor_manager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        heart_rate = sensor_manager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        light = sensor_manager.getDefaultSensor(Sensor.TYPE_LIGHT);
        linear_acceleration = sensor_manager.getDefaultSensor(
            Sensor.TYPE_LINEAR_ACCELERATION);
        magnetic_field = sensor_manager.getDefaultSensor(
            Sensor.TYPE_MAGNETIC_FIELD);
        magnetic_field_uncalibrated = sensor_manager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
        pressure = sensor_manager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        proximity = sensor_manager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        relative_humidity = sensor_manager.getDefaultSensor(
            Sensor.TYPE_RELATIVE_HUMIDITY);
        rotation_vector = sensor_manager.getDefaultSensor(
            Sensor.TYPE_ROTATION_VECTOR);
        step_counter = sensor_manager.getDefaultSensor(
            Sensor.TYPE_STEP_COUNTER);
    }


    /*
     * Returns list of sensors supported on the device with
     * sensor specific information
     * 
     * @return  String serialized JSON array of sensor info objects or null
     *
     * @todo We should loop better, and perform better error checking and signaling
     * 
     * e.g. (de-serialized):
     * [{'max_range': 100,
     *   'vendor': 'Sensortek',
     *   'name': 'STK3310 Proximity sensor',
     *   'power': 3,
     *   'fifo_reserved_event_count': 0,
     *   'is_wakeup_sensor': True,
     *   'fifo_max_event_count': 0,
     *   'string_type': 'android.sensor.proximity',
     *   'max_delay': 0,
     *   'reporting_mode': 1,
     *   'version': 1,
     *   'type': 8,
     *   'resolution': 100,
     *   'min_delay': 0}, ...]
     */
    public String getSensorList() throws JSONException {
        Log.d(TAG, "Entering getSensorList");

        List<Sensor> sensor_list = sensor_manager.getSensorList(Sensor.TYPE_ALL);
        if (sensor_list.size() > 0) {
            JSONArray sensors_properties_array_json = new JSONArray();
            int count = 0;
            int total = 0;
            for (Sensor sensor : sensor_list) {
                JSONObject sensor_json = new JSONObject();
                try {
                    total += 1;
                    sensor_json.put("fifo_max_event_count",
                            sensor.getFifoMaxEventCount());
                    sensor_json.put("fifo_reserved_event_count",
                            sensor.getFifoReservedEventCount());
                    sensor_json.put("max_delay", sensor.getMaxDelay());
                    sensor_json.put("min_delay", sensor.getMinDelay());
                    sensor_json.put("name", sensor.getName());
                    sensor_json.put("reporting_mode", sensor.getReportingMode());
                    sensor_json.put("string_type", sensor.getStringType());
                    sensor_json.put("type", sensor.getType());
                    sensor_json.put("vendor", sensor.getVendor());
                    sensor_json.put("version", sensor.getVersion());
                    sensor_json.put("is_wakeup_sensor", sensor.isWakeUpSensor());


                    String type =  sensor.getStringType();
                    Pattern regex = Pattern.compile("\\.(\\S+)");
                    String nameShow;
                    Matcher match = regex.matcher(type);
                    if (match.find()) {
                        String s = match.group(1);
                        nameShow = s.substring(7);
                        nameShow = nameShow.substring(0, 1).toUpperCase() + nameShow.substring(1);
                        nameShow = nameShow.replace("_"," ");
                    }
                    else{
                        nameShow = "Misc";
                    }
                    sensor_json.put("show_name",nameShow);

                    // Properties encoded as floats may contain NaN's.
                    // JSON and the JSON encoder cannot handle them.
                    // Thus, suppress these properties.
                    Map<String, Float> m = new HashMap<String, Float>();
                    m.put("max_range", sensor.getMaximumRange());
                    m.put("power", sensor.getPower());
                    m.put("resolution", sensor.getResolution());
                    for (Map.Entry<String, Float> entry : m.entrySet()) {
                        if (! Float.isNaN(entry.getValue())) {
                            sensor_json.put(entry.getKey(), entry.getValue());
                        }
                    }

                } catch (org.json.JSONException e) {
                    sensor_json.put("***ERROR***", e.toString());
                }

                sensors_properties_array_json.put(sensor_json);
            }
            return sensors_properties_array_json.toString();
        }
        return null;
    }

    /*
     * Returns most recent acceleration values
     * requires preceding start_sensing(CUSTOM_TYPE_ACCELEROMETER)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Acceleration force along the x axis (including gravity),
     * Acceleration force along the y axis (including gravity),
     * Acceleration force along the z axis (including gravity)
     * ]
     *
     */
    public String getAcceleration() {
        Log.d(TAG, "Entering getAcceleration");

        if (accelerometer_json != null) {
            return accelerometer_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent ambient temperature
     * requires preceding start_sensing(CUSTOM_TYPE_AMBIENT_TEMPERATURE)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Ambient air temperature in °C
     * ]
     *
     */
    public String getAmbientTemperature() {
        Log.d(TAG, "Entering getAmbientTemperature");

        if (ambient_temperature_json != null) {
            return ambient_temperature_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent game rotation vector values
     * requires preceding start_sensing(CUSTOM_TYPE_GAME_ROTATION_VECTOR)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Rotation vector component along the x axis (x * sin(θ/2)),
     * Rotation vector component along the y axis (y * sin(θ/2)),
     * Rotation vector component along the z axis (z * sin(θ/2))
     * ]
     *
     */
    public String getGameRotationVector() {
        Log.d(TAG, "Entering getGameRotationVector");

        if (game_rotation_vector_json != null) {
            return game_rotation_vector_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent geomagnetic rotation vector values
     * requires preceding start_sensing(CUSTOM_TYPE_GEOMAGNETIC_ROTATION_VECTOR)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Rotation vector component along the x axis (x * sin(θ/2)),
     * Rotation vector component along the y axis (y * sin(θ/2)),
     * Rotation vector component along the z axis (z * sin(θ/2))
     * ]
     *
     */
    public String getGeomagneticRotationVector() {
        Log.d(TAG, "Entering getGeomagneticRotationVector");

        if (geomagnetic_rotation_vector_json != null) {
            return geomagnetic_rotation_vector_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent gravity values
     * requires preceding start_sensing(CUSTOM_TYPE_GRAVITY)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Force of gravity along the x axis,
     * Force of gravity along the y axis,
     * Force of gravity along the z axis
     * ]
     * 
     */
    public String getGravity() {
        Log.d(TAG, "Entering getGravity");

        if (gravity_json != null) {
            return gravity_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent gyroscope values
     * requires preceding start_sensing(CUSTOM_TYPE_GYROSCOPE)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received,
     * Time propagated,
     * Rate of rotation around the x axis,
     * Rate of rotation around the y axis,
     * Rate of rotation around the z axis
     * ]
     * 
     */
    public String getGyroscope() {
        Log.d(TAG, "Entering getGyroscope");

        if (gyroscope_json != null) {
            return gyroscope_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent uncalibrated gyroscope values
     * requires preceding start_sensing(CUSTOM_TYPE_GYROSCOPE_UNCALIBRATED)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Rate of rotation (without drift compensation) around the x axis,
     * Rate of rotation (without drift compensation) around the y axis,
     * Rate of rotation (without drift compensation) around the z axis,
     * Estimated drift around the x axis,
     * Estimated drift around the y axis,
     * Estimated drift around the z axis
     * ]
     * 
     */
    public String getGyroscopeUncalibrated() {
        Log.d(TAG, "Entering getGyroscopeUncalibrated");

        if (gyroscope_uncalibrated_json != null) {
            return gyroscope_uncalibrated_json.toString();
        }
        return null;
    }

  

    /*
     * Returns most recent heart rate value
     * requires preceding start_sensing(CUSTOM_TYPE_HEART_RATE)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Heart rate in beats per minute
     * ]
     *
     */
    public String getHeartRate() {
        Log.d(TAG, "Entering getHeartRate");

        if (heart_rate_json != null) {
            return heart_rate_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent light value
     * requires preceding start_sensing(CUSTOM_TYPE_LIGHT)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Illuminance lx
     * ]
     *
     */
    public String getLight() {
        Log.d(TAG, "Entering getLight");

        if (light_json != null) {
            return light_json.toString();
        }
        return null;
    }

    /*
     * Returns most recent linear acceleration values
     * requires preceding start_sensing(CUSTOM_TYPE_LINEAR_ACCELERATION)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Acceleration force along the x axis (excluding gravity),
     * Acceleration force along the y axis (excluding gravity),
     * Acceleration force along the z axis (excluding gravity)
     * ]
     * 
     */
    public String getLinearAcceleration() {
        Log.d(TAG, "Entering getLinearAcceleration");

        if (linear_acceleration_json != null) {
            return linear_acceleration_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent magnetic field values
     * requires preceding start_sensing(CUSTOM_TYPE_MAGNETIC_FIELD)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Geomagnetic field strength along the x axis,
     * Geomagnetic field strength along the y axis,
     * Geomagnetic field strength along the z axis
     * ]
     * 
     */
    public String getMagneticField() {
        Log.d(TAG, "Entering getMagneticField");

        if (magnetic_field_json != null) {
            return magnetic_field_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent uncalibrated magnetic field values
     * requires preceding start_sensing(CUSTOM_TYPE_MAGNETIC_FIELD_UNCALIBRATED)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Geomagnetic field strength (w/o hard iron calibration) along the x axis,
     * Geomagnetic field strength (w/o hard iron calibration) along the y axis,
     * Geomagnetic field strength (w/o hard iron calibration) along the z axis,
     * Iron bias estimation along the x axis,
     * Iron bias estimation along the y axis,
     * Iron bias estimation along the z axis
     * ]
     * 
     */
    public String getMagneticFieldUncalibrated() {
        Log.d(TAG, "Entering getMagneticFieldUncalibrated");

        if (magnetic_field_uncalibrated_json != null) {
            return magnetic_field_uncalibrated_json.toString();
        }
        return null;
    }
   

    /*
     * Returns most recent linear pressure values
     * requires preceding start_sensing(CUSTOM_TYPE_PRESSURE)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Ambient air pressure in hPa or mbar 
     * ]
     * 
     */
    public String getPressure() {
        Log.d(TAG, "Entering getPressure");

        if (pressure_json != null) {
            return pressure_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent proximity values
     * requires preceding start_sensing(CUSTOM_TYPE_PROXIMITY)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Distance from object in cm
     * ]
     * 
     */
    public String getProximity() {
        Log.d(TAG, "Entering getProximity");

        if (proximity_json != null) {
            return proximity_json.toString();
        }
        return null;
    }


    /*
     * Returns most recentrelative humidity values
     * requires preceding start_sensing(CUSTOM_TYPE_RELATIVE_HUMIDITY)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Ambient relative humidity in %
     * ]
     * 
     */
    public String getRelativeHumidity() {
        Log.d(TAG, "Entering getRelativeHumidity");

        if (relative_humidity_json != null) {
            return relative_humidity_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent rotation vector values
     * requires preceding start_sensing(CUSTOM_TYPE_ROTATION_VECTOR)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Rotation vector component along the x axis (x * sin(θ/2)),
     * Rotation vector component along the y axis (y * sin(θ/2)),
     * Rotation vector component along the z axis (z * sin(θ/2)),
     * Scalar component of the rotation vector ((cos(θ/2))
     * ]
     * 
     */
    public String getRotationVector() {
        Log.d(TAG, "Entering getRotationVector");

        if (rotation_vector_json != null) {
            return rotation_vector_json.toString();
        }
        return null;
    }


    /*
     * Returns most recent step count
     * requires preceding start_sensing(CUSTOM_TYPE_STEP_COUNTER)
     *
     * @return  String serialized JSON array of sensor values or null
     * i.e.:
     * [
     * Time received, 
     * Time propagated,
     * Number of steps taken by the user since the last reboot 
     *   while the sensor was activated
     * ]
     * 
     */ 
    public String getStepCounter() {
        Log.d(TAG, "Entering getStepCounter");

        if (step_counter_json != null) {
            return step_counter_json.toString();
        }
        return null;
    }

    /*
     * Registers listener for sensor value updates for a specific sensor
     * 
     * @params  int - Sensor type (c.f. CUSTOM_TYPE_* constants)
     */
    public void start_sensing(int sensor_type) {
        Log.d(TAG, "Entering start_sensing");

        Sensor tmp_sensor = null;

        if (sensor_type == CUSTOM_TYPE_ACCELEROMETER) {
            tmp_sensor = accelerometer;
        } else if (sensor_type == CUSTOM_TYPE_AMBIENT_TEMPERATURE) {
            tmp_sensor = ambient_temperature;
        } else if (sensor_type == CUSTOM_TYPE_GAME_ROTATION_VECTOR) {
            tmp_sensor = game_rotation_vector;
        } else if (sensor_type == CUSTOM_TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            tmp_sensor = geomagnetic_rotation_vector;
        } else if (sensor_type == CUSTOM_TYPE_GRAVITY) {
            tmp_sensor = gravity;
        } else if (sensor_type == CUSTOM_TYPE_GYROSCOPE) {
            tmp_sensor = gyroscope;
        } else if (sensor_type == CUSTOM_TYPE_GYROSCOPE_UNCALIBRATED) {
            tmp_sensor = gyroscope_uncalibrated;
        } else if (sensor_type == CUSTOM_TYPE_HEART_RATE) {
            tmp_sensor = heart_rate;
        } else if (sensor_type == CUSTOM_TYPE_LIGHT) {
            tmp_sensor = light;
        } else if (sensor_type == CUSTOM_TYPE_LINEAR_ACCELERATION) {
            tmp_sensor = linear_acceleration;
        } else if (sensor_type == CUSTOM_TYPE_MAGNETIC_FIELD) {
            tmp_sensor = magnetic_field;
        } else if (sensor_type == CUSTOM_TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
            tmp_sensor = magnetic_field_uncalibrated;
        } else if (sensor_type == CUSTOM_TYPE_PRESSURE) {
            tmp_sensor = pressure;
        } else if (sensor_type == CUSTOM_TYPE_PROXIMITY) {
            tmp_sensor = proximity;
        } else if (sensor_type == CUSTOM_TYPE_RELATIVE_HUMIDITY) {
            tmp_sensor = relative_humidity;
        } else if (sensor_type == CUSTOM_TYPE_ROTATION_VECTOR) {
            tmp_sensor = rotation_vector;
        } else if (sensor_type == CUSTOM_TYPE_STEP_COUNTER) {
            tmp_sensor = step_counter;
        } else {
            Log.d(TAG, String.format("Sensor %d does not exist or app has not the necessary permissions.", sensor_type));
        }

        // All generally existing sensors were declared in the constructor
        // If a Sensor is not available this means it doesn't exist on the 
        // device or the app doesn't have the necessary permissions, or the
        // calling function passed a wrong constant
        // Note: Android checks that listener are registered only once
        if (tmp_sensor != null) {
            sensor_manager.registerListener(this, tmp_sensor, 
                SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    /*
     * De-registers listener for sensor value updates for a specific sensor
     * @params  int - Sensor type (c.f. CUSTOM_TYPE_* constants)
     *
     * CAUTION:
     *   In multi-threading environments other threads may still want to 
     *   use a shared listener
     * 
     */
    public void stop_sensing(int sensor_type) {
        Log.d(TAG, "Entering stop_sensing on sensor type " + java.lang.Integer.toString(sensor_type));

        Sensor tmp_sensor = null;

        if (sensor_type == CUSTOM_TYPE_ACCELEROMETER) {
            tmp_sensor = accelerometer;
        } else if (sensor_type == CUSTOM_TYPE_AMBIENT_TEMPERATURE) {
            tmp_sensor = ambient_temperature;
        } else if (sensor_type == CUSTOM_TYPE_GAME_ROTATION_VECTOR) {
            tmp_sensor = game_rotation_vector;
        } else if (sensor_type == CUSTOM_TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            tmp_sensor = geomagnetic_rotation_vector;
        } else if (sensor_type == CUSTOM_TYPE_GRAVITY) {
            tmp_sensor = gravity;
        } else if (sensor_type == CUSTOM_TYPE_GYROSCOPE) {
            tmp_sensor = gyroscope;
        } else if (sensor_type == CUSTOM_TYPE_GYROSCOPE_UNCALIBRATED) {
            tmp_sensor = gyroscope_uncalibrated;
        } else if (sensor_type == CUSTOM_TYPE_HEART_RATE) {
            tmp_sensor = heart_rate;
        } else if (sensor_type == CUSTOM_TYPE_LIGHT) {
            tmp_sensor = light;
        } else if (sensor_type == CUSTOM_TYPE_LINEAR_ACCELERATION) {
            tmp_sensor = linear_acceleration;
        } else if (sensor_type == CUSTOM_TYPE_MAGNETIC_FIELD) {
            tmp_sensor = magnetic_field;
        } else if (sensor_type == CUSTOM_TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
            tmp_sensor = magnetic_field_uncalibrated;
        } else if (sensor_type == CUSTOM_TYPE_PRESSURE) {
            tmp_sensor = pressure;
        } else if (sensor_type == CUSTOM_TYPE_PROXIMITY) {
            tmp_sensor = proximity;
        } else if (sensor_type == CUSTOM_TYPE_RELATIVE_HUMIDITY) {
            tmp_sensor = relative_humidity;
        } else if (sensor_type == CUSTOM_TYPE_ROTATION_VECTOR) {
            tmp_sensor = rotation_vector;
        } else if (sensor_type == CUSTOM_TYPE_STEP_COUNTER) {
            tmp_sensor = step_counter;
        } else {
            // XXX: Maybe raise an exception here
            Log.d(TAG, String.format("Can't unregister a Sensor that does not exist: $d", sensor_type));
        }
        if (tmp_sensor != null) {
            sensor_manager.unregisterListener(this, tmp_sensor);
        }
    }

    /*
     * Extracts the sensor values from a received sensor update event and
     * creates a JSONArray adding the current time and the time at event
     * propagation.
     * 
     * Stores the JSONArray to the according sensor's data class member
     * of this Service Class
     *
     * [<current time>, <time at event propagation>,
     * <sensor value[0]>, ..., <sensor value[n-1]>]
     *
     * Todo:
     *   Find out if reading and writing to SensorEvents concurrently
     *   requires us to use a Lock
     *
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        // Having one data type makes it easier to give it back to C
        JSONArray result = new JSONArray();

        result.put(System.currentTimeMillis());
        result.put(event.timestamp / 1000000);

        // Note: we always get float arrays but the length varies
        // from Sensor to Sensor
        for (int i = 0; i < event.values.length; i++){
            try {
                result.put(event.values[i]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometer_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            ambient_temperature_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR){
            game_rotation_vector_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR){
            geomagnetic_rotation_vector_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY){
            gravity_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyroscope_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED){
            gyroscope_uncalibrated_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE){
            heart_rate_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT){
            light_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            linear_acceleration_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            magnetic_field_json = result;
        } else if (event.sensor.getType() ==
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED){
            magnetic_field_uncalibrated_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_PRESSURE){
            pressure_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY){
            proximity_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY){
            relative_humidity_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            rotation_vector_json = result;
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            step_counter_json = result;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //pass
    }

}
