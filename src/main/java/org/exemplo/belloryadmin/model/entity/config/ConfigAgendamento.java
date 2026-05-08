package org.exemplo.belloryadmin.model.entity.config;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigAgendamento {

    @Column(name = "tolerancia_agendamento")
    private Integer toleranciaAgendamento = 15;

    @Column(name = "min_dias_agendamento")
    private Integer minDiasAgendamento = 1;

    @Column(name = "max_dias_agendamento")
    private Integer maxDiasAgendamento = 90;

    @Column(name = "cancelamento_cliente")
    private Boolean cancelamentoCliente = false;

    @Column(name="mostrar_agendamento_cancelado")
    private Boolean mostrarAgendamentoCancelado = false;

    @Column(name = "tempo_cancelamento_cliente")
    private Integer tempoCancelamentoCliente;

    @Column(name = "aprovar_agendamento")
    private Boolean aprovarAgendamento = true;

    @Column(name = "aprovar_agendamento_agente")
    private Boolean aprovarAgendamentoAgente = false;

    @Column(name = "ocultar_fimsemana")
    private Boolean ocultarFimSemana = false;

    @Column(name = "ocultar_domingo")
    private Boolean ocultarDomingo = false;

    @Column(name = "cobrar_sinal")
    private Boolean cobrarSinal = false;

    @Column(name = "porcent_sinal")
    private Integer porcentSinal;

    @Column(name = "cobrar_sinal_agente")
    private Boolean cobrarSinalAgente = true;

    @Column(name = "porcent_sinal_agente")
    private Integer porcentSinalAgente = 50;

    @Column(name= "modo_vizualizacao")
    private String modoVizualizacao = "calendar";

    // ===== Fila de Espera =====
    @Column(name = "usar_fila_espera")
    private Boolean usarFilaEspera = false;

    @Column(name = "fila_max_cascata")
    private Integer filaMaxCascata = 5;

    @Column(name = "fila_timeout_minutos")
    private Integer filaTimeoutMinutos = 30;

    @Column(name = "fila_antecedencia_horas")
    private Integer filaAntecedenciaHoras = 3;

}
