package org.exemplo.belloryadmin.model.entity.webhook;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_event_log", schema = "admin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "delivery_id", length = 100)
    private String deliveryId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "organizacao_id")
    private Long organizacaoId;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 50)
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "dt_recebido", nullable = false)
    private LocalDateTime dtRecebido;

    @Column(name = "dt_processado")
    private LocalDateTime dtProcessado;

    @PrePersist
    public void prePersist() {
        if (dtRecebido == null) dtRecebido = LocalDateTime.now();
        if (status == null) status = "RECEIVED";
    }
}
