package com.example.punkdomain.utils;

/**
 * Created by kalpesh on 20/09/2017.
 */

public class Puck_ErrorCodes {

    /**
     The MP provides an error response to all commands via the Error characteristic.
     It also maintains the Connection Status characteristic with indications of the connections and internal state.
     Additionally, there are two types of notification provided when a session is in progress: In Session Data and Trigger Data.

     5.1.1 Error
     The CA should subscribe for notification on the Error characteristic, so that it can handle any errors from MP commands.

     */
    public static final String NO_ERROR="NO_ERROR";
    public static final String SESSION_IN_PROGRESS="SESSION_IN_PROGRESS";
    public static final String NO_SESSION_IN_PROGRESS="NO_SESSION_IN_PROGRESS";
    public static final String ID_NOT_FOUND="ID_NOT_FOUND";
    public static final String MEMORY_FULL="MEMORY_FULL";
    public static final String SLAVE_DEVICE_ERROR="SLAVE_DEVICE_ERROR";
    public static final String ERROR_IN_CONFIG="ERROR_IN_CONFIG";
    public static final String DEVICE_NOT_FOUND="DEVICE_NOT_FOUND";
    public static final String MULTIPLE_DEVICES_FOUND="MULTIPLE_DEVICES_FOUND";
    public static final String UNUSED="<UNUSED>";
    public static final String PAIRING_FAILED="PAIRING_FAILED";
    public static final String VALUE_OUT_OF_RANGE="VALUE_OUT_OF_RANGE";
    public static final String UNKNOWN_COMMAND="UNKNOWN_COMMAND";
    public static final String INVALID_COMMAND="INVALID_COMMAND";
    public static final String NXP_FIRMWARE_UPDATE_FAILED="NXP_FIRMWARE_UPDATE_FAILED";
    public static final String CONFIG_FAILED="CONFIG_FAILED";
    public static final String NO_DATA="NO_DATA";
    public static final String IPC_FAILED="IPC_FAILED";
    public static final String NO_SLAVE_CONNECTED="NO_SLAVE_CONNECTED";
    public static final String NO_GPS_LOCK="NO_GPS_LOCK";


}
