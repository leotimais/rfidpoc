package com.buck.leonardo.rfidpoc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LeituraActivity extends AppCompatActivity {

    private Button btnLeituraCodBarras;
    private Button btnLeituraTags;
    private Button btnAssociar;
    private TextView tvCodigoBarras;
    private TextView tvTagRfid;
    private EditText etCodigoBarras;
    private EditText etTagRfid;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura);

        setTitle("Associação de RFID");

        btnLeituraTags = findViewById(R.id.btn_leitura_tags);
        btnLeituraTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                comecarLeitura();
                Thread syncReadThread = new Thread(mSyncReadRunnable);
                syncReadThread.start();
            }
        });

        btnLeituraCodBarras = findViewById(R.id.btn_leitura_cod_barras);
        btnLeituraCodBarras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iniciarLeituraCodigoBarras();
            }
        });

        btnAssociar = findViewById(R.id.btn_associar);
        btnAssociar.setEnabled(false);
        btnAssociar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tagRfid = etTagRfid.getText().toString();
                String codigoBarras = etCodigoBarras.getText().toString();

                int idEtiqueta = leituras.size() + 1;

                LeituraEtiqueta leitura = new LeituraEtiqueta(idEtiqueta, "17/08/2023", tagRfid, codigoBarras);
                leituras.add(leitura);

                leituraAdapter.notifyDataSetChanged();

                etCodigoBarras.setText("");
                etTagRfid.setText("");
                habilitaDesabilitaAssociar();

//                RequestQueue queue = Volley.newRequestQueue(LeituraActivity.this);
//                String url = mApp.API_URL + "/leitura";
//
//                JSONObject body = new JSONObject();
//
//                String codEmpresa = "01";
//                String user = "admlog";
//
//                String tagRfid = etTagRfid.getText().toString();
//
//                String[] codigoBarras = etCodigoBarras.getText().toString().split("\\|");
//                int ordemProd = Integer.parseInt(codigoBarras[0]);
//                int ordemSeq = Integer.parseInt(codigoBarras[1]);
//
//                try {
//                    body.put("codEmpresa", codEmpresa);
//                    body.put("tagRfid", tagRfid);
//                    body.put("ordemProd", ordemProd);
//                    body.put("ordemSeq", ordemSeq);
//                    body.put("user", user);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        try {
//                            Toast.makeText(LeituraActivity.this, response.getString("mensagem"), Toast.LENGTH_SHORT).show();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        Log.i(TAG, ">>> Response: " + response.toString());
//                    }
//                }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        Toast.makeText(LeituraActivity.this, error.toString(), Toast.LENGTH_LONG).show();
//                        Log.i(TAG, ">>> Response: " + error.toString());
//                    }
//                });
//
//                queue.add(request);
            }
        });

        tvCodigoBarras = findViewById(R.id.tv_codbarras);
        tvTagRfid = findViewById(R.id.tv_lbl_tagrfid);

        etCodigoBarras = findViewById(R.id.et_codbarras);
        etTagRfid = findViewById(R.id.et_tagrfid);

        etCodigoBarras.setEnabled(false);
        etTagRfid.setEnabled(false);

        rvLeituras = findViewById(R.id.rv_leituras);
        rvLayoutManager = new LinearLayoutManager(this);
        rvLeituras.setLayoutManager(rvLayoutManager);
        rvLeituras.setHasFixedSize(true);
        rvLeituras.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));

        listarLeituras();

//        rvLeituras.addOnItemTouchListener(new RecyclerItemClickListener(getApplicationContext(),
//                rvLeituras,
//                new RecyclerItemClickListener.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(View view, int position) {
//                        posicaoSelecionada = position;
//                        excluir();
//                    }
//
//                    @Override
//                    public void onLongItemClick(View view, int position) {
//
//                    }
//                }));

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
        String tagRfid = etTagRfid.getText().toString();
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
        etTagRfid.setText(tag);

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

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
    result -> {
        if(result.getContents() == null) {
            Toast.makeText(LeituraActivity.this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            String codigoBarras = result.getContents();
            etCodigoBarras.setText(codigoBarras);

            Message message = Message.obtain();
            message.what = MSG_UPDATE_UI_BOTAO_ASSOCIAR;
            mUiHandler.removeMessages(message.what);
            mUiHandler.sendMessage(message);
        }
    });

    private void iniciarLeituraCodigoBarras() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setCameraId(0);  // Use a specific camera of the device
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        barcodeLauncher.launch(options);
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

    private void excluir() {
        LeituraEtiqueta leitura = leituras.get(posicaoSelecionada);

        DialogInterface.OnClickListener listener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    leituras.remove(leitura);
                    leituraAdapter.notifyDataSetChanged();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Excluir leitura");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setMessage("Deseja excluir a leitura?");

        builder.setPositiveButton("Sim", listener);
        builder.setNegativeButton("Não", listener);

        AlertDialog alert = builder.create();
        alert.show();
    }
}