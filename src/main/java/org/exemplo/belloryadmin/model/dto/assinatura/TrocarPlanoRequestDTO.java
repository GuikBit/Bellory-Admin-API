package org.exemplo.belloryadmin.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.client.payment.dto.CreditCardHolderInfo;
import org.exemplo.belloryadmin.client.payment.dto.CreditCardInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrocarPlanoRequestDTO {
    private Long newPlanId;
    private String billingType;                    // PIX, BOLETO, CREDIT_CARD, UNDEFINED
    private CreditCardInfo creditCard;             // dados brutos do cartão (opcional)
    private CreditCardHolderInfo creditCardHolderInfo; // titular do cartão (opcional)
    private String creditCardToken;                // token já salvo (opcional)
}
