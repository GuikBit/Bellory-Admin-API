package org.exemplo.belloryadmin.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exemplo.belloryadmin.client.payment.dto.AccessStatusResponse;
import org.exemplo.belloryadmin.client.payment.dto.PlanResponse;
import org.exemplo.belloryadmin.client.payment.dto.SubscriptionResponse;
import org.exemplo.belloryadmin.model.entity.config.ConfigSistema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminOrganizacaoDetalheDTO {

    // ── Dados basicos ──
    private Long id;
    private String nomeFantasia;
    private String razaoSocial;
    private String cnpj;
    private String inscricaoEstadual;
    private String publicoAlvo;
    private String emailPrincipal;
    private String telefone1;
    private String telefone2;
    private String whatsapp;
    private String slug;
    private Boolean ativo;
    private String logoUrl;
    private String bannerUrl;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtCadastro;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtAtualizacao;

    // ── Responsavel ──
    private String responsavelNome;
    private String responsavelEmail;
    private String responsavelTelefone;

    // ── Endereco ──
    private EnderecoDTO endereco;

    // ── Redes Sociais ──
    private RedesSociaisDTO redesSociais;

    // ── IDs Payment API ──
    private Long paymentApiCustomerId;
    private Long paymentApiSubscriptionId;

    // ── Assinatura e Plano (Payment API, fail-safe) ──
    private SubscriptionResponse assinaturaAtiva;
    private AccessStatusResponse accessStatus;
    private PlanResponse planoDetalhado;

    // ── Configuracoes do Sistema ──
    private ConfigSistemaDTO configSistema;

    // ── Metricas (Bellory DB) ──
    private MetricasOrganizacao metricas;

    // ── Instancias ──
    private List<InstanciaResumoDTO> instancias;

    // ── Inner DTOs ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnderecoDTO {
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String uf;
        private String cep;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedesSociaisDTO {
        private String instagram;
        private String facebook;
        private String whatsapp;
        private String linkedin;
        private String youtube;
        private String site;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasOrganizacao {
        private Long totalAgendamentos;
        private Long agendamentosNoMes;
        private Long agendamentosConcluidos;
        private Long agendamentosCancelados;
        private Long agendamentosPendentes;
        private Long totalClientes;
        private Long clientesAtivos;
        private Long totalFuncionarios;
        private Long funcionariosAtivos;
        private Long totalServicos;
        private Long servicosAtivos;
        private BigDecimal faturamentoTotal;
        private BigDecimal faturamentoMes;
        private Long totalCobrancas;
        private Long cobrancasPagas;
        private Long cobrancasPendentes;
        private Long cobrancasVencidas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanciaResumoDTO {
        private Long id;
        private String instanceName;
        private String instanceId;
        private String status;
        private Boolean ativo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigSistemaDTO {
        // Modulos
        private Boolean usaEcommerce;
        private Boolean usaGestaoProdutos;
        private Boolean usaPlanosParaClientes;
        private Boolean disparaNotificacoesPush;
        private String urlAcesso;

        // Agendamento
        private Integer toleranciaAgendamento;
        private Integer minDiasAgendamento;
        private Integer maxDiasAgendamento;
        private Boolean cancelamentoCliente;
        private Boolean aprovarAgendamento;
        private Boolean ocultarFimSemana;
        private Boolean cobrarSinal;
        private Integer porcentSinal;
        private String modoVizualizacao;

        // Servico
        private Boolean mostrarValorAgendamento;
        private Boolean unicoServicoAgendamento;
        private Boolean mostrarAvaliacao;

        // Cliente
        private Boolean precisaCadastroAgendar;
        private Boolean programaFidelidade;

        // Colaborador
        private Boolean selecionarColaboradorAgendamento;
        private Boolean comissaoPadrao;

        // Notificacao
        private Boolean enviarConfirmacaoWhatsapp;
        private Boolean enviarLembreteWhatsapp;
        private Boolean enviarLembreteEmail;
    }
}
