package com.buck.leonardo.rfidpoc;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.TagReadData;

import java.util.ArrayList;
import java.util.List;

public class LeituraActivity extends AppCompatActivity {

    private App mApp;
    private List<String> tagsList;
    private ArrayAdapter<String> tagsListAdapter;
    private Handler mUiHandler;

    private static final String TAG = "LeituraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura);

        Button btnLeitura = findViewById(R.id.btn_leitura);
        btnLeitura.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(LeituraActivity.this, "Clicou", Toast.LENGTH_SHORT).show();
                Thread syncReadThread = new Thread(mSyncReadRunnable);
                syncReadThread.start();
            }
        });

        ListView tagsListView = findViewById(R.id.lv_tags);
        tagsList = new ArrayList<>();
        tagsListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        tagsListView.setAdapter(tagsListAdapter);

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