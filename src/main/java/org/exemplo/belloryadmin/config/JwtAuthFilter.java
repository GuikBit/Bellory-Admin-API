package org.exemplo.belloryadmin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exemplo.belloryadmin.context.TenantContext;
import org.exemplo.belloryadmin.model.dto.ApiKeyAdminUserInfo;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.exemplo.belloryadmin.model.repository.users.UsuarioAdminRepository;
import org.exemplo.belloryadmin.service.ApiKeyAdminService;
import org.exemplo.belloryadmin.service.TokenService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter do Bellory-Admin-API.
 *
 * <p>Versao podada do filtro do Bellory-API: NAO ha ConfigSistema, NAO ha ramo
 * APP. Apenas tokens JWT com {@code userType=PLATFORM_ADMIN} sao aceitos. Antes
 * do JWT, tenta autenticar via header {@code X-API-Key} (chaves emitidas em
 * {@code admin.api_keys}). Tokens APP do cliente nao tem motivo pra chegar aqui
 * — se chegarem, sao ignorados (sem auth) e o SecurityConfig bloqueia o acesso.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioAdminRepository usuarioAdminRepository;
    private final ApiKeyAdminService apiKeyAdminService;

    public JwtAuthFilter(TokenService tokenService,
                         UsuarioAdminRepository usuarioAdminRepository,
                         @Lazy ApiKeyAdminService apiKeyAdminService) {
        this.tokenService = tokenService;
        this.usuarioAdminRepository = usuarioAdminRepository;
        this.apiKeyAdminService = apiKeyAdminService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String apiKey = request.getHeader("X-API-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                ApiKeyAdminUserInfo userInfo = apiKeyAdminService.validateApiKey(apiKey);
                if (userInfo != null) {
                    TenantContext.setContext(
                            userInfo.getUsuarioAdminId(),
                            userInfo.getUsername(),
                            userInfo.getRole()
                    );

                    UsuarioAdmin adminUser = usuarioAdminRepository.findByUsername(userInfo.getUsername())
                            .orElse(null);
                    if (adminUser != null && adminUser.isEnabled()) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                adminUser, null, adminUser.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }

                    filterChain.doFilter(request, response);
                    return;
                }
            }

            String token = recoverToken(request);

            if (token != null) {
                String subject = tokenService.validateToken(token);

                if (subject != null && !subject.isEmpty()) {
                    String userType = tokenService.getUserTypeFromToken(token);

                    if ("PLATFORM_ADMIN".equals(userType)) {
                        Long userId = tokenService.getUserIdFromToken(token);
                        String role = tokenService.getRoleFromToken(token);

                        TenantContext.setContext(userId, subject, role);

                        UsuarioAdmin adminUser = usuarioAdminRepository.findByUsername(subject)
                                .orElse(null);
                        if (adminUser != null && adminUser.isEnabled()) {
                            var authentication = new UsernamePasswordAuthenticationToken(
                                    adminUser, null, adminUser.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                    // Tokens nao-PLATFORM_ADMIN: deixa passar sem auth (SecurityConfig nega)
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }
}
