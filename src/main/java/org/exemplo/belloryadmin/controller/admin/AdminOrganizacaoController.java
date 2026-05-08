package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.PaymentApiClient;
import org.exemplo.belloryadmin.client.payment.dto.ChargeResponse;
import org.exemplo.belloryadmin.client.payment.dto.PlanChangeResponse;
import org.exemplo.belloryadmin.exception.PaymentApiException;
import org.exemplo.belloryadmin.model.dto.admin.AdminOrganizacaoDetalheDTO;
import org.exemplo.belloryadmin.model.dto.admin.AdminOrganizacaoListDTO;
import org.exemplo.belloryadmin.model.dto.assinatura.PlanoUsoDTO;
import org.exemplo.belloryadmin.model.entity.assinatura.Assinatura;
import org.exemplo.belloryadmin.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.belloryadmin.service.admin.AdminOrganizacaoService;
import org.exemplo.belloryadmin.service.plano.PlanoUsoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/organizacoes")
@Tag(name = "Admin - Organizacoes", description = "Gestao e visualizacao de todas as organizacoes/clientes da plataforma")
@Slf4j
public class AdminOrganizacaoController {

    private final AdminOrganizacaoService adminOrganizacaoService;
    private final AssinaturaRepository assinaturaRepository;
    private final PaymentApiClient paymentApiClient;
    private final PlanoUsoService planoUsoService;

    public AdminOrganizacaoController(AdminOrganizacaoService adminOrganizacaoService,
                                      AssinaturaRepository assinaturaRepository,
                                      PaymentApiClient paymentApiClient,
                                      PlanoUsoService planoUsoService) {
        this.adminOrganizacaoService = adminOrganizacaoService;
        this.assinaturaRepository = assinaturaRepository;
        this.paymentApiClient = paymentApiClient;
        this.planoUsoService = planoUsoService;
    }

    @Operation(summary = "Listar todas as organizacoes com contadores e plano")
    @GetMapping
    public ResponseEntity<List<AdminOrganizacaoListDTO>> listarOrganizacoes() {
        return ResponseEntity.ok(adminOrganizacaoService.listarOrganizacoes());
    }

    @Operation(summary = "Detalhar organizacao com dados completos + Payment API")
    @GetMapping("/{id}")
    public ResponseEntity<AdminOrganizacaoDetalheDTO> detalharOrganizacao(
            @Parameter(description = "ID da organizacao") @PathVariable Long id) {
        return ResponseEntity.ok(adminOrganizacaoService.detalharOrganizacao(id));
    }

    @Operation(summary = "Historico de cobrancas da organizacao (via Payment API)")
    @GetMapping("/{id}/cobrancas")
    public ResponseEntity<?> listarCobrancas(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long customerId = resolverCustomerId(id);
        if (customerId == null) {
            return ResponseEntity.ok(Map.of("content", Collections.emptyList(), "totalElements", 0));
        }

        try {
            // Cobranças por customer (inclui recorrentes + avulsas + plan_change)
            PaymentApiClient.PageResponse<ChargeResponse> charges =
                    paymentApiClient.listChargesByCustomerPaged(customerId, page, size);
            return ResponseEntity.ok(charges);
        } catch (PaymentApiException e) {
            log.warn("Falha ao buscar cobrancas do customer {} (org={}): {}", customerId, id, e.getMessage());
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Historico de trocas de plano da organizacao (via Payment API)")
    @GetMapping("/{id}/trocas-plano")
    public ResponseEntity<?> listarTrocasPlano(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long subscriptionId = resolverSubscriptionId(id);
        if (subscriptionId == null) {
            return ResponseEntity.ok(Map.of("content", Collections.emptyList(), "totalElements", 0));
        }

        try {
            return ResponseEntity.ok(paymentApiClient.getPlanChangeHistory(subscriptionId, page, size));
        } catch (PaymentApiException e) {
            log.warn("Falha ao buscar trocas de plano (subscription={}, org={}): {}", subscriptionId, id, e.getMessage());
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Uso atual do plano da organizacao (limites vs uso)")
    @GetMapping("/{id}/uso-plano")
    public ResponseEntity<PlanoUsoDTO> usoPlanOrganizacao(@PathVariable Long id) {
        PlanoUsoDTO uso = planoUsoService.getUso(id);
        return ResponseEntity.ok(uso);
    }

    // ── Helpers ──

    private Long resolverCustomerId(Long organizacaoId) {
        return assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .map(Assinatura::getPaymentApiCustomerId)
                .orElse(null);
    }

    private Long resolverSubscriptionId(Long organizacaoId) {
        return assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .map(Assinatura::getPaymentApiSubscriptionId)
                .orElse(null);
    }

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
