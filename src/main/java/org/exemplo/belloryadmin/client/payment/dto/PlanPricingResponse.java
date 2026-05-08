package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanPricingResponse {
    private Long id;
    private String codigo;
    private String name;
    private BigDecimal precoMensal;
    private BigDecimal precoSemestral;
    private BigDecimal precoAnual;
    private BigDecimal descontoPercentualAnual;
    private PromoPricing promoMensal;
    private PromoPricing promoAnual;
    private List<PlanLimitDto> features;
    private List<PlanLimitDto> limits;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PromoPricing {
        private Boolean ativa;
        private BigDecimal preco;
        private String texto;
        private LocalDateTime validaAte;
    }
}
