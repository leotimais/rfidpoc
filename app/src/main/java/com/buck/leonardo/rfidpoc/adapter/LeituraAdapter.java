package com.buck.leonardo.rfidpoc.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.buck.leonardo.rfidpoc.R;
import com.buck.leonardo.rfidpoc.model.LeituraEtiqueta;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class LeituraAdapter extends RecyclerView.Adapter<LeituraAdapter.ViewHolder> {
    private List<LeituraEtiqueta> lista;

    public LeituraAdapter(List<LeituraEtiqueta> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public LeituraAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup group, int viewType) {
        View view = LayoutInflater.from(group.getContext())
                .inflate(R.layout.leitura_adapter, group, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeituraAdapter.ViewHolder holder, int position) {
        LeituraEtiqueta leitura = lista.get(position);

        String data = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        try {
            data = format.format(dateFormat.parse(leitura.getDataHoraLeitura()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        holder.tvIdEtiqueta.setText(String.valueOf(leitura.getId()));
        holder.tvDataLeitura.setText(data);
        holder.tvTagRfid.setText(leitura.getEtiqRfid());
        holder.tvOpSeq.setText(leitura.getOp()+"/"+leitura.getSeq());
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIdEtiqueta;
        TextView tvDataLeitura;
        TextView tvTagRfid;
        TextView tvOpSeq;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvIdEtiqueta = itemView.findViewById(R.id.tv_idetiqueta);
            tvDataLeitura = itemView.findViewById(R.id.tv_dataleitura);
            tvTagRfid = itemView.findViewById(R.id.tv_tagrfid);
            tvOpSeq = itemView.findViewById(R.id.tv_opseq);
        }
    }
}
