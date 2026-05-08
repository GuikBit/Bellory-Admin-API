package org.exemplo.belloryadmin.model.repository.tracking;

import org.exemplo.belloryadmin.model.entity.tracking.TrackingErrorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingErrorEventRepository extends JpaRepository<TrackingErrorEvent, Long> {

    @Query("SELECT COUNT(e) FROM TrackingErrorEvent e WHERE e.occurredAt BETWEEN :start AND :end")
    long countErrorsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT ee.error_type, COUNT(*) AS count
        FROM site.tracking_error_events ee
        WHERE ee.occurred_at BETWEEN :start AND :end
        GROUP BY ee.error_type
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Object[]> getErrorsByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT ee.message, COUNT(*) AS count, MAX(ee.occurred_at) AS last_seen
        FROM site.tracking_error_events ee
        WHERE ee.occurred_at BETWEEN :start AND :end
        GROUP BY ee.message
        ORDER BY count DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> getTopErrors(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
