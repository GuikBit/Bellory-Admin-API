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
public class UpdateCouponRequest {
    private String description;
    private BigDecimal discountValue;
    private String applicationType;       // FIRST_CHARGE, RECURRING
    private Integer recurrenceMonths;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer maxUses;
    private Integer maxUsesPerCustomer;
    private String allowedPlans;
    private String allowedCustomers;
    private String allowedCycle;
}
