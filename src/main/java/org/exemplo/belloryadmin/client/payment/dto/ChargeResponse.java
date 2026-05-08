package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargeResponse {
    private Long id;
    private Long companyId;
    private Long customerId;
    private Long subscriptionId;
    private Long installmentId;
    private String asaasId;
    private String billingType;
    private BigDecimal value;
    private LocalDate dueDate;
    private String status;
    private String origin;
    private String externalReference;
    private String pixQrcode;
    private String pixCopyPaste;
    private String boletoUrl;
    private String invoiceUrl;
    private Integer installmentNumber;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal originalValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
