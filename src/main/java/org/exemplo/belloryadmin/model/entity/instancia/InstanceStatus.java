package org.exemplo.belloryadmin.model.entity.instancia;

public enum InstanceStatus {
    CONNECTED("Conectado"),
    CONNECTING("Conectando"),
    DISCONNECTED("Desconectado"),
    QRCODE("Aguardando QR Code"),
    ERROR("Erro"),
    OPEN("Open");

    private final String descricao;

    InstanceStatus(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
