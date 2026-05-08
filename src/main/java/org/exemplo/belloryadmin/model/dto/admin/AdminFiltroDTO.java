package org.exemplo.belloryadmin.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFiltroDTO {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataInicio;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataFim;
    private Long organizacaoId;
    private String planoCodigo;
    private Boolean ativo;
}
