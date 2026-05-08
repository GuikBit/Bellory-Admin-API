package org.exemplo.belloryadmin.model.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventConfigDTO {
    private Long id;
    private String eventType;
    private String descricao;
    private boolean pushEnabled;
    private boolean emailEnabled;
    private boolean invalidarCache;
    private boolean ativo;
}
