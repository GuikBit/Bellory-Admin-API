package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanChangePreviewResponse {
    private Long subscriptionId;
    private Long currentPlanId;
    private String currentPlanName;
    private BigDecimal currentPlanValue;
    private Long newPlanId;
    private String newPlanName;
    private BigDecimal newPlanValue;
    private String changeType;       // UPGRADE, DOWNGRADE, SIDEGRADE
    private String policy;           // IMMEDIATE_PRORATA, END_OF_CYCLE, IMMEDIATE_NO_PRORATA
    private BigDecimal delta;
    private BigDecimal prorationCredit;
    private BigDecimal prorationCharge;
}
