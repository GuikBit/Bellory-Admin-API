package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.webhook.WebhookConfigDTO;
import org.exemplo.belloryadmin.model.dto.webhook.WebhookEventConfigDTO;
import org.exemplo.belloryadmin.model.entity.webhook.WebhookConfig;
import org.exemplo.belloryadmin.model.entity.webhook.WebhookEventConfig;
import org.exemplo.belloryadmin.model.entity.webhook.WebhookEventLog;
import org.exemplo.belloryadmin.model.repository.webhook.WebhookConfigRepository;
import org.exemplo.belloryadmin.model.repository.webhook.WebhookEventConfigRepository;
import org.exemplo.belloryadmin.model.repository.webhook.WebhookEventLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/webhook")
@Tag(name = "Admin Webhook", description = "Configuracao e historico de webhooks da Payment API")
@Slf4j
public class AdminWebhookController {

    private final WebhookConfigRepository configRepository;
    private final WebhookEventConfigRepository eventConfigRepository;
    private final WebhookEventLogRepository eventLogRepository;

    public AdminWebhookController(WebhookConfigRepository configRepository,
                                  WebhookEventConfigRepository eventConfigRepository,
                                  WebhookEventLogRepository eventLogRepository) {
        this.configRepository = configRepository;
        this.eventConfigRepository = eventConfigRepository;
        this.eventLogRepository = eventLogRepository;
    }

    // ==================== TOKEN CONFIG ====================

    @Operation(summary = "Retorna a configuracao do webhook (token)")
    @GetMapping("/config")
    public ResponseEntity<WebhookConfigDTO> getConfig() {
        return configRepository.findFirstByAtivoTrue()
                .map(c -> ResponseEntity.ok(toDTO(c)))
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Cria ou atualiza o token do webhook")
    @PostMapping("/config")
    public ResponseEntity<WebhookConfigDTO> salvarConfig(@RequestBody WebhookConfigDTO dto) {
        WebhookConfig config = configRepository.findFirstByAtivoTrue()
                .orElse(new WebhookConfig());

        config.setToken(dto.getToken());
        config.setAtivo(true);
        config.setDescricao(dto.getDescricao());
        configRepository.save(config);

        return ResponseEntity.ok(toDTO(config));
    }

    // ==================== EVENT CONFIG ====================

    @Operation(summary = "Lista todas as configuracoes de eventos (toggle push/email por evento)")
    @GetMapping("/eventos/config")
    public ResponseEntity<List<WebhookEventConfigDTO>> listarEventConfigs() {
        List<WebhookEventConfigDTO> configs = eventConfigRepository.findAllByOrderByEventTypeAsc()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "Atualiza a configuracao de um evento especifico")
    @PutMapping("/eventos/config/{id}")
    public ResponseEntity<WebhookEventConfigDTO> atualizarEventConfig(
            @PathVariable Long id,
            @RequestBody WebhookEventConfigDTO dto) {

        return eventConfigRepository.findById(id)
                .map(config -> {
                    config.setPushEnabled(dto.isPushEnabled());
                    config.setEmailEnabled(dto.isEmailEnabled());
                    config.setInvalidarCache(dto.isInvalidarCache());
                    config.setAtivo(dto.isAtivo());
                    if (dto.getDescricao() != null) config.setDescricao(dto.getDescricao());
                    eventConfigRepository.save(config);
                    return ResponseEntity.ok(toDTO(config));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== EVENT LOG ====================

    @Operation(summary = "Lista o historico de eventos recebidos (paginado)")
    @GetMapping("/eventos/log")
    public ResponseEntity<Page<WebhookEventLog>> listarEventLog(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long organizacaoId,
            @PageableDefault(size = 20, sort = "dtRecebido") Pageable pageable) {

        Page<WebhookEventLog> page;
        if (eventType != null) {
            page = eventLogRepository.findByEventTypeOrderByDtRecebidoDesc(eventType, pageable);
        } else if (organizacaoId != null) {
            page = eventLogRepository.findByOrganizacaoIdOrderByDtRecebidoDesc(organizacaoId, pageable);
        } else {
            page = eventLogRepository.findAllByOrderByDtRecebidoDesc(pageable);
        }

        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Busca um evento especifico por ID")
    @GetMapping("/eventos/log/{id}")
    public ResponseEntity<WebhookEventLog> getEventLog(@PathVariable Long id) {
        return eventLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== MAPPERS ====================

    private WebhookConfigDTO toDTO(WebhookConfig config) {
        return WebhookConfigDTO.builder()
                .id(config.getId())
                .token(config.getToken())
                .ativo(config.isAtivo())
                .descricao(config.getDescricao())
                .build();
    }

    private WebhookEventConfigDTO toDTO(WebhookEventConfig config) {
        return WebhookEventConfigDTO.builder()
                .id(config.getId())
                .eventType(config.getEventType())
                .descricao(config.getDescricao())
                .pushEnabled(config.isPushEnabled())
                .emailEnabled(config.isEmailEnabled())
                .invalidarCache(config.isInvalidarCache())
                .ativo(config.isAtivo())
                .build();
    }
}
