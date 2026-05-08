package org.exemplo.belloryadmin.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.exemplo.belloryadmin.client.payment.dto.PlanLimitDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssinaturaStatusDTO {
    // Status principal
    private boolean bloqueado;
    private String statusAssinatura;
    private String situacao;          // SituacaoAssinatura - enum semantico para o frontend
    private String mensagem;

    // Plano
    private String planoCodigo;
    private String planoNome;
    private boolean planoGratuito;

    // Limites e features do plano (para controle de acesso no frontend)
    private List<PlanLimitDto> planoLimites;
    private List<PlanLimitDto> planoFeatures;

    // Trial
    private Integer diasRestantesTrial;
    private LocalDate dtFimTrial;

    // Cobranca pendente
    private Boolean temCobrancaPendente;
    private BigDecimal valorPendente;
    private LocalDate dtVencimentoProximaCobranca;
    private List<CobrancaPendenteDTO> cobrancasPendentes;

    // Cancelamento
    private LocalDate dtAcessoAte;

    // Ciclo
    private String cicloCobranca;
    private LocalDate dtProximoVencimento;

    // Troca de plano agendada
    private String planoAgendadoCodigo;
    private String planoAgendadoNome;
    private String cicloAgendado;
}
