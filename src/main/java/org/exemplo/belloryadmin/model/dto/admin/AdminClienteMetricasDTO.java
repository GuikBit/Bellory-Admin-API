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
public class AdminClienteMetricasDTO {
    private Long totalClientesGeral;
    private Long clientesAtivos;
    private Long clientesInativos;
    private Double mediaClientesPorOrganizacao;

    private List<ClientePorOrganizacao> porOrganizacao;
    private List<ClienteMensal> evolucaoMensal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientePorOrganizacao {
        private Long organizacaoId;
        private String nomeFantasia;
        private Long totalClientes;
        private Long clientesAtivos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteMensal {
        private String mes;
        private Long novosClientes;
        private Long totalAcumulado;
    }
}
