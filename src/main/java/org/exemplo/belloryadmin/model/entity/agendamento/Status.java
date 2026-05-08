package org.exemplo.belloryadmin.model.entity.agendamento;

public enum Status {
    PENDENTE("Pendente"),
    AGENDADO("Agendado"),
    CONFIRMADO("Confirmado"),
    AGUARDANDO_CONFIRMACAO("A confirmar"),
    EM_ESPERA("Em Espera"),
    CONCLUIDO("Concluido"),
    CANCELADO("Cancelado"),
    EM_ANDAMENTO("Em Andamento"),
    NAO_COMPARECEU("Nao Compareceu"),
    REAGENDADO("Reagendado"),
    VENCIDA("Vencida"),
    PAGO("Pago");

    private final String descricao;

    Status(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
