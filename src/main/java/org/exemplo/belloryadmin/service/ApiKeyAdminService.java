package org.exemplo.belloryadmin.service;

import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.ApiKeyAdminUserInfo;
import org.exemplo.belloryadmin.model.entity.config.ApiKeyAdmin;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.exemplo.belloryadmin.model.repository.config.ApiKeyAdminRepository;
import org.exemplo.belloryadmin.model.repository.users.UsuarioAdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiKeyAdminService {

    private final ApiKeyAdminRepository apiKeyRepository;
    private final UsuarioAdminRepository usuarioAdminRepository;

    private static final String API_KEY_PREFIX = "bly_adm_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public Map<String, Object> generateApiKey(
            Long usuarioAdminId,
            String name,
            String description,
            LocalDateTime expiresAt) {

        UsuarioAdmin usuario = usuarioAdminRepository.findById(usuarioAdminId)
                .orElseThrow(() -> new IllegalArgumentException("UsuarioAdmin nao encontrado: " + usuarioAdminId));

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawKey = API_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String keyHash = hashApiKey(rawKey);

        ApiKeyAdmin apiKey = ApiKeyAdmin.builder()
                .usuarioAdmin(usuario)
                .username(usuario.getUsername())
                .keyHash(keyHash)
                .name(name)
                .description(description)
                .expiresAt(expiresAt)
                .ativo(true)
                .build();

        ApiKeyAdmin saved = apiKeyRepository.save(apiKey);

        return Map.of(
                "apiKey", rawKey,
                "entity", saved
        );
    }

    @Transactional
    public ApiKeyAdminUserInfo validateApiKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(API_KEY_PREFIX)) {
            return null;
        }

        String keyHash = hashApiKey(rawKey);
        ApiKeyAdmin apiKey = apiKeyRepository.findByKeyHashAndAtivoTrue(keyHash).orElse(null);

        if (apiKey == null || apiKey.isExpired()) {
            return null;
        }

        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        UsuarioAdmin u = apiKey.getUsuarioAdmin();
        return ApiKeyAdminUserInfo.builder()
                .usuarioAdminId(u.getId())
                .username(u.getUsername())
                .nomeCompleto(u.getNomeCompleto())
                .email(u.getEmail())
                .role(u.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApiKeyAdmin> listByUsuarioAdmin(Long usuarioAdminId) {
        return apiKeyRepository.findByUsuarioAdminIdAndAtivoTrue(usuarioAdminId);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyAdmin> listAll() {
        return apiKeyRepository.findAllByAtivoTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public void revokeApiKey(Long apiKeyId, Long usuarioAdminId) {
        ApiKeyAdmin apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API Key nao encontrada"));

        if (!apiKey.getUsuarioAdmin().getId().equals(usuarioAdminId)) {
            throw new IllegalArgumentException("Sem permissao para revogar esta API Key");
        }

        apiKey.setAtivo(false);
        apiKeyRepository.save(apiKey);
    }

    private String hashApiKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash da API Key", e);
        }
    }
}
