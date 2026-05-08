package org.exemplo.belloryadmin.model.dto.lead.admin;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.model.entity.lead.enums.PrioridadeLead;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoNegocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Criar lead manualmente pelo painel admin (sem turnstile/honeypot).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadCreateDTO {

    @NotBlank
    @Size(min = 2, max = 100)
    private String nome;

    @NotBlank
    @Size(max = 160)
    @Email
    private String email;

    @NotBlank
    @Size(min = 10, max = 20)
    @Pattern(regexp = "^[\\d\\s()+\\-]+$")
    private String telefone;

    @NotNull
    private TipoNegocio tipoNegocio;

    @Size(max = 2000)
    private String mensagem;

    @Size(max = 120)
    private String origem;

    private PrioridadeLead prioridade;
    private List<String> tags;
    private BigDecimal valorEstimado;
    private LocalDate dataPrevistaFechamento;

    /** Status inicial opcional. Se nulo, usa o status com {@code ehStatusInicial=true}. */
    private Long statusId;

    /** Atribuir ao criar (opcional). */
    private Long responsavelId;
}
