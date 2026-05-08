package org.exemplo.belloryadmin.model.repository.tracking;

import org.exemplo.belloryadmin.model.entity.tracking.TrackingInteractionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingInteractionEventRepository extends JpaRepository<TrackingInteractionEvent, Long> {

    @Query(value = """
        SELECT ie.element_id, ie.element_label, ie.section,
               COUNT(*) AS total_clicks,
               COUNT(DISTINCT ie.visitor_id) AS unique_visitors
        FROM site.tracking_interaction_events ie
        WHERE ie.event_type IN ('click_cta', 'click_button', 'click_plan')
            AND ie.occurred_at BETWEEN :start AND :end
        GROUP BY ie.element_id, ie.element_label, ie.section
        ORDER BY total_clicks DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getTopCTAs(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT ie.event_type AS type, ie.element_id AS elementId, ie.occurred_at AS timestamp
        FROM site.tracking_interaction_events ie
        WHERE ie.occurred_at >= :since
        ORDER BY ie.occurred_at DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getRecentInteractions(@Param("since") LocalDateTime since);
}
