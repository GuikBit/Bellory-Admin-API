package org.exemplo.belloryadmin.model.entity.tracking;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_performance_snapshots", schema = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingPerformanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TrackingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private TrackingVisitor visitor;

    @Column(name = "page_load_time_ms")
    private Integer pageLoadTimeMs;

    @Column(name = "fcp_ms")
    private Integer fcpMs;

    @Column(name = "lcp_ms")
    private Integer lcpMs;

    @Column(name = "fid_ms")
    private Integer fidMs;

    @Column(precision = 5, scale = 3)
    private BigDecimal cls;

    @Column(name = "ttfb_ms")
    private Integer ttfbMs;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(length = 100)
    private String browser;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }
}
