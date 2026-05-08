package org.exemplo.belloryadmin.model.dto.template;

import jakarta.validation.constraints.Size;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.template.CategoriaTemplate;
import org.exemplo.belloryadmin.model.entity.template.TipoTemplate;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateBelloryUpdateDTO {

    @Size(max = 50, message = "Codigo deve ter no maximo 50 caracteres")
    private String codigo;

    @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
    private String nome;

    private String descricao;

    private TipoTemplate tipo;

    private CategoriaTemplate categoria;

    @Size(max = 255, message = "Assunto deve ter no maximo 255 caracteres")
    private String assunto;

    private String conteudo;

    private List<VariavelTemplateDTO> variaveisDisponiveis;

    @Size(max = 50, message = "Icone deve ter no maximo 50 caracteres")
    private String icone;
}
