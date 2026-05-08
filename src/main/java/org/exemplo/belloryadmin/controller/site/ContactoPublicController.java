package org.exemplo.belloryadmin.controller.site;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.lead.publico.ContactPayloadDTO;
import org.exemplo.belloryadmin.model.dto.lead.publico.ContactResponseDTO;
import org.exemplo.belloryadmin.service.lead.ContactPublicService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint publico do formulario de contato (CONTACT_BACKEND_SPEC.md).
 *
 * <p>Sem JWT — protegido pelo SecurityConfig em allowlist. As camadas de
 * seguranca (origin, rate limit, honeypot, time-trap, turnstile) ficam
 * em {@link ContactPublicService}.</p>
 */
@RestController
@RequestMapping("/api/contato")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Site - Contato", description = "Recebe leads do formulario publico (landing page Bellory)")
public class ContactoPublicController {

    private final ContactPublicService contactService;

    @Operation(summary = "Receber lead do formulario de contato",
            description = "Endpoint publico chamado pela landing page bellory.com.br. "
                    + "Aplica origin check, rate limit, honeypot, time-trap, validacao Zod-like e Turnstile.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactResponseDTO> receber(
            @Valid @RequestBody ContactPayloadDTO payload,
            HttpServletRequest request) {

        String origin = request.getHeader("Origin");
        if (origin == null) {
            origin = request.getHeader("Referer");
        }
        String ip = clientIp(request);
        String userAgent = request.getHeader("User-Agent");

        return contactService.processar(payload, origin, ip, userAgent);
    }

    /**
     * Bean validation falhou — retornamos no formato da spec (400 / code=validation)
     * em vez do default do GlobalExceptionHandler para nao vazar shape interno.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ContactResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        log.info("Lead rejeitado (validation): {} campos invalidos", ex.getBindingResult().getErrorCount());
        return ResponseEntity.badRequest()
                .body(ContactResponseDTO.error("validation", "Dados invalidos"));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ContactResponseDTO> handleUnreadable(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ContactResponseDTO.error("validation", "JSON invalido"));
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real;
        }
        return request.getRemoteAddr();
    }
}
