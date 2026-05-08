package org.exemplo.belloryadmin.model.entity.lead;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.lead.enums.PrioridadeLead;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoNegocio;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lead do funil comercial. Pode chegar via formulario publico
 * ({@code POST /api/contato}) ou ser criado manualmente pelo admin.
 *
 * <p>Soft delete via {@link #deletedAt}. Listagens admin filtram
 * {@code deleted_at IS NULL} por padrao.</p>
 */
@Entity
@Table(name = "lead", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private LeadStatus status;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, length = 160)
    private String email;

    @Column(name = "telefone", nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_negocio", nullable = false, length = 20)
    private TipoNegocio tipoNegocio;

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "origem", length = 120)
    private String origem;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 10)
    @Builder.Default
    private PrioridadeLead prioridade = PrioridadeLead.MEDIA;

    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "valor_estimado", precision = 12, scale = 2)
    private BigDecimal valorEstimado;

    @Column(name = "data_prevista_fechamento")
    private LocalDate dataPrevistaFechamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private UsuarioAdmin responsavel;

    // ===== Metadados de captura (publico) =====

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "fill_time_ms")
    private Integer fillTimeMs;

    @Column(name = "turnstile_ok", nullable = false)
    @Builder.Default
    private boolean turnstileOk = false;

    @Column(name = "policy_version", length = 20)
    private String policyVersion;

    // ===== Timestamps =====

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }
}
