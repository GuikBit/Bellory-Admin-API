package org.exemplo.belloryadmin.model.entity.webhook;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_event_config", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEventConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, unique = true, length = 100)
    private String eventType;

    @Column(length = 255)
    private String descricao;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "invalidar_cache", nullable = false)
    private boolean invalidarCache;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "dt_criacao", nullable = false, updatable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "dt_atualizacao")
    private LocalDateTime dtAtualizacao;

    @PrePersist
    public void prePersist() {
        if (dtCriacao == null) dtCriacao = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        dtAtualizacao = LocalDateTime.now();
    }
}
