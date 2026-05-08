package org.exemplo.belloryadmin.model.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfigDTO {
    private Long id;
    private String token;
    private boolean ativo;
    private String descricao;
}
