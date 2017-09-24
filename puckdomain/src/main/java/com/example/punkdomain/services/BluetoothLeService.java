package com.example.punkdomain.services;

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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.example.punkdomain.utils.BLEQueue;
import com.example.punkdomain.utils.GattAttributes;
import com.example.punkdomain.utils.QueueItem;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static com.example.punkdomain.utils.GattAttributes.DATA_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.DEVICE_LIST_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.IN_SESSIONDATA_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.MEMORY_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.RSSI_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.SESSION_LIST_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.SLAVE_BATTERY_LEVEL_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.SLAVE_FIRMWARE_REVISION_CHARACTERISTIC;
import static com.example.punkdomain.utils.GattAttributes.TRIGGER_DATA_CHARACTERISTIC;

/**
 * This service handles all the interaction with the BLE device.
 */
public class BluetoothLeService extends Service {


    private final static String TAG = BluetoothLeService.class.getSimpleName(); // Tag for logging

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private final IBinder mBinder = new LocalBinder();

    private BLEQueue bleQueue = new BLEQueue();
    private boolean bleQueueIsFree = true;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =           "com.ublox.BLE.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =        "com.ublox.BLE.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.ublox.BLE.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =           "com.ublox.BLE.ACTION_DATA_AVAILABLE";
    public final static String ACTION_RSSI_UPDATE =              "com.ublox.BLE.ACTION_RSSI_UPDATE";


    public final static String EXTRA_TYPE = "com.ublox.BLE.EXTRA_TYPE";
    public final static String EXTRA_UUID = "com.ublox.BLE.EXTRA_UUID";
    public final static String EXTRA_DATA = "com.ublox.BLE.EXTRA_DATA";
    public final static String EXTRA_RSSI = "com.ublox.BLE.EXTRA_RSSI";

    // Implements callback methods for GATT events that the app cares about. For example,
    // connection change and services discovered.

    /**
     * This abstract class is used to implement BluetoothGatt callbacks.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                // Attempts to discover services after successful connection.
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         * @param gatt:  BluetoothGatt: GATT client invoked readCharacteristic(BluetoothGattCharacteristic)
         * @param characteristic BluetoothGattCharacteristic: Characteristic that was read from the associated remote device.
         * @param status int: GATT_SUCCESS if the read operation was completed successfully.
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, BLEQueue.ITEM_TYPE_READ);
            bleQueueIsFree = true;
            processQueue();
            parseValue(gatt,characteristic);

        }

        /**
         * Callback indicating the result of a characteristic write operation.
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, BLEQueue.ITEM_TYPE_READ);
            bleQueueIsFree = true;
            processQueue();
            parseValue(gatt,characteristic);
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, BLEQueue.ITEM_TYPE_NOTIFICATION);
            parseValue(gatt,characteristic);

        }

        /**
         * ]Callback indicating the result of a descriptor write operation.
         * @param gatt
         * @param descriptor
         * @param status
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            bleQueueIsFree = true;
            processQueue();

        }

        /**
         * Callback reporting the RSSI for a remote device connection.
         * @param gatt
         * @param rssi
         * @param status
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            broadcastRssi(rssi);
        }
    };



    /**
     * Sends a broadcast to registered receivers
     * @param action The Intent action that are going to be sent in the broadcast
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Sends a broadcast to registered receivers with the current rssi
     * @param rssi The current rssi
     */
    private void broadcastRssi(int rssi) {
        Intent intent = new Intent(ACTION_RSSI_UPDATE);
        intent.putExtra(EXTRA_RSSI, rssi);
        sendBroadcast(intent);
    }

    /**
     *
     * @param action The action of the broadcast intent
     * @param characteristic The characteristics that it is about
     * @param itemType Item type from the class BLEQueue
     */
    private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic, int itemType) {
        Intent intent = new Intent(action);

        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_TYPE, itemType);

        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Close to connection
        close();
        return super.onUnbind(intent);
    }



    public boolean initialize() {
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

        mHandler.postDelayed(rCheckRssi, 2000);
        return true;
    }

    // Code to receive rssi from connected device every two seconds
    Handler mHandler = new Handler();
    Runnable rCheckRssi = new Runnable() {
        @Override
        public void run() {
            if (mBluetoothGatt != null && mConnectionState == STATE_CONNECTED && bleQueue.hasItems() == 0) {
                try {
                    mBluetoothGatt.readRemoteRssi();
                } catch (Exception e) {}
            }
            mHandler.postDelayed(rCheckRssi, 2000);
        }
    };

    /**
     * Connect to BLE device
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully.
     */
    public boolean connect(final String address) {
        // Init the request queue for this device
        bleQueue = new BLEQueue();

        // If the address or bluetoothadapter is null we cant connect
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        // Checks if we successfully connected
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback, TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * BluetoothGattCallback.onConnectionStateChange(BluetoothGatt, int, int)
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
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
     * Request a read on a given BluetoothGattCharacteristic. The read result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            bleQueue.addRead(characteristic);
        }
        processQueue();
    }

    /**
     * Request a notifications on a given BluetoothGattCharacteristic. The read result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)
     * callback.
     *
     * @param characteristic The characteristic to read from.
     * @param enabled true to enable notifications, false to disable
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            bleQueue.addNotification(characteristic, enabled);
        }
        processQueue();
    }

    /**
     * Request a notifications on a given BluetoothGattCharacteristic. The read result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)
     * callback.
     *
     * @param characteristic The characteristic to read from.
     * @param data byte array with the data to write
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            bleQueue.addWrite(characteristic, data);
        }
        processQueue();
    }



    private void parseValue(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
      //  Log.d(TAG, characteristic.getUuid().toString());

        int offset = 0;
        if (GattAttributes.UUID_ERROR_CHARACTERISTIC.equals(characteristic.getUuid())) {
             int error = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
           // System.out.println("Error code is " +error);
        //    Toast.makeText(getApplicationContext(),"Error"+ error,Toast.LENGTH_LONG).show();
        //   mCallbacks.onErrorRead(gatt.getDevice(), error);

        } else if (GattAttributes.CONNECTION_STATUS_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final int connectionStatus = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        //    mCallbacks.onConnectionStatusRead(gatt.getDevice(), connectionStatus);

        } else if (DEVICE_LIST_CHARACTERISTIC.equals(characteristic.getUuid())) {
            //TODO Check and change the format
            final int deviceList = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);

        } else if (SESSION_LIST_CHARACTERISTIC.equals(characteristic.getUuid())) {
            //TODO Check and change the format
            final int sessionList = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);

        } else if (DATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
            //TODO Check and change the format
            final int data = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);

        } else if (SLAVE_BATTERY_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final int slaveBattery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);

        } else if (SLAVE_FIRMWARE_REVISION_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final String firmware = characteristic.getStringValue(0);

        } else if (MEMORY_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final int memory = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);

        } else if (RSSI_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final int rssi = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);

        } else if (IN_SESSIONDATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final int count = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            final int memory = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            final int type1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
            final int value1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);

            final int type2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 11);
            final int value2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 12);

            final int type3 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 20);
            final int value3 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 21);

            final int type4 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 29);
            final int value4 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 30);

         //   mCallbacks.onInsessionDataRead(gatt.getDevice(), count, memory, type1, value1, type2, value2, type3, value3, type4, value4);
//

        } else if (TRIGGER_DATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final int triggerType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            final int metricType = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            final int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
          //  mCallbacks.onTriggerDataRead(gatt.getDevice(), triggerType, metricType, value);

        } else if (GattAttributes.UUID_CHARACTERISTIC_FIRMWARE_REVISION.equals(characteristic.getUuid())) {
            //TODO Check and change the format
            final String firmware = characteristic.getStringValue(0);
          //  mCallbacks.onFirmwareRead(gatt.getDevice(), firmware);

        }


    }
    /**
     * Function that is handling the request queue.
     * To think about is that BLE on Android only can handle one request at the time.
     * Android do not handle this by itself..
     */
    private void processQueue() {
        if (bleQueueIsFree) {
            bleQueueIsFree = false;
            QueueItem queueItem = bleQueue.getNextItem();
            if (queueItem == null) {
                bleQueueIsFree = true;
                return;
            } else {
                boolean status = false;
                switch (queueItem.itemType) {
                    case BLEQueue.ITEM_TYPE_READ:
                        status = mBluetoothGatt.readCharacteristic(queueItem.characteristic);
                        break;
                    case BLEQueue.ITEM_TYPE_WRITE:
                        status = mBluetoothGatt.writeCharacteristic(queueItem.characteristic);
                        break;
                    case BLEQueue.ITEM_TYPE_NOTIFICATION:
                        mBluetoothGatt.setCharacteristicNotification(queueItem.characteristic, true);
                        BluetoothGattDescriptor descriptor = queueItem.characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            status = mBluetoothGatt.writeDescriptor(descriptor);
                        } else {
                            status = false;
                        }
                        break;
                }
                if (!status) {
                    bleQueueIsFree = true;
                }
            }
        }
    }



    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after BluetoothGatt.discoverServices() completes successfully.
     *
     * A List of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }



//
//
//    public void readCustomCharacteristic() {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        /*check if the service is available on the device*/
//        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString("00001110-0000-1000-8000-00805f9b34fb"));
//        if(mCustomService == null){
//            Log.w(TAG, "Custom BLE Service not found");
//            return;
//        }
//        /*get the read characteristic from the service*/
//        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString("00000002-0000-1000-8000-00805f9b34fb"));
//        if(mBluetoothGatt.readCharacteristic(mReadCharacteristic) == false){
//            Log.w(TAG, "Failed to read characteristic");
//        }
//    }
//
    public void writeCustomCharacteristic(int value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID_PUCK_INTERFACE));
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(GattAttributes.COMMAND_CHARACTERISTIC));
        mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);

        if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
            Log.w(TAG, "Failed to write characteristic");
        }
    }
}