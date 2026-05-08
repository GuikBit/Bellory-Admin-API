package org.exemplo.belloryadmin.service.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.tracking.*;
import org.exemplo.belloryadmin.model.entity.tracking.*;
import org.exemplo.belloryadmin.model.repository.tracking.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingProcessingService {

    private final TrackingVisitorRepository visitorRepository;
    private final TrackingSessionRepository sessionRepository;
    private final TrackingPageViewRepository pageViewRepository;
    private final TrackingInteractionEventRepository interactionEventRepository;
    private final TrackingScrollEventRepository scrollEventRepository;
    private final TrackingConversionEventRepository conversionEventRepository;
    private final TrackingErrorEventRepository errorEventRepository;
    private final TrackingPerformanceSnapshotRepository performanceSnapshotRepository;

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    @Async
    public void processPayloadAsync(TrackingPayloadDTO payload) {
        try {
            processPayload(payload);
        } catch (Exception e) {
            log.error("Erro ao processar payload de tracking: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processPayload(TrackingPayloadDTO payload) {
        UUID visitorId = UUID.fromString(payload.getVisitor().getVisitorId());
        UUID sessionId = UUID.fromString(payload.getVisitor().getSessionId());

        // 1. UPSERT Visitor
        TrackingVisitor visitor = upsertVisitor(visitorId, payload);

        // 2. UPSERT Session
        TrackingSession session = upsertSession(sessionId, visitor, payload);

        // 3. Processar eventos
        if (payload.getEvents() != null) {
            int pageViewCount = 0;
            for (TrackingEventDTO event : payload.getEvents()) {
                switch (event.getCategory()) {
                    case "navigation" -> {
                        processNavigationEvent(event, session, visitor);
                        pageViewCount++;
                    }
                    case "interaction" -> processInteractionEvent(event, session, visitor);
                    case "scroll" -> processScrollEvent(event, session, visitor);
                    case "conversion" -> processConversionEvent(event, session, visitor);
                    case "error" -> processErrorEvent(event, session, visitor);
                    default -> log.debug("Categoria de evento desconhecida: {}", event.getCategory());
                }
            }

            // Atualizar contagem de page views do visitor
            if (pageViewCount > 0) {
                visitorRepository.incrementPageViews(visitorId, pageViewCount);
            }
        }

        // 4. Processar performance
        if (payload.getPerformance() != null) {
            processPerformance(payload.getPerformance(), session, visitor, payload.getDevice());
        }

        // 5. Atualizar session com dados atualizados
        updateSessionFromPayload(session, payload);
    }

    private TrackingVisitor upsertVisitor(UUID visitorId, TrackingPayloadDTO payload) {
        return visitorRepository.findById(visitorId).map(existing -> {
            existing.setLastSeenAt(LocalDateTime.now());
            if (payload.getGeo() != null) {
                if (payload.getGeo().getCountry() != null) existing.setCountry(payload.getGeo().getCountry());
                if (payload.getGeo().getState() != null) existing.setState(payload.getGeo().getState());
                if (payload.getGeo().getCity() != null) existing.setCity(payload.getGeo().getCity());
                if (payload.getGeo().getLatitude() != null) existing.setLatitude(BigDecimal.valueOf(payload.getGeo().getLatitude()));
                if (payload.getGeo().getLongitude() != null) existing.setLongitude(BigDecimal.valueOf(payload.getGeo().getLongitude()));
                if (payload.getGeo().getSource() != null) existing.setGeoSource(payload.getGeo().getSource());
            }
            return visitorRepository.save(existing);
        }).orElseGet(() -> {
            TrackingVisitor.TrackingVisitorBuilder builder = TrackingVisitor.builder()
                    .id(visitorId)
                    .firstSeenAt(LocalDateTime.now())
                    .lastSeenAt(LocalDateTime.now())
                    .totalSessions(1)
                    .totalPageViews(0)
                    .isConverted(false);

            if (payload.getGeo() != null) {
                builder.country(payload.getGeo().getCountry());
                builder.state(payload.getGeo().getState());
                builder.city(payload.getGeo().getCity());
                builder.latitude(payload.getGeo().getLatitude() != null ? BigDecimal.valueOf(payload.getGeo().getLatitude()) : null);
                builder.longitude(payload.getGeo().getLongitude() != null ? BigDecimal.valueOf(payload.getGeo().getLongitude()) : null);
                builder.geoSource(payload.getGeo().getSource());
            }

            return visitorRepository.save(builder.build());
        });
    }

    private TrackingSession upsertSession(UUID sessionId, TrackingVisitor visitor, TrackingPayloadDTO payload) {
        return sessionRepository.findById(sessionId).map(existing -> {
            // Atualizar dados da sessao existente
            if (payload.getSession() != null) {
                SessionDTO s = payload.getSession();
                if (s.getLastActiveAt() != null) {
                    existing.setEndedAt(toLocalDateTime(s.getLastActiveAt()));
                }
                if (s.getDuration() != null) {
                    existing.setDurationMs(s.getDuration().intValue());
                }
                if (s.getPageCount() != null) {
                    existing.setPageCount(s.getPageCount());
                    existing.setIsBounce(s.getPageCount() <= 1);
                }
                if (s.getCurrentPage() != null) {
                    existing.setExitPage(s.getCurrentPage());
                }
            }
            return sessionRepository.save(existing);
        }).orElseGet(() -> {
            TrackingSession.TrackingSessionBuilder builder = TrackingSession.builder()
                    .id(sessionId)
                    .visitor(visitor)
                    .startedAt(LocalDateTime.now())
                    .entryPage("/")
                    .trafficSource("direct")
                    .deviceType("desktop");

            if (payload.getSession() != null) {
                SessionDTO s = payload.getSession();
                if (s.getStartedAt() != null) builder.startedAt(toLocalDateTime(s.getStartedAt()));
                if (s.getLastActiveAt() != null) builder.endedAt(toLocalDateTime(s.getLastActiveAt()));
                if (s.getDuration() != null) builder.durationMs(s.getDuration().intValue());
                if (s.getPageCount() != null) {
                    builder.pageCount(s.getPageCount());
                    builder.isBounce(s.getPageCount() <= 1);
                }
                if (s.getEntryPage() != null) builder.entryPage(s.getEntryPage());
                if (s.getCurrentPage() != null) builder.exitPage(s.getCurrentPage());
                if (s.getTrafficSource() != null) builder.trafficSource(s.getTrafficSource());
                if (s.getReferrer() != null) builder.referrer(s.getReferrer());

                if (s.getUtmParams() != null) {
                    Map<String, String> utm = s.getUtmParams();
                    builder.utmSource(utm.get("utm_source"));
                    builder.utmMedium(utm.get("utm_medium"));
                    builder.utmCampaign(utm.get("utm_campaign"));
                    builder.utmTerm(utm.get("utm_term"));
                    builder.utmContent(utm.get("utm_content"));
                }
            }

            if (payload.getDevice() != null) {
                DeviceDTO d = payload.getDevice();
                if (d.getDeviceType() != null) builder.deviceType(d.getDeviceType());
                if (d.getOs() != null) builder.os(d.getOs());
                if (d.getBrowser() != null) builder.browser(d.getBrowser());
                if (d.getScreenSize() != null) builder.screenSize(d.getScreenSize());
                if (d.getViewportSize() != null) builder.viewportSize(d.getViewportSize());
                builder.touchEnabled(d.isTouchEnabled());
                if (d.getLanguage() != null) builder.language(d.getLanguage());
            }

            if (payload.getGeo() != null) {
                builder.country(payload.getGeo().getCountry());
                builder.state(payload.getGeo().getState());
                builder.city(payload.getGeo().getCity());
                builder.latitude(payload.getGeo().getLatitude() != null ? BigDecimal.valueOf(payload.getGeo().getLatitude()) : null);
                builder.longitude(payload.getGeo().getLongitude() != null ? BigDecimal.valueOf(payload.getGeo().getLongitude()) : null);
                builder.geoSource(payload.getGeo().getSource());
            }

            // Incrementar sessoes do visitor (se nao for a primeira)
            if (visitor.getTotalSessions() > 0) {
                visitorRepository.incrementSession(visitor.getId(), LocalDateTime.now());
            }

            return sessionRepository.save(builder.build());
        });
    }

    private void processNavigationEvent(TrackingEventDTO event, TrackingSession session, TrackingVisitor visitor) {
        TrackingPageView pageView = TrackingPageView.builder()
                .session(session)
                .visitor(visitor)
                .path(event.getPath() != null ? event.getPath() : "/")
                .title(event.getTitle())
                .referrer(event.getReferrer())
                .timeOnPageMs(event.getTimeOnPreviousPage() != null ? event.getTimeOnPreviousPage().intValue() : null)
                .viewedAt(event.getTimestamp() != null ? toLocalDateTime(event.getTimestamp()) : LocalDateTime.now())
                .build();
        pageViewRepository.save(pageView);
    }

    private void processInteractionEvent(TrackingEventDTO event, TrackingSession session, TrackingVisitor visitor) {
        TrackingInteractionEvent interaction = TrackingInteractionEvent.builder()
                .session(session)
                .visitor(visitor)
                .eventType(event.getType())
                .elementId(event.getElementId() != null ? event.getElementId() : "unknown")
                .elementLabel(event.getElementLabel())
                .section(event.getSection())
                .metadata(event.getMetadata())
                .occurredAt(event.getTimestamp() != null ? toLocalDateTime(event.getTimestamp()) : LocalDateTime.now())
                .build();
        interactionEventRepository.save(interaction);
    }

    private void processScrollEvent(TrackingEventDTO event, TrackingSession session, TrackingVisitor visitor) {
        TrackingScrollEvent scrollEvent = TrackingScrollEvent.builder()
                .session(session)
                .visitor(visitor)
                .pagePath(event.getPath() != null ? event.getPath() : session.getExitPage() != null ? session.getExitPage() : "/")
                .maxDepth(event.getMaxDepth() != null ? event.getMaxDepth() : 0)
                .visibleSection(event.getVisibleSection())
                .occurredAt(event.getTimestamp() != null ? toLocalDateTime(event.getTimestamp()) : LocalDateTime.now())
                .build();
        scrollEventRepository.save(scrollEvent);
    }

    private void processConversionEvent(TrackingEventDTO event, TrackingSession session, TrackingVisitor visitor) {
        TrackingConversionEvent.TrackingConversionEventBuilder builder = TrackingConversionEvent.builder()
                .session(session)
                .visitor(visitor)
                .eventType(event.getType())
                .occurredAt(event.getTimestamp() != null ? toLocalDateTime(event.getTimestamp()) : LocalDateTime.now());

        Map<String, Object> meta = event.getMetadata();
        if (meta != null) {
            builder.planId(getStringFromMeta(meta, "planId"));
            builder.planName(getStringFromMeta(meta, "planName"));
            builder.billingCycle(getStringFromMeta(meta, "billingCycle"));
            builder.registrationStep(getIntFromMeta(meta, "registrationStep"));
            builder.registrationStepName(getStringFromMeta(meta, "registrationStepName"));
            builder.lastFeatureViewed(getStringFromMeta(meta, "lastFeatureViewed"));
            builder.timeToConvertMs(getLongFromMeta(meta, "timeToConvert"));
            builder.sessionsToConvert(getIntFromMeta(meta, "sessionsToConvert"));
        }

        conversionEventRepository.save(builder.build());

        // Marcar visitor como convertido se cadastro_completed
        if ("cadastro_completed".equals(event.getType())) {
            String planId = meta != null ? getStringFromMeta(meta, "planId") : null;
            visitorRepository.markConverted(visitor.getId(), LocalDateTime.now(), planId);
        }
    }

    private void processErrorEvent(TrackingEventDTO event, TrackingSession session, TrackingVisitor visitor) {
        TrackingErrorEvent errorEvent = TrackingErrorEvent.builder()
                .session(session)
                .visitor(visitor)
                .errorType(event.getType())
                .message(event.getMessage() != null ? event.getMessage() : "Unknown error")
                .stack(event.getStack())
                .url(event.getUrl())
                .statusCode(event.getStatusCode())
                .occurredAt(event.getTimestamp() != null ? toLocalDateTime(event.getTimestamp()) : LocalDateTime.now())
                .build();
        errorEventRepository.save(errorEvent);
    }

    private void processPerformance(PerformanceDTO perf, TrackingSession session, TrackingVisitor visitor, DeviceDTO device) {
        TrackingPerformanceSnapshot snapshot = TrackingPerformanceSnapshot.builder()
                .session(session)
                .visitor(visitor)
                .pageLoadTimeMs(perf.getPageLoadTime())
                .fcpMs(perf.getFcp())
                .lcpMs(perf.getLcp())
                .fidMs(perf.getFid())
                .cls(perf.getCls() != null ? perf.getCls() : null)
                .ttfbMs(perf.getTtfb())
                .deviceType(device != null ? device.getDeviceType() : null)
                .browser(device != null ? device.getBrowser() : null)
                .recordedAt(LocalDateTime.now())
                .build();
        performanceSnapshotRepository.save(snapshot);
    }

    private void updateSessionFromPayload(TrackingSession session, TrackingPayloadDTO payload) {
        if (payload.getSession() != null) {
            SessionDTO s = payload.getSession();
            if (s.getCurrentPage() != null) {
                session.setExitPage(s.getCurrentPage());
            }
            if (s.getLastActiveAt() != null) {
                session.setEndedAt(toLocalDateTime(s.getLastActiveAt()));
            }
            if (s.getDuration() != null) {
                session.setDurationMs(s.getDuration().intValue());
            }
            if (s.getPageCount() != null) {
                session.setPageCount(s.getPageCount());
                session.setIsBounce(s.getPageCount() <= 1);
            }
            sessionRepository.save(session);
        }
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZONE_SP);
    }

    private String getStringFromMeta(Map<String, Object> meta, String key) {
        Object val = meta.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getIntFromMeta(Map<String, Object> meta, String key) {
        Object val = meta.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Long getLongFromMeta(Map<String, Object> meta, String key) {
        Object val = meta.get(key);
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
