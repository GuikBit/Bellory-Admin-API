package org.exemplo.belloryadmin.model.entity.organizacao;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Responsavel {
    private String nome;
    private String email;
    private String telefone;
}
