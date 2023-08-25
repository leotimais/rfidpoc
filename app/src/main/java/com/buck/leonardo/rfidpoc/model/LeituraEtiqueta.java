package com.buck.leonardo.rfidpoc.model;

public class LeituraEtiqueta {

    private int id;
    private String empresa;
    private int op;
    private int seq;
    private String etiqRfid;
    private String dataHoraLeitura;
    private String dataHoraEfetivacao;
    private String status;
    private String usuarioLeitura;
    private String usuarioEfetivacao;

    public LeituraEtiqueta(int id, String empresa, int op, int seq, String etiqRfid, String dataHoraLeitura, String dataHoraEfetivacao, String status, String usuarioLeitura, String usuarioEfetivacao) {
        this.id = id;
        this.empresa = empresa;
        this.op = op;
        this.seq = seq;
        this.etiqRfid = etiqRfid;
        this.dataHoraLeitura = dataHoraLeitura;
        this.dataHoraEfetivacao = dataHoraEfetivacao;
        this.status = status;
        this.usuarioLeitura = usuarioLeitura;
        this.usuarioEfetivacao = usuarioEfetivacao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public int getOp() {
        return op;
    }

    public void setOp(int op) {
        this.op = op;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getEtiqRfid() {
        return etiqRfid;
    }

    public void setEtiqRfid(String etiqRfid) {
        this.etiqRfid = etiqRfid;
    }

    public String getDataHoraLeitura() {
        return dataHoraLeitura;
    }

    public void setDataHoraLeitura(String dataHoraLeitura) {
        this.dataHoraLeitura = dataHoraLeitura;
    }

    public String getDataHoraEfetivacao() {
        return dataHoraEfetivacao;
    }

    public void setDataHoraEfetivacao(String dataHoraEfetivacao) {
        this.dataHoraEfetivacao = dataHoraEfetivacao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsuarioLeitura() {
        return usuarioLeitura;
    }

    public void setUsuarioLeitura(String usuarioLeitura) {
        this.usuarioLeitura = usuarioLeitura;
    }

    public String getUsuarioEfetivacao() {
        return usuarioEfetivacao;
    }

    public void setUsuarioEfetivacao(String usuarioEfetivacao) {
        this.usuarioEfetivacao = usuarioEfetivacao;
    }
}
