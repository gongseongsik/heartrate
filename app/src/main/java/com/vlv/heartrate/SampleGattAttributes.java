package com.vlv.heartrate;


import static com.vlv.heartrate.Config.BATTERY_LEVEL;
import static com.vlv.heartrate.Config.BATTERY_SERVICE;
import static com.vlv.heartrate.Config.DEVICE_INFORMATION_SERVICE;
import static com.vlv.heartrate.Config.HEART_RATE_MEASUREMENT;
import static com.vlv.heartrate.Config.HEART_RATE_SERVICE;
import static com.vlv.heartrate.Config.MANUFACTURER_NAME_STRING;
import static com.vlv.heartrate.Config.SPP_CHARACTERISTIC_COMMAND_NOFITY;
import static com.vlv.heartrate.Config.SPP_CHARACTERISTIC_COMMAND_RECEIVE;
import static com.vlv.heartrate.Config.SPP_CHARACTERISTIC_DATA_NOTIFY;
import static com.vlv.heartrate.Config.SPP_CHARACTERISTIC_DATA_RECEIVE;
import static com.vlv.heartrate.Config.SPP_SERVICE;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    static {
        // Sample Services.
        attributes.put(HEART_RATE_SERVICE, "Heart Rate Service");
        attributes.put(DEVICE_INFORMATION_SERVICE, "Device Information Service");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put(SPP_SERVICE, "SPP Service");

        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        attributes.put(BATTERY_LEVEL, "Battery_Level");

        attributes.put(SPP_CHARACTERISTIC_DATA_RECEIVE, "SPP_DATA_RECEIVE");
        attributes.put(SPP_CHARACTERISTIC_DATA_NOTIFY, "SPP_DATA_NOTIFY");
        attributes.put(SPP_CHARACTERISTIC_COMMAND_RECEIVE, "SPP_COMMAND_RECEIVE");
        attributes.put(SPP_CHARACTERISTIC_COMMAND_NOFITY, "SPP_COMMAND_NOTIFY");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
