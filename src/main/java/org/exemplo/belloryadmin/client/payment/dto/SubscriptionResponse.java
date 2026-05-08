package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionResponse {
    private Long id;
    private Long companyId;
    private Long customerId;
    private Long planId;
    private String planName;
    private String asaasId;
    private PaymentBillingType billingType;
    private BigDecimal effectivePrice;
    private PaymentSubscriptionCycle cycle;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDate nextDueDate;
    private PaymentSubscriptionStatus status;
    private LocalDateTime trialEndDate;
    private Integer trialDaysAtCreation;
    private Boolean inTrial;
    private String couponCode;
    private BigDecimal couponDiscountAmount;
    private Integer couponUsesRemaining;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
