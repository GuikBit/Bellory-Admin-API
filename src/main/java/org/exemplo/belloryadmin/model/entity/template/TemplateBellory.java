package org.exemplo.belloryadmin.model.entity.template;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "template_bellory", schema = "admin",
        indexes = {
                @Index(name = "idx_template_tipo_categoria", columnList = "tipo, categoria"),
                @Index(name = "idx_template_ativo", columnList = "ativo")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateBellory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoTemplate tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaTemplate categoria;

    @Column(name = "assunto", length = 255)
    private String assunto;

    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "variaveis_disponiveis", columnDefinition = "jsonb")
    private String variaveisDisponiveis;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "padrao", nullable = false)
    private boolean padrao = false;

    @Column(name = "icone", length = 50)
    private String icone;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "user_criacao")
    private Long userCriacao;

    @Column(name = "user_atualizacao")
    private Long userAtualizacao;

    @PrePersist
    protected void onCreate() {
        this.dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }
}
