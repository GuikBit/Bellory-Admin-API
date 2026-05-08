package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessSummary {
    private Integer activeSubscriptions;
    private Integer suspendedSubscriptions;
    private Integer overdueCharges;
    private BigDecimal totalOverdueValue;
    private Integer oldestOverdueDays;
    private BigDecimal creditBalance;
}
