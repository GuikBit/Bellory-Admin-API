package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanChangeResponse {
    private Long id;
    private Long subscriptionId;
    private Long previousPlanId;
    private String previousPlanName;
    private Long requestedPlanId;
    private String requestedPlanName;
    private String changeType;       // UPGRADE, DOWNGRADE, SIDEGRADE
    private String policy;           // IMMEDIATE_PRORATA, END_OF_CYCLE, IMMEDIATE_NO_PRORATA
    private BigDecimal deltaAmount;
    private BigDecimal prorationCredit;
    private BigDecimal prorationCharge;
    private String status;           // COMPLETED, SCHEDULED, FAILED
    private Long chargeId;
    private Long creditLedgerId;
    private LocalDateTime scheduledFor;
    private LocalDateTime effectiveAt;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String failureReason;
}
