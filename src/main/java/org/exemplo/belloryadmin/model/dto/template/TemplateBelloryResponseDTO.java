package org.exemplo.belloryadmin.model.dto.template;

import lombok.*;
import org.exemplo.belloryadmin.model.entity.template.CategoriaTemplate;
import org.exemplo.belloryadmin.model.entity.template.TipoTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateBelloryResponseDTO {

    private Long id;
    private String codigo;
    private String nome;
    private String descricao;
    private TipoTemplate tipo;
    private CategoriaTemplate categoria;
    private String assunto;
    private String conteudo;
    private List<VariavelTemplateDTO> variaveisDisponiveis;
    private boolean ativo;
    private boolean padrao;
    private String icone;

    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
    private Long userCriacao;
    private Long userAtualizacao;
}
