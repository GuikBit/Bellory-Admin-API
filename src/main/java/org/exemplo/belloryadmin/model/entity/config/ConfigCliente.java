package org.exemplo.belloryadmin.model.entity.config;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigCliente {
    @Column(name = "precisa_cadastro_agendar")
    private Boolean precisaCadastroAgendar = false;

    @Column(name = "programa_fidelidade")
    private Boolean programaFidelidade = false;

    @Column(name = "valor_gasto_um_ponto", precision = 10, scale = 2)
    private BigDecimal valorGastoUmPonto;
}

