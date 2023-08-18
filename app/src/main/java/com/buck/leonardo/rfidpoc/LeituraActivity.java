package com.buck.leonardo.rfidpoc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.buck.leonardo.rfidpoc.adapter.LeituraAdapter;
import com.buck.leonardo.rfidpoc.model.LeituraEtiqueta;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.TagReadData;
import com.honeywell.rfidservice.rfid.TagReadOption;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LeituraActivity extends AppCompatActivity {

    private EditText etEtiqRfid;
    private EditText etCodigoBarras;
    private Button btnLerRfid;
    private Button btnAssociar;
    private RecyclerView rvLeituras;
    private RecyclerView.LayoutManager rvLayoutManager;
    private LeituraAdapter leituraAdapter;
    private List<LeituraEtiqueta> leituras = new ArrayList<>();

    private int posicaoSelecionada = -1;

    private App mApp;
    private List<String> tagsList = new ArrayList<>();;
    private Handler mUiHandler;
    private HandlerThread mReadHandlerThread = new HandlerThread("ReadHandler");
    private Handler mReadHandler;

    public List<Map<String, ?>> mOldList = Collections.synchronizedList(new ArrayList<Map<String, ?>>());

    private TagReadOption mTagReadOption = new TagReadOption();

    private static final String TAG = "LeituraActivity";

    private static final int MSG_UPDATE_UI_TEXTO_TAG = 0;
    private static final int MSG_UPDATE_UI_BOTAO_ASSOCIAR = 1;

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
                    String data = intent.getStringExtra("data");
                    setCodigoDeBarras(data);
                }
            }
        }
    };

    private void setCodigoDeBarras(final String text) {
        if (etCodigoBarras != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    etCodigoBarras.setText(text);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura);

        setTitle("Associação de RFID");

        etEtiqRfid = (EditText) findViewById(R.id.et_leitura_etiqrfid);
        etCodigoBarras = (EditText) findViewById(R.id.et_leitura_codbarras);
        btnLerRfid = (Button) findViewById(R.id.btn_lerrfid);
        btnAssociar = (Button) findViewById(R.id.btn_associar);

        btnLerRfid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                comecarLeitura();
                Thread syncReadThread = new Thread(mSyncReadRunnable);
                syncReadThread.start();
            }
        });

        btnAssociar.setEnabled(false);
        btnAssociar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validarLeitura();
            }
        });

        etCodigoBarras.setEnabled(false);
        etEtiqRfid.setEnabled(false);

        rvLeituras = findViewById(R.id.rv_leituras);
        rvLayoutManager = new LinearLayoutManager(this);
        rvLeituras.setLayoutManager(rvLayoutManager);
        rvLeituras.setHasFixedSize(true);
        rvLeituras.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));

        listarLeituras();

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Toast.makeText(LeituraActivity.this, "Leitura excluida", Toast.LENGTH_SHORT).show();
                int position  = viewHolder.getAdapterPosition();
                LeituraEtiqueta leitura = leituras.get(position);
                leituras.remove(leitura);
                leituraAdapter.notifyDataSetChanged();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(rvLeituras);

        mApp = App.getInstance();

        initHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(barcodeDataReceiver, new IntentFilter(ACTION_BARCODE_DATA));
        claimScanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    private void initHandler() {
        mUiHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.i(TAG, "HandleMessage msg.what:" + msg.what);

                switch (msg.what) {
                    case MSG_UPDATE_UI_TEXTO_TAG:
                        selecionaTagLida();
                        break;
                    case MSG_UPDATE_UI_BOTAO_ASSOCIAR:
                        habilitaDesabilitaAssociar();
                        break;
                    default:
                        break;
                }
            }
        };

//        Thread syncReadThread = new Thread(mSyncReadRunnable);
//        syncReadThread.start();
//
//        mReadHandlerThread.start();
//        mReadHandler = new Handler(mReadHandlerThread.getLooper());
    }

    private void habilitaDesabilitaAssociar() {
        String codBarras = etCodigoBarras.getText().toString();
        String tagRfid = etEtiqRfid.getText().toString();
        boolean codBarrasLido = true;
        boolean tagRfidLida = true;

        if (codBarras == null || codBarras.trim().isEmpty())
            codBarrasLido = false;
        if (tagRfid == null || tagRfid.trim().isEmpty())
            tagRfidLida = false;

        if (codBarrasLido && tagRfidLida)
            btnAssociar.setEnabled(true);
        else
            btnAssociar.setEnabled(false);
    }

    private void selecionaTagLida() {
        HashSet<String> removeDuplicados = new HashSet<>(tagsList);
        List<String> tags = new ArrayList<>(removeDuplicados);

        if (tags.size() > 1) {
            Toast.makeText(this, "Mais de uma TAG RFID lida, realize nova leitura", Toast.LENGTH_SHORT).show();
            return;
        }

        String tag = tags.get(0);
        etEtiqRfid.setText(tag);

        Message message = Message.obtain();
        message.what = MSG_UPDATE_UI_BOTAO_ASSOCIAR;
        mUiHandler.removeMessages(message.what);
        mUiHandler.sendMessage(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSyncReadRunnable.release();
        mUiHandler.removeCallbacksAndMessages(null);
    }

    private void comecarLeitura() {
        try {
            if (mApp.checkIsRFIDReady()) {
                mApp.ListMs.clear();
                mReadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        comecarLeituraInterna();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void atualizarListView() {

    }

    private void comecarLeituraInterna() {
        RfidReader reader = getReader();
        reader.read(-1, mTagReadOption);
    }

    private SyncReadRunnable mSyncReadRunnable = new SyncReadRunnable();

    private class SyncReadRunnable implements Runnable {
        private boolean mRun = true;

        void release() {
            mRun = false;
        }

        @Override
        public void run() {
            while(mRun) {
                TagReadData[] trds = getReader().syncRead(-1, 1000);
                for (TagReadData t : trds) {
                    String tag = t.getEpcHexStr();
                    Log.i(">>> Tag: ", tag);

                    tagsList.add(tag);
                    Message message = Message.obtain();
                    message.what = MSG_UPDATE_UI_TEXTO_TAG;
                    mUiHandler.removeMessages(message.what);
                    mUiHandler.sendMessage(message);
                }
            }
        }
    }

    private RfidReader getReader() {
        return mApp.rfidReader;
    }

    private void validarLeitura() {
        String etiqRfid = etEtiqRfid.getText().toString();
        String codigoBarras = etCodigoBarras.getText().toString();

//        int idEtiqueta = leituras.size() + 1;
//        LeituraEtiqueta leitura = new LeituraEtiqueta(idEtiqueta, "17/08/2023", etiqRfid, codigoBarras);
//        leituras.add(leitura);
//        leituraAdapter.notifyDataSetChanged();

        etCodigoBarras.setText("");
        etEtiqRfid.setText("");
        habilitaDesabilitaAssociar();

        RequestQueue queue = Volley.newRequestQueue(LeituraActivity.this);
        String url = mApp.API_URL + "/leitura";

        JSONObject body = new JSONObject();

        String codEmpresa = "01";
        String user = "admlog";

        String[] codigoBarrasSplit = codigoBarras.split("\\|");
        int ordemProd = Integer.parseInt(codigoBarrasSplit[0]);
        int ordemSeq = Integer.parseInt(codigoBarrasSplit[1]);

        try {
            body.put("codEmpresa", codEmpresa);
            body.put("tagRfid", etiqRfid);
            body.put("ordemProd", ordemProd);
            body.put("ordemSeq", ordemSeq);
            body.put("user", user);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Toast.makeText(LeituraActivity.this, response.getString("mensagem"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, ">>> Response: " + response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LeituraActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                Log.i(TAG, ">>> Response: " + error.toString());
            }
        });

        queue.add(request);
    }

    private void listarLeituras() {
//        RequestQueue queue = Volley.newRequestQueue(LeituraActivity.this);
//        String url = mApp.API_URL + "/leitura";
//
//        String codEmpresa = "01";
//        String usuario = "admlog";
//
//        url += "?codEmpresa="+codEmpresa+"&usuario="+usuario;
//
//        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                Toast.makeText(LeituraActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
//                Log.i(TAG, ">>> Response: " + response.toString());
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(LeituraActivity.this, error.toString(), Toast.LENGTH_LONG).show();
//                Log.i(TAG, ">>> Error: " + error.toString());
//            }
//        });
//
//        request.setRetryPolicy(new DefaultRetryPolicy(3600, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//
//        queue.add(request);

        LeituraEtiqueta leitura1 = new LeituraEtiqueta(1, "17/08/2023", "ABC000000001", "12345|1");
        leituras.add(leitura1);

        LeituraEtiqueta leitura2 = new LeituraEtiqueta(2, "17/08/2023", "ABC000000002", "12345|2");
        leituras.add(leitura2);

        LeituraEtiqueta leitura3 = new LeituraEtiqueta(3, "17/08/2023", "ABC000000003", "12346|1");
        leituras.add(leitura3);

        leituraAdapter = new LeituraAdapter(leituras);
        rvLeituras.setAdapter(leituraAdapter);
    }
}