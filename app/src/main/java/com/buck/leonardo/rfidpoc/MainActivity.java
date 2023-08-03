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
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.honeywell.rfidservice.ConnectionState;
import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 0;
    private static final int BLUETOOTH_PERMISSION_CODE = 1;

    private ListView lvDispositivos;
    private Button btnEncontrar;
    private Button btnCreateReader;

    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> dispositivoList;
    private ArrayAdapter<String> dispositivoListAdapter;

    private App mApp;
    private RfidManager mRfidMgr;

    private static final String TAG = "MainActivity";

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                dispositivoList.add(device);
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }
                dispositivoListAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }

                dispositivoList.add(device);
                dispositivoListAdapter.add(device.getName() + "\n" + device.getAddress());
                dispositivoListAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApp = (App) getApplication();
        mRfidMgr = mApp.getRfidManager();

        lvDispositivos = findViewById(R.id.lv_dispositivos);
        dispositivoList = new ArrayList<>();
        dispositivoListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvDispositivos.setAdapter(dispositivoListAdapter);

        lvDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                BluetoothDevice dispositivoSelecionado = dispositivoList.get(position);

                pararBuscaDispositivosBluetooth();

                if (!isConnected() || dispositivoSelecionado != getSelectedDev()) {
                    disconnect();

                    connect(dispositivoSelecionado.getAddress());
                    setSelectedDev(dispositivoSelecionado);
                    view.setBackgroundColor(Color.YELLOW);
                    dispositivoListAdapter.notifyDataSetChanged();
                    btnCreateReader.setEnabled(true);
                    mRfidMgr.setTriggerMode(TriggerMode.RFID);
                }
            }
        });

        btnEncontrar = findViewById(R.id.btn_encontrar);
        btnEncontrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) {
                    Toast.makeText(MainActivity.this, "Bluetooth não suportado!", Toast.LENGTH_LONG).show();
                } else if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "Bluetooth não está ativado!", Toast.LENGTH_LONG).show();
                } else {
                    setSelectedDev(null);
                    dispositivoList.clear();
                    dispositivoListAdapter.clear();
                    disconnect();
                    encontrarDispositivosBluetooth();
                }
            }
        });

        btnCreateReader = findViewById(R.id.btn_create_reader);
        btnCreateReader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRfidMgr.addEventListener(mEventListener);
                mRfidMgr.createReader();
            }
        });
        btnCreateReader.setEnabled(false);
    }

    private void encontrarDispositivosBluetooth() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
                return;
            }
        }

        bluetoothAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    private void pararBuscaDispositivosBluetooth() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
                return;
            }
        }

        bluetoothAdapter.cancelDiscovery();
        btnEncontrar.setText("Encontrar dispositivos");
        btnEncontrar.setEnabled(true);
    }

    private BluetoothDevice getSelectedDev() {
        return App.getInstance().selectedBleDev;
    }

    private void setSelectedDev(BluetoothDevice dev) {
        App.getInstance().selectedBleDev = dev;
    }

    private void connect(String mac) {
        mRfidMgr.setAutoReconnect(true);
        mRfidMgr.connect(mac);
    }

    private void disconnect() {
        mRfidMgr.disconnect();
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
                Log.i(TAG, "onReaderCreated: " + success);
                mApp.rfidReader = reader;

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                        return;
                    }
                }

                Intent intent = new Intent(MainActivity.this, DispositivoActivity.class);
                intent.putExtra("dispositivo", getSelectedDev().getName());
                startActivity(intent);
            } else {
                Log.i(TAG, "onReaderCreated: " + success);
            }
        }

        @Override
        public void onRfidTriggered(boolean trigger) {
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode curMode) {
        }
    };

    private ConnectionState getCnntState() {
        return mRfidMgr.getConnectionState();
    }

    private boolean isConnected() {
        return getCnntState() == ConnectionState.STATE_CONNECTED;
    }
}