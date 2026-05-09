package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.context.TenantContext;
import org.exemplo.belloryadmin.model.entity.config.ApiKeyAdmin;
import org.exemplo.belloryadmin.service.ApiKeyAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys (Admin)", description = "Gerenciamento de chaves de API da plataforma")
public class ApiKeyAdminController {

    private final ApiKeyAdminService apiKeyAdminService;

    @Operation(summary = "Criar nova API Key")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> request) {
        Long usuarioAdminId = TenantContext.getCurrentUserId();
        if (usuarioAdminId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Nao autenticado"));
        }

        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Integer expiresInDays = (Integer) request.get("expiresInDays");
        LocalDateTime expiresAt = expiresInDays != null ? LocalDateTime.now().plusDays(expiresInDays) : null;

        Map<String, Object> result = apiKeyAdminService.generateApiKey(usuarioAdminId, name, description, expiresAt);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API Key criada com sucesso. Copie agora, nao sera exibida novamente!",
                "apiKey", result.get("apiKey"),
                "id", ((ApiKeyAdmin) result.get("entity")).getId(),
                "name", name,
                "expiresAt", expiresAt != null ? expiresAt : "Sem expiracao"
        ));
    }

    @Operation(summary = "Listar API Keys do usuario admin atual")
    @GetMapping
    public ResponseEntity<?> listMine() {
        Long usuarioAdminId = TenantContext.getCurrentUserId();
        List<ApiKeyAdmin> apiKeys = apiKeyAdminService.listByUsuarioAdmin(usuarioAdminId);
        return ResponseEntity.ok(Map.of("success", true, "apiKeys", toResponseList(apiKeys)));
    }

    @Operation(summary = "Listar todas as API Keys da plataforma (auditoria)")
    @GetMapping("/all")
    public ResponseEntity<?> listAll() {
        List<ApiKeyAdmin> apiKeys = apiKeyAdminService.listAll();
        return ResponseEntity.ok(Map.of("success", true, "apiKeys", toResponseList(apiKeys)));
    }

    @Operation(summary = "Revogar uma API Key")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> revoke(@PathVariable Long id) {
        Long usuarioAdminId = TenantContext.getCurrentUserId();
        apiKeyAdminService.revokeApiKey(id, usuarioAdminId);
        return ResponseEntity.ok(Map.of("success", true, "message", "API Key revogada"));
    }

    private List<Map<String, Object>> toResponseList(List<ApiKeyAdmin> apiKeys) {
        return apiKeys.stream().map(key -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", key.getId());
            m.put("name", key.getName());
            m.put("description", key.getDescription() != null ? key.getDescription() : "");
            m.put("username", key.getUsername());
            m.put("lastUsedAt", key.getLastUsedAt());
            m.put("expiresAt", key.getExpiresAt());
            m.put("createdAt", key.getCreatedAt());
            return m;
        }).toList();
    }
}
