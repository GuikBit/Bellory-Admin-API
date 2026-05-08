package org.exemplo.belloryadmin.model.repository.webhook;

import org.exemplo.belloryadmin.model.entity.webhook.WebhookEventConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventConfigRepository extends JpaRepository<WebhookEventConfig, Long> {
    Optional<WebhookEventConfig> findByEventType(String eventType);
    List<WebhookEventConfig> findAllByOrderByEventTypeAsc();
}
