package com.buck.leonardo.rfidpoc;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
import java.util.List;
import java.util.Map;

public class LeituraActivity extends AppCompatActivity {

    private ListView tagsListView;
    private Button btnLeituraCodBarras;
    private Button btnLeituraTags;

    private App mApp;
    private List<String> tagsList;
    private ArrayAdapter<String> tagsListAdapter;
    private Handler mUiHandler;
    private HandlerThread mReadHandlerThread = new HandlerThread("ReadHandler");
    private Handler mReadHandler;

    public List<Map<String, ?>> mOldList = Collections.synchronizedList(new ArrayList<Map<String, ?>>());

    private TagReadOption mTagReadOption = new TagReadOption();

    private String codigoBarras;

    private static final String TAG = "LeituraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura);

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

        btnLeituraTags.setEnabled(false);

        tagsListView = findViewById(R.id.lv_tags);
        tagsList = new ArrayList<>();
        tagsListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        tagsListView.setAdapter(tagsListAdapter);
        tagsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                RequestQueue queue = Volley.newRequestQueue(LeituraActivity.this);
                String url = "http://aa1b-191-13-138-182.ngrok-free.app/api/leitura";

                String tag = tagsList.get(position);

                JSONObject body = new JSONObject();
                try {
                    body.put("codBarraOrigem", codigoBarras);
                    body.put("tag", tag);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(LeituraActivity.this, ">>> Response: " + response.toString(), Toast.LENGTH_LONG).show();
                        Log.i(TAG, ">>> Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(LeituraActivity.this, ">>> Response: " + error.toString(), Toast.LENGTH_LONG).show();
                        Log.i(TAG, ">>> Response: " + error.toString());
                    }
                });

                queue.add(request);
            }
        });

        mApp = App.getInstance();

        initHandler();
    }

    private void initHandler() {
        mUiHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.i(TAG, "HandleMessage msg.what:" + msg.what);
                updateListView();
            }
        };

//        Thread syncReadThread = new Thread(mSyncReadRunnable);
//        syncReadThread.start();
//
//        mReadHandlerThread.start();
//        mReadHandler = new Handler(mReadHandlerThread.getLooper());
    }

    private void updateListView() {
        tagsListAdapter.clear();
        for (String tag: tagsList) {
            tagsListAdapter.add(tag);
            tagsListAdapter.notifyDataSetChanged();
        }
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
                    message.what = 0;
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
            codigoBarras = result.getContents();
            Toast.makeText(LeituraActivity.this, "Código de barras lido: " + codigoBarras, Toast.LENGTH_SHORT).show();
            btnLeituraTags.setEnabled(true);
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