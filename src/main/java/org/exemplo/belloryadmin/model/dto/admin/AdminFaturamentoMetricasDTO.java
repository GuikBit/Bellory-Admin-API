package org.exemplo.belloryadmin.model.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFaturamentoMetricasDTO {
    private BigDecimal faturamentoTotalGeral;
    private BigDecimal faturamentoMesAtual;
    private BigDecimal faturamentoMesAnterior;
    private BigDecimal crescimentoPercentual;
    private BigDecimal ticketMedio;
    private Long totalPagamentos;
    private Long pagamentosConfirmados;

    private List<FaturamentoPorOrganizacao> porOrganizacao;
    private List<FaturamentoMensal> evolucaoMensal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaturamentoPorOrganizacao {
        private Long organizacaoId;
        private String nomeFantasia;
        private String planoCodigo;
        private BigDecimal faturamentoTotal;
        private BigDecimal faturamentoMes;
        private Long totalCobrancas;
        private Long cobrancasPagas;
        private Long cobrancasPendentes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaturamentoMensal {
        private String mes;
        private BigDecimal valor;
        private Long quantidadePagamentos;
    }
}
