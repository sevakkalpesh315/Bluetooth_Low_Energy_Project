package com.example.punkdomain.utils;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.LinkedList;

public class BLEQueue {

    public static final int ITEM_TYPE_WRITE = 0;
    public static final int ITEM_TYPE_READ = 1;
    public static final int ITEM_TYPE_NOTIFICATION = 2;

    private LinkedList<QueueItem> queueItems = new LinkedList<>();

    private QueueItem lastItem = null;

    public void addWrite(BluetoothGattCharacteristic characteristic, byte[] data) {
        characteristic.setValue(data);
        QueueItem queueItem = new QueueItem();
        queueItem.itemType = ITEM_TYPE_WRITE;
        queueItem.characteristic = characteristic;
        queueItems.addLast(queueItem);
    }

    public void addRead(BluetoothGattCharacteristic characteristic) {
        QueueItem queueItem = new QueueItem();
        queueItem.itemType = ITEM_TYPE_READ;
        queueItem.characteristic = characteristic;
        queueItems.addLast(queueItem);
    }

    public void addNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        QueueItem queueItem = new QueueItem();
        queueItem.itemType = ITEM_TYPE_NOTIFICATION;
        queueItem.characteristic = characteristic;
        queueItems.addLast(queueItem);
    }

    public QueueItem getNextItem() {
        if (queueItems.size() != 0) {
            lastItem = queueItems.pollFirst();
        } else {
            lastItem = null;
        }
        return lastItem;
    }

    public QueueItem getLastItem() {
        return lastItem;
    }

    public int hasItems() {
        return queueItems.size();
    }


}
