package org.exemplo.belloryadmin.model.entity.instancia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;

/**
 * Instance - versao admin (read-mostly).
 *
 * <p>Sem Tools/WebhookConfig/Settings/KnowledgeBase (admin nao gerencia
 * configuracao operacional de instance — so lista por organizacao).</p>
 */
@Entity
@Table(name = "instance", schema = "app",
    indexes = {
        @Index(name = "idx_instance_organizacao_id", columnList = "organizacao_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String instanceId;

    @Column(nullable = false, unique = true, length = 100)
    private String instanceName;

    @Column(nullable = false)
    private String integration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InstanceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id", nullable = false)
    @JsonIgnore
    private Organizacao organizacao;

    @Column(name = "ativo")
    private boolean ativo;

    @Column(name = "deletado")
    private boolean deletado = false;
}
