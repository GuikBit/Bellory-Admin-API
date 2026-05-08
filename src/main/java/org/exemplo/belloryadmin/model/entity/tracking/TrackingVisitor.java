package org.exemplo.belloryadmin.model.entity.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tracking_visitors", schema = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingVisitor {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "total_sessions")
    @Builder.Default
    private Integer totalSessions = 1;

    @Column(name = "total_page_views")
    @Builder.Default
    private Integer totalPageViews = 0;

    @Column(name = "is_converted")
    @Builder.Default
    private Boolean isConverted = false;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "conversion_plan_id", length = 50)
    private String conversionPlanId;

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (firstSeenAt == null) firstSeenAt = LocalDateTime.now();
        if (lastSeenAt == null) lastSeenAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
