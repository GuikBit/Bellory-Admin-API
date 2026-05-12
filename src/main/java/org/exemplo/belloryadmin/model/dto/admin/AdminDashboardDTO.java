package org.exemplo.belloryadmin.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard administrativo consolidado (v2).
 *
 * <p>Estrutura quebrada em blocos por dominio. Os campos podem vir {@code null}
 * quando a fonte estiver indisponivel (ex: bloco {@code planos} quando a Payment
 * API esta fora). O payload e cacheado no Redis por alguns minutos
 * ({@code admin.dashboard.cache-ttl-seconds}).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDashboardDTO {

    /** Momento em que este snapshot foi calculado (pre-cache). */
    private LocalDateTime geradoEm;

    private OrganizacoesBlock organizacoes;
    private AgendamentosBlock agendamentos;
    private ClientesBlock clientes;
    private FuncionariosBlock funcionarios;
    private ServicosBlock servicos;
    private InstanciasBlock instancias;
    private FinanceiroBlock financeiro;
    private PlanosBlock planos;
    private LeadsBlock leads;
    private TopOrganizacoes topOrganizacoes;
    private List<OrgLocationDTO> localizacoes;

    // ==================== ORGANIZACOES ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizacoesBlock {
        private Long total;
        private Long ativas;
        private Long inativas;
        private Long novasHoje;
        private Long novasUltimos7Dias;
        private Long novasMesAtual;
        private Long novasMesAnterior;
        private BigDecimal crescimentoMensalPercentual;
        /** Orgs ativas que ainda nao tem nenhum agendamento (onboarding incompleto). */
        private Long ativasSemAgendamentos;
        /** Orgs ativas sem nenhuma instancia WhatsApp. */
        private Long ativasSemInstancia;
        /** % de orgs ativas com pelo menos 1 agendamento. */
        private BigDecimal taxaAtivacao;
        private AdocaoFeatures adocaoFeatures;
        /** UF -> quantidade de orgs ativas. */
        private List<ContagemPorChave> porEstado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdocaoFeatures {
        private Long usaEcommerce;
        private Long usaGestaoProdutos;
        private Long usaPlanosParaClientes;
        private Long disparaNotificacoesPush;
    }

    // ==================== AGENDAMENTOS ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendamentosBlock {
        private Long total;
        private Long hoje;
        private Long ultimos7Dias;
        private Long mesAtual;
        private Long mesAnterior;
        private BigDecimal crescimentoMensalPercentual;
        /** status -> quantidade. */
        private Map<String, Long> porStatus;
        private BigDecimal taxaConclusao;
        private BigDecimal taxaCancelamento;
        private BigDecimal taxaNoShow;
        /** Ultimos 12 meses (por dtCriacao). */
        private List<SerieMensal> evolucaoMensal;
    }

    // ==================== CLIENTES ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientesBlock {
        private Long total;
        private Long ativos;
        private Long inativos;
        private Long cadastroIncompleto;
        private Long novosHoje;
        private Long novosUltimos7Dias;
        private Long novosMesAtual;
        private Double mediaPorOrganizacao;
        private List<SerieMensal> evolucaoMensal;
    }

    // ==================== FUNCIONARIOS ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FuncionariosBlock {
        private Long total;
        private Long ativos;
        private Long inativos;
        private Double mediaPorOrganizacao;
    }

    // ==================== SERVICOS ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicosBlock {
        private Long total;
        private Long ativos;
        private Long inativos;
        private BigDecimal precoMedio;
        private Double mediaPorOrganizacao;
    }

    // ==================== INSTANCIAS ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanciasBlock {
        private Long total;
        private Long conectadas;
        private Long desconectadas;
        /** InstanceStatus -> quantidade. */
        private Map<String, Long> porStatus;
        private Long organizacoesSemInstancia;
    }

    // ==================== FINANCEIRO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinanceiroBlock {
        private BigDecimal faturamentoTotal;
        private BigDecimal faturamentoHoje;
        private BigDecimal faturamentoMesAtual;
        private BigDecimal faturamentoMesAnterior;
        private BigDecimal crescimentoMensalPercentual;
        private BigDecimal ticketMedio;
        /** Soma das cobrancas PENDENTE + PARCIALMENTE_PAGO. */
        private BigDecimal valorPendente;
        /** Soma das cobrancas VENCIDA. */
        private BigDecimal valorVencido;
        /** % de cobrancas vencidas sobre o total. */
        private BigDecimal taxaInadimplencia;
        private Long totalCobrancas;
        private Map<String, Long> cobrancasPorStatus;
        private Map<String, Long> cobrancasPorTipo;
        private Long totalPagamentos;
        private Map<String, Long> pagamentosPorStatus;
        /** Ultimos 12 meses (por dtPagamento, status CONFIRMADO). */
        private List<SerieMensalValor> evolucaoMensal;
    }

    // ==================== PLANOS (Payment API) ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanosBlock {
        /** false quando a Payment API esta indisponivel — os demais campos vem nulos/zerados. */
        private boolean disponivel;
        /** Assinaturas locais vinculadas a Payment API. */
        private Long totalAssinaturas;
        private Long organizacoesSemAssinatura;
        private Long assinaturasAtivas;
        private Long assinaturasEmTrial;
        private Long assinaturasCanceladas;
        /** MRR aproximado: soma do effectivePrice das assinaturas ativas com ciclo mensal. */
        private BigDecimal mrrEstimado;
        private List<PlanoDistribuicao> distribuicao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanoDistribuicao {
        private Long planId;
        private String codigo;
        private String nome;
        private Boolean gratuito;
        private BigDecimal precoMensal;
        private Long quantidade;
    }

    // ==================== LEADS ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeadsBlock {
        private Long total;
        private Long novosHoje;
        private Long novosUltimos7Dias;
        private Long novosMesAtual;
        /** Leads em colunas marcadas como status final do kanban. */
        private Long emStatusFinal;
        /** Nome da coluna do kanban -> quantidade. */
        private Map<String, Long> porStatus;
        private Map<String, Long> porTipoNegocio;
        private Map<String, Long> porPrioridade;
        private Map<String, Long> porOrigem;
        private BigDecimal valorEstimadoPipeline;
    }

    // ==================== TOP ORGANIZACOES ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopOrganizacoes {
        private List<OrgRanking> porFaturamento;
        private List<OrgRanking> porAgendamentos;
        private List<OrgRanking> porClientes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgRanking {
        private Long organizacaoId;
        private String nomeFantasia;
        /** Preenchido no ranking por faturamento. */
        private BigDecimal valor;
        /** Preenchido nos rankings por contagem (agendamentos / clientes). */
        private Long quantidade;
    }

    // ==================== UTILITARIOS ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContagemPorChave {
        private String chave;
        private Long quantidade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieMensal {
        private String mes;        // YYYY-MM
        private Long quantidade;
        private Long acumulado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerieMensalValor {
        private String mes;        // YYYY-MM
        private BigDecimal valor;
        private Long quantidade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgLocationDTO {
        private String cidade;
        private String estado;
        private Double latitude;
        private Double longitude;
        private Long quantidade;
    }
}
