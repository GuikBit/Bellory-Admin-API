package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanLimitCheckResponse {
    private String key;
    private String label;
    private PaymentPlanLimitType type;
    private Long value;
    private Boolean enabled;
    private boolean unlimited;
    private boolean found;
    private Integer currentUsage;
    private Boolean allowed;
}
