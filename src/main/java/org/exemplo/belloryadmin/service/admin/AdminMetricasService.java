package org.exemplo.belloryadmin.service.admin;

import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.admin.*;
import org.exemplo.belloryadmin.model.entity.instancia.Instance;
import org.exemplo.belloryadmin.model.repository.admin.AdminQueryRepository;
import org.exemplo.belloryadmin.model.repository.instance.InstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMetricasService {

    private final AdminQueryRepository adminQueryRepository;
    private final InstanceRepository instanceRepository;

    // ===== AGENDAMENTOS =====
    public AdminAgendamentoMetricasDTO getMetricasAgendamentos() {
        Long total = adminQueryRepository.countTotalAgendamentos();
        Long concluidos = adminQueryRepository.countAgendamentosConcluidos();
        Long cancelados = adminQueryRepository.countAgendamentosCancelados();
        Long pendentes = adminQueryRepository.countAgendamentosPendentes();
        Long agendados = adminQueryRepository.countAgendamentosAgendados();
        Long naoCompareceu = adminQueryRepository.countAgendamentosNaoCompareceu();

        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        Long totalNoMes = adminQueryRepository.countAgendamentosNoPeriodo(inicioMes, LocalDateTime.now());

        BigDecimal taxaConclusao = total > 0 ? BigDecimal.valueOf(concluidos * 100.0 / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal taxaCancelamento = total > 0 ? BigDecimal.valueOf(cancelados * 100.0 / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal taxaNoShow = total > 0 ? BigDecimal.valueOf(naoCompareceu * 100.0 / total).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Por organizacao
        List<Object[]> porOrgData = adminQueryRepository.countAgendamentosPorOrganizacao();
        List<AdminAgendamentoMetricasDTO.AgendamentoPorOrganizacao> porOrganizacao = porOrgData.stream()
                .map(row -> AdminAgendamentoMetricasDTO.AgendamentoPorOrganizacao.builder()
                        .organizacaoId((Long) row[0])
                        .nomeFantasia((String) row[1])
                        .total((Long) row[2])
                        .concluidos((Long) row[3])
                        .cancelados((Long) row[4])
                        .pendentes((Long) row[5])
                        .build())
                .collect(Collectors.toList());

        // Evolucao mensal (ultimos 12 meses)
        LocalDateTime inicio12Meses = LocalDateTime.now().minusMonths(12).withDayOfMonth(1).with(LocalTime.MIN);
        List<Object[]> mensalData = adminQueryRepository.countAgendamentosMensais(inicio12Meses);
        List<AdminAgendamentoMetricasDTO.AgendamentoMensal> evolucaoMensal = mensalData.stream()
                .map(row -> AdminAgendamentoMetricasDTO.AgendamentoMensal.builder()
                        .mes((String) row[0])
                        .total((Long) row[1])
                        .concluidos((Long) row[2])
                        .cancelados((Long) row[3])
                        .build())
                .collect(Collectors.toList());

        return AdminAgendamentoMetricasDTO.builder()
                .totalGeral(total)
                .totalNoMes(totalNoMes)
                .concluidos(concluidos)
                .cancelados(cancelados)
                .pendentes(pendentes)
                .agendados(agendados)
                .naoCompareceu(naoCompareceu)
                .taxaConclusao(taxaConclusao)
                .taxaCancelamento(taxaCancelamento)
                .taxaNoShow(taxaNoShow)
                .porOrganizacao(porOrganizacao)
                .evolucaoMensal(evolucaoMensal)
                .build();
    }

    // ===== FATURAMENTO =====
    public AdminFaturamentoMetricasDTO getMetricasFaturamento() {
        BigDecimal faturamentoTotal = adminQueryRepository.calcularFaturamentoTotal();
        Long totalPagamentos = adminQueryRepository.countTotalPagamentos();
        Long pagamentosConfirmados = adminQueryRepository.countPagamentosConfirmados();

        LocalDateTime inicioMesAtual = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime fimMesAtual = LocalDateTime.now();
        LocalDateTime inicioMesAnterior = inicioMesAtual.minusMonths(1);
        LocalDateTime fimMesAnterior = inicioMesAtual.minusSeconds(1);

        BigDecimal faturamentoMesAtual = adminQueryRepository.calcularFaturamentoPeriodo(inicioMesAtual, fimMesAtual);
        BigDecimal faturamentoMesAnterior = adminQueryRepository.calcularFaturamentoPeriodo(inicioMesAnterior, fimMesAnterior);

        BigDecimal crescimento = BigDecimal.ZERO;
        if (faturamentoMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            crescimento = faturamentoMesAtual.subtract(faturamentoMesAnterior)
                    .divide(faturamentoMesAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal ticketMedio = pagamentosConfirmados > 0
                ? faturamentoTotal.divide(BigDecimal.valueOf(pagamentosConfirmados), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Por organizacao
        List<Object[]> porOrgData = adminQueryRepository.calcularFaturamentoPorOrganizacao();
        List<AdminFaturamentoMetricasDTO.FaturamentoPorOrganizacao> porOrganizacao = porOrgData.stream()
                .map(row -> {
                    Long orgId = (Long) row[0];
                    BigDecimal faturamentoMesOrg = adminQueryRepository.calcularFaturamentoByOrganizacaoNoPeriodo(orgId, inicioMesAtual, fimMesAtual);
                    Long cobrancasPagas = adminQueryRepository.countCobrancasPagasByOrganizacao(orgId);
                    Long cobrancasPendentes = adminQueryRepository.countCobrancasPendentesByOrganizacao(orgId);

                    return AdminFaturamentoMetricasDTO.FaturamentoPorOrganizacao.builder()
                            .organizacaoId(orgId)
                            .nomeFantasia((String) row[1])
                            .faturamentoTotal((BigDecimal) row[2])
                            .faturamentoMes(faturamentoMesOrg)
                            .totalCobrancas((Long) row[3])
                            .cobrancasPagas(cobrancasPagas)
                            .cobrancasPendentes(cobrancasPendentes)
                            .build();
                })
                .collect(Collectors.toList());

        // Evolucao mensal
        LocalDateTime inicio12Meses = LocalDateTime.now().minusMonths(12).withDayOfMonth(1).with(LocalTime.MIN);
        List<Object[]> mensalData = adminQueryRepository.calcularFaturamentoMensal(inicio12Meses);
        List<AdminFaturamentoMetricasDTO.FaturamentoMensal> evolucaoMensal = mensalData.stream()
                .map(row -> AdminFaturamentoMetricasDTO.FaturamentoMensal.builder()
                        .mes((String) row[0])
                        .valor((BigDecimal) row[1])
                        .quantidadePagamentos((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        return AdminFaturamentoMetricasDTO.builder()
                .faturamentoTotalGeral(faturamentoTotal)
                .faturamentoMesAtual(faturamentoMesAtual)
                .faturamentoMesAnterior(faturamentoMesAnterior)
                .crescimentoPercentual(crescimento)
                .ticketMedio(ticketMedio)
                .totalPagamentos(totalPagamentos)
                .pagamentosConfirmados(pagamentosConfirmados)
                .porOrganizacao(porOrganizacao)
                .evolucaoMensal(evolucaoMensal)
                .build();
    }

    // ===== SERVICOS =====
    public AdminServicoMetricasDTO getMetricasServicos() {
        Long total = adminQueryRepository.countTotalServicos();
        Long ativos = adminQueryRepository.countServicosAtivos();
        BigDecimal precoMedio = adminQueryRepository.calcularPrecoMedioServicos();

        List<Object[]> porOrgData = adminQueryRepository.countServicosPorOrganizacao();
        List<AdminServicoMetricasDTO.ServicoPorOrganizacao> porOrganizacao = porOrgData.stream()
                .map(row -> AdminServicoMetricasDTO.ServicoPorOrganizacao.builder()
                        .organizacaoId((Long) row[0])
                        .nomeFantasia((String) row[1])
                        .totalServicos((Long) row[2])
                        .servicosAtivos((Long) row[3])
                        .build())
                .collect(Collectors.toList());

        return AdminServicoMetricasDTO.builder()
                .totalServicosGeral(total)
                .servicosAtivos(ativos)
                .servicosInativos(total - ativos)
                .precoMedio(precoMedio != null ? precoMedio.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .porOrganizacao(porOrganizacao)
                .maisAgendados(new ArrayList<>())
                .build();
    }

    // ===== FUNCIONARIOS =====
    public AdminFuncionarioMetricasDTO getMetricasFuncionarios() {
        Long total = adminQueryRepository.countTotalFuncionarios();
        Long ativos = adminQueryRepository.countFuncionariosAtivos();
        Long totalOrgs = adminQueryRepository.count();
        Double media = totalOrgs > 0 ? (double) total / totalOrgs : 0.0;

        List<Object[]> porOrgData = adminQueryRepository.countFuncionariosPorOrganizacao();
        List<AdminFuncionarioMetricasDTO.FuncionarioPorOrganizacao> porOrganizacao = porOrgData.stream()
                .map(row -> AdminFuncionarioMetricasDTO.FuncionarioPorOrganizacao.builder()
                        .organizacaoId((Long) row[0])
                        .nomeFantasia((String) row[1])
                        .totalFuncionarios((Long) row[2])
                        .funcionariosAtivos((Long) row[3])
                        .totalServicosVinculados(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());

        return AdminFuncionarioMetricasDTO.builder()
                .totalFuncionariosGeral(total)
                .funcionariosAtivos(ativos)
                .funcionariosInativos(total - ativos)
                .mediaFuncionariosPorOrganizacao(Math.round(media * 100.0) / 100.0)
                .porOrganizacao(porOrganizacao)
                .build();
    }

    // ===== CLIENTES =====
    public AdminClienteMetricasDTO getMetricasClientes() {
        Long total = adminQueryRepository.countTotalClientes();
        Long ativos = adminQueryRepository.countClientesAtivos();
        Long totalOrgs = adminQueryRepository.count();
        Double media = totalOrgs > 0 ? (double) total / totalOrgs : 0.0;

        List<Object[]> porOrgData = adminQueryRepository.countClientesPorOrganizacao();
        List<AdminClienteMetricasDTO.ClientePorOrganizacao> porOrganizacao = porOrgData.stream()
                .map(row -> AdminClienteMetricasDTO.ClientePorOrganizacao.builder()
                        .organizacaoId((Long) row[0])
                        .nomeFantasia((String) row[1])
                        .totalClientes((Long) row[2])
                        .clientesAtivos((Long) row[3])
                        .build())
                .collect(Collectors.toList());

        // Evolucao mensal
        LocalDateTime inicio12Meses = LocalDateTime.now().minusMonths(12).withDayOfMonth(1).with(LocalTime.MIN);
        List<Object[]> mensalData = adminQueryRepository.countClientesMensais(inicio12Meses);
        long acumulado = 0;
        List<AdminClienteMetricasDTO.ClienteMensal> evolucaoMensal = new ArrayList<>();
        for (Object[] row : mensalData) {
            Long novos = (Long) row[1];
            acumulado += novos;
            evolucaoMensal.add(AdminClienteMetricasDTO.ClienteMensal.builder()
                    .mes((String) row[0])
                    .novosClientes(novos)
                    .totalAcumulado(acumulado)
                    .build());
        }

        return AdminClienteMetricasDTO.builder()
                .totalClientesGeral(total)
                .clientesAtivos(ativos)
                .clientesInativos(total - ativos)
                .mediaClientesPorOrganizacao(Math.round(media * 100.0) / 100.0)
                .porOrganizacao(porOrganizacao)
                .evolucaoMensal(evolucaoMensal)
                .build();
    }

    // ===== INSTANCIAS =====
    public AdminInstanciaMetricasDTO getMetricasInstancias() {
        Long total = adminQueryRepository.countTotalInstancias();
        Long ativas = adminQueryRepository.countInstanciasAtivas();

        List<Instance> todasInstancias = instanceRepository.findAll().stream()
                .filter(i -> !i.isDeletado())
                .collect(Collectors.toList());

        List<AdminInstanciaMetricasDTO.InstanciaDetalheDTO> detalhes = todasInstancias.stream()
                .map(i -> AdminInstanciaMetricasDTO.InstanciaDetalheDTO.builder()
                        .id(i.getId())
                        .instanceName(i.getInstanceName())
                        .instanceId(i.getInstanceId())
                        .integration(i.getIntegration())
                        .status(i.getStatus() != null ? i.getStatus().name() : null)
                        .ativo(i.isAtivo())
                        .organizacaoId(i.getOrganizacao() != null ? i.getOrganizacao().getId() : null)
                        .nomeFantasiaOrganizacao(i.getOrganizacao() != null ? i.getOrganizacao().getNomeFantasia() : null)
                        .build())
                .collect(Collectors.toList());

        List<Object[]> porOrgData = adminQueryRepository.countInstanciasPorOrganizacao();
        List<AdminInstanciaMetricasDTO.InstanciaPorOrganizacao> porOrganizacao = porOrgData.stream()
                .map(row -> AdminInstanciaMetricasDTO.InstanciaPorOrganizacao.builder()
                        .organizacaoId((Long) row[0])
                        .nomeFantasia((String) row[1])
                        .totalInstancias((Long) row[2])
                        .instanciasAtivas((Long) row[3])
                        .instanciasConectadas((Long) row[3])
                        .build())
                .collect(Collectors.toList());

        long deletadas = instanceRepository.count() - total;

        return AdminInstanciaMetricasDTO.builder()
                .totalInstancias(total)
                .instanciasAtivas(ativas)
                .instanciasDeletadas(deletadas)
                .instanciasConectadas(ativas)
                .instanciasDesconectadas(total - ativas)
                .porOrganizacao(porOrganizacao)
                .todasInstancias(detalhes)
                .build();
    }

    // ===== PLANOS =====
    // Metricas de planos foram movidas para a Payment API (GET /api/v1/plans e /reports).
    // Removido do Bellory junto com as entities PlanoBellory.
}
