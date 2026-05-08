package org.exemplo.belloryadmin.model.dto.lead.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoAtividade;

/**
 * Adicionar atividade manual (comentario, ligacao, email, etc.) no lead.
 * O service rejeita tipos automaticos (LEAD_CRIADO, MUDANCA_STATUS, ATRIBUICAO).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAtividadeDTO {

    @NotNull
    private TipoAtividade tipo;

    @Size(max = 4000)
    private String descricao;
}
