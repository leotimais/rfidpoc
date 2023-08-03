package com.buck.leonardo.rfidpoc.data;

public class LeituraDto {
    private String codBarraOrigem;
    private String tag;

    public LeituraDto() {
    }

    public LeituraDto(String codBarraOrigem, String tag) {
        this.codBarraOrigem = codBarraOrigem;
        this.tag = tag;
    }

    public String getCodBarraOrigem() {
        return codBarraOrigem;
    }

    public void setCodBarraOrigem(String codBarraOrigem) {
        this.codBarraOrigem = codBarraOrigem;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
