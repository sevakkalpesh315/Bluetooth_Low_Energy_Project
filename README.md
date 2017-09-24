# Android-u-blox-BLE
The u-blox Bluetooth low energy Android app allows developers to evaluate the stand-alone Bluetooth low energy modules from u-blox.

https://www.madebyfire.com/


# 

These files are all working with Android Studio 2.3 and with SDK version 21 (Android 5.0 - Lollipop).

Needed files to get BLE working in your app:  
```
\services\BluetoothLeService.java
\utils\BLEQueue.java
\utils\GattAttributes.java
\utils\QueueItem.java
```

In the app manifest you need at least the following:  
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />  
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />  
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

In the application tag you must have:  
```xml
<service android:name="com.[yourcompany].[yourapp].services.BluetoothLeService" android:enabled="true" />
```

For a simple and striped down Activity see BasicBLEActivity.java, here we have the simplest way of connecting to the service to be able to connect, read, write and to get notifications from a BLE device like NINA-B1. This Activity is well documented to set you started in no time.

