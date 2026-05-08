package org.exemplo.belloryadmin.controller.site;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.tracking.TrackingPayloadDTO;
import org.exemplo.belloryadmin.service.tracking.TrackingProcessingService;
import org.exemplo.belloryadmin.service.tracking.TrackingRateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tracking", description = "Endpoint de ingestão de eventos de tracking do site bellory.com.br")
public class TrackingController {

    private final TrackingProcessingService trackingProcessingService;
    private final TrackingRateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    private static final int MAX_PAYLOAD_SIZE = 100 * 1024; // 100KB

    @Operation(summary = "Receber eventos de tracking",
            description = "Recebe o payload de tracking do frontend. Suporta application/json e text/plain (sendBeacon).")
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<Map<String, String>> receiveTracking(
            @RequestBody String rawBody,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Rate limiting: 60 req/min por IP
        if (!rateLimiterService.allowRequest(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", "error", "message", "Rate limit exceeded"));
        }

        // Validar tamanho maximo do payload
        if (rawBody.length() > MAX_PAYLOAD_SIZE) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("status", "error", "message", "Payload too large"));
        }

        try {
            TrackingPayloadDTO payload = objectMapper.readValue(rawBody, TrackingPayloadDTO.class);

            if (payload.getVisitor() == null || payload.getVisitor().getVisitorId() == null
                    || payload.getVisitor().getSessionId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "Missing visitor or session data"));
            }

            // Processar assincronamente
            trackingProcessingService.processPayloadAsync(payload);

            return ResponseEntity.ok(Map.of("status", "ok"));

        } catch (JsonProcessingException e) {
            log.warn("Invalid tracking payload from IP {}: {}", clientIp, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Invalid JSON payload"));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
