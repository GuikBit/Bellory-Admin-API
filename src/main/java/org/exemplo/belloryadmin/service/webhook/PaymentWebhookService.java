package org.exemplo.belloryadmin.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.webhook.WebhookEnvelopeDTO;
import org.exemplo.belloryadmin.model.entity.assinatura.Assinatura;
import org.exemplo.belloryadmin.model.entity.webhook.WebhookConfig;
import org.exemplo.belloryadmin.model.entity.webhook.WebhookEventLog;
import org.exemplo.belloryadmin.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.belloryadmin.model.repository.webhook.WebhookConfigRepository;
import org.exemplo.belloryadmin.model.repository.webhook.WebhookEventLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PaymentWebhookService {

    private final WebhookConfigRepository configRepository;
    private final WebhookEventLogRepository eventLogRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final WebhookEventProcessor eventProcessor;
    private final ObjectMapper objectMapper;

    public PaymentWebhookService(WebhookConfigRepository configRepository,
                                 WebhookEventLogRepository eventLogRepository,
                                 AssinaturaRepository assinaturaRepository,
                                 WebhookEventProcessor eventProcessor,
                                 ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.eventLogRepository = eventLogRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.eventProcessor = eventProcessor;
        this.objectMapper = objectMapper;
    }

    /**
     * Valida o token do webhook usando comparacao tempo-constante.
     */
    public boolean validarToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return false;
        }
        String tokenRecebido = bearerToken.substring(7);

        Optional<WebhookConfig> config = configRepository.findFirstByAtivoTrue();
        if (config.isEmpty()) {
            log.warn("Webhook: nenhuma config ativa encontrada no banco");
            return false;
        }

        return MessageDigest.isEqual(
                tokenRecebido.getBytes(),
                config.get().getToken().getBytes()
        );
    }

    /**
     * Recebe o evento, salva no log e dispara processamento async.
     * Retorna true se aceito (novo), false se duplicata (idempotencia).
     */
    @Transactional
    public boolean receberEvento(WebhookEnvelopeDTO envelope, String deliveryId, String rawPayload) {
        // Idempotencia: evento ja recebido?
        if (eventLogRepository.existsByEventId(envelope.getId())) {
            log.info("Webhook: evento duplicado ignorado eventId={}", envelope.getId());
            return false;
        }

        // Resolver organizacaoId a partir dos dados do evento
        Long organizacaoId = resolverOrganizacaoId(envelope);

        // Salvar no log
        WebhookEventLog eventLog = WebhookEventLog.builder()
                .eventId(envelope.getId())
                .deliveryId(deliveryId)
                .eventType(envelope.getType())
                .companyId(envelope.getCompanyId())
                .organizacaoId(organizacaoId)
                .resourceType(envelope.getResource() != null ? envelope.getResource().getType() : null)
                .resourceId(envelope.getResource() != null ? envelope.getResource().getId() : null)
                .payload(rawPayload)
                .status("RECEIVED")
                .dtRecebido(LocalDateTime.now())
                .build();

        eventLogRepository.save(eventLog);

        // Processar async
        eventProcessor.processarAsync(eventLog.getId(), envelope, organizacaoId);

        return true;
    }

    /**
     * Resolve o organizacaoId a partir do customerId ou subscriptionId no payload do evento.
     */
    private Long resolverOrganizacaoId(WebhookEnvelopeDTO envelope) {
        Map<String, Object> data = envelope.getData();
        if (data == null) return null;

        // Tentar via customerId
        Object customerIdObj = data.get("customerId");
        if (customerIdObj != null) {
            try {
                Long customerId = Long.valueOf(customerIdObj.toString());
                Optional<Assinatura> assinatura = assinaturaRepository.findByPaymentApiCustomerId(customerId);
                if (assinatura.isPresent()) {
                    return assinatura.get().getOrganizacao().getId();
                }
            } catch (NumberFormatException e) {
                log.debug("Webhook: customerId nao numerico: {}", customerIdObj);
            }
        }

        // Tentar via subscriptionId
        Object subscriptionIdObj = data.get("subscriptionId");
        if (subscriptionIdObj != null) {
            try {
                Long subscriptionId = Long.valueOf(subscriptionIdObj.toString());
                Optional<Assinatura> assinatura = assinaturaRepository.findByPaymentApiSubscriptionId(subscriptionId);
                if (assinatura.isPresent()) {
                    return assinatura.get().getOrganizacao().getId();
                }
            } catch (NumberFormatException e) {
                log.debug("Webhook: subscriptionId nao numerico: {}", subscriptionIdObj);
            }
        }

        log.warn("Webhook: nao foi possivel resolver organizacaoId para evento {} (type={})",
                envelope.getId(), envelope.getType());
        return null;
    }
}
