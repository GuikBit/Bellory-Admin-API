package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.PaymentApiClient;
import org.exemplo.belloryadmin.client.payment.dto.*;
import org.exemplo.belloryadmin.exception.PaymentApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/assinaturas")
@Tag(name = "Admin - Assinaturas", description = "Gestao de assinaturas e customers via Payment API")
@Slf4j
public class AdminAssinaturaController {

    private final PaymentApiClient paymentApiClient;

    public AdminAssinaturaController(PaymentApiClient paymentApiClient) {
        this.paymentApiClient = paymentApiClient;
    }

    // ==================== ASSINATURAS ====================

    @Operation(summary = "Listar assinaturas (paginado, com filtros por status e customer)")
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(paymentApiClient.listSubscriptions(status, customerId, page, size));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Buscar assinatura por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getSubscription(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Atualizar assinatura (billing type, data vencimento, descricao)")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody UpdateSubscriptionRequest req) {
        try {
            return ResponseEntity.ok(paymentApiClient.updateSubscription(id, req));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Cancelar assinatura")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.cancelSubscription(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Pausar assinatura")
    @PostMapping("/{id}/pausar")
    public ResponseEntity<?> pausar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean confirmCouponRemoval) {
        try {
            return ResponseEntity.ok(paymentApiClient.pauseSubscription(id, confirmCouponRemoval));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Retomar assinatura pausada")
    @PostMapping("/{id}/retomar")
    public ResponseEntity<?> retomar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.resumeSubscription(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Atualizar forma de pagamento da assinatura")
    @PatchMapping("/{id}/forma-pagamento")
    public ResponseEntity<?> atualizarFormaPagamento(
            @PathVariable Long id,
            @RequestBody UpdatePaymentMethodRequest req) {
        try {
            return ResponseEntity.ok(paymentApiClient.updatePaymentMethod(id, req));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    // ==================== COBRANCAS DA ASSINATURA ====================

    @Operation(summary = "Listar cobrancas vinculadas a uma assinatura")
    @GetMapping("/{id}/cobrancas")
    public ResponseEntity<?> listarCobrancas(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.listChargesBySubscription(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    // ==================== TROCAS DE PLANO ====================

    @Operation(summary = "Historico de trocas de plano da assinatura")
    @GetMapping("/{id}/trocas-plano")
    public ResponseEntity<?> historicoTrocasPlano(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(paymentApiClient.getPlanChangeHistory(id, page, size));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Cancelar troca de plano pendente/agendada")
    @DeleteMapping("/{subscriptionId}/trocas-plano/{changeId}")
    public ResponseEntity<?> cancelarTrocaPlano(
            @PathVariable Long subscriptionId,
            @PathVariable Long changeId) {
        try {
            paymentApiClient.cancelPlanChange(subscriptionId, changeId);
            return ResponseEntity.noContent().build();
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    // ==================== CUSTOMERS ====================

    @Operation(summary = "Listar customers da Payment API (com busca por nome/email/documento)")
    @GetMapping("/customers")
    public ResponseEntity<?> listarCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(paymentApiClient.listCustomers(search, page, size));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Buscar customer por ID")
    @GetMapping("/customers/{id}")
    public ResponseEntity<?> buscarCustomer(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getCustomer(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Verificar status de acesso do customer")
    @GetMapping("/customers/{id}/access-status")
    public ResponseEntity<?> accessStatus(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getAccessStatus(id));
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
