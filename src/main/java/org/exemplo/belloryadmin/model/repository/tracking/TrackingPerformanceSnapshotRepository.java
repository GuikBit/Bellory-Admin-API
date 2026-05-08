package org.exemplo.belloryadmin.model.repository.tracking;

import org.exemplo.belloryadmin.model.entity.tracking.TrackingPerformanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingPerformanceSnapshotRepository extends JpaRepository<TrackingPerformanceSnapshot, Long> {

    @Query(value = """
        SELECT
            ROUND(AVG(ps.page_load_time_ms)) AS avg_load_time,
            ROUND(AVG(ps.fcp_ms)) AS avg_fcp,
            ROUND(AVG(ps.lcp_ms)) AS avg_lcp,
            ROUND(AVG(ps.fid_ms)) AS avg_fid,
            ROUND(AVG(ps.cls)::NUMERIC, 3) AS avg_cls,
            ROUND(AVG(ps.ttfb_ms)) AS avg_ttfb
        FROM site.tracking_performance_snapshots ps
        WHERE ps.recorded_at BETWEEN :start AND :end
        """, nativeQuery = true)
    List<Object[]> getAveragePerformance(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT ps.device_type,
               ROUND(AVG(ps.page_load_time_ms)) AS avg_load_time,
               ROUND(AVG(ps.lcp_ms)) AS avg_lcp,
               ROUND(AVG(ps.fcp_ms)) AS avg_fcp,
               ROUND(AVG(ps.cls)::NUMERIC, 3) AS avg_cls,
               COUNT(*) AS samples
        FROM site.tracking_performance_snapshots ps
        WHERE ps.recorded_at BETWEEN :start AND :end
        GROUP BY ps.device_type
        """, nativeQuery = true)
    List<Object[]> getPerformanceByDevice(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            percentile_cont(0.5) WITHIN GROUP (ORDER BY ps.page_load_time_ms) AS p50_load,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY ps.lcp_ms) AS p50_lcp,
            percentile_cont(0.75) WITHIN GROUP (ORDER BY ps.page_load_time_ms) AS p75_load,
            percentile_cont(0.75) WITHIN GROUP (ORDER BY ps.lcp_ms) AS p75_lcp,
            percentile_cont(0.95) WITHIN GROUP (ORDER BY ps.page_load_time_ms) AS p95_load,
            percentile_cont(0.95) WITHIN GROUP (ORDER BY ps.lcp_ms) AS p95_lcp
        FROM site.tracking_performance_snapshots ps
        WHERE ps.recorded_at BETWEEN :start AND :end
        """, nativeQuery = true)
    List<Object[]> getPerformancePercentiles(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
