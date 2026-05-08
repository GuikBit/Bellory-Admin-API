package org.exemplo.belloryadmin.model.entity.servico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.enums.TipoCategoria;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;

@Entity
@Table(name = "categoria", schema = "app", indexes = {
        @Index(name = "idx_categoria_organizacao_id", columnList = "organizacao_id"),
        @Index(name = "idx_categoria_org_tipo_ativo", columnList = "organizacao_id, tipo, ativo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false, length = 255)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoCategoria tipo;

    @Column(nullable = false)
    private boolean ativo = true;
}
