package com.buck.leonardo.rfidpoc;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.widget.Toast;

import com.honeywell.rfidservice.ConnectionState;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App extends Application {
    private static final String TAG = "App";

    public static final String API_URL = "http://26e4-201-148-119-161.ngrok-free.app/api";

    public RfidReader rfidReader;

    public List<BluetoothDevice> bleScanDevices = new ArrayList<>();
    public Map<String, Integer> bleScanDevicesRssi = new HashMap<>();
    public BluetoothDevice selectedBleDev;

    public List<Map<String, ?>> ListMs = Collections.synchronizedList(new ArrayList<Map<String, ?>>());

    public float batteryTemperature = 0;

    private static App mInstance;
    public RfidManager rfidMgr;

    public static App getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        rfidMgr = RfidManager.getInstance(this);

        Log.i(TAG, "App onCreate");
    }

    public boolean isRFIDReady() {
        return rfidMgr.readerAvailable() && rfidMgr.getConnectionState() == ConnectionState.STATE_CONNECTED && rfidMgr.getTriggerMode() == TriggerMode.RFID;
    }
    public boolean isReady() {
        return rfidMgr.readerAvailable() && rfidMgr.getConnectionState() == ConnectionState.STATE_CONNECTED;
    }

    public boolean isBatteryTemperatureTooHigh() {
        return batteryTemperature >= 60;
    }

    public boolean checkIsRFIDReady() {
        if (rfidMgr.getConnectionState() != ConnectionState.STATE_CONNECTED) {
            Log.i(TAG, "Bluetooth is not connected!");
            return false;
        }
        if (!rfidMgr.readerAvailable()) {
            Log.i(TAG, "Reader is null!");
            return false;
        }
        if (rfidMgr.getTriggerMode() != TriggerMode.RFID) {
            Log.i(TAG, "Current Mode is not RFID mode!");
            return false;
        }
        return true;
    }

    public boolean checkIsReady() {
        if (rfidMgr.getConnectionState() != ConnectionState.STATE_CONNECTED) {
            Log.i(TAG, "Bluetooth is not connected!");
            return false;
        }
        if (!rfidMgr.readerAvailable()) {
            Log.i(TAG, "Reader is null!");
            return false;
        }
        return true;
    }

    public RfidManager getRfidManager() {
        return rfidMgr;
    }
}
