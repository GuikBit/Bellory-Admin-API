package org.exemplo.belloryadmin.service.lead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Verifica tokens do Cloudflare Turnstile contra o endpoint siteverify.
 *
 * <p>Conforme {@code CONTACT_BACKEND_SPEC.md} §2.6: a chave secreta SO fica
 * no backend. Falha de rede equivale a token invalido (fail-closed).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TurnstileVerifierService {

    private final RestTemplate restTemplate;

    @Value("${bellory.contact.turnstile.secret-key:}")
    private String secretKey;

    @Value("${bellory.contact.turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyUrl;

    public TurnstileResult verify(String token, String remoteIp) {
        if (secretKey == null || secretKey.isBlank()) {
            log.error("Turnstile secret key nao configurada (bellory.contact.turnstile.secret-key)");
            return TurnstileResult.fail("missing-input-secret");
        }
        if (token == null || token.isBlank()) {
            return TurnstileResult.fail("missing-input-response");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                body.add("remoteip", remoteIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<TurnstileApiResponse> resp = restTemplate.postForEntity(
                    verifyUrl, request, TurnstileApiResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                TurnstileApiResponse api = resp.getBody();
                return new TurnstileResult(api.isSuccess(), api.getHostname(), api.getErrorCodes());
            }

            log.warn("Turnstile siteverify retornou status nao-2xx: {}", resp.getStatusCode());
            return TurnstileResult.fail("siteverify-non-2xx");
        } catch (Exception e) {
            log.warn("Falha ao chamar Turnstile siteverify: {}", e.getMessage());
            return TurnstileResult.fail("siteverify-network-error");
        }
    }

    // ===== POJOs =====

    public record TurnstileResult(boolean success, String hostname, List<String> errorCodes) {
        public static TurnstileResult fail(String code) {
            return new TurnstileResult(false, null, List.of(code));
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TurnstileApiResponse {
        private boolean success;
        private String hostname;
        @JsonProperty("error-codes")
        private List<String> errorCodes;
        @JsonProperty("challenge_ts")
        private String challengeTs;
    }
}
