package org.exemplo.belloryadmin.model.dto.assinatura;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.client.payment.dto.ChargeResponse;
import org.exemplo.belloryadmin.client.payment.dto.PlanResponse;
import org.exemplo.belloryadmin.client.payment.dto.SubscriptionResponse;

import java.util.List;

/**
 * Agregado com os 3 JSONs brutos da Payment API (assinatura + plano + cobrancas)
 * da organizacao logada. Serve para telas de detalhes/financeiro que precisam do
 * contexto completo em uma unica chamada ao Bellory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssinaturaCompletaDTO {
    private SubscriptionResponse assinatura;
    private PlanResponse plano;
    private List<ChargeResponse> cobrancas;
}
