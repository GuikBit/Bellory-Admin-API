package org.exemplo.belloryadmin.model.entity.lead.enums;

/**
 * Tipos de atividade no historico de um lead.
 *
 * <p>Auto: registrados pelo sistema quando o lead muda de estado.</p>
 * <p>Manual: registrados pelo usuario admin (relata acoes feitas com o cliente).</p>
 */
public enum TipoAtividade {

    // ===== Automaticos (system) =====
    LEAD_CRIADO,
    MUDANCA_STATUS,
    ATRIBUICAO,

    // ===== Manuais (admin user) =====
    COMENTARIO,
    LIGACAO,
    EMAIL,
    WHATSAPP,
    REUNIAO,
    NOTA_INTERNA;

    public boolean isAutomatico() {
        return this == LEAD_CRIADO || this == MUDANCA_STATUS || this == ATRIBUICAO;
    }

    public boolean isManual() {
        return !isAutomatico();
    }
}
