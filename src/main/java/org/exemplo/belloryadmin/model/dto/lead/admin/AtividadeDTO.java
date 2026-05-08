package org.exemplo.belloryadmin.model.dto.lead.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoAtividade;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtividadeDTO {
    private Long id;
    private TipoAtividade tipo;
    private String descricao;
    private Map<String, Object> dados;
    private ResponsavelMiniDTO autor;
    private LocalDateTime dtCriacao;
    private boolean automatica;
}
