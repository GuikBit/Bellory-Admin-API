package org.exemplo.belloryadmin.model.entity.pagamento;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;
import org.exemplo.belloryadmin.model.entity.users.Cliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pagamento - versao admin (read-mostly).
 *
 * <p>Sem cobranca/cartaoCredito (admin so soma valor por org/status).</p>
 */
@Entity
@Table(name = "pagamento", schema = "app", indexes = {
    @Index(name = "idx_pagamento_organizacao_id", columnList = "organizacao_id"),
    @Index(name = "idx_pagamento_status", columnList = "status_pagamento"),
    @Index(name = "idx_pagamento_dt_criacao", columnList = "dt_criacao")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "dt_pagamento")
    private LocalDateTime dtPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento;

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    public enum StatusPagamento {
        PENDENTE,
        PROCESSANDO,
        CONFIRMADO,
        RECUSADO,
        CANCELADO,
        ESTORNADO
    }
}
