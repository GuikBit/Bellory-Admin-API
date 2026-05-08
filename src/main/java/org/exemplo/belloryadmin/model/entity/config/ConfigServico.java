package org.exemplo.belloryadmin.model.entity.config;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigServico {
    @Column(name = "mostrar_valor_agendamento")
    private Boolean mostrarValorAgendamento = false;

    @Column(name = "unico_servico_agendamento")
    private Boolean unicoServicoAgendamento = false;

    @Column(name = "mostrar_avaliacao")
    private Boolean mostrarAvaliacao = false;
}
