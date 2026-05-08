package org.exemplo.belloryadmin.model.dto.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TemplateBelloryCreateDTO {

    @NotBlank(message = "Codigo e obrigatorio")
    @Size(max = 50, message = "Codigo deve ter no maximo 50 caracteres")
    private String codigo;

    @NotBlank(message = "Nome e obrigatorio")
    @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
    private String nome;

    private String descricao;

    @NotNull(message = "Tipo e obrigatorio")
    private TipoTemplate tipo;

    @NotNull(message = "Categoria e obrigatoria")
    private CategoriaTemplate categoria;

    @Size(max = 255, message = "Assunto deve ter no maximo 255 caracteres")
    private String assunto;

    @NotBlank(message = "Conteudo e obrigatorio")
    private String conteudo;

    private List<VariavelTemplateDTO> variaveisDisponiveis;

    @Size(max = 50, message = "Icone deve ter no maximo 50 caracteres")
    private String icone;
}
