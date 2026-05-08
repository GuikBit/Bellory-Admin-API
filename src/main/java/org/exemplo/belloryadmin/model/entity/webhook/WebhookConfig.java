package org.exemplo.belloryadmin.model.entity.webhook;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "PaymentWebhookConfig")
@Table(name = "webhook_config", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(nullable = false)
    private boolean ativo;

    @Column(length = 255)
    private String descricao;

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
