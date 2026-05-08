package org.exemplo.belloryadmin.model.entity.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_conversion_events", schema = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingConversionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TrackingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private TrackingVisitor visitor;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "plan_id", length = 50)
    private String planId;

    @Column(name = "plan_name", length = 100)
    private String planName;

    @Column(name = "billing_cycle", length = 20)
    private String billingCycle;

    @Column(name = "registration_step")
    private Integer registrationStep;

    @Column(name = "registration_step_name", length = 100)
    private String registrationStepName;

    @Column(name = "last_feature_viewed", length = 255)
    private String lastFeatureViewed;

    @Column(name = "time_to_convert_ms")
    private Long timeToConvertMs;

    @Column(name = "sessions_to_convert")
    private Integer sessionsToConvert;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
