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
public class AdminInstanciaMetricasDTO {
    private Long totalInstancias;
    private Long instanciasAtivas;
    private Long instanciasDeletadas;
    private Long instanciasConectadas;
    private Long instanciasDesconectadas;

    private List<InstanciaPorOrganizacao> porOrganizacao;
    private List<InstanciaDetalheDTO> todasInstancias;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanciaPorOrganizacao {
        private Long organizacaoId;
        private String nomeFantasia;
        private Long totalInstancias;
        private Long instanciasAtivas;
        private Long instanciasConectadas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanciaDetalheDTO {
        private Long id;
        private String instanceName;
        private String instanceId;
        private String integration;
        private String status;
        private Boolean ativo;
        private Long organizacaoId;
        private String nomeFantasiaOrganizacao;
    }
}
