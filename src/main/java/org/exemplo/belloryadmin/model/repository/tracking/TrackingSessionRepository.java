package org.exemplo.belloryadmin.model.repository.tracking;

import org.exemplo.belloryadmin.model.entity.tracking.TrackingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrackingSessionRepository extends JpaRepository<TrackingSession, UUID> {

    @Query("SELECT COUNT(s) FROM TrackingSession s WHERE s.startedAt BETWEEN :start AND :end")
    long countSessionsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(AVG(s.durationMs), 0) FROM TrackingSession s WHERE s.startedAt BETWEEN :start AND :end")
    double avgDurationInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(AVG(s.pageCount), 0) FROM TrackingSession s WHERE s.startedAt BETWEEN :start AND :end")
    double avgPagesInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(s) FROM TrackingSession s WHERE s.isBounce = true AND s.startedAt BETWEEN :start AND :end")
    long countBouncesInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.traffic_source AS source,
               COUNT(DISTINCT s.visitor_id) AS visitors,
               COUNT(s.id) AS sessions,
               COALESCE(
                   ROUND(COUNT(DISTINCT ce.visitor_id)::NUMERIC / NULLIF(COUNT(DISTINCT s.visitor_id), 0) * 100, 2),
                   0
               ) AS conversionRate
        FROM site.tracking_sessions s
        LEFT JOIN site.tracking_conversion_events ce ON ce.visitor_id = s.visitor_id
            AND ce.event_type = 'cadastro_completed'
            AND ce.occurred_at BETWEEN :start AND :end
        WHERE s.started_at BETWEEN :start AND :end
        GROUP BY s.traffic_source
        ORDER BY visitors DESC
        """, nativeQuery = true)
    List<Object[]> getTrafficSourceStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.utm_campaign AS campaign, s.utm_source AS source, s.utm_medium AS medium,
               COUNT(DISTINCT s.visitor_id) AS visitors,
               COUNT(DISTINCT ce.visitor_id) AS conversions
        FROM site.tracking_sessions s
        LEFT JOIN site.tracking_conversion_events ce ON ce.visitor_id = s.visitor_id
            AND ce.event_type = 'cadastro_completed'
        WHERE s.started_at BETWEEN :start AND :end
            AND s.utm_campaign IS NOT NULL
        GROUP BY s.utm_campaign, s.utm_source, s.utm_medium
        ORDER BY visitors DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getCampaignStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.referrer AS referrer, COUNT(DISTINCT s.visitor_id) AS visitors
        FROM site.tracking_sessions s
        WHERE s.started_at BETWEEN :start AND :end
            AND s.referrer IS NOT NULL AND s.referrer != ''
        GROUP BY s.referrer
        ORDER BY visitors DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getTopReferrers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT s.visitor.id) FROM TrackingSession s WHERE s.startedAt >= :since")
    long countActiveVisitorsSince(@Param("since") LocalDateTime since);

    @Query(value = """
        SELECT s.device_type, COUNT(DISTINCT s.visitor_id) AS visitors,
               COALESCE(
                   ROUND(COUNT(DISTINCT ce.visitor_id)::NUMERIC / NULLIF(COUNT(DISTINCT s.visitor_id), 0) * 100, 2),
                   0
               ) AS conversionRate
        FROM site.tracking_sessions s
        LEFT JOIN site.tracking_conversion_events ce ON ce.visitor_id = s.visitor_id
            AND ce.event_type = 'cadastro_completed'
        WHERE s.started_at BETWEEN :start AND :end
        GROUP BY s.device_type
        """, nativeQuery = true)
    List<Object[]> getDeviceStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.browser, COUNT(DISTINCT s.visitor_id) AS visitors
        FROM site.tracking_sessions s
        WHERE s.started_at BETWEEN :start AND :end
        GROUP BY s.browser
        ORDER BY visitors DESC
        """, nativeQuery = true)
    List<Object[]> getBrowserStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.os, COUNT(DISTINCT s.visitor_id) AS visitors
        FROM site.tracking_sessions s
        WHERE s.started_at BETWEEN :start AND :end
        GROUP BY s.os
        ORDER BY visitors DESC
        """, nativeQuery = true)
    List<Object[]> getOsStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.country, COUNT(DISTINCT s.visitor_id) AS visitors
        FROM site.tracking_sessions s
        WHERE s.started_at BETWEEN :start AND :end AND s.country IS NOT NULL
        GROUP BY s.country
        ORDER BY visitors DESC
        """, nativeQuery = true)
    List<Object[]> getCountryStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.state, COUNT(DISTINCT s.visitor_id) AS visitors
        FROM site.tracking_sessions s
        WHERE s.started_at BETWEEN :start AND :end AND s.state IS NOT NULL
        GROUP BY s.state
        ORDER BY visitors DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getStateStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT s.city, COUNT(DISTINCT s.visitor_id) AS visitors
        FROM site.tracking_sessions s
        WHERE s.started_at BETWEEN :start AND :end AND s.city IS NOT NULL
        GROUP BY s.city
        ORDER BY visitors DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getCityStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
