package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.lead.admin.*;
import org.exemplo.belloryadmin.model.entity.error.ResponseAPI;
import org.exemplo.belloryadmin.model.entity.lead.enums.PrioridadeLead;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoAtividade;
import org.exemplo.belloryadmin.service.lead.LeadAtividadeService;
import org.exemplo.belloryadmin.service.lead.LeadService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/leads")
@RequiredArgsConstructor
@Tag(name = "Admin - Leads", description = "Gestao de leads no painel administrativo (kanban, CRUD, historico)")
public class AdminLeadController {

    private final LeadService leadService;
    private final LeadAtividadeService atividadeService;

    // ===== Kanban =====

    @Operation(summary = "Estrutura do kanban", description = "Retorna colunas (status ativos) com seus leads agrupados.")
    @GetMapping("/kanban")
    public ResponseEntity<ResponseAPI<List<LeadKanbanColumnDTO>>> kanban() {
        return ResponseEntity.ok(ResponseAPI.<List<LeadKanbanColumnDTO>>builder()
                .success(true)
                .message("Kanban carregado")
                .dados(leadService.kanban())
                .build());
    }

    // ===== Listagem paginada =====

    @Operation(summary = "Listar leads (paginado, com filtros)")
    @GetMapping
    public ResponseEntity<ResponseAPI<Page<LeadListDTO>>> listar(
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) PrioridadeLead prioridade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Busca por nome/email/telefone") @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LeadListDTO> result = leadService.listar(statusId, responsavelId, prioridade, from, to, q, page, size);
        return ResponseEntity.ok(ResponseAPI.<Page<LeadListDTO>>builder()
                .success(true)
                .message("Leads listados com sucesso")
                .dados(result)
                .build());
    }

    // ===== Detalhe =====

    @Operation(summary = "Detalhe do lead")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<LeadDetailDTO>> detalhar(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseAPI.<LeadDetailDTO>builder()
                .success(true)
                .message("Lead encontrado")
                .dados(leadService.detalhar(id))
                .build());
    }

    // ===== Criar manual =====

    @Operation(summary = "Criar lead manualmente")
    @PostMapping
    public ResponseEntity<ResponseAPI<LeadDetailDTO>> criar(@Valid @RequestBody LeadCreateDTO dto) {
        LeadDetailDTO criado = leadService.criarManual(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseAPI.<LeadDetailDTO>builder()
                .success(true)
                .message("Lead criado com sucesso")
                .dados(criado)
                .build());
    }

    // ===== Atualizar campos =====

    @Operation(summary = "Atualizar campos do lead (PATCH parcial)")
    @PatchMapping("/{id}")
    public ResponseEntity<ResponseAPI<LeadDetailDTO>> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody LeadUpdateDTO dto) {
        return ResponseEntity.ok(ResponseAPI.<LeadDetailDTO>builder()
                .success(true)
                .message("Lead atualizado com sucesso")
                .dados(leadService.atualizar(id, dto))
                .build());
    }

    // ===== Mover entre colunas =====

    @Operation(summary = "Mover lead para outra coluna do kanban")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ResponseAPI<LeadDetailDTO>> moverStatus(
            @PathVariable UUID id,
            @Valid @RequestBody MoveStatusDTO dto) {
        return ResponseEntity.ok(ResponseAPI.<LeadDetailDTO>builder()
                .success(true)
                .message("Status atualizado")
                .dados(leadService.moverStatus(id, dto))
                .build());
    }

    // ===== Atribuir =====

    @Operation(summary = "Atribuir/desatribuir responsavel")
    @PatchMapping("/{id}/assign")
    public ResponseEntity<ResponseAPI<LeadDetailDTO>> atribuir(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDTO dto) {
        return ResponseEntity.ok(ResponseAPI.<LeadDetailDTO>builder()
                .success(true)
                .message("Atribuicao atualizada")
                .dados(leadService.atribuir(id, dto))
                .build());
    }

    // ===== Soft / hard delete =====

    @Operation(summary = "Excluir lead (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> softDelete(@PathVariable UUID id) {
        leadService.softDelete(id);
        return ResponseEntity.ok(ResponseAPI.<Void>builder()
                .success(true)
                .message("Lead excluido com sucesso")
                .build());
    }

    @Operation(summary = "Excluir lead permanentemente (LGPD)",
            description = "Apaga o registro e todo o historico. Usar apenas a pedido do titular dos dados.")
    @DeleteMapping("/{id}/lgpd")
    public ResponseEntity<ResponseAPI<Void>> hardDeleteLgpd(@PathVariable UUID id) {
        leadService.hardDeleteLgpd(id);
        return ResponseEntity.ok(ResponseAPI.<Void>builder()
                .success(true)
                .message("Lead apagado permanentemente (LGPD)")
                .build());
    }

    // ===== Atividades / historico =====

    @Operation(summary = "Listar atividades (historico) do lead")
    @GetMapping("/{id}/atividades")
    public ResponseEntity<ResponseAPI<List<AtividadeDTO>>> listarAtividades(
            @PathVariable UUID id,
            @Parameter(description = "Filtrar por tipo") @RequestParam(required = false) TipoAtividade tipo) {
        return ResponseEntity.ok(ResponseAPI.<List<AtividadeDTO>>builder()
                .success(true)
                .message("Atividades listadas")
                .dados(atividadeService.listar(id, tipo))
                .build());
    }

    @Operation(summary = "Adicionar atividade manual no lead",
            description = "Comentario, ligacao, email, whatsapp, reuniao ou nota interna. Tipos automaticos sao rejeitados.")
    @PostMapping("/{id}/atividades")
    public ResponseEntity<ResponseAPI<AtividadeDTO>> criarAtividade(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAtividadeDTO dto) {
        AtividadeDTO criada = atividadeService.criarManual(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseAPI.<AtividadeDTO>builder()
                .success(true)
                .message("Atividade adicionada")
                .dados(criada)
                .build());
    }

    @Operation(summary = "Apagar atividade manual",
            description = "Atividades automaticas (LEAD_CRIADO, MUDANCA_STATUS, ATRIBUICAO) nao podem ser apagadas.")
    @DeleteMapping("/{id}/atividades/{atividadeId}")
    public ResponseEntity<ResponseAPI<Void>> apagarAtividade(
            @PathVariable UUID id,
            @PathVariable Long atividadeId) {
        atividadeService.apagarManual(atividadeId);
        return ResponseEntity.ok(ResponseAPI.<Void>builder()
                .success(true)
                .message("Atividade apagada")
                .build());
    }
}
