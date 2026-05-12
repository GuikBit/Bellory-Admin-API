package org.exemplo.belloryadmin.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.PaymentApiClient;
import org.exemplo.belloryadmin.client.payment.dto.PaymentSubscriptionCycle;
import org.exemplo.belloryadmin.client.payment.dto.PaymentSubscriptionStatus;
import org.exemplo.belloryadmin.client.payment.dto.PlanResponse;
import org.exemplo.belloryadmin.client.payment.dto.SubscriptionResponse;
import org.exemplo.belloryadmin.model.dto.admin.AdminDashboardDTO;
import org.exemplo.belloryadmin.model.dto.admin.AdminDashboardDTO.*;
import org.exemplo.belloryadmin.model.repository.admin.AdminQueryRepository;
import org.exemplo.belloryadmin.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.belloryadmin.model.repository.lead.LeadRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final String CACHE_KEY = "admin:dashboard:v2";
    private static final int TOP_LIMIT = 10;

    private final AdminQueryRepository adminQueryRepository;
    private final LeadRepository leadRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PaymentApiClient paymentApiClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration cacheTtl;

    public AdminDashboardService(
            AdminQueryRepository adminQueryRepository,
            LeadRepository leadRepository,
            AssinaturaRepository assinaturaRepository,
            PaymentApiClient paymentApiClient,
            StringRedisTemplate paymentApiRedisTemplate,
            @Qualifier("paymentApiObjectMapper") ObjectMapper paymentApiObjectMapper,
            @Value("${admin.dashboard.cache-ttl-seconds:600}") long cacheTtlSeconds) {
        this.adminQueryRepository = adminQueryRepository;
        this.leadRepository = leadRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.paymentApiClient = paymentApiClient;
        this.redis = paymentApiRedisTemplate;
        this.mapper = paymentApiObjectMapper;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public AdminDashboardDTO getDashboard() {
        AdminDashboardDTO cached = readCache();
        if (cached != null) return cached;

        AdminDashboardDTO dto = computeDashboard();
        writeCache(dto);
        return dto;
    }

    /** Forca recalculo, ignorando e sobrescrevendo o cache. */
    public AdminDashboardDTO refreshDashboard() {
        AdminDashboardDTO dto = computeDashboard();
        writeCache(dto);
        return dto;
    }

    // ==================== CALCULO ====================

    private AdminDashboardDTO computeDashboard() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioHoje = agora.toLocalDate().atStartOfDay();
        LocalDateTime inicio7Dias = agora.minusDays(7);
        LocalDateTime inicioMes = agora.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime inicioMesAnterior = inicioMes.minusMonths(1);
        LocalDateTime fimMesAnterior = inicioMes.minusSeconds(1);
        LocalDateTime inicio12Meses = agora.minusMonths(12).withDayOfMonth(1).with(LocalTime.MIN);

        return AdminDashboardDTO.builder()
                .geradoEm(agora)
                .organizacoes(buildOrganizacoes(agora, inicioHoje, inicio7Dias, inicioMes, inicioMesAnterior, fimMesAnterior))
                .agendamentos(buildAgendamentos(agora, inicioHoje, inicio7Dias, inicioMes, inicioMesAnterior, fimMesAnterior, inicio12Meses))
                .clientes(buildClientes(inicioHoje, inicio7Dias, inicioMes, inicio12Meses))
                .funcionarios(buildFuncionarios())
                .servicos(buildServicos())
                .instancias(buildInstancias())
                .financeiro(buildFinanceiro(agora, inicioHoje, inicioMes, inicioMesAnterior, fimMesAnterior, inicio12Meses))
                .planos(buildPlanos())
                .leads(buildLeads(inicioHoje, inicio7Dias, inicioMes, agora))
                .topOrganizacoes(buildTopOrganizacoes())
                .localizacoes(buildLocalizacoes())
                .build();
    }

    // ---------- Organizacoes ----------

    private OrganizacoesBlock buildOrganizacoes(LocalDateTime agora, LocalDateTime inicioHoje, LocalDateTime inicio7Dias,
                                                LocalDateTime inicioMes, LocalDateTime inicioMesAnterior, LocalDateTime fimMesAnterior) {
        long total = adminQueryRepository.count();
        long ativas = nz(adminQueryRepository.countOrganizacoesAtivas());
        long inativas = nz(adminQueryRepository.countOrganizacoesInativas());
        long novasMesAtual = nz(adminQueryRepository.countOrganizacoesNoPeriodo(inicioMes, agora));
        long novasMesAnterior = nz(adminQueryRepository.countOrganizacoesNoPeriodo(inicioMesAnterior, fimMesAnterior));
        long semAgendamento = nz(adminQueryRepository.countOrganizacoesAtivasSemAgendamento());

        long[] features = firstRowAsLongs(adminQueryRepository.countAdocaoFeaturesOrganizacoesAtivas(), 4);

        List<ContagemPorChave> porEstado = new ArrayList<>();
        for (Object[] row : adminQueryRepository.countOrganizacoesAtivasPorUf()) {
            porEstado.add(ContagemPorChave.builder()
                    .chave((String) row[0])
                    .quantidade(asLong(row[1]))
                    .build());
        }

        return OrganizacoesBlock.builder()
                .total(total)
                .ativas(ativas)
                .inativas(inativas)
                .novasHoje(nz(adminQueryRepository.countOrganizacoesNoPeriodo(inicioHoje, agora)))
                .novasUltimos7Dias(nz(adminQueryRepository.countOrganizacoesNoPeriodo(inicio7Dias, agora)))
                .novasMesAtual(novasMesAtual)
                .novasMesAnterior(novasMesAnterior)
                .crescimentoMensalPercentual(crescimento(BigDecimal.valueOf(novasMesAtual), BigDecimal.valueOf(novasMesAnterior)))
                .ativasSemAgendamentos(semAgendamento)
                .ativasSemInstancia(nz(adminQueryRepository.countOrganizacoesAtivasSemInstancia()))
                .taxaAtivacao(percentual(ativas - semAgendamento, ativas))
                .adocaoFeatures(AdocaoFeatures.builder()
                        .usaEcommerce(features[0])
                        .usaGestaoProdutos(features[1])
                        .usaPlanosParaClientes(features[2])
                        .disparaNotificacoesPush(features[3])
                        .build())
                .porEstado(porEstado)
                .build();
    }

    // ---------- Agendamentos ----------

    private AgendamentosBlock buildAgendamentos(LocalDateTime agora, LocalDateTime inicioHoje, LocalDateTime inicio7Dias,
                                                LocalDateTime inicioMes, LocalDateTime inicioMesAnterior, LocalDateTime fimMesAnterior,
                                                LocalDateTime inicio12Meses) {
        Map<String, Long> porStatus = new LinkedHashMap<>();
        long total = 0;
        for (Object[] row : adminQueryRepository.countAgendamentosAgrupadosPorStatus()) {
            String status = row[0] != null ? row[0].toString() : "DESCONHECIDO";
            long qtd = asLong(row[1]);
            porStatus.merge(status, qtd, Long::sum);
            total += qtd;
        }
        long concluidos = porStatus.getOrDefault("CONCLUIDO", 0L);
        long cancelados = porStatus.getOrDefault("CANCELADO", 0L);
        long naoCompareceu = porStatus.getOrDefault("NAO_COMPARECEU", 0L);

        long mesAtual = nz(adminQueryRepository.countAgendamentosNoPeriodo(inicioMes, agora));
        long mesAnterior = nz(adminQueryRepository.countAgendamentosNoPeriodo(inicioMesAnterior, fimMesAnterior));

        return AgendamentosBlock.builder()
                .total(total)
                .hoje(nz(adminQueryRepository.countAgendamentosNoPeriodo(inicioHoje, agora)))
                .ultimos7Dias(nz(adminQueryRepository.countAgendamentosNoPeriodo(inicio7Dias, agora)))
                .mesAtual(mesAtual)
                .mesAnterior(mesAnterior)
                .crescimentoMensalPercentual(crescimento(BigDecimal.valueOf(mesAtual), BigDecimal.valueOf(mesAnterior)))
                .porStatus(porStatus)
                .taxaConclusao(percentual(concluidos, total))
                .taxaCancelamento(percentual(cancelados, total))
                .taxaNoShow(percentual(naoCompareceu, total))
                .evolucaoMensal(serieMensalComAcumulado(adminQueryRepository.countAgendamentosMensais(inicio12Meses)))
                .build();
    }

    // ---------- Clientes ----------

    private ClientesBlock buildClientes(LocalDateTime inicioHoje, LocalDateTime inicio7Dias, LocalDateTime inicioMes,
                                        LocalDateTime inicio12Meses) {
        long total = nz(adminQueryRepository.countTotalClientes());
        long ativos = nz(adminQueryRepository.countClientesAtivos());
        long totalOrgs = adminQueryRepository.count();
        LocalDateTime agora = LocalDateTime.now();

        return ClientesBlock.builder()
                .total(total)
                .ativos(ativos)
                .inativos(total - ativos)
                .cadastroIncompleto(nz(adminQueryRepository.countClientesCadastroIncompleto()))
                .novosHoje(nz(adminQueryRepository.countClientesNoPeriodo(inicioHoje, agora)))
                .novosUltimos7Dias(nz(adminQueryRepository.countClientesNoPeriodo(inicio7Dias, agora)))
                .novosMesAtual(nz(adminQueryRepository.countClientesNoPeriodo(inicioMes, agora)))
                .mediaPorOrganizacao(media(total, totalOrgs))
                .evolucaoMensal(serieMensalComAcumulado(adminQueryRepository.countClientesMensais(inicio12Meses)))
                .build();
    }

    // ---------- Funcionarios ----------

    private FuncionariosBlock buildFuncionarios() {
        long total = nz(adminQueryRepository.countTotalFuncionarios());
        long ativos = nz(adminQueryRepository.countFuncionariosAtivos());
        long totalOrgs = adminQueryRepository.count();
        return FuncionariosBlock.builder()
                .total(total)
                .ativos(ativos)
                .inativos(total - ativos)
                .mediaPorOrganizacao(media(total, totalOrgs))
                .build();
    }

    // ---------- Servicos ----------

    private ServicosBlock buildServicos() {
        long total = nz(adminQueryRepository.countTotalServicos());
        long ativos = nz(adminQueryRepository.countServicosAtivos());
        long totalOrgs = adminQueryRepository.count();
        BigDecimal precoMedio = adminQueryRepository.calcularPrecoMedioServicos();
        return ServicosBlock.builder()
                .total(total)
                .ativos(ativos)
                .inativos(total - ativos)
                .precoMedio(precoMedio != null ? precoMedio.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .mediaPorOrganizacao(media(total, totalOrgs))
                .build();
    }

    // ---------- Instancias ----------

    private InstanciasBlock buildInstancias() {
        long total = nz(adminQueryRepository.countTotalInstancias());
        long conectadas = nz(adminQueryRepository.countInstanciasAtivas());

        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (Object[] row : adminQueryRepository.countInstanciasAgrupadasPorStatus()) {
            String status = row[0] != null ? row[0].toString() : "DESCONHECIDO";
            porStatus.merge(status, asLong(row[1]), Long::sum);
        }

        return InstanciasBlock.builder()
                .total(total)
                .conectadas(conectadas)
                .desconectadas(total - conectadas)
                .porStatus(porStatus)
                .organizacoesSemInstancia(nz(adminQueryRepository.countOrganizacoesAtivasSemInstancia()))
                .build();
    }

    // ---------- Financeiro ----------

    private FinanceiroBlock buildFinanceiro(LocalDateTime agora, LocalDateTime inicioHoje, LocalDateTime inicioMes,
                                            LocalDateTime inicioMesAnterior, LocalDateTime fimMesAnterior, LocalDateTime inicio12Meses) {
        BigDecimal faturamentoTotal = nz(adminQueryRepository.calcularFaturamentoTotal());
        BigDecimal faturamentoMesAtual = nz(adminQueryRepository.calcularFaturamentoPeriodo(inicioMes, agora));
        BigDecimal faturamentoMesAnterior = nz(adminQueryRepository.calcularFaturamentoPeriodo(inicioMesAnterior, fimMesAnterior));
        long pagamentosConfirmados = nz(adminQueryRepository.countPagamentosConfirmados());

        // Cobrancas por status (+ somas de valor)
        Map<String, Long> cobrancasPorStatus = new LinkedHashMap<>();
        BigDecimal valorPendente = BigDecimal.ZERO;
        BigDecimal valorVencido = BigDecimal.ZERO;
        long totalCobrancas = 0;
        long cobrancasVencidas = 0;
        for (Object[] row : adminQueryRepository.countECalcularCobrancasPorStatus()) {
            String status = row[0] != null ? row[0].toString() : "DESCONHECIDO";
            long qtd = asLong(row[1]);
            BigDecimal soma = asBigDecimal(row[2]);
            cobrancasPorStatus.merge(status, qtd, Long::sum);
            totalCobrancas += qtd;
            if ("PENDENTE".equals(status) || "PARCIALMENTE_PAGO".equals(status)) {
                valorPendente = valorPendente.add(soma);
            }
            if ("VENCIDA".equals(status)) {
                valorVencido = valorVencido.add(soma);
                cobrancasVencidas += qtd;
            }
        }

        Map<String, Long> cobrancasPorTipo = new LinkedHashMap<>();
        for (Object[] row : adminQueryRepository.countCobrancasPorTipo()) {
            cobrancasPorTipo.merge(row[0] != null ? row[0].toString() : "DESCONHECIDO", asLong(row[1]), Long::sum);
        }

        Map<String, Long> pagamentosPorStatus = new LinkedHashMap<>();
        long totalPagamentos = 0;
        for (Object[] row : adminQueryRepository.countPagamentosPorStatus()) {
            long qtd = asLong(row[1]);
            pagamentosPorStatus.merge(row[0] != null ? row[0].toString() : "DESCONHECIDO", qtd, Long::sum);
            totalPagamentos += qtd;
        }

        List<SerieMensalValor> evolucao = new ArrayList<>();
        for (Object[] row : adminQueryRepository.calcularFaturamentoMensal(inicio12Meses)) {
            evolucao.add(SerieMensalValor.builder()
                    .mes((String) row[0])
                    .valor(asBigDecimal(row[1]))
                    .quantidade(asLong(row[2]))
                    .build());
        }

        BigDecimal ticketMedio = pagamentosConfirmados > 0
                ? faturamentoTotal.divide(BigDecimal.valueOf(pagamentosConfirmados), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return FinanceiroBlock.builder()
                .faturamentoTotal(faturamentoTotal)
                .faturamentoHoje(nz(adminQueryRepository.calcularFaturamentoPeriodo(inicioHoje, agora)))
                .faturamentoMesAtual(faturamentoMesAtual)
                .faturamentoMesAnterior(faturamentoMesAnterior)
                .crescimentoMensalPercentual(crescimento(faturamentoMesAtual, faturamentoMesAnterior))
                .ticketMedio(ticketMedio)
                .valorPendente(valorPendente)
                .valorVencido(valorVencido)
                .taxaInadimplencia(percentual(cobrancasVencidas, totalCobrancas))
                .totalCobrancas(totalCobrancas)
                .cobrancasPorStatus(cobrancasPorStatus)
                .cobrancasPorTipo(cobrancasPorTipo)
                .totalPagamentos(totalPagamentos)
                .pagamentosPorStatus(pagamentosPorStatus)
                .evolucaoMensal(evolucao)
                .build();
    }

    // ---------- Planos (Payment API) ----------

    private PlanosBlock buildPlanos() {
        long totalAssinaturas = assinaturaRepository.count();
        long totalOrgs = adminQueryRepository.count();
        long orgsSemAssinatura = Math.max(0, totalOrgs - totalAssinaturas);

        try {
            List<PlanResponse> plans = paymentApiClient.listPlans();
            Map<Long, PlanResponse> planById = new LinkedHashMap<>();
            for (PlanResponse p : plans) {
                if (p != null && p.getId() != null) planById.put(p.getId(), p);
            }

            List<SubscriptionResponse> subs = listAllSubscriptions();
            Map<Long, Long> contagemPorPlano = new LinkedHashMap<>();
            long ativas = 0, emTrial = 0, canceladas = 0;
            BigDecimal mrr = BigDecimal.ZERO;
            for (SubscriptionResponse s : subs) {
                if (s == null) continue;
                PaymentSubscriptionStatus st = s.getStatus();
                boolean trial = Boolean.TRUE.equals(s.getInTrial()) || st == PaymentSubscriptionStatus.TRIALING;
                if (st == PaymentSubscriptionStatus.CANCELED) canceladas++;
                if (st == PaymentSubscriptionStatus.ACTIVE) ativas++;
                if (trial) emTrial++;
                if (st == PaymentSubscriptionStatus.ACTIVE || trial) {
                    if (s.getPlanId() != null) contagemPorPlano.merge(s.getPlanId(), 1L, Long::sum);
                    if (st == PaymentSubscriptionStatus.ACTIVE
                            && s.getCycle() == PaymentSubscriptionCycle.MONTHLY
                            && s.getEffectivePrice() != null) {
                        mrr = mrr.add(s.getEffectivePrice());
                    }
                }
            }

            List<PlanoDistribuicao> distribuicao = new ArrayList<>();
            contagemPorPlano.forEach((planId, qtd) -> {
                PlanResponse p = planById.get(planId);
                distribuicao.add(PlanoDistribuicao.builder()
                        .planId(planId)
                        .codigo(p != null ? p.getCodigo() : null)
                        .nome(p != null ? p.getName() : null)
                        .gratuito(p != null ? p.getIsFree() : null)
                        .precoMensal(p != null ? p.getPrecoMensal() : null)
                        .quantidade(qtd)
                        .build());
            });
            distribuicao.sort((a, b) -> {
                int ta = tierOrder(planById.get(a.getPlanId()));
                int tb = tierOrder(planById.get(b.getPlanId()));
                if (ta != tb) return Integer.compare(ta, tb);
                return Long.compare(b.getQuantidade(), a.getQuantidade());
            });

            return PlanosBlock.builder()
                    .disponivel(true)
                    .totalAssinaturas(totalAssinaturas)
                    .organizacoesSemAssinatura(orgsSemAssinatura)
                    .assinaturasAtivas(ativas)
                    .assinaturasEmTrial(emTrial)
                    .assinaturasCanceladas(canceladas)
                    .mrrEstimado(mrr.setScale(2, RoundingMode.HALF_UP))
                    .distribuicao(distribuicao)
                    .build();
        } catch (Exception e) {
            log.warn("Dashboard: bloco de planos indisponivel (Payment API): {}", e.getMessage());
            return PlanosBlock.builder()
                    .disponivel(false)
                    .totalAssinaturas(totalAssinaturas)
                    .organizacoesSemAssinatura(orgsSemAssinatura)
                    .distribuicao(new ArrayList<>())
                    .build();
        }
    }

    private List<SubscriptionResponse> listAllSubscriptions() {
        List<SubscriptionResponse> all = new ArrayList<>();
        int page = 0;
        int size = 100;
        int maxPages = 50; // guard rail
        while (page < maxPages) {
            PaymentApiClient.PageResponse<SubscriptionResponse> resp =
                    paymentApiClient.listSubscriptions(null, null, page, size);
            if (resp == null || resp.getContent() == null || resp.getContent().isEmpty()) break;
            all.addAll(resp.getContent());
            int totalPages = resp.getTotalPages() != null ? resp.getTotalPages() : (page + 1);
            page++;
            if (page >= totalPages) break;
        }
        return all;
    }

    private static int tierOrder(PlanResponse p) {
        if (p == null || p.getTierOrder() == null) return Integer.MAX_VALUE;
        return p.getTierOrder();
    }

    // ---------- Leads ----------

    private LeadsBlock buildLeads(LocalDateTime inicioHoje, LocalDateTime inicio7Dias, LocalDateTime inicioMes, LocalDateTime agora) {
        return LeadsBlock.builder()
                .total(leadRepository.countByDeletedAtIsNull())
                .novosHoje(leadRepository.countByDeletedAtIsNullAndDtCriacaoBetween(inicioHoje, agora))
                .novosUltimos7Dias(leadRepository.countByDeletedAtIsNullAndDtCriacaoBetween(inicio7Dias, agora))
                .novosMesAtual(leadRepository.countByDeletedAtIsNullAndDtCriacaoBetween(inicioMes, agora))
                .emStatusFinal(leadRepository.countEmStatusFinal())
                .porStatus(toCountMap(leadRepository.countAgrupadosPorStatus()))
                .porTipoNegocio(toCountMap(leadRepository.countAgrupadosPorTipoNegocio()))
                .porPrioridade(toCountMap(leadRepository.countAgrupadosPorPrioridade()))
                .porOrigem(toCountMap(leadRepository.countAgrupadosPorOrigem()))
                .valorEstimadoPipeline(nz(leadRepository.sumValorEstimadoPipeline()))
                .build();
    }

    // ---------- Top organizacoes ----------

    private TopOrganizacoes buildTopOrganizacoes() {
        List<OrgRanking> porFaturamento = new ArrayList<>();
        for (Object[] row : adminQueryRepository.calcularFaturamentoPorOrganizacao()) {
            if (porFaturamento.size() >= TOP_LIMIT) break;
            porFaturamento.add(OrgRanking.builder()
                    .organizacaoId(asLong(row[0]))
                    .nomeFantasia((String) row[1])
                    .valor(asBigDecimal(row[2]))
                    .build());
        }

        List<OrgRanking> porAgendamentos = new ArrayList<>();
        for (Object[] row : adminQueryRepository.countAgendamentosPorOrganizacao()) {
            if (porAgendamentos.size() >= TOP_LIMIT) break;
            porAgendamentos.add(OrgRanking.builder()
                    .organizacaoId(asLong(row[0]))
                    .nomeFantasia((String) row[1])
                    .quantidade(asLong(row[2]))
                    .build());
        }

        List<OrgRanking> porClientes = new ArrayList<>();
        for (Object[] row : adminQueryRepository.countClientesPorOrganizacao()) {
            if (porClientes.size() >= TOP_LIMIT) break;
            porClientes.add(OrgRanking.builder()
                    .organizacaoId(asLong(row[0]))
                    .nomeFantasia((String) row[1])
                    .quantidade(asLong(row[2]))
                    .build());
        }

        return TopOrganizacoes.builder()
                .porFaturamento(porFaturamento)
                .porAgendamentos(porAgendamentos)
                .porClientes(porClientes)
                .build();
    }

    // ---------- Localizacoes ----------

    private List<OrgLocationDTO> buildLocalizacoes() {
        List<OrgLocationDTO> out = new ArrayList<>();
        for (Object[] row : adminQueryRepository.findLocalizacoesOrganizacoesAgregadas()) {
            String cidade = (String) row[0];
            String uf = (String) row[1];
            String latStr = (String) row[2];
            String lngStr = (String) row[3];
            Long qtd = asLong(row[4]);
            try {
                out.add(OrgLocationDTO.builder()
                        .cidade(cidade != null ? cidade.trim() : null)
                        .estado(uf != null ? uf.trim().toUpperCase() : null)
                        .latitude(Double.parseDouble(latStr))
                        .longitude(Double.parseDouble(lngStr))
                        .quantidade(qtd)
                        .build());
            } catch (NumberFormatException | NullPointerException ignored) {
            }
        }
        return out;
    }

    // ==================== CACHE ====================

    private AdminDashboardDTO readCache() {
        try {
            String json = redis.opsForValue().get(CACHE_KEY);
            if (json == null) return null;
            return mapper.readValue(json, AdminDashboardDTO.class);
        } catch (Exception e) {
            log.warn("Dashboard: falha lendo cache: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(AdminDashboardDTO dto) {
        try {
            redis.opsForValue().set(CACHE_KEY, mapper.writeValueAsString(dto), cacheTtl);
        } catch (Exception e) {
            log.warn("Dashboard: falha gravando cache: {}", e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static Long asLong(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private static BigDecimal asBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private static Double media(long total, long divisor) {
        if (divisor <= 0) return 0.0;
        return Math.round(((double) total / divisor) * 100.0) / 100.0;
    }

    private static BigDecimal percentual(long parte, long total) {
        if (total <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(parte * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal crescimento(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) <= 0) {
            return (atual != null && atual.compareTo(BigDecimal.ZERO) > 0) ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return atual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Converte rows {@code [chave, count]} num mapa ordenado, ignorando chaves nulas. */
    private static Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String chave = row[0] != null ? row[0].toString() : "(sem valor)";
            map.merge(chave, asLong(row[1]), Long::sum);
        }
        return map;
    }

    /** Constroi serie mensal {@code [mes, count, ...]} adicionando o acumulado corrente. */
    private static List<SerieMensal> serieMensalComAcumulado(List<Object[]> rows) {
        List<SerieMensal> serie = new ArrayList<>();
        long acumulado = 0;
        for (Object[] row : rows) {
            long qtd = asLong(row[1]);
            acumulado += qtd;
            serie.add(SerieMensal.builder()
                    .mes((String) row[0])
                    .quantidade(qtd)
                    .acumulado(acumulado)
                    .build());
        }
        return serie;
    }

    private static long[] firstRowAsLongs(List<Object[]> rows, int len) {
        long[] out = new long[len];
        if (rows != null && !rows.isEmpty()) {
            Object[] row = rows.get(0);
            for (int i = 0; i < len && i < row.length; i++) {
                out[i] = asLong(row[i]);
            }
        }
        return out;
    }
}
