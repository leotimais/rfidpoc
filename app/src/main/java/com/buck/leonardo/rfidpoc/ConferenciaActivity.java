package com.buck.leonardo.rfidpoc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.buck.leonardo.rfidpoc.adapter.LeituraAdapter;
import com.buck.leonardo.rfidpoc.model.LeituraEtiqueta;
import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.TagReadData;
import com.honeywell.rfidservice.rfid.TagReadOption;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ConferenciaActivity extends AppCompatActivity {
    private EditText etEtiqRfid;
    private EditText etOpSeqDev;
    private EditText etCodItem;
    private EditText etCodigoBarras;
    private RecyclerView rvEtiqLidas;
    private RecyclerView.LayoutManager rvLayoutManager;
    private LeituraAdapter etiqLidasAdapter;
    private List<LeituraEtiqueta> etiqLidasList = new ArrayList<>();

    private App mApp;

    private HandlerThread mReadHandlerThread = new HandlerThread("ReadHandler");
    private Handler mReadHandler;
    private Handler mUiHandler;

    private static final int MSG_UPDATE_UI_NORMAL_MODE = 0;
    private static final int MSG_UPDATE_UI_FAST_MODE = 1;

    private List<String> tagsList = new ArrayList<>();
    private TagReadOption mTagReadOption = new TagReadOption();

    private static final String TAG = "ConferenciaActivity";

    private static final String ACTION_BARCODE_DATA = "com.honeywell.sample.action.BARCODE_DATA";
    private static final String ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER";
    private static final String ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER";
    private static final String EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER";
    private static final String EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE";
    private static final String EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES";

    private BroadcastReceiver barcodeDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BARCODE_DATA.equals(intent.getAction())) {
                int version = intent.getIntExtra("version", 0);
                if (version >= 1) {
                    String aimId = intent.getStringExtra("aimId");
                    String charset = intent.getStringExtra("charset");
                    String codeId = intent.getStringExtra("codeId");
                    String data = intent.getStringExtra("data");
                    byte[] dataBytes = intent.getByteArrayExtra("dataBytes");
                    String dataBytesStr = bytesToHexString(dataBytes);
                    String timestamp = intent.getStringExtra("timestamp");
                    String text = String.format(
                            "Data:%s\n" +
                                    "Charset:%s\n" +
                                    "Bytes:%s\n" +
                                    "AimId:%s\n" +
                                    "CodeId:%s\n" +
                                    "Timestamp:%s\n",
                            data, charset, dataBytesStr, aimId, codeId, timestamp);
                    setText(text);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conferencia);
        etEtiqRfid = (EditText) findViewById(R.id.et_conf_etiqrfid);
        etOpSeqDev = (EditText) findViewById(R.id.et_conf_opseqdev);
        etCodItem = (EditText) findViewById(R.id.et_conf_coditem);
        etCodigoBarras = (EditText) findViewById(R.id.et_conf_codbarras);

        etEtiqRfid.setEnabled(false);
        etOpSeqDev.setEnabled(false);
        etCodItem.setEnabled(false);
        etCodigoBarras.setEnabled(false);

        rvEtiqLidas = (RecyclerView) findViewById(R.id.rv_conf_etiqlidas);
        rvLayoutManager = new LinearLayoutManager(this);
        rvEtiqLidas.setLayoutManager(rvLayoutManager);
        rvEtiqLidas.setHasFixedSize(true);
        rvEtiqLidas.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));

        criarItemToucher();

        mApp = App.getInstance();
        initHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApp.getRfidManager().addEventListener(mEventListner);
        registerReceiver(barcodeDataReceiver, new IntentFilter(ACTION_BARCODE_DATA));
        claimScanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApp.getRfidManager().addEventListener(mEventListner);
        unregisterReceiver(barcodeDataReceiver);
        releaseScanner();
    }

    private void claimScanner() {
        Bundle properties = new Bundle();
        properties.putBoolean("DPR_DATA_INTENT", true);
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA);
        sendBroadcast(new Intent(ACTION_CLAIM_SCANNER)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "MyProfile1")
                .putExtra(EXTRA_PROPERTIES, properties)
        );
    }

    private void releaseScanner() {
        sendBroadcast(new Intent(ACTION_RELEASE_SCANNER));
    }

    private void setText(final String text) {
        if (etCodigoBarras != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    etCodigoBarras.setText(text);
                }
            });
        }
    }

    private String bytesToHexString(byte[] arr) {
        String s = "[]";
        if (arr != null) {
            s = "[";
            for (int i = 0; i < arr.length; i++) {
                s += "0x" + Integer.toHexString(arr[i]) + ", ";
            }
            s = s.substring(0, s.length() - 2) + "]";
        }
        return s;
    }

    private void criarItemToucher() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Toast.makeText(ConferenciaActivity.this, "Leitura excluída", Toast.LENGTH_SHORT).show();
                int position  = viewHolder.getAdapterPosition();
                LeituraEtiqueta leitura = etiqLidasList.get(position);
                etiqLidasList.remove(leitura);
                etiqLidasAdapter.notifyDataSetChanged();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(rvEtiqLidas);
    }

    private void initHandler() {
        mUiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.i(TAG, ">>> [mUiHandler]  handleMessage msg.what:" + msg.what);

                switch (msg.what) {
                    case MSG_UPDATE_UI_NORMAL_MODE:
                        atualizarEtiquetaLida();
                        break;
                    default:
                        break;
                }
            }
        };

        Thread syncReadThread = new Thread(mSyncReadRunnable);
        syncReadThread.start();

        mReadHandlerThread.start();
        mReadHandler = new Handler(mReadHandlerThread.getLooper());
    }

    private void atualizarEtiquetaLida() {
        HashSet<String> removeDuplicados = new HashSet<>(tagsList);
        List<String> tags = new ArrayList<>(removeDuplicados);

        if (tags.size() > 1) {
            Toast.makeText(this, "Mais de uma Etiqueta RFID lida, realize nova leitura", Toast.LENGTH_SHORT).show();
            return;
        }

        String tag = tags.get(0);
        etEtiqRfid.setText(tag);
    }

    private SyncReadRunnable mSyncReadRunnable = new SyncReadRunnable();

    private class SyncReadRunnable implements Runnable {
        private boolean mRun = true;

        void release() {
            mRun = false;
        }

        @Override
        public void run() {
            while (mRun) {
                TagReadData[] trds = getReader().syncRead(-1, 1000);
                for (TagReadData t : trds) {
                    String tag = t.getEpcHexStr();

                    tagsList.add(tag);
                    Message message = Message.obtain();
                    message.what = MSG_UPDATE_UI_NORMAL_MODE;
                    mUiHandler.removeMessages(message.what);
                    mUiHandler.sendMessage(message);
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private EventListener mEventListner = new EventListener() {
        @Override
        public void onDeviceConnected(Object o) {
            Log.i(TAG, ">>> onDeviceConnected");
        }

        @Override
        public void onDeviceDisconnected(Object o) {
            Log.i(TAG, ">>> onDeviceDisconnected");
        }

        @Override
        public void onReaderCreated(boolean b, RfidReader rfidReader) {
            Log.i(TAG, ">>> onReaderCreated");
        }

        @Override
        public void onRfidTriggered(boolean trigger) {
            Log.i(TAG, ">>> onRfidTriggered");

            if (trigger) {
                comecarLeitura();
            } else {
                pararLeitura();
            }
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode) {
            Log.i(TAG, ">>> onTriggerModeSwitched");
        }
    };

    private void pararLeitura() {
        mReadHandler.post(new Runnable() {
            @Override
            public void run() {
                pararLeituraInterno();
            }
        });
    }

    private void pararLeituraInterno() {
        RfidReader reader = getReader();
        reader.stopRead();
    }

    private void comecarLeitura() {
        mReadHandler.post(new Runnable() {
            @Override
            public void run() {
                comecarLeituraInterno();
            }
        });
    }

    private void comecarLeituraInterno() {
        RfidReader reader = getReader();
//        reader.setOnTagReadListener(dataListener);
        reader.read(-1, mTagReadOption);

        Message msg = Message.obtain();
        msg.what = MSG_UPDATE_UI_FAST_MODE;
        mUiHandler.sendMessage(msg);
    }

    private RfidReader getReader() {
        return mApp.rfidReader;
    }
}