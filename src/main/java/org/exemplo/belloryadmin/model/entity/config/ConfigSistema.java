package org.exemplo.belloryadmin.model.entity.config;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;

import java.time.LocalDateTime;

/**
 * ConfigSistema do admin (read-mostly).
 *
 * <p>Inclui os 5 embeddables (Agendamento, Servico, Cliente, Colaborador,
 * Notificacao) porque o {@code AdminOrganizacaoService} expoe todos eles no
 * detalhe da organizacao.</p>
 */
@Entity
@Table(name = "config_sistema", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false, unique = true)
    private Organizacao organizacao;

    @Column(name = "usa_ecommerce", nullable = false)
    private boolean usaEcommerce = true;

    @Column(name = "usa_gestao_produtos", nullable = false)
    private boolean usaGestaoProdutos = true;

    @Column(name = "usa_planos_para_clientes", nullable = false)
    private boolean usaPlanosParaClientes = false;

    @Column(name = "dispara_notificacoes_push", nullable = false)
    private boolean disparaNotificacoesPush = true;

    @Column(name = "url_acesso", nullable = false)
    private String urlAcesso = "";

    @Column(name = "tenant_id", nullable = false)
    private String tenantId = "";

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "toleranciaAgendamento", column = @Column(name = "agend_tolerancia")),
    })
    private ConfigAgendamento configAgendamento;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "config_servico", column = @Column(name = "config_servico")),
    })
    private ConfigServico configServico;

    @Embedded
    @AttributeOverrides({ @AttributeOverride(name = "config_cliente", column = @Column(name = "config_cliente")) })
    private ConfigCliente configCliente;

    @Embedded
    @AttributeOverrides({ @AttributeOverride(name = "config_colaborador", column = @Column(name = "config_colaborador")) })
    private ConfigColaborador configColaborador;

    @Embedded
    @AttributeOverrides({ @AttributeOverride(name = "config_notificacao", column = @Column(name = "config_notificacao")) })
    private ConfigNotificacao configNotificacao;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;
}
