package org.exemplo.belloryadmin.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.exemplo.belloryadmin.model.dto.webhook.WebhookEnvelopeDTO;
import org.exemplo.belloryadmin.service.webhook.PaymentWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook")
@Tag(name = "Webhook", description = "Recebe eventos da Payment API")
@Slf4j
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(PaymentWebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Endpoint que recebe webhooks da Payment API")
    @PostMapping("/payment")
    public ResponseEntity<Void> receberWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Event-Id", required = false) String eventId,
            @RequestHeader(value = "X-Event-Type", required = false) String eventType,
            @RequestHeader(value = "X-Delivery-Id", required = false) String deliveryId,
            @RequestBody String rawPayload) {

        log.info("Webhook recebido: eventId={} type={} deliveryId={}", eventId, eventType, deliveryId);

        // 1. Validar token
        if (!webhookService.validarToken(authorization)) {
            log.warn("Webhook: token invalido para eventId={}", eventId);
            return ResponseEntity.status(401).build();
        }

        // 2. Deserializar envelope
        WebhookEnvelopeDTO envelope;
        try {
            envelope = objectMapper.readValue(rawPayload, WebhookEnvelopeDTO.class);
        } catch (Exception e) {
            log.error("Webhook: falha ao deserializar payload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        // 3. Salvar e processar async
        webhookService.receberEvento(envelope, deliveryId, rawPayload);

        // 4. Retornar 200 rapido
        return ResponseEntity.ok().build();
    }
}
