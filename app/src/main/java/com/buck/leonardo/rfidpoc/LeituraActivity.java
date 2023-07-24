package com.buck.leonardo.rfidpoc;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class LeituraActivity extends AppCompatActivity {

    private App mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura);

        mApp = (App) getApplication();
    }
}