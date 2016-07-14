import androidlog, sensor, sys, time
l = androidlog.log
l('Lets get some sensor info')

l("Sensor Info")
l(repr(sensor.get_sensor_list()))

l('Oh, wow, lovely sensors, why not poll them?')
time.sleep(10)

l("Acceleration:            " + repr(sensor.get_acceleration()))
l("Temperature:             " + repr(sensor.get_ambient_temperature()))
l("Game Rotation:           " + repr(sensor.get_game_rotation_vector()))
l("Geomagnetic Rotation:    " + repr(sensor.get_geomagnetic_rotation_vector()))
l("Gravity:                 " + repr(sensor.get_gravity()))
l("Gyroscope:               " + repr(sensor.get_gyroscope()))
l("Gyro Uncalibrated:       " + repr(sensor.get_gyroscope_uncalibrated()))
l("Heart Rate:              " + repr(sensor.get_heart_rate()))
l("Get light:               " + repr(sensor.get_light()))
l("Linear Acceleration:     " + repr(sensor.get_linear_acceleration()))
l("Magnetic field:          " + repr(sensor.get_magnetic_field()))
l("Magnetic Uncalibrated:   " + repr(sensor.get_magnetic_field_uncalibrated()))
l("Pressure:                " + repr(sensor.get_pressure()))
l("Proximity:               " + repr(sensor.get_proximity()))
l("Humidity:                " + repr(sensor.get_relative_humidity()))
l("Rotation:                " + repr(sensor.get_rotation_vector()))
l("Step counter:            " + repr(sensor.get_step_counter()))



l('Bye, bye!')