package org.exemplo.belloryadmin.model.repository.tracking;

import org.exemplo.belloryadmin.model.entity.tracking.TrackingScrollEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingScrollEventRepository extends JpaRepository<TrackingScrollEvent, Long> {

    @Query(value = """
        SELECT max_depth,
               ROUND(COUNT(*)::NUMERIC / NULLIF(:totalSessions, 0) * 100, 1) AS percentage
        FROM site.tracking_scroll_events
        WHERE occurred_at BETWEEN :start AND :end
        GROUP BY max_depth
        ORDER BY max_depth
        """, nativeQuery = true)
    List<Object[]> getScrollDepthDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("totalSessions") long totalSessions);

    @Query(value = """
        SELECT visible_section, COUNT(DISTINCT session_id) AS sessions,
               ROUND(COUNT(DISTINCT session_id)::NUMERIC / NULLIF(:totalSessions, 0) * 100, 1) AS view_rate
        FROM site.tracking_scroll_events
        WHERE occurred_at BETWEEN :start AND :end AND visible_section IS NOT NULL
        GROUP BY visible_section
        ORDER BY view_rate DESC
        """, nativeQuery = true)
    List<Object[]> getSectionVisibility(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("totalSessions") long totalSessions);
}
