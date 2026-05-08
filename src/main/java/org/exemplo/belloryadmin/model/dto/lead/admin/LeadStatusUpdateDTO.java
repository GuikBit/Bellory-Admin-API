package org.exemplo.belloryadmin.model.dto.lead.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadStatusUpdateDTO {

    @Size(max = 80)
    private String nome;

    @Size(max = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve ser hex no formato #RRGGBB")
    private String cor;

    private Integer ordem;
    private Boolean ativo;
    private Boolean ehStatusInicial;
    private Boolean ehStatusFinal;
}
