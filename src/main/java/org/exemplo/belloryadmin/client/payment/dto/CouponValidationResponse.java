package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponValidationResponse {
    private boolean valid;
    private String message;
    private String discountType;
    private String applicationType;
    private BigDecimal percentualDiscount;
    private BigDecimal discountAmount;
    private BigDecimal originalValue;
    private BigDecimal finalValue;
}
