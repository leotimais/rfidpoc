package com.buck.leonardo.rfidpoc;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class DispositivoActivity extends AppCompatActivity {

    private TextView tvDispositivo;
    private Button btnVincular;

    private final String TAG = "DispositivoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivo);

        setTitle("Dispositivo conectado");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tvDispositivo = findViewById(R.id.tv_dispositivo);
        btnVincular = findViewById(R.id.btn_vincular);
        btnVincular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DispositivoActivity.this, LeituraActivity.class);
                startActivity(intent);
            }
        });

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            String dispositivo = bundle.getString("dispositivo");
            tvDispositivo.setText(dispositivo);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }
}