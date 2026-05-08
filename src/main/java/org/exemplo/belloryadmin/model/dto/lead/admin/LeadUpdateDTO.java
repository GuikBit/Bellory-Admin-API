package org.exemplo.belloryadmin.model.dto.lead.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
 * Atualizar campos do lead. Todos os campos sao opcionais — so os
 * informados sao atualizados (PATCH semantics).
 *
 * <p>Mudanca de status e atribuicao ficam em endpoints proprios
 * (PATCH /status, PATCH /assign) para registrar atividades automaticas.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadUpdateDTO {

    @Size(min = 2, max = 100)
    private String nome;

    @Email
    @Size(max = 160)
    private String email;

    @Size(min = 10, max = 20)
    @Pattern(regexp = "^[\\d\\s()+\\-]+$")
    private String telefone;

    private TipoNegocio tipoNegocio;

    @Size(max = 2000)
    private String mensagem;

    @Size(max = 120)
    private String origem;

    private PrioridadeLead prioridade;
    private List<String> tags;
    private BigDecimal valorEstimado;
    private LocalDate dataPrevistaFechamento;
}
