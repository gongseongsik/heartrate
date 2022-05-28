package com.vlv.heartrate;

import static com.jstyle.blesdk1963base.Util.ResolveUtil.ByteToHexString;
import static com.jstyle.blesdk1963base.Util.ResolveUtil.getValue;
import static com.vlv.heartrate.Config.BATTERY_LEVEL;
import static com.vlv.heartrate.Config.CLIENT_CHARACTERISTIC_CONFIG;
import static com.vlv.heartrate.Config.HEART_RATE_MEASUREMENT;
import static com.vlv.heartrate.Config.SPP_CHARACTERISTIC_DATA_NOTIFY;
import static com.vlv.heartrate.Config.SPP_CHARACTERISTIC_DATA_RECEIVE;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.jstyle.blesdk1963base.Util.BleSDK;
import com.jstyle.blesdk1963base.constant.BleConst;
import com.jstyle.blesdk1963base.constant.DeviceConst;
import com.jstyle.blesdk1963base.constant.DeviceKey;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String DATA_TYPE =
            "com.example.bluetooth.le.DATA_TYPE";

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(HEART_RATE_MEASUREMENT);
    public final static UUID UUID_BATTERY_LEVEL = UUID.fromString(BATTERY_LEVEL);
    public final static UUID UUID_SPP_DATA_NOTIFY = UUID.fromString(SPP_CHARACTERISTIC_DATA_NOTIFY);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            Log.i("TAG", "onConnectionStateChange");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i("TAG", "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i("TAG", "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i("TAG", "onCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        Log.i("TAG", "broadcastUpdate char = " + characteristic.getUuid().toString());

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(DATA_TYPE, "HEARTRATE");
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            final int battery_level = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            intent.putExtra(DATA_TYPE, "BATTERY_LEVEL");
            intent.putExtra(EXTRA_DATA, String.valueOf(battery_level));
            Log.d(TAG, String.format("Received battery_level: %d", battery_level));
        } else if (UUID_SPP_DATA_NOTIFY.equals(characteristic.getUuid())) {
//            final int spp_char = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            final byte[] spp_chars = characteristic.getValue();
            final String temp;
            temp = new String(spp_chars, StandardCharsets.UTF_8);
            Log.d(TAG, "Received spp_char: ");
//            getDeviceTime(spp_chars);
//            sendToDevice(spp_chars);
            DataParsingWithData(spp_chars);
            return;
        } else {
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
        }
        sendBroadcast(intent);
    }

    public static void DataParsingWithData(byte[] value) {
        switch (value[0]) {
            case DeviceConst.CMD_Set_Goal:
                break;
            case DeviceConst.CMD_GET_TIME:
                getDeviceTime(value);
                break;
            case DeviceConst.CMD_Get_Version:
                getDeviceVersion(value);
                break;
            case DeviceConst.CMD_Get_Name:
                getDeviceName(value);
                break;
        }
    }

    public static Map<String,Object> getDeviceName(byte[] value) {
        Map<String,Object> maps=new HashMap<>();
        maps.put(DeviceKey.DataType, BleConst.GetDeviceName);
        maps.put(DeviceKey.End,true);
        Map<String,String>mapData=new HashMap<>();
        maps.put(DeviceKey.Data, mapData);
        String name = "";
        for (int i = 1; i < 15; i++) {
            int charValue=getValue(value[i],0);
            if(charValue==0||charValue>127)continue;
            name += (char) charValue;
        }
        mapData.put(DeviceKey.DeviceName, name);
        Log.i("TAG", "getDeviceVersion() = " + name);
        return maps;
    }

    public static Map<String,Object> getDeviceVersion(byte[] value) {
        //    String version = "";
        Map<String,Object> maps=new HashMap<>();
        maps.put(DeviceKey.DataType, BleConst.GetDeviceVersion);
        maps.put(DeviceKey.End,true);
        Map<String,String>mapData=new HashMap<>();
        maps.put(DeviceKey.Data, mapData);
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 1; i < 5; i++) {
            stringBuffer.append(String.format("%X", value[i])).append(i == 4 ? "" : ".");
        }
        mapData.put(DeviceKey.DeviceVersion, stringBuffer.toString());
        Log.i("TAG", "getDeviceVersion() = " + stringBuffer);

        return maps;
    }

    public static  Map<String,Object> getDeviceTime(byte[] value) {
        Map<String,Object> maps=new HashMap<>();
        maps.put(DeviceKey.DataType, BleConst.GetDeviceTime);
        maps.put(DeviceKey.End,true);

        Map<String,String>mapData=new HashMap<>();
        String date = "20"+ByteToHexString(value[1]) + "-"
                + ByteToHexString(value[2]) + "-" + ByteToHexString(value[3]) + " "
                + ByteToHexString(value[4]) + ":" + ByteToHexString(value[5]) + ":" + ByteToHexString(value[6]);

        Log.i("TAG", "getDeviceTime() = " + date);

        String gpsDate = ByteToHexString(value[9]) + "."
                + ByteToHexString(value[10]) + "." + ByteToHexString(value[11]) ;
        mapData.put(DeviceKey.DeviceTime, date);
        mapData.put(DeviceKey.GPSTime, gpsDate);
        maps.put(DeviceKey.Data, mapData);

        return maps;
    }
    public void Request_Battery_Level() {
/*
        Log.i("TAG", "Request_Battery_Level");
        BluetoothGattService batteryService = mBluetoothGatt.getService(UUID.fromString(Config.BATTERY_SERVICE));
        if(batteryService == null) {
            Log.d(TAG, "Battery service not found!");
            return;
        }

        BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(UUID.fromString(BATTERY_LEVEL));
        if(batteryLevel == null) {
            Log.d(TAG, "Battery level not found!");
            return;
        }
        mBluetoothGatt.readCharacteristic(batteryLevel);
*/
        sendToDevice(BleSDK.GetDeviceTime());
    }

    public void sendToDevice(byte[] temp) {
        Log.i("TAG", "sendToDevice");
        BluetoothGattService sppService = mBluetoothGatt.getService(UUID.fromString(Config.SPP_SERVICE));
        if(sppService == null) {
            Log.d(TAG, "spp service not found!");
            return;
        }

        BluetoothGattCharacteristic spp_send = sppService.getCharacteristic(UUID.fromString(SPP_CHARACTERISTIC_DATA_RECEIVE));
        spp_send.setValue(temp);
        if(spp_send == null) {
            Log.d(TAG, "spp char not found!");
            return;
        }
        mBluetoothGatt.writeCharacteristic(spp_send);
    }

    public void Request_HeartRate_Measurement() {
        Log.i("TAG", "Request_HeartRate_Measurement");
        BluetoothGattService heartRateService = mBluetoothGatt.getService(UUID.fromString(Config.HEART_RATE_SERVICE));
        if(heartRateService == null) {
            Log.d(TAG, "heartRateService not found!");
            return;
        }

        BluetoothGattCharacteristic heartRate = heartRateService.getCharacteristic(UUID.fromString(HEART_RATE_MEASUREMENT));
        if(heartRate == null) {
            Log.d(TAG, "heartRate not found!");
            return;
        }
        mBluetoothGatt.readCharacteristic(heartRate);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_SPP_DATA_NOTIFY.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}