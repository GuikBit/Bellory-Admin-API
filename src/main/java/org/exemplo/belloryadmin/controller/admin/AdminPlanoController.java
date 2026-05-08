package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.PaymentApiClient;
import org.exemplo.belloryadmin.client.payment.dto.*;
import org.exemplo.belloryadmin.exception.PaymentApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/planos")
@Tag(name = "Admin - Planos", description = "CRUD de planos via Payment API")
@Slf4j
public class AdminPlanoController {

    private final PaymentApiClient paymentApiClient;

    public AdminPlanoController(PaymentApiClient paymentApiClient) {
        this.paymentApiClient = paymentApiClient;
    }

    // ==================== LISTAGEM ====================

    @Operation(summary = "Listar todos os planos (paginado)")
    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            return ResponseEntity.ok(paymentApiClient.listPlans());
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Buscar plano por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getPlan(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Buscar plano ativo por codigo (case-insensitive)")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> buscarPorCodigo(@PathVariable String codigo) {
        try {
            return ResponseEntity.ok(paymentApiClient.getPlanByCodigo(codigo));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    // ==================== CRIACAO / EDICAO ====================

    @Operation(summary = "Criar novo plano")
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody CreatePlanRequest req) {
        try {
            PlanResponse response = paymentApiClient.createPlan(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Atualizar plano (campos imutaveis: codigo)")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody UpdatePlanRequest req) {
        try {
            return ResponseEntity.ok(paymentApiClient.updatePlan(id, req));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Criar nova versao do plano (mantém codigo, desativa versao anterior)")
    @PostMapping("/{id}/nova-versao")
    public ResponseEntity<?> novaVersao(@PathVariable Long id, @RequestBody UpdatePlanRequest req) {
        try {
            PlanResponse response = paymentApiClient.createNewPlanVersion(id, req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    // ==================== ATIVAR / DESATIVAR / DELETAR ====================

    @Operation(summary = "Ativar plano")
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<?> ativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.activatePlan(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Desativar plano (impede novas assinaturas, existentes continuam)")
    @PatchMapping("/{id}/desativar")
    public ResponseEntity<?> desativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.deactivatePlan(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Deletar plano (soft delete, bloqueado se tiver assinaturas ativas)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            paymentApiClient.deletePlan(id);
            return ResponseEntity.noContent().build();
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    // ==================== PRICING / LIMITES / FEATURES ====================

    @Operation(summary = "Retorna precos efetivos com promocoes ativas")
    @GetMapping("/{id}/pricing")
    public ResponseEntity<?> pricing(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getPlanPricing(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Retorna limites estruturados do plano")
    @GetMapping("/{id}/limites")
    public ResponseEntity<?> limites(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getPlanLimits(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Retorna features estruturadas do plano")
    @GetMapping("/{id}/features")
    public ResponseEntity<?> features(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getPlanFeatures(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Verificar limite especifico do plano com uso atual")
    @GetMapping("/{id}/limites/{key}")
    public ResponseEntity<?> verificarLimite(
            @PathVariable Long id,
            @PathVariable String key,
            @RequestParam(required = false) Integer usage) {
        try {
            return ResponseEntity.ok(paymentApiClient.checkPlanLimit(id, key, usage));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    // ==================== HELPER ====================

    private ResponseEntity<?> buildErrorResponse(PaymentApiException e) {
        int code = e.getStatusCode();
        if (code == 0) code = 502;
        return ResponseEntity.status(Math.min(code, 599))
                .body(Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "details", e.getResponseBody() != null ? e.getResponseBody() : ""
                ));
    }
}
