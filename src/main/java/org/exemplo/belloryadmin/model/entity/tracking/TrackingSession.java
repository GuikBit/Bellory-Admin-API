package org.exemplo.belloryadmin.model.entity.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tracking_sessions", schema = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingSession {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private TrackingVisitor visitor;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_ms")
    @Builder.Default
    private Integer durationMs = 0;

    @Column(name = "page_count")
    @Builder.Default
    private Integer pageCount = 0;

    @Column(name = "entry_page", length = 500, nullable = false)
    private String entryPage;

    @Column(name = "exit_page", length = 500)
    private String exitPage;

    @Column(name = "traffic_source", length = 50, nullable = false)
    private String trafficSource;

    @Column(columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "utm_source", length = 255)
    private String utmSource;

    @Column(name = "utm_medium", length = 255)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 255)
    private String utmCampaign;

    @Column(name = "utm_term", length = 255)
    private String utmTerm;

    @Column(name = "utm_content", length = 255)
    private String utmContent;

    @Column(name = "device_type", length = 20, nullable = false)
    private String deviceType;

    @Column(length = 100)
    private String os;

    @Column(length = 100)
    private String browser;

    @Column(name = "screen_size", length = 20)
    private String screenSize;

    @Column(name = "viewport_size", length = 20)
    private String viewportSize;

    @Column(name = "touch_enabled")
    @Builder.Default
    private Boolean touchEnabled = false;

    @Column(length = 10)
    private String language;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String city;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "geo_source", length = 10)
    private String geoSource;

    @Column(name = "is_bounce")
    @Builder.Default
    private Boolean isBounce = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
