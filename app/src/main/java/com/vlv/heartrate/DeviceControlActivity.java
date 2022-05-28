package com.vlv.heartrate;

import static com.vlv.heartrate.Config.DATA_TYPE_BATTERY_LEVEL;
import static com.vlv.heartrate.Config.DATA_TYPE_HEARTRATE;
import static java.lang.Integer.parseInt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.jstyle.blesdk1963base.Util.BleSDK;

import org.achartengine.GraphicalView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mHeartRate;
    private TextView mBatteryLevel;
    private ImageView mImageView;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private GraphicalView graphView;
    private LineGraphView lineGraph;
    private int hrValue = 0;
    private int counter = 0;
    private final static int REFRESH_INTERVAL = 1000; // 1 second interval
    private Handler handler = new Handler();
    private boolean isGraphInProgress = false;
    private int timer_count = 0;
    Button gettime;
    Button getver;
    Button getname;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                mBluetoothLeService.Request_Battery_Level();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.v("received", "data");
                final String data_type = intent.getStringExtra((BluetoothLeService.DATA_TYPE));
                if (data_type.equals(DATA_TYPE_HEARTRATE)) {
                    Log.i("TAG", "BroadcastReceiver HEARTRATE");
                } else if (data_type.equals(DATA_TYPE_BATTERY_LEVEL)) {
                    Log.i("TAG", "BroadcastReceiver BATTERY_LEVEL");
                } else {
                    Log.i("TAG", "BroadcastReceiver OTHER");
                }
                displayData(intent.getStringExtra(BluetoothLeService.DATA_TYPE), intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        mHeartRate.setText(R.string.no_data);
        mBatteryLevel.setText("null");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mImageView = (ImageView)findViewById(R.id.imageView);
        mImageView.setImageResource(R.drawable.ic_launcher);
        gettime = findViewById(R.id.gettime);
        gettime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mBluetoothLeService.sendToDevice(BleSDK.GetDeviceTime());
            }
        });
        getver = findViewById(R.id.getver);
        getver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.sendToDevice(BleSDK.GetDeviceVersion());
            }
        });
        getname = findViewById(R.id.getname);
        getname.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mBluetoothLeService.sendToDevice(BleSDK.GetDeviceName());
            }
        });
        // Sets up UI references.
        mHeartRate = (TextView) findViewById((R.id.data_value));
        mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        setGUI();
    }

    private void setGUI() {
        lineGraph = LineGraphView.getLineGraphView();
//        hrValueView = findViewById(R.id.text_hrs_value);
//        hrLocationView = findViewById(R.id.text_hrs_position);
//        batteryLevelView = findViewById(R.id.battery);
        showGraph();
        startShowGraph();
    }

    private void showGraph() {
        graphView = lineGraph.getView(this);
        ViewGroup layout = findViewById(R.id.graph_hrs);
        layout.addView(graphView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String type, String data) {
        if (data != null) {
            if (type.equals(DATA_TYPE_HEARTRATE)) {
                mHeartRate.setText(data);
                hrValue = parseInt(data);
            } else if (type.equals(DATA_TYPE_BATTERY_LEVEL)) {
                mBatteryLevel.setText(data);
            } else {

            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        Log.i("TAG", "displayGattServices............");

        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.

        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            Log.i("TAG", "displayGattServices service uuid = " + uuid.toString());

            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.i("TAG", "displayGattServices characteristic uuid = " + uuid.toString());

                String gattInfo = SampleGattAttributes.lookup(uuid, unknownCharaString);
                if (gattInfo == "Heart Rate Measurement") {
                    Log.i("TAG", "Heart Rate Measurement set notification");
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    Log.v("loop", uuid);
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }

                if (gattInfo == "Battery_Level") {
                    Log.i("TAG", "Battery_Level set notification");
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    Log.v("loop", uuid);
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }


                if (gattInfo == "SPP_DATA_NOTIFY") {
                    Log.i("TAG", "SPP_DATA_NOTIFY set notification");
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    Log.v("loop", uuid);
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }

                if (gattInfo == "SPP_DATA_NOTIFY") {
                    Log.i("TAG", "SPP_DATA_NOTIFY set notification");
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    Log.v("loop", uuid);
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }
                gattCharacteristicGroupData.add(currentCharaData);

            }
            mGattCharacteristics.add(charas);

            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateGraph(final int hrmValue) {
        counter++;
        lineGraph.addValue(new Point(counter, hrmValue));
        graphView.repaint();
    }

    private Runnable repeatTask = new Runnable() {
        @Override
        public void run() {
            timer_count++;
            if (timer_count >= 60) {
                timer_count = 0;
                mBluetoothLeService.Request_Battery_Level();
            }
            if (hrValue > 0) {
                updateGraph(hrValue);
            }
            if (isGraphInProgress)
                handler.postDelayed(repeatTask, REFRESH_INTERVAL);
        }
    };

    void startShowGraph() {
        isGraphInProgress = true;
        repeatTask.run();
    }

    void stopShowGraph() {
        isGraphInProgress = false;
        handler.removeCallbacks(repeatTask);
    }
}
