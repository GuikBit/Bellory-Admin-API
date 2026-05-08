package org.exemplo.belloryadmin.model.entity.organizacao;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.config.ConfigSistema;
import org.exemplo.belloryadmin.model.entity.endereco.Endereco;

import java.time.LocalDateTime;

/**
 * Organizacao - versao admin (read-mostly).
 *
 * <p>Sem o embedded {@code Tema} (admin nao consome o tema visual da org).
 * Demais relacionamentos sao mantidos LAZY pra Hibernate so trazer quando o
 * AdminQueryRepository fizer LEFT JOIN FETCH explicito.</p>
 */
@Entity
@Table(name = "organizacao", schema = "app", indexes = {
        @Index(name = "idx_organizacao_slug_ativo", columnList = "slug, ativo"),
        @Index(name = "idx_organizacao_plano_id", columnList = "plano_id"),
        @Index(name = "idx_organizacao_ativo", columnList = "ativo"),
        @Index(name = "idx_organizacao_dt_cadastro", columnList = "dt_cadastro")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "cnpj", unique = true, nullable = false, length = 18)
    private String cnpj;

    @Column(name = "razao_social")
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "publico_alvo")
    private String publicoAlvo;

    @Column(name = "inscricao_estadual", length = 50)
    private String inscricaoEstadual;

    @Column(name = "emailPrincipal")
    private String emailPrincipal;

    @Column(name = "telefone1", length = 20)
    private String telefone1;

    @Column(name = "telefone2", length = 20)
    private String telefone2;

    @Column(name = "whatsapp", length = 20)
    private String whatsapp;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nome", column = @Column(name = "responsavel_nome")),
            @AttributeOverride(name = "email", column = @Column(name = "responsavel_email")),
            @AttributeOverride(name = "telefone", column = @Column(name = "responsavel_telefone"))
    })
    private Responsavel responsavel;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_principal_id", referencedColumnName = "id")
    private Endereco enderecoPrincipal;

    @Column(unique = true, nullable = false)
    private String slug;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_sistema_id")
    private ConfigSistema configSistema;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "instagram", column = @Column(name = "redes_sociais_instagram")),
            @AttributeOverride(name = "facebook", column = @Column(name = "redes_sociais_facebook")),
            @AttributeOverride(name = "whatsapp", column = @Column(name = "redes_sociais_whatsapp")),
            @AttributeOverride(name = "linkedin", column = @Column(name = "redes_sociais_linkedin")),
            @AttributeOverride(name = "messenger", column = @Column(name = "redes_sociais_messenger")),
            @AttributeOverride(name = "site", column = @Column(name = "redes_sociais_site")),
            @AttributeOverride(name = "youtube", column = @Column(name = "redes_sociais_youtube"))
    })
    private RedesSociais redesSociais;

    @OneToOne(mappedBy = "organizacao", fetch = FetchType.LAZY)
    private DadosFaturamentoOrganizacao dadosFaturamento;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "dt_cadastro", nullable = false, updatable = false)
    private LocalDateTime dtCadastro;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;
}
