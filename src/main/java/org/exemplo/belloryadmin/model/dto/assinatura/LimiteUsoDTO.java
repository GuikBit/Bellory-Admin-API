package org.exemplo.belloryadmin.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LimiteUsoDTO {

    private String key;
    private String label;
    private String tipo;        // NUMBER, BOOLEAN, UNLIMITED

    // Para tipo NUMBER
    private Long limite;
    private Long usado;
    private Long disponivel;

    // Para tipo BOOLEAN
    private Boolean habilitado;
}
