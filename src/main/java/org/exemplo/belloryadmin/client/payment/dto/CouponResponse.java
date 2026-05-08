package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponResponse {
    private Long id;
    private Long companyId;
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
    private Integer usageCount;
    private String allowedPlans;
    private String allowedCustomers;
    private String allowedCycle;
    private Boolean active;
    private Boolean currentlyValid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
