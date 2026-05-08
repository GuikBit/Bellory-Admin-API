package org.exemplo.belloryadmin.model.entity.servico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Servico - versao admin (read-mostly).
 *
 * <p>Sem ManyToMany reverso pra Funcionario, sem ElementCollection de produtos
 * e imagens, sem anamnese. Admin so usa em queries de COUNT/AVG por org.</p>
 */
@Entity
@Table(name = "servico", schema = "app", indexes = {
        @Index(name = "idx_servico_organizacao_id", columnList = "organizacao_id"),
        @Index(name = "idx_servico_categoria_id", columnList = "categoria_id"),
        @Index(name = "idx_servico_org_ativo", columnList = "organizacao_id, isDeletado, ativo"),
        @Index(name = "idx_servico_ativo", columnList = "ativo"),
        @Index(name = "idx_servico_deletado", columnList = "isDeletado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(nullable = false, length = 255)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false)
    private boolean isDeletado = false;
}
