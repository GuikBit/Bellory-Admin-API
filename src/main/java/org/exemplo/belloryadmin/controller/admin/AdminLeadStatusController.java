package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.lead.admin.LeadStatusCreateDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.LeadStatusDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.LeadStatusUpdateDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.ReorderItemDTO;
import org.exemplo.belloryadmin.model.entity.error.ResponseAPI;
import org.exemplo.belloryadmin.service.lead.LeadStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/leads/status")
@RequiredArgsConstructor
@Tag(name = "Admin - Lead Status", description = "Configuracao das colunas do kanban de leads")
public class AdminLeadStatusController {

    private final LeadStatusService service;

    @Operation(summary = "Listar todos os status (ativos + inativos)")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<LeadStatusDTO>>> listar(
            @Parameter(description = "Se true, retorna apenas ativos") @RequestParam(defaultValue = "false") boolean ativosApenas) {
        List<LeadStatusDTO> data = ativosApenas ? service.listarAtivos() : service.listarTodos();
        return ResponseEntity.ok(ResponseAPI.<List<LeadStatusDTO>>builder()
                .success(true)
                .message("Status listados com sucesso")
                .dados(data)
                .build());
    }

    @Operation(summary = "Criar coluna do kanban")
    @PostMapping
    public ResponseEntity<ResponseAPI<LeadStatusDTO>> criar(@Valid @RequestBody LeadStatusCreateDTO dto) {
        LeadStatusDTO criado = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseAPI.<LeadStatusDTO>builder()
                .success(true)
                .message("Status criado com sucesso")
                .dados(criado)
                .build());
    }

    @Operation(summary = "Atualizar coluna do kanban")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<LeadStatusDTO>> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody LeadStatusUpdateDTO dto) {
        LeadStatusDTO atualizado = service.atualizar(id, dto);
        return ResponseEntity.ok(ResponseAPI.<LeadStatusDTO>builder()
                .success(true)
                .message("Status atualizado com sucesso")
                .dados(atualizado)
                .build());
    }

    @Operation(summary = "Reordenar colunas do kanban (bulk)")
    @PatchMapping("/reorder")
    public ResponseEntity<ResponseAPI<List<LeadStatusDTO>>> reordenar(
            @Valid @RequestBody List<ReorderItemDTO> itens) {
        List<LeadStatusDTO> result = service.reordenar(itens);
        return ResponseEntity.ok(ResponseAPI.<List<LeadStatusDTO>>builder()
                .success(true)
                .message("Status reordenados com sucesso")
                .dados(result)
                .build());
    }

    @Operation(summary = "Inativar coluna do kanban", description = "Bloqueia se houver leads no status (use moveTo para realocar).")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> inativar(
            @PathVariable Long id,
            @Parameter(description = "Status destino caso existam leads neste status") @RequestParam(required = false) Long moveTo) {
        service.inativar(id, moveTo);
        return ResponseEntity.ok(ResponseAPI.<Void>builder()
                .success(true)
                .message("Status inativado com sucesso")
                .build());
    }
}
