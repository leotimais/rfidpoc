package com.buck.leonardo.rfidpoc.model;

public class LeituraEtiqueta {

    private int id;
    private String dataHoraLeitura;
    private String tagRfid;
    private String opSeq;

    public LeituraEtiqueta(int id, String dataHoraLeitura, String tagRfid, String opSeq) {
        this.id = id;
        this.dataHoraLeitura = dataHoraLeitura;
        this.tagRfid = tagRfid;
        this.opSeq = opSeq;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDataHoraLeitura() {
        return dataHoraLeitura;
    }

    public void setDataHoraLeitura(String dataHoraLeitura) {
        this.dataHoraLeitura = dataHoraLeitura;
    }

    public String getTagRfid() {
        return tagRfid;
    }

    public void setTagRfid(String tagRfid) {
        this.tagRfid = tagRfid;
    }

    public String getOpSeq() {
        return opSeq;
    }

    public void setOpSeq(String opSeq) {
        this.opSeq = opSeq;
    }
}
