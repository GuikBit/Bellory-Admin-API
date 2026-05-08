package org.exemplo.belloryadmin.service.lead;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.lead.publico.ContactPayloadDTO;
import org.exemplo.belloryadmin.model.dto.lead.publico.ContactResponseDTO;
import org.exemplo.belloryadmin.model.entity.lead.Lead;
import org.exemplo.belloryadmin.model.entity.lead.LeadStatus;
import org.exemplo.belloryadmin.model.entity.lead.enums.PrioridadeLead;
import org.exemplo.belloryadmin.model.repository.lead.LeadRepository;
import org.exemplo.belloryadmin.model.repository.lead.LeadStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pipeline completa do POST /api/contato (CONTACT_BACKEND_SPEC.md).
 *
 * <ol>
 *   <li>Origin/Referer check</li>
 *   <li>Rate limit por IP</li>
 *   <li>Honeypot ({@code website})</li>
 *   <li>Time-trap ({@code fillTimeMs &lt; 1500})</li>
 *   <li>Validacao Bean Validation (delegada ao controller)</li>
 *   <li>Verificacao Cloudflare Turnstile</li>
 *   <li>Sanitizacao + hash IP + persist</li>
 * </ol>
 *
 * <p>Honeypot/time-trap respondem 200 OK FAKE (nao revelam ao bot).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactPublicService {

    private static final int MIN_FILL_TIME_MS = 1500;
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final String SOURCE_TAG = "site";

    private final ContactRateLimiterService rateLimiter;
    private final TurnstileVerifierService turnstile;
    private final LeadRepository leadRepository;
    private final LeadStatusRepository statusRepository;
    private final LeadAtividadeService atividadeService;

    @Value("${bellory.contact.allowed-origins:}")
    private String allowedOriginsRaw;

    @Value("${bellory.contact.ip-hash-salt:dev-salt-change-me}")
    private String ipHashSalt;

    private Set<String> allowedOrigins() {
        if (allowedOriginsRaw == null || allowedOriginsRaw.isBlank()) {
            return Set.of();
        }
        return Set.of(allowedOriginsRaw.split("\\s*,\\s*"));
    }

    /**
     * Processa o request publico. Retorna ja o ResponseEntity formatado com
     * o codigo HTTP e o JSON {@code {ok, code, id}} esperado pelo frontend.
     */
    @Transactional
    public ResponseEntity<ContactResponseDTO> processar(ContactPayloadDTO payload, String origin,
                                                        String ip, String userAgent) {

        // 1. Origin / Referer check
        if (!isOriginAllowed(origin)) {
            log.info("Lead rejeitado: origem nao permitida = {}", origin);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ContactResponseDTO.error("forbidden_origin", "Origem nao autorizada"));
        }

        // 2. Rate limit
        ContactRateLimiterService.RateLimitResult rl = rateLimiter.check(ip);
        if (!rl.allowed()) {
            log.info("Lead rejeitado: rate limit ({} window) ip={}", rl.windowExceeded(), ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ContactResponseDTO.error("rate_limited", "Muitas tentativas. Tente novamente mais tarde."));
        }

        // 3. Honeypot — 200 OK fake
        if (payload.getWebsite() != null && !payload.getWebsite().trim().isEmpty()) {
            log.info("Lead descartado (honeypot) ip={}", ip);
            return ResponseEntity.ok(ContactResponseDTO.success("drop_honeypot"));
        }

        // 4. Time-trap — 200 OK fake
        if (payload.getFillTimeMs() == null || payload.getFillTimeMs() < MIN_FILL_TIME_MS) {
            log.info("Lead descartado (too_fast: {}ms) ip={}", payload.getFillTimeMs(), ip);
            return ResponseEntity.ok(ContactResponseDTO.success("drop_too_fast"));
        }

        // 5. Bean Validation ja foi feita no controller (@Valid)

        // 6. Turnstile
        TurnstileVerifierService.TurnstileResult ts =
                turnstile.verify(payload.getTurnstileToken(), ip);
        if (!ts.success()) {
            log.info("Lead rejeitado: turnstile falhou ip={} errors={}", ip, ts.errorCodes());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ContactResponseDTO.error("turnstile_failed", "Validacao de seguranca falhou"));
        }

        // 7. Sanitize + persist
        try {
            Lead lead = persistir(payload, ip, userAgent, true);
            atividadeService.registrarLeadCriado(lead, true);
            String publicId = "lead_" + lead.getId().toString().replace("-", "").substring(0, 12);
            log.info("Lead criado via site: {} ({}) origem={}", lead.getNome(), lead.getEmail(), lead.getOrigem());
            return ResponseEntity.ok(ContactResponseDTO.success(publicId));
        } catch (IllegalStateException e) {
            log.error("Falha ao persistir lead: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ContactResponseDTO.error("server", "Erro interno"));
        }
    }

    // ===== helpers =====

    private boolean isOriginAllowed(String origin) {
        Set<String> allowed = allowedOrigins();
        if (allowed.isEmpty()) {
            // Sem config = bloqueia tudo (fail-closed)
            return false;
        }
        if (origin == null || origin.isBlank()) {
            return false;
        }
        return allowed.contains(origin.trim());
    }

    private Lead persistir(ContactPayloadDTO payload, String ip, String userAgent, boolean turnstileOk) {
        LeadStatus statusInicial = statusRepository.findByEhStatusInicialTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhum LeadStatus marcado como inicial — verifique o seed."));

        String nome = normalizar(payload.getName());
        String email = normalizar(payload.getEmail()).toLowerCase();
        String telefone = normalizar(payload.getPhone());
        String mensagem = sanitizarMensagem(payload.getMessage());

        Lead lead = Lead.builder()
                .status(statusInicial)
                .nome(nome)
                .email(email)
                .telefone(telefone)
                .tipoNegocio(payload.getBusinessType())
                .mensagem(mensagem)
                .origem(payload.getSource() != null ? payload.getSource() : "landing/contato")
                .prioridade(PrioridadeLead.MEDIA)
                .tags(new String[]{SOURCE_TAG})
                .ipHash(hashIp(ip))
                .userAgent(truncar(userAgent, 1024))
                .fillTimeMs(payload.getFillTimeMs())
                .turnstileOk(turnstileOk)
                .policyVersion(payload.getPolicyVersion())
                .build();

        return leadRepository.save(lead);
    }

    private String normalizar(String s) {
        if (s == null) return null;
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFC);
    }

    private String sanitizarMensagem(String m) {
        if (m == null) return "";
        // Strip tags HTML, mantem quebras de linha
        return HTML_TAG.matcher(m).replaceAll("").trim();
    }

    private String hashIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((ip + ipHashSalt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("Falha ao hashear IP: {}", e.getMessage());
            return null;
        }
    }

    private String truncar(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
