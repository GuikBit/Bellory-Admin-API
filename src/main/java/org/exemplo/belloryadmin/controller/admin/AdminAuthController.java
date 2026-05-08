package org.exemplo.belloryadmin.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.exemplo.belloryadmin.model.dto.admin.auth.AdminLoginResponseDTO;
import org.exemplo.belloryadmin.model.dto.admin.auth.AdminUserInfoDTO;
import org.exemplo.belloryadmin.model.dto.auth.ErrorResponseDTO;
import org.exemplo.belloryadmin.model.dto.auth.LoginRequestDTO;
import org.exemplo.belloryadmin.model.dto.auth.RefreshTokenRequestDTO;
import org.exemplo.belloryadmin.model.dto.auth.RefreshTokenResponseDTO;
import org.exemplo.belloryadmin.model.dto.auth.TokenValidationResponseDTO;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.exemplo.belloryadmin.service.TokenService;
import org.exemplo.belloryadmin.service.admin.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "Admin Auth", description = "Autenticação do painel administrativo da plataforma")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final TokenService tokenService;

    public AdminAuthController(AdminAuthService adminAuthService, TokenService tokenService) {
        this.adminAuthService = adminAuthService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "Login do painel admin")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request,
                                   HttpServletRequest httpRequest) {
        try {
            String token = adminAuthService.authenticate(request.getUsername(), request.getPassword());
            UsuarioAdmin admin = adminAuthService.findByUsername(request.getUsername().trim().toLowerCase());
            AdminUserInfoDTO userInfo = adminAuthService.toUserInfoDTO(admin);
            LocalDateTime expiresAt = tokenService.getExpirationDateTime();

            AdminLoginResponseDTO response = AdminLoginResponseDTO.builder()
                    .success(true)
                    .message("Login realizado com sucesso")
                    .token(token)
                    .user(userInfo)
                    .expiresAt(expiresAt)
                    .build();

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            String errorCode = e.getMessage().contains("desativada") ? "ACCOUNT_DISABLED" : "INVALID_CREDENTIALS";
            HttpStatus status = e.getMessage().contains("desativada") ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;

            return ResponseEntity.status(status)
                    .body(createErrorResponse(e.getMessage(), errorCode, httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Validar token admin")
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader,
                                           HttpServletRequest httpRequest) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Token não fornecido ou formato inválido",
                                "MISSING_TOKEN", httpRequest.getRequestURI()));
            }

            String token = authHeader.replace("Bearer ", "");

            if (tokenService.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token expirado", "TOKEN_EXPIRED", httpRequest.getRequestURI()));
            }

            String userType = tokenService.getUserTypeFromToken(token);
            if (!"PLATFORM_ADMIN".equals(userType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token não é de admin da plataforma",
                                "INVALID_TOKEN_TYPE", httpRequest.getRequestURI()));
            }

            String username = tokenService.validateToken(token);
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token inválido", "INVALID_TOKEN", httpRequest.getRequestURI()));
            }

            Long userId = tokenService.getUserIdFromToken(token);
            String role = tokenService.getRoleFromToken(token);

            TokenValidationResponseDTO response = TokenValidationResponseDTO.builder()
                    .valid(true)
                    .username(username)
                    .userId(userId)
                    .userType("PLATFORM_ADMIN")
                    .roles(Collections.singletonList(role))
                    .expiresAt(tokenService.getExpirationDateTime())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro ao validar token", "VALIDATION_ERROR",
                            httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Renovar token admin")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request,
                                          HttpServletRequest httpRequest) {
        try {
            String userType = tokenService.getUserTypeFromToken(request.getToken());
            if (!"PLATFORM_ADMIN".equals(userType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token não é de admin da plataforma",
                                "INVALID_TOKEN_TYPE", httpRequest.getRequestURI()));
            }

            String newToken = tokenService.refreshToken(request.getToken());
            LocalDateTime expiresAt = tokenService.getExpirationDateTime();

            RefreshTokenResponseDTO response = RefreshTokenResponseDTO.builder()
                    .success(true)
                    .message("Token renovado com sucesso")
                    .newToken(newToken)
                    .expiresAt(expiresAt)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Não foi possível renovar o token: " + e.getMessage(),
                            "REFRESH_FAILED", httpRequest.getRequestURI()));
        }
    }

    @Operation(summary = "Obter dados do admin logado")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAdmin(@RequestHeader("Authorization") String authHeader,
                                             HttpServletRequest httpRequest) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Token não fornecido", "MISSING_TOKEN",
                                httpRequest.getRequestURI()));
            }

            String token = authHeader.replace("Bearer ", "");
            String username = tokenService.validateToken(token);

            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Token inválido", "INVALID_TOKEN",
                                httpRequest.getRequestURI()));
            }

            UsuarioAdmin admin = adminAuthService.findByUsername(username);
            AdminUserInfoDTO userInfo = adminAuthService.toUserInfoDTO(admin);

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro ao buscar informações do usuário",
                            "USER_INFO_ERROR", httpRequest.getRequestURI()));
        }
    }

    private ErrorResponseDTO createErrorResponse(String message, String errorCode, String path) {
        return ErrorResponseDTO.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
