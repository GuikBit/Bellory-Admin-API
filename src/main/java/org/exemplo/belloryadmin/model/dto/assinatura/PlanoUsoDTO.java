package org.exemplo.belloryadmin.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanoUsoDTO {

    private String planoCodigo;
    private String planoNome;
    private boolean planoGratuito;
    private List<LimiteUsoDTO> limites;
    private List<LimiteUsoDTO> features;
}
