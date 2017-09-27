package com.ublox.BLE.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.punkdomain.services.BluetoothLeService;
import com.example.punkdomain.utils.BLEQueue;
import com.example.punkdomain.utils.GattAttributes;
import com.example.punkdomain.utils.Puck_CommandCharacteristics;
import com.example.punkdomain.utils.Puck_ErrorCodes;
import com.ublox.BLE.R;
import com.ublox.BLE.fragments.OverviewFragment;
import com.ublox.BLE.fragments.ServicesFragment;
import com.ublox.BLE.fragments.SessionListAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.example.punkdomain.utils.GattAttributes.DATA_CHARACTERISTIC;
import static com.ublox.BLE.R.id.tvAccelerometerRange;


public class MainActivity extends Activity implements ActionBar.TabListener, OverviewFragment.IOverviewFragmentInteraction, ServicesFragment.IServiceFragmentInteraction, AdapterView.OnItemSelectedListener {
    private RecyclerView mRecyclerView;

    /**
     * Used for Session Data process
     */
    ByteArrayOutputStream outputStream;
    byte[] dataComplete;
    int wBufferLength,bPageCount,bCompression,boLast,tPageData, intCondition;
    ArrayList<Integer> wOffset = new ArrayList<Integer>();
    ArrayList<Integer> dwTimestamp = new ArrayList<Integer>();



    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICES = "device";
    StringBuffer stringBuffer;
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private int currentDevice = 0;

    private ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    private BluetoothLeService mBluetoothLeService;
    private static boolean mConnected = false;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private BluetoothGattCharacteristic characteristicRedLED;
    private BluetoothGattCharacteristic characteristicGreenLED;
    private BluetoothGattCharacteristic characteristicCommand;
    private BluetoothGattCharacteristic characteristicSessionList;
    private BluetoothGattCharacteristic characteristicSessionID;
    private BluetoothGattCharacteristic characteristicDataID;

    private BluetoothGattCharacteristic characteristicError;
    private BluetoothGattCharacteristic characteristicMemory;


    private BluetoothGattCharacteristic characteristicFifo;
    byte sessionType;
    byte metricType;
    byte[] destination;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
     //       Log.i("BLE-Device Address", ""+mDevices.get(currentDevice).getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /**
     * The service updates the broadcast receiver.
     * Returns the list of characteristics for the service
     * Sets the notification for Battery level; so that we can update the change in value on regular basis
     *
     */

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                sendToActiveFragment(mBluetoothLeService.getSupportedGattServices());
                for (BluetoothGattService service : mBluetoothLeService.getSupportedGattServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String uuid = characteristic.getUuid().toString();
                      //  Toast.makeText(MainActivity.this,"UUID:   "+uuid , Toast.LENGTH_SHORT).show();

                        if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_RANGE)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                            //    Toast.makeText(MainActivity.this,"UUID_CHARACTERISTIC_ACC_RANGE " , Toast.LENGTH_SHORT).show();
                            } catch (Exception ignore) {
                            }
                        }
                            else if (uuid.equals(GattAttributes.UUID_ERROR_CHARACTERISTIC)
                                ) {
                                try {
                                    //characteristicError = characteristic;
                                    mBluetoothLeService.readCharacteristic(characteristic);
                                    mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                                //    Toast.makeText(MainActivity.this,"UUID_ERROR_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();

                                } catch (Exception ignore) {}
                        }
                        else if (uuid.equals(GattAttributes.COMMAND_CHARACTERISTIC)) {
                            try {
                                characteristicCommand = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);
                            //    Toast.makeText(MainActivity.this,"COMMAND_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();

                            } catch (Exception ignore) {
                            }

                        } else if (uuid.equals(GattAttributes.CONNECTION_STATUS_CHARACTERISTIC)) {
                                try {
                                    mBluetoothLeService.readCharacteristic(characteristic);
                                    mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                               //     Toast.makeText(MainActivity.this,"CONNECTION_STATUS_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();
                                    Log.i("MDFIRE","CONNECTION_STATUS_CHARACTERISTIC");


                                } catch (Exception ignore) {}
                        }else if (uuid.equals(GattAttributes.CONFIG_DATA_CHARACTERISTIC)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                            //    Toast.makeText(MainActivity.this,"CONFIG_DATA_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();
                                Log.i("MDFIRE","CONFIG_DATA_CHARACTERISTIC");


                            } catch (Exception ignore) {}
                        }
                        else if (uuid.equals(GattAttributes.DEVICE_LIST_CHARACTERISTIC)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                           //     Toast.makeText(MainActivity.this,"DEVICE_LIST_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();
                                Log.i("MDFIRE","DEVICE_LIST_CHARACTERISTIC");
                            } catch (Exception ignore) {}
                        }

                        else if (uuid.equals(GattAttributes.SESSION_LIST_CHARACTERISTIC)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                               // Toast.makeText(MainActivity.this,"SESSION_LIST_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();

                            } catch (Exception ignore) {}
                        }

                        else if (uuid.equals(GattAttributes.SESSION_ID_CHARACTERISTIC)) {
                            try {
                                characteristicSessionID= characteristic;
                                mBluetoothLeService.readCharacteristic(characteristicSessionID);
                                mBluetoothLeService.setCharacteristicNotification(characteristicSessionID, true);
                                 Toast.makeText(MainActivity.this,"SESSION_ID_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();

                            } catch (Exception ignore) {}

                        }
                        else if (uuid.equals(GattAttributes.DATA_ID_CHARACTERISTIC)
                                ) {
                            try {
                                characteristicDataID= characteristic;
                                mBluetoothLeService.readCharacteristic(characteristicDataID);
                                mBluetoothLeService.setCharacteristicNotification(characteristicDataID, true);
                                Toast.makeText(MainActivity.this,"DATA_ID_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();


                            } catch (Exception ignore) {
                            }
                        }else if (uuid.equals(DATA_CHARACTERISTIC)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                                 Toast.makeText(MainActivity.this, "DATA_CHARACTERISTIC ", Toast.LENGTH_SHORT).show();

                            } catch (Exception ignore) {
                            }
                        }
                        else if (uuid.equals(GattAttributes.MEMORY_CHARACTERISTIC)) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                              //  Toast.makeText(MainActivity.this,"MEMORY_CHARACTERISTIC " , Toast.LENGTH_SHORT).show();
                                Log.i("MDFIRE","MEMORY_CHARACTERISTIC");

                            } catch (Exception ignore) {}
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_GREEN_LED)) {
                            try {
                                characteristicMemory = characteristic;
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                            } catch (Exception ignore) {}

                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_X)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Y)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Z)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL)
                                || uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TEMP_VALUE)
                                ) {
                            try {
                                mBluetoothLeService.readCharacteristic(characteristic);
                                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                            } catch (Exception ignore) {}
                        } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_FIFO)) {
                            characteristicFifo = characteristic;
                            sendToActiveFragment(characteristicFifo);
                        }
                    }
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                String extraUuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                int extraType = intent.getIntExtra(BluetoothLeService.EXTRA_TYPE, -1);
                byte[] extraData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                sendToActiveFragment(extraUuid, extraType, extraData);

            } else if (BluetoothLeService.ACTION_RSSI_UPDATE.equals(action)) {
                int rssi = intent.getIntExtra(BluetoothLeService.EXTRA_RSSI, 0);
                sendToActiveFragment(rssi);
            }
        }
    };

    private List<BluetoothGattService> mServices;

    private void sendToActiveFragment(List<BluetoothGattService> services) {
        mServices = services;
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (ServicesFragment.class.isInstance(fragment)) {
            ServicesFragment servicesFragment = (ServicesFragment) fragment;
            servicesFragment.displayGattServices(services);
        }
    }

    private void sendToActiveFragment(BluetoothGattCharacteristic characteristic) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }
    }

    private void sendToActiveFragment(int rssi) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (fragment == null) {
            return;
        }

        if (OverviewFragment.class.isInstance(fragment)) {
            View v = fragment.getView();
            ((TextView) v.findViewById(R.id.tvRSSI)).setText(String.format("%d", rssi));
        }
    }

    public static String bytesToString(byte[] b) {
        try {
            return new String(b, "UTF-8");
        }

        catch (UnsupportedEncodingException ex) {
        }

        return null;
    }

    private int toInt(byte[] b1){
        String s1 = bytesToString(b1);
        int y = Integer.parseInt(s1);
        return y;
    }

    private void sendToActiveFragment(String uuid, int type, byte[] data) {
        Fragment fragment = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());


        if (fragment == null) {
            return;
        }

        if (OverviewFragment.class.isInstance(fragment)) {
            View v = fragment.getView();



            ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    sessionID);

            ArrayAdapter<String> adapter1=new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    metrics);

            /**
             * view
             */
            ((Spinner) v.findViewById(R.id.spinner1)).setAdapter(adapter);
            ((Spinner) v.findViewById(R.id.spinner2)).setAdapter(adapter1);



            if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
                ((TextView) v.findViewById(R.id.tvBatteryLevel)).setText(String.format("%d", data[0]));
            }

            if (uuid.equals(GattAttributes.SESSION_ID_CHARACTERISTIC)) {
             //   ((TextView) v.findViewById(R.id.tvTemperature)).setText(String.format("%d", data[0]));
              //  Toast.makeText(MainActivity.this,"SESSION_ID_CHARACTERISTIC" + String.format("%d", data[0]), Toast.LENGTH_SHORT ).show();
            }
            if (uuid.equals(GattAttributes.DATA_ID_CHARACTERISTIC)) {
                //   ((TextView) v.findViewById(R.id.tvTemperature)).setText(String.format("%d", data[0]));
                //Toast.makeText(MainActivity.this,"DATA_ID_CHARACTERISTIC" + Arrays.toString(data), Toast.LENGTH_SHORT ).show();
            }
            if (uuid.equals(DATA_CHARACTERISTIC)) {

                dataRecieved(data);

                if(data.length!=0) {
                 //   dataRecieved(data);

                    // dataRecieved(data);
                  //  stringBuffer.append(Arrays.toString(concat(data)));
                    dataRecieved(data);
                    //   ((TextView) v.findViewById(R.id.tvTemperature)).setText(String.format("%d", data[0]));
                 //   Log.i("DATA_CHAR_VALUE",""+ Arrays.toString(concat(data)));
                }

                else{
                    Toast.makeText(MainActivity.this,"No value", Toast.LENGTH_SHORT ).show();

                }


            }


            if (uuid.equals(GattAttributes.MEMORY_CHARACTERISTIC)) {
               //((TextView) v.findViewById(R.id.tvTemperature)).setText(String.format("%d", data[0]));
            }

            if (uuid.equals(GattAttributes.SESSION_LIST_CHARACTERISTIC)) {


                /**
                 * Static Data
                 */

                byte[] list = {12, 0, 1, 0, 0, 0, 40, 61, -67, 89, 72, 0, 0, 0, 0, 96, 0, 0, 2, 0,
                        0, 0, -14, 61, -62, 89, 72, 0, 0, 0, 0, 96, 0, 0, 3, 0, 0, 0, -57, 65, -67, 89, 72, 0, 0,
                        0, 0, 96, 0, 0, 4, 0, 0, 0, 80, 66, -62, 89, 72, 0, 0, 0, 0, 96, 0, 0, 5, 0, 0, 0, -29,
                        75, -62, 89, 72, 0, 0, 0, 0, 96, 0, 0, 6, 0, 0, 0, -77, 121, -67, 89, 96, 0, 0, 0, 0,
                        118, 0, 0, 7, 0, 0, 0, -92, -78, 82, 123, 24, 0, 0, 0, 0, 30, 0, 0, 8, 0, 0, 0, 111, -67,
                        82, 123, 24, 0, 0, 0, 0, 28, 0, 0, 9, 0, 0, 0, -61, 120, -61, 89, 24, 0, 0, 0, 0, 30, 0,
                        0, 10, 0, 0, 0, -24, 120, -61, 89, 24, 0, 0, 0, 0, 30, 0, 0, 11, 0, 0, 0, 48, 121, -61,
                        89, 24, 0, 0, 0, 0, 30, 0, 0, 12, 0, 0, 0};
                if(data.length!=0) {
                    byte[][] output = processList(data);
                    for(byte[] line: output){
//            Log.i("lne", String.valueOf(line));
                        for(int i: line){
//                Log.i("Line", String.valueOf(i));
                        }
//            Log.i("NewLine","NewLine");
                    }

                    ((RecyclerView) v.findViewById(R.id.list)).setLayoutManager(new LinearLayoutManager(this));
                    ((RecyclerView) v.findViewById(R.id.list)).setAdapter(new SessionListAdapter(output));

                }
                else{
                    Toast.makeText(MainActivity.this,"No value", Toast.LENGTH_SHORT ).show();
                }
            }
            if (uuid.equals(GattAttributes.UUID_ERROR_CHARACTERISTIC)) {
                ((TextView) v.findViewById(tvAccelerometerRange)).setText(String.format("%d", data[0]));
                String value = String.format("%d", data[0]);
                // Toast.makeText(MainActivity.this,"Value"+value, Toast.LENGTH_SHORT ).show();
                if (value.equalsIgnoreCase("0")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.NO_ERROR, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("1")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.SESSION_IN_PROGRESS, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("2"))
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.NO_SESSION_IN_PROGRESS , Toast.LENGTH_SHORT).show();
                  else if (value.equalsIgnoreCase("3")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.ID_NOT_FOUND, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("4")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.MEMORY_FULL, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("5"))
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.SLAVE_DEVICE_ERROR , Toast.LENGTH_SHORT).show();
                  else if (value.equalsIgnoreCase("6")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.ERROR_IN_CONFIG, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("7")) {
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.DEVICE_NOT_FOUND, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("8"))
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.MULTIPLE_DEVICES_FOUND , Toast.LENGTH_SHORT).show();
                  else if (value.equalsIgnoreCase("9")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.UNUSED, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("10")) {
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.PAIRING_FAILED , Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("11"))
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.VALUE_OUT_OF_RANGE , Toast.LENGTH_SHORT).show();
                  else if (value.equalsIgnoreCase("12")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.UNKNOWN_COMMAND, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("13")) {
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.INVALID_COMMAND , Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("14"))
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.NXP_FIRMWARE_UPDATE_FAILED , Toast.LENGTH_SHORT).show();
                  else if (value.equalsIgnoreCase("15")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.CONFIG_FAILED, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("16")) {
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.NO_DATA , Toast.LENGTH_SHORT).show();;}
                  else if (value.equalsIgnoreCase("17")) {
                    Toast.makeText(MainActivity.this, Puck_ErrorCodes.IPC_FAILED, Toast.LENGTH_SHORT).show();
                } else if (value.equalsIgnoreCase("18")) {
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.NO_SLAVE_CONNECTED, Toast.LENGTH_SHORT).show();}
                  else if (value.equalsIgnoreCase("19")) {
                    Toast.makeText(MainActivity.this,Puck_ErrorCodes.NO_GPS_LOCK , Toast.LENGTH_SHORT).show();

                }
            }

            if (type == BLEQueue.ITEM_TYPE_READ) {

                StringBuilder stringBuilder = new StringBuilder(data.length);

                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_GREEN_LED)) {
                    Switch sGreen = (Switch) v.findViewById(R.id.sGreenLight);
                    if (data[0]  == 0) {
                        sGreen.setChecked(false);
                    } else {
                        sGreen.setChecked(true);
                    }
                } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RED_LED)) {
                    Switch sRed = (Switch) v.findViewById(R.id.sRedLight);
                    if (data[0]  == 0) {
                        sRed.setChecked(false);
                    } else {
                        sRed.setChecked(true);
                    }
                }

            } else if (type == BLEQueue.ITEM_TYPE_NOTIFICATION) {
                if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_X)) {
                    ((ProgressBar) v.findViewById(R.id.pbX)).setProgress(data[0] + 128);
                } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Y)) {
                    ((ProgressBar) v.findViewById(R.id.pbY)).setProgress(data[0] + 128);
                } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_ACC_Z)) {
                    ((ProgressBar) v.findViewById(R.id.pbZ)).setProgress(data[0] + 128);
                }
            }
        } else if (ServicesFragment.class.isInstance(fragment)) {
            View v = fragment.getView();
        //    Log.i("SERVICE", "gotData, uuid: " + uuid);
        //    Log.i("SERVICE", "wanted __UUID: " + ((ServicesFragment) fragment).currentUuid);


            if (((ServicesFragment) fragment).currentUuid.equals(uuid)) {
                StringBuilder stringBuilder = new StringBuilder(data.length);

                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                TextView tvValue = (TextView) v.findViewById(R.id.tvValue);
                tvValue.setText(new String(data) + "\n<" + stringBuilder.toString() + ">");
             //   Log.i("SERVICE", "gotData: " + tvValue.getText().toString());
            }

        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_UPDATE);
        return intentFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
        invalidateOptionsMenu();
        stringBuffer= new StringBuffer( );

    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            mConnected = false;
        } catch (Exception ignore) {}
        unregisterReceiver(mGattUpdateReceiver);
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getActionBar().setTitle("");
//        getActionBar().setLogo(R.drawable.logo);
//        getActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_main);

        // Get a ref to the actionbar and set the navigation mode
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Initiate the view pager that holds our views
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        mViewPager.setOffscreenPageLimit(3); // Set to num tabs to keep the fragments in memory

        // Add the tabs and give them titles
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                actionBar.newTab()
                    .setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }

        // get the information from the device scan
        final Intent intent = getIntent();
        mDevices = intent.getParcelableArrayListExtra(EXTRAS_DEVICES);


        //getActionBar().setTitle(mDeviceName);
        getActionBar().setTitle("");

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        Spinner actionBarSpinner = new Spinner(this);

        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : mDevices) {
            if (device.getName() != null) {
                deviceNames.add(device.getName());
            } else {
                deviceNames.add("Unknown device");
            }
        }

        String[] spinner = new String[deviceNames.size()];
        deviceNames.toArray(spinner);

        ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                spinner);

        // Specify a SpinnerAdapter to populate the dropdown list.

        actionBarSpinner.setAdapter(spinnerArrayAdapter);

        // Set up the dropdown list navigation in the action bar.
        actionBarSpinner.setOnItemSelectedListener(this);

        actionBar.setCustomView(actionBarSpinner);

        actionBar.setDisplayShowCustomEnabled(true);
    }

    public boolean isConnected() {
        return mConnected;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_connected, menu);
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
                mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
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

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onGreenLight(boolean enabled) {
        byte[] valueOn = {1};
        byte[] valueOff = {0};
        try {
            if (enabled) {
                mBluetoothLeService.writeCharacteristic(characteristicGreenLED, valueOn);
            } else {
                mBluetoothLeService.writeCharacteristic(characteristicGreenLED, valueOff);
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onRedLight(boolean enabled) {
        byte[] valueOn = {1};
        byte[] valueOff = {0};
        try {
            if (enabled) {
                mBluetoothLeService.writeCharacteristic(characteristicRedLED, valueOn);
            } else {
                mBluetoothLeService.writeCharacteristic(characteristicRedLED, valueOff);
            }
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onRead(BluetoothGattCharacteristic characteristic) {
        try {
            mBluetoothLeService.readCharacteristic(characteristic);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onWrite(BluetoothGattCharacteristic characteristic, byte[] value) {
        try {
            mBluetoothLeService.writeCharacteristic(characteristic, value);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onNotify(BluetoothGattCharacteristic characteristic) {
        try {
            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        currentDevice = position;
        try {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        } catch (Exception ignore) {}
        try {
            mBluetoothLeService.connect(mDevices.get(currentDevice).getAddress());
        } catch (Exception ignore) {}
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private OverviewFragment mOverviewFragment = null;
        private ServicesFragment mServicesFragment = null;

        long lastSendLed = 0;

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                if (mOverviewFragment == null)
                    mOverviewFragment = OverviewFragment.newInstance();

                // Need to have a delay so that this don't get called to often.
                // Else the Bluetooth manager will crash..
                long timeout = (System.currentTimeMillis() - lastSendLed);
                if (timeout > 1000 || timeout < 0) {
                    lastSendLed = System.currentTimeMillis();
                    if (characteristicGreenLED != null)
                        mBluetoothLeService.readCharacteristic(characteristicGreenLED);
                    if (characteristicRedLED != null)
                        mBluetoothLeService.readCharacteristic(characteristicRedLED);
                }
                return mOverviewFragment;
            } else if (position == 1) {
                if (mServicesFragment == null) {
                    mServicesFragment = ServicesFragment.newInstance();
                }
                if (!mServicesFragment.hasGottenServices()) {
                    if (mServices != null)
                        mServicesFragment.displayGattServices(mServices);
                }
                return mServicesFragment;

            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * Parsing SessionList
     */
    public byte[][] processList(byte[] list){
        byte length = list[0];
        byte[][] output = new byte[length][16];

        for(int i = 1; i < length+1 ;i++){
            byte[] newList =  new byte[16];
            newList = Arrays.copyOfRange(list,((i*16)-15)+1,(i*16)+2);
            Log.i("newList",Arrays.toString(newList));
            output[i-1] = newList;

        }

        return output;
    }

    @Override
    public void setStartSession() {
        byte[] startSession_bytes = {Puck_CommandCharacteristics.START_SESSION, 1, 21, 12, 19, 10, 29, 56};

        try {
            mBluetoothLeService.writeCharacteristic(characteristicCommand, startSession_bytes);
            Log.i("setStartSession","session has started");
            Toast.makeText(MainActivity.this,"setStartSession " , Toast.LENGTH_SHORT).show();

        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }


    @Override
    public void setStopSession() {
        byte[] stopSession_bytes = {Puck_CommandCharacteristics.STOP_SESSION};
        try {
            mBluetoothLeService.writeCharacteristic(characteristicCommand, stopSession_bytes);
            Log.i("setStopSession","session has stopped");
            Toast.makeText(MainActivity.this,"setStopSession " , Toast.LENGTH_SHORT).show();

        } catch (Exception ignore) {
            ignore.printStackTrace();
            Toast.makeText(MainActivity.this,"StopSession Error "+ ignore.getMessage() , Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void setListSession() {
        byte[] listSession_bytes = {Puck_CommandCharacteristics.LIST_SESSION};
        try {
            mBluetoothLeService.writeCharacteristic(characteristicCommand, listSession_bytes);
            Log.i("setListSession","session has listed");
            Toast.makeText(MainActivity.this,"setListSession " , Toast.LENGTH_SHORT).show();

        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    @Override
    public void setPauseSession() {
//        byte[] pauseSession_bytes = {Puck_CommandCharacteristics.PAUSE_SESSION};
//        try {
//            mBluetoothLeService.writeCharacteristic(characteristicCommand, pauseSession_bytes);
//            Log.i("setListSession","session has listed");
//            Toast.makeText(MainActivity.this,"setPauseSession " , Toast.LENGTH_SHORT).show();
//
//        } catch (Exception ignore) {
//            ignore.printStackTrace();
//        }
        Toast.makeText(MainActivity.this, "DATA_CHARACTERISTIC" + Arrays.toString(String.valueOf(stringBuffer).getBytes()), Toast.LENGTH_SHORT).show();
     //   StringBuilder stringBuilder = new StringBuilder(stringBuffer.length());

//        for(byte byteChar : String.valueOf(stringBuffer).getBytes())
//            stringBuilder.append(String.format("%d ", byteChar));
//       // stringBuilder.append(String.format("%02X ", byteChar));


    }

    /**
     * Demo purpose Only
     *
     */

    String[] sessionID={"1","2", "3","4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14"};
    String[] metrics={"0", "1","2", "3", "4", "5","6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"
            , "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46"};


    @Override
    public void sendSession(int foo) {
         sessionType =(byte)foo;

    }

    @Override
    public void sendMetric(int foo) {
        metricType =(byte)foo;

    }
    @Override
    public void setDetailsSession() {
        byte[] detailsSession_bytes= {1,0};
        byte[] metricType_bytes = {8};
//for(int i=0; i<= metrics.length; i++) {
//    byte metricType = (byte) i;
//    metricType_bytes = new byte[]{metricType};

   // Toast.makeText(MainActivity.this,"metricType_bytes "+metricType + metricType_bytes, Toast.LENGTH_SHORT).show();

        mBluetoothLeService.writeCharacteristic(characteristicSessionID, detailsSession_bytes);
        mBluetoothLeService.writeCharacteristic(characteristicDataID, metricType_bytes);


    byte[] getSessionData = {Puck_CommandCharacteristics.GET_SESSION_DATA};

    //   Toast.makeText(MainActivity.this,"metricType_bytes " , Toast.LENGTH_SHORT).show();
        mBluetoothLeService.writeCharacteristic(characteristicCommand, getSessionData);
        Log.i("characteristicCommand",""+  metricType_bytes+"---"+ sessionType);
   // }
    }

    /**
     * Appending Byte arrays for session Data
     */

    static public byte[] concat(byte[]... bufs) {
        if (bufs.length == 0)
            return null;
        if (bufs.length == 1)
            return bufs[0];
        for (int i = 0; i < bufs.length - 1; i++) {
            byte[] res = Arrays.copyOf(bufs[i], bufs[i].length+bufs[i + 1].length);
            System.arraycopy(bufs[i + 1], 0, res, bufs[i].length, bufs[i + 1].length);
            bufs[i + 1] = res;
        }
        return bufs[bufs.length - 1];
    }



    public void dataRecieved(byte[] data) {

        /**
         * Buffer length is bufferlength +  65 (header size)
         */
        intCondition= intCondition + data.length;

        if (outputStream == null) {
            if(data==null|| data.length==0){
                Toast.makeText(MainActivity.this,"null" , Toast.LENGTH_SHORT ).show();

                return;
            }
            outputStream = new ByteArrayOutputStream();
        }

        if (data != null && data.length != 0 ) {
            try {

                outputStream.write(data);

                Toast.makeText(MainActivity.this,"outputStream" , Toast.LENGTH_SHORT ).show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }  if(intCondition == wBufferLength +65){
            dataComplete = outputStream.toByteArray();
            Toast.makeText(MainActivity.this,"dataComplete"+ dataComplete , Toast.LENGTH_SHORT ).show();

            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            wBufferLength = bytesToInt(Arrays.copyOfRange(dataComplete, 0, 2));
            Log.i("ByteProcessing", "wBufferLength = " + wBufferLength + " (" + (wBufferLength + 65) + ")");
            Toast.makeText(MainActivity.this,"wBufferLength = " + Arrays.toString(data), Toast.LENGTH_SHORT ).show();

            bPageCount = bytesToInt(Arrays.copyOfRange(dataComplete, 2, 3));
            Log.i("ByteProcessing", "bPageCount = " + bPageCount);


            bCompression = bytesToInt(Arrays.copyOfRange(dataComplete, 3, 4));
            Log.i("ByteProcessing", "bCompression = " + bCompression);

            boLast = bytesToInt(Arrays.copyOfRange(dataComplete, 4, 5));
            Log.i("ByteProcessing", "boLast = " + boLast);

            for (int i = 0; i < 10; i++) {
                wOffset.add(bytesToInt(Arrays.copyOfRange(dataComplete, 5 + (6 * i), 7 + (6 * i))));
                Log.i("ByteProcessing", "wOffset[" + i + "] = " + wOffset.get(i));

                dwTimestamp.add(bytesToInt(Arrays.copyOfRange(dataComplete, 7 + (6 * i), 5 + (6 * (i + 1)))));
                Log.i("ByteProcessing", "dwTimestamp[" + i + "] = " + dwTimestamp.get(i));
            }

            int wOffsetCombined = 0;
            int dwTimestampCombined = 0;
            for (int i = 0; i < wOffset.size(); i++) {
                wOffsetCombined = wOffsetCombined + wOffset.get(i);
            }
            for (int i = 0; i < dwTimestamp.size(); i++) {
                dwTimestampCombined = dwTimestampCombined + dwTimestamp.get(i);
            }
            Log.i("ByteProcessing", "wOffset combined = " + wOffsetCombined);
            Log.i("ByteProcessing", "dwTimestamp combined = " + dwTimestampCombined);

            tPageData = ByteBuffer.wrap(dataComplete, 65, dataComplete.length - 65).getInt();
            Log.i("ByteProcessing", "tPageData = " + tPageData);
        }
    }

    public int bytesToInt(byte[] bytes){

        int out = bytes[0] & 0xFF;
        Log.i("ByteInterProcessing","stage 0 int " + out + " (" + Integer.toBinaryString(out) + ")");

        for(int i=1;i < bytes.length;i++){
            int interout = (bytes[i] & 0xFF) << (8 * i);
            Log.i("ByteInterProcessing","stage " + i + " interout " + interout + " (" + Integer.toBinaryString(interout) + ")");
            out = interout | out;
            Log.i("ByteInterProcessing","stage " + i + " int " + out + " (" + Integer.toBinaryString(out) + ")");
        }

        Log.i("ByteInterProcessing","return int " + out + " (" + Integer.toBinaryString(out) + ")");
        return out;
    }
}
