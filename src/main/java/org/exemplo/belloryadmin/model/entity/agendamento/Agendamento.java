package org.exemplo.belloryadmin.model.entity.agendamento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;
import org.exemplo.belloryadmin.model.entity.users.Cliente;

import java.time.LocalDateTime;

/**
 * Agendamento - versao admin (read-mostly).
 *
 * <p>Sem ManyToMany de servicos/funcionarios, sem OneToMany de cobrancas/
 * questionarios, sem BloqueioAgenda. AdminQueryRepository so usa COUNT por
 * organizacao_id, status, dtCriacao.</p>
 */
@Entity
@Table(name = "agendamento", schema = "app", indexes = {
        @Index(name = "idx_agendamento_organizacao_id", columnList = "organizacao_id"),
        @Index(name = "idx_agendamento_cliente_id", columnList = "cliente_id"),
        @Index(name = "idx_agendamento_status", columnList = "status"),
        @Index(name = "idx_agendamento_dt_agendamento", columnList = "dtAgendamento"),
        @Index(name = "idx_agendamento_org_dt", columnList = "organizacao_id, dtAgendamento"),
        @Index(name = "idx_agendamento_org_status", columnList = "organizacao_id, status"),
        @Index(name = "idx_agendamento_dt_criacao", columnList = "dtCriacao")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "dtAgendamento", nullable = false)
    private LocalDateTime dtAgendamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "dtCriacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Column(name = "dtAtualizacao")
    private LocalDateTime dtAtualizacao;
}
