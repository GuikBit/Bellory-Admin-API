package org.exemplo.belloryadmin.model.dto.template;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariavelTemplateDTO {

    private String nome;
    private String descricao;
    private String exemplo;
}
