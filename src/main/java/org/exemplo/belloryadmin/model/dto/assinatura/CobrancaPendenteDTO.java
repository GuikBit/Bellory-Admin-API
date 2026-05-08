package org.exemplo.belloryadmin.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CobrancaPendenteDTO {

    private Long id;
    private Long subscriptionId;

    private BigDecimal valor;
    private BigDecimal valorOriginal;
    private LocalDate dtVencimento;

    private String status;
    private String billingType;
    private Integer installmentNumber;

    // Links/dados de pagamento
    private String pixQrcode;
    private String pixCopyPaste;
    private String boletoUrl;
    private String invoiceUrl;
}
