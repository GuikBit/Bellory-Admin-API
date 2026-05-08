package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponUsageResponse {
    private Long id;
    private Long couponId;
    private String couponCode;
    private Long customerId;
    private Long subscriptionId;
    private Long chargeId;
    private BigDecimal originalValue;
    private BigDecimal discountAmount;
    private BigDecimal finalValue;
    private String planCode;
    private String cycle;
    private LocalDateTime usedAt;
}
