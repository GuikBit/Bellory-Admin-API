package org.exemplo.belloryadmin.model.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAgendamentoMetricasDTO {
    private Long totalGeral;
    private Long totalNoMes;
    private Long concluidos;
    private Long cancelados;
    private Long pendentes;
    private Long agendados;
    private Long naoCompareceu;
    private BigDecimal taxaConclusao;
    private BigDecimal taxaCancelamento;
    private BigDecimal taxaNoShow;

    // Por organizacao
    private List<AgendamentoPorOrganizacao> porOrganizacao;

    // Evolucao mensal (ultimos 12 meses)
    private List<AgendamentoMensal> evolucaoMensal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendamentoPorOrganizacao {
        private Long organizacaoId;
        private String nomeFantasia;
        private Long total;
        private Long concluidos;
        private Long cancelados;
        private Long pendentes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendamentoMensal {
        private String mes;
        private Long total;
        private Long concluidos;
        private Long cancelados;
    }
}
