package com.buck.leonardo.rfidpoc;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LeituraActivity extends AppCompatActivity {

    private ListView tagsListView;
    private Button btnLeitura;

    private App mApp;
    private List<String> tagsList;
    private ArrayAdapter<String> tagsListAdapter;
    private Handler mUiHandler;

    private static final String TAG = "LeituraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura);

        btnLeitura = findViewById(R.id.btn_leitura);
        btnLeitura.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread syncReadThread = new Thread(mSyncReadRunnable);
                syncReadThread.start();
            }
        });

        tagsListView = findViewById(R.id.lv_tags);
        tagsList = new ArrayList<>();
        tagsListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        tagsListView.setAdapter(tagsListAdapter);
        tagsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RequestQueue queue = Volley.newRequestQueue(LeituraActivity.this);
                String url = "http://aa1b-191-13-138-182.ngrok-free.app/WeatherForecast";

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
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
}