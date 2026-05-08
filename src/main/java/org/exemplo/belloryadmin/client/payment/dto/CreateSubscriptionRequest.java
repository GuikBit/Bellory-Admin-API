package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateSubscriptionRequest {
    private Long customerId;
    private Long planId;
    private PaymentBillingType billingType;
    private PaymentSubscriptionCycle cycle;
    private LocalDate nextDueDate;
    private String description;
    private String externalReference;
    private CreditCardInfo creditCard;
    private CreditCardHolderInfo creditCardHolderInfo;
    private String creditCardToken;
    private String remoteIp;
    private String couponCode;
}
