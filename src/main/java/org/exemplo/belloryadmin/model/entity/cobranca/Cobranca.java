package org.exemplo.belloryadmin.model.entity.cobranca;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;
import org.exemplo.belloryadmin.model.entity.users.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cobranca - versao admin (read-mostly).
 *
 * <p>Sem agendamento/compra/cobrancaRelacionada/pagamentos. Admin so usa em
 * COUNT por organizacao_id e statusCobranca.</p>
 */
@Entity
@Table(name = "cobranca", schema = "app", indexes = {
    @Index(name = "idx_cobranca_organizacao_id", columnList = "organizacao_id"),
    @Index(name = "idx_cobranca_status", columnList = "status_cobranca"),
    @Index(name = "idx_cobranca_dt_criacao", columnList = "dt_criacao")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_cobranca", nullable = false, length = 50)
    private StatusCobranca statusCobranca;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cobranca", nullable = false, length = 30)
    private TipoCobranca tipoCobranca;

    @Column(name = "dt_vencimento")
    private LocalDate dtVencimento;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    public enum StatusCobranca {
        PENDENTE,
        PARCIALMENTE_PAGO,
        PAGO,
        VENCIDA,
        CANCELADA,
        ESTORNADA
    }

    public enum TipoCobranca {
        AGENDAMENTO,
        COMPRA,
        TAXA_ADICIONAL,
        MULTA
    }
}
