package com.buck.leonardo.rfidpoc;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
                RequestQueue queue = Volley.newRequestQueue(LeituraActivity.this);
                String url = "http://b568-191-13-138-182.ngrok-free.app/api/leitura";

                JSONObject body = new JSONObject();

                String codEmpresa = "01";
                String user = "admlog";

                String tagRfid = tvTagRfid.getText().toString();

                String[] codigoBarras = tvCodigoBarras.getText().toString().split("\\|");
                int ordemProd = Integer.parseInt(codigoBarras[0]);
                int ordemSeq = Integer.parseInt(codigoBarras[1]);


                try {
                    body.put("codEmpresa", codEmpresa);
                    body.put("tagRfid", tagRfid);
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
        });

        tvCodigoBarras = findViewById(R.id.tv_cod_barras);
        tvCodigoBarras.setText("");

        tvTagRfid = findViewById(R.id.tv_tag_rfid);
        tvTagRfid.setText("");

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
        String codBarras = tvCodigoBarras.getText().toString();
        String tagRfid = tvTagRfid.getText().toString();
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
        tvTagRfid.setText(tag);

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
            tvCodigoBarras.setText(codigoBarras);

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
}