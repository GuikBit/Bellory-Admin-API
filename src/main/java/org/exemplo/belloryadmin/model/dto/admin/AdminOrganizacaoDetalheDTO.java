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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detalhe completo de uma organizacao/cliente para o painel admin (v2).
 *
 * <p>As metricas planas antigas foram reorganizadas em blocos por dominio
 * ({@code engajamento}, {@code onboarding}, {@code agendamentos}, {@code clientes},
 * {@code funcionarios}, {@code servicos}, {@code financeiro}). Os dados de
 * assinatura/plano vem da Payment API (fail-safe: {@code null} se indisponivel).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminOrganizacaoDetalheDTO {

    /** Momento em que este detalhe foi montado. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime geradoEm;

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

    // ── Endereco / Redes ──
    private EnderecoDTO endereco;
    private RedesSociaisDTO redesSociais;

    // ── Payment API (fail-safe) ──
    private Long paymentApiCustomerId;
    private Long paymentApiSubscriptionId;
    private SubscriptionResponse assinaturaAtiva;
    private AccessStatusResponse accessStatus;
    private PlanResponse planoDetalhado;

    // ── Configuracoes do sistema ──
    private ConfigSistemaDTO configSistema;

    // ── Instancias WhatsApp ──
    private List<InstanciaResumoDTO> instancias;

    // ── Blocos de desempenho / uso ──
    private EngajamentoDTO engajamento;
    private OnboardingDTO onboarding;
    private AgendamentosDetalheDTO agendamentos;
    private ClientesDetalheDTO clientes;
    private FuncionariosDetalheDTO funcionarios;
    private ServicosDetalheDTO servicos;
    private FinanceiroDetalheDTO financeiro;

    // ==================== INNER DTOs ====================

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
    public static class InstanciaResumoDTO {
        private Long id;
        private String instanceName;
        private String instanceId;
        private String status;
        private Boolean ativo;
    }

    // ---------- Engajamento ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngajamentoDTO {
        /** Idade da conta em dias (desde dtCadastro). */
        private Long diasDesdeCadastro;
        /** Classificacao derivada: NOVO | ATIVO | EM_RISCO | INATIVO. */
        private String statusUso;
        /** Pontuacao 0-100 composta (atividade recente + instancia conectada + onboarding + faturamento no mes). */
        private Integer healthScore;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime ultimaAtividadeEm;
        /** Dias desde a ultima atividade (agendamento criado / cliente cadastrado). null se nunca houve atividade. */
        private Long diasSemAtividade;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime ultimoAgendamentoCriadoEm;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime ultimoAgendamentoRealizadoEm;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime proximoAgendamentoEm;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime ultimoClienteCadastradoEm;

        private Long agendamentosUltimos7Dias;
        private Long agendamentosUltimos30Dias;
        private Long clientesNovosUltimos30Dias;

        /** True se ao menos uma instancia esta com status CONNECTED. */
        private Boolean instanciaConectada;
    }

    // ---------- Onboarding ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingDTO {
        private Boolean temFuncionario;
        private Boolean temServico;
        private Boolean temCliente;
        private Boolean temInstanciaConectada;
        private Boolean temAgendamento;
        private Boolean configPreenchida;
        private Boolean temAssinatura;
        private Integer itensConcluidos;
        private Integer totalItens;
        private Integer percentualConcluido;
    }

    // ---------- Agendamentos ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendamentosDetalheDTO {
        private Long total;
        private Long noMes;
        private Long futuros;
        /** status -> quantidade (apenas status com contagem > 0). */
        private Map<String, Long> porStatus;
        private BigDecimal taxaConclusao;
        private BigDecimal taxaCancelamento;
        private BigDecimal taxaNoShow;
        /** Ultimos 12 meses (por dtCriacao). */
        private List<SerieMensalAgendamento> evolucaoMensal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieMensalAgendamento {
        private String mes;        // YYYY-MM
        private Long total;
        private Long concluidos;
        private Long cancelados;
    }

    // ---------- Clientes ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientesDetalheDTO {
        private Long total;
        private Long ativos;
        private Long inativos;
        private Long cadastroIncompleto;
        private Long novosNoMes;
        private Long novosUltimos30Dias;
        private Double mediaAgendamentosPorCliente;
        /** Ultimos 12 meses (por dtCriacao). */
        private List<SerieMensalCliente> evolucaoMensal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieMensalCliente {
        private String mes;        // YYYY-MM
        private Long novos;
        private Long acumulado;
    }

    // ---------- Funcionarios ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FuncionariosDetalheDTO {
        private Long total;
        private Long ativos;
        private Long inativos;
        /** situacao -> quantidade. */
        private Map<String, Long> porSituacao;
    }

    // ---------- Servicos ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicosDetalheDTO {
        private Long total;
        private Long ativos;
        private Long inativos;
        private BigDecimal precoMedio;
        private List<ContagemPorCategoria> porCategoria;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContagemPorCategoria {
        private String categoria;
        private Long quantidade;
    }

    // ---------- Financeiro ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinanceiroDetalheDTO {
        private BigDecimal faturamentoTotal;
        private BigDecimal faturamentoMes;
        private BigDecimal ticketMedio;
        /** Soma das cobrancas PENDENTE + PARCIALMENTE_PAGO. */
        private BigDecimal valorPendente;
        /** Soma das cobrancas VENCIDA. */
        private BigDecimal valorVencido;
        private Long totalCobrancas;
        private Map<String, Long> cobrancasPorStatus;
        private Map<String, Long> cobrancasPorTipo;
        private Map<String, Long> pagamentosPorStatus;
        /** Ultimos 12 meses (por dtPagamento, status CONFIRMADO). */
        private List<SerieMensalFaturamento> evolucaoMensal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieMensalFaturamento {
        private String mes;        // YYYY-MM
        private BigDecimal valor;
        private Long quantidade;
    }

    // ---------- ConfigSistema (expandido) ----------

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
        private Boolean mostrarAgendamentoCancelado;
        private Integer tempoCancelamentoCliente;
        private Boolean aprovarAgendamento;
        private Boolean aprovarAgendamentoAgente;
        private Boolean ocultarFimSemana;
        private Boolean ocultarDomingo;
        private Boolean cobrarSinal;
        private Integer porcentSinal;
        private Boolean cobrarSinalAgente;
        private Integer porcentSinalAgente;
        private String modoVizualizacao;

        // Fila de espera
        private Boolean usarFilaEspera;
        private Integer filaMaxCascata;
        private Integer filaTimeoutMinutos;
        private Integer filaAntecedenciaHoras;

        // Servico
        private Boolean mostrarValorAgendamento;
        private Boolean unicoServicoAgendamento;
        private Boolean mostrarAvaliacao;

        // Cliente
        private Boolean precisaCadastroAgendar;
        private Boolean programaFidelidade;
        private BigDecimal valorGastoUmPonto;

        // Colaborador
        private Boolean selecionarColaboradorAgendamento;
        private Boolean mostrarNotasComentarioColaborador;
        private Boolean comissaoPadrao;

        // Notificacao
        private Boolean enviarConfirmacaoWhatsapp;
        private Boolean enviarLembreteWhatsapp;
        private Boolean enviarLembreteSms;
        private Boolean enviarLembreteEmail;
        private Boolean enviarConfirmacaoForaHorario;
        private Integer tempoParaConfirmacao;
        private Integer tempoLembretePosConfirmacao;
        private String mensagemTemplateConfirmacao;
        private String mensagemTemplateLembrete;
    }
}
