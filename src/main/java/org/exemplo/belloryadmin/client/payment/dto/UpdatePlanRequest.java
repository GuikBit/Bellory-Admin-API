package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePlanRequest {
    private String name;
    private String description;
    private BigDecimal precoMensal;
    private BigDecimal precoAnual;
    private BigDecimal precoSemestral;
    private BigDecimal descontoPercentualAnual;

    private Boolean promoMensalAtiva;
    private BigDecimal promoMensalPreco;
    private String promoMensalTexto;
    private LocalDateTime promoMensalInicio;
    private LocalDateTime promoMensalFim;

    private Boolean promoAnualAtiva;
    private BigDecimal promoAnualPreco;
    private String promoAnualTexto;
    private LocalDateTime promoAnualInicio;
    private LocalDateTime promoAnualFim;

    private Integer trialDays;
    private BigDecimal setupFee;
    private Integer tierOrder;
    private Boolean isFree;
    private List<PlanLimitDto> limits;
    private List<PlanLimitDto> features;
}
