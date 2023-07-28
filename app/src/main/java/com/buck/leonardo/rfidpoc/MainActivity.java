package com.buck.leonardo.rfidpoc;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 0;
    private static final int BLUETOOTH_PERMISSION_CODE = 1;

    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> deviceList;
    private ArrayAdapter<String> deviceListAdapter;

    private App mApp;
    private RfidManager mRfidMgr;

    private static final String TAG = "MainActivity";

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceList.add(device);
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }
                deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT},2);
                        return;
                    }
                }
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                deviceList.add(device);
                deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                deviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApp = (App) getApplication();
        mRfidMgr = mApp.getRfidManager();

        mRfidMgr.addEventListener(mEventListener);

        ListView deviceListView = findViewById(R.id.device_list_view);
        deviceList = new ArrayList<>();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);

        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                BluetoothDevice selectedDevice = deviceList.get(position);
                // Faça algo com o dispositivo selecionado
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT},2);
                        return;
                    }
                }
                Toast.makeText(MainActivity.this, "Dispositivo selecionado: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                mRfidMgr.setAutoReconnect(true);
                mRfidMgr.connect(selectedDevice.getAddress());
                Toast.makeText(MainActivity.this, "Estado do RFID: " + mRfidMgr.getConnectionState(), Toast.LENGTH_SHORT).show();
                mRfidMgr.createReader();
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // O dispositivo não suporta Bluetooth
            Toast.makeText(this, "Bluetooth não suportado", Toast.LENGTH_SHORT).show();
            finish();
        }

        requestBluetoothePermission();
    }

    private void requestBluetoothePermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN},2);
                return;
            }
        }

        bluetoothAdapter.startDiscovery();
        Toast.makeText(this, bluetoothAdapter.getName(), Toast.LENGTH_SHORT).show();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRfidMgr.addEventListener(mEventListener);
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRfidMgr.removeEventListener(mEventListener);
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, inicie a descoberta de dispositivos Bluetooth
                startBluetoothDiscovery();
                Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                // Permissão negada, informe ao usuário que a descoberta de dispositivos Bluetooth não pode ser realizada
                Toast.makeText(this, "Permissão de Bluetooth negada", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == BLUETOOTH_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted now you can scan the bluetooth", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Oops you just denied the permission", Toast.LENGTH_LONG).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startBluetoothDiscovery() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            showExplanation("Permission Needed", "Rationale", Manifest.permission.BLUETOOTH_SCAN, 0);
            return;
        }
        bluetoothAdapter.startDiscovery();
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, 0);
    }

    private EventListener mEventListener = new EventListener() {
        @Override
        public void onDeviceConnected(Object data) {
            Log.i("MainActivity", "onDeviceConnected: " + data);
        }

        @Override
        public void onDeviceDisconnected(Object data) {
            Log.i("MainActivity", "onDeviceDisconnected: " + data);
        }

        @Override
        public void onReaderCreated(boolean success, RfidReader reader) {
            if (success) {
                Log.i("MainActivity", "onReaderCreated: " + success);
                mApp.rfidReader = reader;
                Intent intent = new Intent(MainActivity.this, LeituraActivity.class);
                startActivity(intent);
            } else {
                Log.i("MainActivity", "onReaderCreated: " + success);
            }
        }

        @Override
        public void onRfidTriggered(boolean trigger) {
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode curMode) {
        }
    };
}