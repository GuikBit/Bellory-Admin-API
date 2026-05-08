package org.exemplo.belloryadmin.model.entity.lead;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Coluna configuravel do kanban de leads.
 *
 * <p>O administrador pode criar/editar/ordenar essas colunas pela UI.
 * Exatamente um registro deve ter {@code ehStatusInicial = true} (constraint
 * parcial uniqueness no banco). Status com {@code ehStatusFinal = true} marcam
 * leads como ganhos/perdidos para fins de funil.</p>
 */
@Entity
@Table(name = "lead_status", schema = "admin",
        indexes = {
                @Index(name = "idx_lead_status_ordem", columnList = "ordem")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "cor", nullable = false, length = 7)
    @Builder.Default
    private String cor = "#6B7280";

    @Column(name = "ordem", nullable = false)
    @Builder.Default
    private Integer ordem = 0;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(name = "eh_status_inicial", nullable = false)
    @Builder.Default
    private boolean ehStatusInicial = false;

    @Column(name = "eh_status_final", nullable = false)
    @Builder.Default
    private boolean ehStatusFinal = false;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    protected void onCreate() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dtAtualizacao = LocalDateTime.now();
    }
}
