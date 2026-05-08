package org.exemplo.belloryadmin.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDTO {
    private Long totalOrganizacoes;
    private Long organizacoesAtivas;
    private Long organizacoesInativas;
    private Long totalAgendamentos;
    private Long totalClientes;
    private Long totalFuncionarios;
    private Long totalServicos;
    private Long totalInstancias;
    private Long instanciasConectadas;
    private Long instanciasDesconectadas;
    private BigDecimal faturamentoTotal;
    private Long totalCobrancas;
    private Long cobrancasPendentes;
    private Long cobrancasPagas;
    private DistribuicaoPlanos distribuicaoPlanos;
    private List<OrgLocationDTO> localizacoes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistribuicaoPlanos {
        private Long gratuito;
        private Long basico;
        private Long plus;
        private Long premium;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgLocationDTO {
        private String cidade;
        private String estado;
        private Double latitude;
        private Double longitude;
        private Long quantidade;
    }
}
