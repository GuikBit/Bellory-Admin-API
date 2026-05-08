package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidateCouponRequest {
    private String couponCode;
    private String scope;        // SUBSCRIPTION, CHARGE
    private String planCode;
    private String cycle;        // MONTHLY, QUARTERLY, YEARLY
    private BigDecimal value;
    private Long customerId;
}
