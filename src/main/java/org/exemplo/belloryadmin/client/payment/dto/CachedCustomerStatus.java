package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Snapshot consolidado de um customer na Payment API, montado pela AssinaturaCacheService
 * a partir de /customers/{id}/access-status + /subscriptions + /plans/{id}/limits + features.
 * Tudo que o Bellory precisa em runtime (interceptor, login, LimiteValidator) vem daqui.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CachedCustomerStatus {

    private Long customerId;
    private String customerName;

    private boolean allowed;
    private List<String> reasons;
    private String customBlockMessage;
    private AccessSummary summary;

    private Long subscriptionId;
    private PaymentSubscriptionStatus subscriptionStatus;
    private PaymentSubscriptionCycle cycle;
    private PaymentBillingType billingType;
    private LocalDate nextDueDate;
    private BigDecimal effectivePrice;

    private Boolean inTrial;
    private LocalDateTime trialEndDate;

    private Long planId;
    private String planCodigo;
    private String planName;
    private boolean planIsFree;
    private List<PlanLimitDto> limits;
    private List<PlanLimitDto> features;

    private List<ChargeResponse> cobrancasPendentes;

    private LocalDateTime fetchedAt;
}
