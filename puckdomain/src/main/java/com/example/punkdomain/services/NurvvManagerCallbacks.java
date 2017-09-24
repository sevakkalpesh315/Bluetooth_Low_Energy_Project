package com.example.punkdomain.services;

import android.bluetooth.BluetoothDevice;


/**
 * Created by huseyinatasoy on 06/09/2017.
 */

public interface NurvvManagerCallbacks extends BleManagerCallbacks {
    void onErrorRead(BluetoothDevice bluetoothDevice, int value);

    void onConnectionStatusRead(BluetoothDevice bluetoothDevice, int value);

    void onDeviceListRead(BluetoothDevice bluetoothDevice, int value);

    void onSessionListRead(BluetoothDevice bluetoothDevice, int value);

    void onDataRead(BluetoothDevice bluetoothDevice, int value);

    void onSlaveBatteryRead(BluetoothDevice bluetoothDevice, int value);

    void onSlaveFirmwareRead(BluetoothDevice bluetoothDevice, String value);

    void onMemoryRead(BluetoothDevice bluetoothDevice, int value);

    void onRssiRead(BluetoothDevice bluetoothDevice, int value);

    // 24 characteristics value
    void onInsessionDataRead(BluetoothDevice bluetoothDevice, int count, int memory, int type1, int value1, int type2, int value2, int type3, int value3, int type4, int value4);

    void onTriggerDataRead(BluetoothDevice bluetoothDevice, int triggerType, int metricType, int value);

    void onFirmwareRead(BluetoothDevice bluetoothDevice, String value);
}
