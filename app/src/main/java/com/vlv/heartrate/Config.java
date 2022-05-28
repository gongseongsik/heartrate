package com.vlv.heartrate;

public class Config {
    public static final int PERMISSION_REQUEST_READ_PHONE_NUMBER = 1;
    public static final int PERMISSION_REQUEST_ACTION_MANAGE_OVERLAY_PERMISSION = 2;

//    public static final String HEARTRATE_DEVICE_NAME = "Movesense";
    public static final String HEARTRATE_DEVICE_NAME = "1963YH";
    public static final String DATA_TYPE_HEARTRATE = "HEARTRATE";
    public static final String DATA_TYPE_BATTERY_LEVEL = "BATTERY_LEVEL";

    // Service Lists
    public static String HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    // Characteristics
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";

    public static String SPP_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String SPP_CHARACTERISTIC_DATA_RECEIVE = "0000fff6-0000-1000-8000-00805f9b34fb";
    public static String SPP_CHARACTERISTIC_DATA_NOTIFY = "0000fff7-0000-1000-8000-00805f9b34fb";
    public static String SPP_CHARACTERISTIC_COMMAND_RECEIVE = "0000abf3-0000-1000-8000-00805f9b34fb";
    public static String SPP_CHARACTERISTIC_COMMAND_NOFITY = "0000abf4-0000-1000-8000-00805f9b34fb";
}
