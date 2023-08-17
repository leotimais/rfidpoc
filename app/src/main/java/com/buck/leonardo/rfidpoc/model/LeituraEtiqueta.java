package com.buck.leonardo.rfidpoc.model;

public class LeituraEtiqueta {

    private int idEtiqueta;
    private String dataLeitura;
    private String tagRfid;
    private String opSeq;

    public LeituraEtiqueta(int idEtiqueta, String dataLeitura, String tagRfid, String opSeq) {
        this.idEtiqueta = idEtiqueta;
        this.dataLeitura = dataLeitura;
        this.tagRfid = tagRfid;
        this.opSeq = opSeq;
    }

    public int getIdEtiqueta() {
        return idEtiqueta;
    }

    public void setIdEtiqueta(int idEtiqueta) {
        this.idEtiqueta = idEtiqueta;
    }

    public String getDataLeitura() {
        return dataLeitura;
    }

    public void setDataLeitura(String dataLeitura) {
        this.dataLeitura = dataLeitura;
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
