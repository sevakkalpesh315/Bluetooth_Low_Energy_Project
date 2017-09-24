package com.example.punkdomain.utils;

/**
 * Created by kalpesh on 23/09/2017.
 */

public class Puck_CommandCharacteristics {

    /**
     The API takes two forms: commands sent by the CA and status characteristics provided by the MP.
     The command functions are initiated by the CA writing to the command characteristic.
     The MP will acknowledge when the action has been completed. The CA should always check the Error characteristic to see if the action completed successfully.
     Commands:
     */
    public static final byte START_SESSION= 1;
    public static final byte STOP_SESSION= 2;
    public static final byte DELETE_SESSION = 3;
    public static final byte LIST_SESSION= 4;
    public static final byte CALIBRATE= 5;
    public static final byte PAIR_SLAVE= 6;
    public static final byte LIST_MONITORS= 7;
    public static final byte PAIR_DEVICE= 8;
    public static final byte RESET_MASTER_FOR_DFU= 9;
    public static final byte RESET_SLAVE_FOR_DFU= 10;
    public static final byte NXP_FIRMWARE_UPDATE_START= 11;
    public static final byte NXP_FIRMWARE_UPDATE_END= 12;
    public static final byte NXP_FIRMWARE_UPDATE_CANCEL= 13;
    public static final byte GET_SESSION_DATA= 14;
    public static final byte PAUSE_SESSION= 15;
}
