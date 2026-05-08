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
 * Detalhe completo do lead — usado quando o admin clica num card.
 * Atividades sao retornadas via endpoint separado para suportar filtros.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadDetailDTO {
    private UUID id;
    private LeadStatusDTO status;
    private String nome;
    private String email;
    private String telefone;
    private TipoNegocio tipoNegocio;
    private String mensagem;
    private String origem;
    private PrioridadeLead prioridade;
    private List<String> tags;
    private BigDecimal valorEstimado;
    private LocalDate dataPrevistaFechamento;
    private ResponsavelMiniDTO responsavel;
    private boolean turnstileOk;
    private Integer fillTimeMs;
    private String policyVersion;
    private LocalDateTime dtCriacao;
    private LocalDateTime dtAtualizacao;
}
