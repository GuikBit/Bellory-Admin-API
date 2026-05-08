package org.exemplo.belloryadmin.model.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFuncionarioMetricasDTO {
    private Long totalFuncionariosGeral;
    private Long funcionariosAtivos;
    private Long funcionariosInativos;
    private Double mediaFuncionariosPorOrganizacao;

    private List<FuncionarioPorOrganizacao> porOrganizacao;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FuncionarioPorOrganizacao {
        private Long organizacaoId;
        private String nomeFantasia;
        private Long totalFuncionarios;
        private Long funcionariosAtivos;
        private Long totalServicosVinculados;
    }
}
