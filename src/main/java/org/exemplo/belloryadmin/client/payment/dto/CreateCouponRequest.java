package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateCouponRequest {
    private String code;
    private String description;
    private String discountType;          // PERCENTAGE, FIXED_AMOUNT
    private BigDecimal discountValue;
    private String scope;                 // SUBSCRIPTION, CHARGE
    private String applicationType;       // FIRST_CHARGE, RECURRING
    private Integer recurrenceMonths;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer maxUses;
    private Integer maxUsesPerCustomer;
    private String allowedPlans;          // CSV de codigos de plano
    private String allowedCustomers;      // CSV de IDs de customer
    private String allowedCycle;          // MONTHLY, QUARTERLY, YEARLY
}
