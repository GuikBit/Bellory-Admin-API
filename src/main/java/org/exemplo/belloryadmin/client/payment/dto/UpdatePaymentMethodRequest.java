package org.exemplo.belloryadmin.client.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePaymentMethodRequest {
    private PaymentBillingType billingType;
    private CreditCardInfo creditCard;
    private CreditCardHolderInfo creditCardHolderInfo;
    private String creditCardToken;
    private String remoteIp;
}
