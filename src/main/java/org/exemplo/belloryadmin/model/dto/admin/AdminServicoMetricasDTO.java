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
public class AdminServicoMetricasDTO {
    private Long totalServicosGeral;
    private Long servicosAtivos;
    private Long servicosInativos;
    private BigDecimal precoMedio;

    private List<ServicoPorOrganizacao> porOrganizacao;
    private List<ServicoMaisAgendado> maisAgendados;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicoPorOrganizacao {
        private Long organizacaoId;
        private String nomeFantasia;
        private Long totalServicos;
        private Long servicosAtivos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicoMaisAgendado {
        private Long servicoId;
        private String nomeServico;
        private String nomeOrganizacao;
        private Long totalAgendamentos;
        private BigDecimal preco;
    }
}
