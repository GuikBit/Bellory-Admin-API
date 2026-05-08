package org.exemplo.belloryadmin.model.repository.tracking;

import org.exemplo.belloryadmin.model.entity.tracking.TrackingPageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingPageViewRepository extends JpaRepository<TrackingPageView, Long> {

    @Query(value = """
        SELECT pv.path, COUNT(*) AS views, COALESCE(AVG(pv.time_on_page_ms), 0) AS avgTimeOnPage
        FROM site.tracking_page_views pv
        WHERE pv.viewed_at BETWEEN :start AND :end
        GROUP BY pv.path
        ORDER BY views DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> getTopPages(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("limit") int limit);

    @Query("SELECT COUNT(pv) FROM TrackingPageView pv WHERE pv.viewedAt BETWEEN :start AND :end")
    long countPageViewsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT pv.path, COUNT(*) AS views
        FROM site.tracking_page_views pv
        WHERE pv.viewed_at >= :since
        GROUP BY pv.path
        ORDER BY views DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> getActivePages(@Param("since") LocalDateTime since);

    @Query(value = """
        SELECT pv.path, COUNT(*) AS exit_count,
               ROUND(COUNT(*)::NUMERIC / NULLIF(:totalSessions, 0) * 100, 1) AS exit_rate
        FROM site.tracking_page_views pv
        INNER JOIN (
            SELECT session_id, MAX(viewed_at) AS last_view
            FROM site.tracking_page_views
            GROUP BY session_id
        ) last_pv ON pv.session_id = last_pv.session_id AND pv.viewed_at = last_pv.last_view
        INNER JOIN site.tracking_sessions s ON s.id = pv.session_id
        WHERE s.started_at BETWEEN :start AND :end
        GROUP BY pv.path
        ORDER BY exit_count DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getExitPages(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("totalSessions") long totalSessions);
}
