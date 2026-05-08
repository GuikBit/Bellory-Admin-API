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
public class ConfigNotificacao {

    @Column(name = "enviar_confirmacao_whatsapp")
    private Boolean enviarConfirmacaoWhatsapp = false;

    @Column(name = "enviar_lembrete_whatsapp")
    private Boolean enviarLembreteWhatsapp = false;

    @Column(name = "enviar_lembrete_sms")
    private Boolean enviarLembreteSMS = false;

    @Column(name = "enviar_lembrete_email")
    private Boolean enviarLembreteEmail = false;

    @Column(name = "enviar_confitmacao_fora_horario")
    private Boolean enviarConfirmacaoForaHorario = false;

    @Column(name = "tempo_confirmacao")
    private Integer tempoParaConfirmacao = 24;

    @Column(name = "tempo_lembrete_pos_confirmacao")
    private Integer tempoLembretePosConfirmacao = 2;

    @Column(name = "mensagem_template_confirmacao")
    private String mensagemTemplateConfirmacao =
            "Olá *{{nome_cliente}}*! \uD83D\uDC4B\n" +
            "\n" +
            "✅ Seu agendamento está *aguardando confirmação*!\n" +
            "\n" +
            "\uD83D\uDCCB *Detalhes do agendamento:*\n" +
            "- Serviço: {{servico}}\n" +
            "- Data: {{data_agendamento}}\n" +
            "- Horário: {{hora_agendamento}}\n" +
            "- Profissional: {{profissional}}\n" +
            "- Local: {{local}}\n" +
            "- Valor: {{valor}}\n" +
            "\n" +
            "\uD83D\uDCCD _{{nome_empresa}}_\n" +
            "\n" +
            "Podemos confirmar? Digite: \uD83D\uDE0A\n" +
            "*Sim* para confirmar ✅\n" +
            "*Não* para cancelar ❌\n" +
            "*Remarcar* para reagendar o serviço \uD83D\uDCC5\n" +
            "\n" +
            "_Estamos aguardando o seu retorno._";

    @Column(name = "mensagem_template_lembrete")
    private String mensagemTemplateLembrete =
            "Olá *{{nome_cliente}}*! \uD83D\uDD14\n" +
            "\n" +
            "⏰ *Lembrete do seu agendamento:*\n" +
            "\n" +
            "Você tem um horário marcado em breve!\n" +
            "\n" +
            "\uD83D\uDCCB *Detalhes:*\n" +
            "- Serviço: {{servico}}\n" +
            "- Data: {{data_agendamento}}\n" +
            "- Horário: {{hora_agendamento}}\n" +
            "- Profissional: {{profissional}}\n" +
            "- Local: {{local}}\n" +
            "\n" +
            "\uD83D\uDCB0 Valor: {{valor}}\n" +
            "\n" +
            "\uD83D\uDCCD _{{nome_empresa}}_\n" +
            "\n" +
            "~Não se atrase!~ ⏱\uFE0F Chegue com alguns minutos de antecedência.\n" +
            "\n" +
            "Até logo! \uD83D\uDE0A";


}
