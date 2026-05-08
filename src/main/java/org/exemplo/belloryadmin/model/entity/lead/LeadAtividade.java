package org.exemplo.belloryadmin.model.entity.lead;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoAtividade;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Item do historico de um lead. Cobre:
 * <ul>
 *   <li>Auto: LEAD_CRIADO, MUDANCA_STATUS, ATRIBUICAO</li>
 *   <li>Manual: COMENTARIO, LIGACAO, EMAIL, WHATSAPP, REUNIAO, NOTA_INTERNA</li>
 * </ul>
 *
 * <p>{@link #dados} guarda payload extra dependendo do tipo
 * (ex: {@code {fromStatus, toStatus}} para MUDANCA_STATUS).</p>
 */
@Entity
@Table(name = "lead_atividade", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadAtividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoAtividade tipo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados", columnDefinition = "jsonb")
    private Map<String, Object> dados;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private UsuarioAdmin autor;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @PrePersist
    protected void onCreate() {
        if (this.dtCriacao == null) {
            this.dtCriacao = LocalDateTime.now();
        }
    }
}
