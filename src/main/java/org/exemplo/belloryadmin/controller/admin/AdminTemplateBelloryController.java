package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.template.*;
import org.exemplo.belloryadmin.model.entity.error.ResponseAPI;
import org.exemplo.belloryadmin.model.entity.template.CategoriaTemplate;
import org.exemplo.belloryadmin.model.entity.template.TipoTemplate;
import org.exemplo.belloryadmin.service.admin.AdminTemplateBelloryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
@Tag(name = "Admin - Templates", description = "CRUD de templates globais da plataforma Bellory")
public class AdminTemplateBelloryController {

    private final AdminTemplateBelloryService service;

    @Operation(summary = "Listar templates", description = "Lista templates com filtros opcionais por tipo e categoria")
    @GetMapping
    public ResponseEntity<ResponseAPI<List<TemplateBelloryResponseDTO>>> listar(
            @Parameter(description = "Filtrar por tipo") @RequestParam(required = false) TipoTemplate tipo,
            @Parameter(description = "Filtrar por categoria") @RequestParam(required = false) CategoriaTemplate categoria) {
        try {
            List<TemplateBelloryResponseDTO> templates = service.listarTodos(tipo, categoria);
            return ResponseEntity.ok(ResponseAPI.<List<TemplateBelloryResponseDTO>>builder()
                    .success(true)
                    .message("Templates listados com sucesso")
                    .dados(templates)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseAPI.<List<TemplateBelloryResponseDTO>>builder()
                            .success(false)
                            .message("Erro ao listar templates: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Buscar template por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseAPI<TemplateBelloryResponseDTO>> buscarPorId(
            @Parameter(description = "ID do template") @PathVariable Long id) {
        try {
            TemplateBelloryResponseDTO template = service.buscarPorId(id);
            return ResponseEntity.ok(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                    .success(true)
                    .message("Template encontrado")
                    .dados(template)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Criar novo template")
    @PostMapping
    public ResponseEntity<ResponseAPI<TemplateBelloryResponseDTO>> criar(
            @Valid @RequestBody TemplateBelloryCreateDTO dto) {
        try {
            TemplateBelloryResponseDTO template = service.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                            .success(true)
                            .message("Template criado com sucesso")
                            .dados(template)
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Atualizar template")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseAPI<TemplateBelloryResponseDTO>> atualizar(
            @Parameter(description = "ID do template") @PathVariable Long id,
            @Valid @RequestBody TemplateBelloryUpdateDTO dto) {
        try {
            TemplateBelloryResponseDTO template = service.atualizar(id, dto);
            return ResponseEntity.ok(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                    .success(true)
                    .message("Template atualizado com sucesso")
                    .dados(template)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Desativar template (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseAPI<Void>> desativar(
            @Parameter(description = "ID do template") @PathVariable Long id) {
        try {
            service.desativar(id);
            return ResponseEntity.ok(ResponseAPI.<Void>builder()
                    .success(true)
                    .message("Template desativado com sucesso")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<Void>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Ativar template")
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ResponseAPI<TemplateBelloryResponseDTO>> ativar(
            @Parameter(description = "ID do template") @PathVariable Long id) {
        try {
            TemplateBelloryResponseDTO template = service.ativar(id);
            return ResponseEntity.ok(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                    .success(true)
                    .message("Template ativado com sucesso")
                    .dados(template)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Marcar template como padrao", description = "Define este template como padrao para o tipo+categoria. Desmarca o anterior.")
    @PatchMapping("/{id}/padrao")
    public ResponseEntity<ResponseAPI<TemplateBelloryResponseDTO>> marcarComoPadrao(
            @Parameter(description = "ID do template") @PathVariable Long id) {
        try {
            TemplateBelloryResponseDTO template = service.marcarComoPadrao(id);
            return ResponseEntity.ok(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                    .success(true)
                    .message("Template marcado como padrao com sucesso")
                    .dados(template)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<TemplateBelloryResponseDTO>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Preview do template", description = "Substitui variaveis com dados de exemplo e retorna o template renderizado")
    @PostMapping("/{id}/preview")
    public ResponseEntity<ResponseAPI<String>> preview(
            @Parameter(description = "ID do template") @PathVariable Long id,
            @RequestBody(required = false) TemplatePreviewRequestDTO request) {
        try {
            if (request == null) {
                request = new TemplatePreviewRequestDTO();
            }
            String resultado = service.preview(id, request);
            return ResponseEntity.ok(ResponseAPI.<String>builder()
                    .success(true)
                    .message("Preview gerado com sucesso")
                    .dados(resultado)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseAPI.<String>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
