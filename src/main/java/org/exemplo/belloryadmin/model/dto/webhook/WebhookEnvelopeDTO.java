package org.exemplo.belloryadmin.model.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Envelope do evento recebido da Payment API.
 * Formato: { id, type, occurredAt, companyId, resource: { type, id }, data: { ... } }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEnvelopeDTO {
    private String id;
    private String type;
    private LocalDateTime occurredAt;
    private Long companyId;
    private ResourceDTO resource;
    private Map<String, Object> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceDTO {
        private String type;
        private String id;
    }
}
