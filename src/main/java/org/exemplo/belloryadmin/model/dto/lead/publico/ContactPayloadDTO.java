package org.exemplo.belloryadmin.model.dto.lead.publico;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoNegocio;

/**
 * Payload do formulario publico de contato (CONTACT_BACKEND_SPEC.md).
 *
 * <p>Bean Validation cobre os limites de tamanho/formato. As checagens
 * de seguranca (origin, rate limit, honeypot, time-trap, turnstile)
 * ficam no service.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactPayloadDTO {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Size(max = 160)
    @Email
    private String email;

    @NotBlank
    @Size(min = 10, max = 20)
    @Pattern(regexp = "^[\\d\\s()+\\-]+$")
    private String phone;

    @NotNull
    private TipoNegocio businessType;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String message;

    /**
     * Honeypot. Espera-se sempre vazio. Bot que preenche cai aqui.
     * NAO usar @NotNull/@Size aqui — validacao acontece no service
     * para podermos retornar 200 OK fake (nao revelar ao bot).
     */
    private String website;

    @NotBlank
    private String turnstileToken;

    /**
     * Tempo (ms) que o usuario gastou preenchendo. < 1500 = bot
     * provavel (validacao no service para retornar 200 OK fake).
     */
    @NotNull
    private Integer fillTimeMs;

    @Size(max = 120)
    private String source;

    /**
     * Versao da politica de privacidade consentida pelo usuario.
     */
    @Size(max = 20)
    private String policyVersion;
}
