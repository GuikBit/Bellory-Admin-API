package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.admin.AdminDashboardDTO;
import org.exemplo.belloryadmin.service.admin.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard", description = "Dashboard geral do painel administrativo com visao consolidada de todas as organizacoes")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "Obter dashboard geral",
            description = "Retorna metricas consolidadas de todas as organizacoes agrupadas por dominio: " +
                    "organizacoes, agendamentos, clientes, funcionarios, servicos, instancias, financeiro, " +
                    "planos (Payment API), leads, top organizacoes e localizacoes. Resposta cacheada por alguns minutos.")
    @GetMapping
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        return ResponseEntity.ok(adminDashboardService.getDashboard());
    }

    @Operation(summary = "Recalcular dashboard",
            description = "Forca o recalculo do dashboard ignorando o cache e atualizando-o.")
    @PostMapping("/refresh")
    public ResponseEntity<AdminDashboardDTO> refreshDashboard() {
        return ResponseEntity.ok(adminDashboardService.refreshDashboard());
    }
}
