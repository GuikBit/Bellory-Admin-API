package org.exemplo.belloryadmin.model.dto.lead.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.model.entity.lead.enums.PrioridadeLead;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoNegocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Card resumido do lead (kanban + listagem).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadListDTO {
    private UUID id;
    private String nome;
    private String email;
    private String telefone;
    private TipoNegocio tipoNegocio;
    private LeadStatusDTO status;
    private PrioridadeLead prioridade;
    private List<String> tags;
    private BigDecimal valorEstimado;
    private LocalDate dataPrevistaFechamento;
    private ResponsavelMiniDTO responsavel;
    private String origem;
    private LocalDateTime dtCriacao;
}
