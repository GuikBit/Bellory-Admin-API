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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/cupons")
@Tag(name = "Admin - Cupons", description = "CRUD de cupons de desconto via Payment API")
@Slf4j
public class AdminCupomController {

    private final PaymentApiClient paymentApiClient;

    public AdminCupomController(PaymentApiClient paymentApiClient) {
        this.paymentApiClient = paymentApiClient;
    }

    @Operation(summary = "Listar todos os cupons (ativos e inativos)")
    @GetMapping
    public ResponseEntity<?> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(paymentApiClient.listCoupons(page, size));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Listar apenas cupons ativos e dentro da validade")
    @GetMapping("/ativos")
    public ResponseEntity<?> listarAtivos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(paymentApiClient.listActiveCoupons(page, size));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Buscar cupom por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.getCoupon(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Buscar cupom por codigo (case-insensitive)")
    @GetMapping("/code/{code}")
    public ResponseEntity<?> buscarPorCodigo(@PathVariable String code) {
        try {
            return ResponseEntity.ok(paymentApiClient.getCouponByCode(code));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Criar novo cupom de desconto")
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody CreateCouponRequest req) {
        try {
            CouponResponse response = paymentApiClient.createCoupon(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Atualizar cupom (campos imutaveis: code, scope, discountType)")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody UpdateCouponRequest req) {
        try {
            return ResponseEntity.ok(paymentApiClient.updateCoupon(id, req));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Desativar cupom (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desativar(@PathVariable Long id) {
        try {
            paymentApiClient.deleteCoupon(id);
            return ResponseEntity.noContent().build();
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Reativar cupom desativado")
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<?> reativar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentApiClient.activateCoupon(id));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Historico de uso do cupom")
    @GetMapping("/{id}/usos")
    public ResponseEntity<?> listarUsos(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            return ResponseEntity.ok(paymentApiClient.getCouponUsages(id, page, size));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
    }

    @Operation(summary = "Validar cupom (com todas as verificacoes, incluindo per-customer)")
    @PostMapping("/validar")
    public ResponseEntity<?> validar(@RequestBody ValidateCouponRequest req) {
        try {
            return ResponseEntity.ok(paymentApiClient.validateCouponPublic(req));
        } catch (PaymentApiException e) {
            return buildErrorResponse(e);
        }
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
