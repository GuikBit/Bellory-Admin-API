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
public class ConfigColaborador {
    @Column(name = "selecionar_colaborador_agendamento")
    private Boolean selecionarColaboradorAgendamento = false;

    @Column(name = "mostrar_notas_colaborador")
    private Boolean mostrarNotasComentarioColaborador = false;

    @Column(name = "comissao_padrao")
    private Boolean comissaoPadrao = false;
}
