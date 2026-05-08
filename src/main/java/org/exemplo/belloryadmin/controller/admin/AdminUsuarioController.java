package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.exemplo.belloryadmin.model.dto.admin.auth.AdminUserInfoDTO;
import org.exemplo.belloryadmin.model.dto.admin.auth.AdminUsuarioCreateDTO;
import org.exemplo.belloryadmin.model.dto.admin.auth.AdminUsuarioUpdateDTO;
import org.exemplo.belloryadmin.service.admin.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@Tag(name = "Admin Usuarios", description = "CRUD de usuários administradores da plataforma")
public class AdminUsuarioController {

    private final AdminAuthService adminAuthService;

    public AdminUsuarioController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Listar todos os usuários admin")
    @GetMapping
    public ResponseEntity<List<AdminUserInfoDTO>> listarTodos() {
        return ResponseEntity.ok(adminAuthService.listarTodos());
    }

    @Operation(summary = "Buscar usuário admin por ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminAuthService.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Criar novo usuário admin")
    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody AdminUsuarioCreateDTO dto) {
        try {
            AdminUserInfoDTO created = adminAuthService.criar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Atualizar usuário admin")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @Valid @RequestBody AdminUsuarioUpdateDTO dto) {
        try {
            return ResponseEntity.ok(adminAuthService.atualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "Desativar usuário admin (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desativar(@PathVariable Long id) {
        try {
            adminAuthService.desativar(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Usuário desativado com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
