package org.exemplo.belloryadmin.controller.site;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.tracking.analytics.*;
import org.exemplo.belloryadmin.service.tracking.AdminAnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin - Analytics", description = "Endpoints de analytics do site bellory.com.br para o painel administrativo")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    @Operation(summary = "Overview geral",
            description = "Retorna métricas gerais de visitantes, sessões, conversões e top pages para o período informado.")
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewDTO> getOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end_date) {
        return ResponseEntity.ok(analyticsService.getOverview(start_date, end_date));
    }

    @Operation(summary = "Análise de tráfego",
            description = "Retorna dados de fontes de tráfego, campanhas UTM e top referrers.")
    @GetMapping("/traffic")
    public ResponseEntity<AnalyticsTrafficDTO> getTraffic(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end_date) {
        return ResponseEntity.ok(analyticsService.getTraffic(start_date, end_date));
    }

    @Operation(summary = "Análise comportamental",
            description = "Retorna dados de CTAs mais clicados, scroll depth, visibilidade de seções e páginas de saída.")
    @GetMapping("/behavior")
    public ResponseEntity<AnalyticsBehaviorDTO> getBehavior(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end_date) {
        return ResponseEntity.ok(analyticsService.getBehavior(start_date, end_date));
    }

    @Operation(summary = "Funil de conversão",
            description = "Retorna dados do funil de conversão, distribuição de planos, preferência de cobrança e tempo médio de conversão.")
    @GetMapping("/conversions")
    public ResponseEntity<AnalyticsConversionsDTO> getConversions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end_date) {
        return ResponseEntity.ok(analyticsService.getConversions(start_date, end_date));
    }

    @Operation(summary = "Contexto (devices, geo, performance, erros)",
            description = "Retorna dados de dispositivos, navegadores, SO, geolocalização, performance (Web Vitals) e erros.")
    @GetMapping("/context")
    public ResponseEntity<AnalyticsContextDTO> getContext(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start_date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end_date) {
        return ResponseEntity.ok(analyticsService.getContext(start_date, end_date));
    }

    @Operation(summary = "Dados em tempo real",
            description = "Retorna visitantes ativos, páginas ativas, eventos recentes e métricas dos últimos 30 minutos.")
    @GetMapping("/realtime")
    public ResponseEntity<AnalyticsRealtimeDTO> getRealtime() {
        return ResponseEntity.ok(analyticsService.getRealtime());
    }
}
