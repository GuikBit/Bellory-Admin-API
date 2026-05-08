package org.exemplo.belloryadmin.model.repository.tracking;

import org.exemplo.belloryadmin.model.entity.tracking.TrackingConversionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingConversionEventRepository extends JpaRepository<TrackingConversionEvent, Long> {

    @Query("SELECT COUNT(DISTINCT c.visitor.id) FROM TrackingConversionEvent c WHERE c.eventType = :type AND c.occurredAt BETWEEN :start AND :end")
    long countByTypeInPeriod(@Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            COUNT(DISTINCT CASE WHEN ce.event_type = 'plan_viewed' THEN ce.visitor_id END) AS viewed_plan,
            COUNT(DISTINCT CASE WHEN ce.event_type = 'cadastro_started' THEN ce.visitor_id END) AS started,
            COUNT(DISTINCT CASE WHEN ce.event_type = 'cadastro_step_completed' AND ce.registration_step = 0 THEN ce.visitor_id END) AS step0,
            COUNT(DISTINCT CASE WHEN ce.event_type = 'cadastro_step_completed' AND ce.registration_step = 1 THEN ce.visitor_id END) AS step1,
            COUNT(DISTINCT CASE WHEN ce.event_type = 'cadastro_step_completed' AND ce.registration_step = 2 THEN ce.visitor_id END) AS step2,
            COUNT(DISTINCT CASE WHEN ce.event_type = 'cadastro_step_completed' AND ce.registration_step = 3 THEN ce.visitor_id END) AS step3,
            COUNT(DISTINCT CASE WHEN ce.event_type = 'cadastro_step_completed' AND ce.registration_step = 4 THEN ce.visitor_id END) AS step4,
            COUNT(DISTINCT CASE WHEN ce.event_type = 'cadastro_completed' THEN ce.visitor_id END) AS completed
        FROM site.tracking_conversion_events ce
        WHERE ce.occurred_at BETWEEN :start AND :end
        """, nativeQuery = true)
    List<Object[]> getFunnelData(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT ce.plan_id, ce.plan_name, COUNT(*) AS count
        FROM site.tracking_conversion_events ce
        WHERE ce.event_type = 'cadastro_completed' AND ce.occurred_at BETWEEN :start AND :end
            AND ce.plan_id IS NOT NULL
        GROUP BY ce.plan_id, ce.plan_name
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Object[]> getPlanDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT ce.billing_cycle, COUNT(*) AS count
        FROM site.tracking_conversion_events ce
        WHERE ce.event_type = 'cadastro_completed' AND ce.occurred_at BETWEEN :start AND :end
            AND ce.billing_cycle IS NOT NULL
        GROUP BY ce.billing_cycle
        """, nativeQuery = true)
    List<Object[]> getBillingPreference(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT
            COALESCE(AVG(ce.time_to_convert_ms), 0) AS avg_time,
            COALESCE(AVG(ce.sessions_to_convert), 0) AS avg_sessions
        FROM site.tracking_conversion_events ce
        WHERE ce.event_type = 'cadastro_completed' AND ce.occurred_at BETWEEN :start AND :end
        """, nativeQuery = true)
    List<Object[]> getAverageTimeToConvert(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT ce.event_type AS type, ce.plan_id AS planId, ce.occurred_at AS timestamp
        FROM site.tracking_conversion_events ce
        WHERE ce.occurred_at >= :since
        ORDER BY ce.occurred_at DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> getRecentConversions(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM TrackingConversionEvent c WHERE c.occurredAt >= :since")
    long countConversionsSince(@Param("since") LocalDateTime since);
}
