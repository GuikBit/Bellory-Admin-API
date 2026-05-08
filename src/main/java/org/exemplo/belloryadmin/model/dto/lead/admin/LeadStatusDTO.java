package org.exemplo.belloryadmin.model.dto.lead.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadStatusDTO {
    private Long id;
    private String codigo;
    private String nome;
    private String cor;
    private Integer ordem;
    private boolean ativo;
    private boolean ehStatusInicial;
    private boolean ehStatusFinal;
}
