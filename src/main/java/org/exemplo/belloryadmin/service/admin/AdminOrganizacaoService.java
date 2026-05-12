package org.exemplo.belloryadmin.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.PaymentApiClient;
import org.exemplo.belloryadmin.client.payment.dto.AccessStatusResponse;
import org.exemplo.belloryadmin.client.payment.dto.PlanResponse;
import org.exemplo.belloryadmin.client.payment.dto.SubscriptionResponse;
import org.exemplo.belloryadmin.model.dto.admin.AdminOrganizacaoDetalheDTO;
import org.exemplo.belloryadmin.model.dto.admin.AdminOrganizacaoDetalheDTO.*;
import org.exemplo.belloryadmin.model.dto.admin.AdminOrganizacaoListDTO;
import org.exemplo.belloryadmin.model.entity.assinatura.Assinatura;
import org.exemplo.belloryadmin.model.entity.config.ConfigSistema;
import org.exemplo.belloryadmin.model.entity.endereco.Endereco;
import org.exemplo.belloryadmin.model.entity.instancia.Instance;
import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;
import org.exemplo.belloryadmin.model.entity.organizacao.RedesSociais;
import org.exemplo.belloryadmin.model.repository.admin.AdminQueryRepository;
import org.exemplo.belloryadmin.model.repository.assinatura.AssinaturaRepository;
import org.exemplo.belloryadmin.model.repository.instance.InstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminOrganizacaoService {

    private static final String STATUS_CONECTADO = "CONNECTED";

    private final AdminQueryRepository adminQueryRepository;
    private final InstanceRepository instanceRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PaymentApiClient paymentApiClient;

    public AdminOrganizacaoService(AdminQueryRepository adminQueryRepository,
                                   InstanceRepository instanceRepository,
                                   AssinaturaRepository assinaturaRepository,
                                   PaymentApiClient paymentApiClient) {
        this.adminQueryRepository = adminQueryRepository;
        this.instanceRepository = instanceRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.paymentApiClient = paymentApiClient;
    }

    public List<AdminOrganizacaoListDTO> listarOrganizacoes() {
        List<Organizacao> organizacoes = adminQueryRepository.findAllOrganizacoesComPlano();

        return organizacoes.stream().map(org -> {
            Long totalAgendamentos = adminQueryRepository.countAgendamentosByOrganizacao(org.getId());
            Long totalClientes = adminQueryRepository.countClientesByOrganizacao(org.getId());
            Long totalFuncionarios = adminQueryRepository.countFuncionariosByOrganizacao(org.getId());
            Long totalServicos = adminQueryRepository.countServicosByOrganizacao(org.getId());
            List<Instance> instancias = instanceRepository.findByOrganizacaoIdAndDeletadoFalse(org.getId());

            // Tentar buscar dados do plano da Payment API (fail-safe)
            String planoNome = null;
            String planoCodigo = null;
            Optional<Assinatura> assinatura = assinaturaRepository.findByOrganizacaoId(org.getId());
            if (assinatura.isPresent() && assinatura.get().getPaymentApiCustomerId() != null) {
                try {
                    List<SubscriptionResponse> subs = paymentApiClient.listSubscriptionsByCustomer(
                            assinatura.get().getPaymentApiCustomerId());
                    SubscriptionResponse ativa = subs.stream()
                            .filter(s -> "ACTIVE".equals(s.getStatus() != null ? s.getStatus().name() : null))
                            .findFirst()
                            .orElse(subs.isEmpty() ? null : subs.get(0));
                    if (ativa != null) {
                        planoNome = ativa.getPlanName();
                        if (ativa.getPlanId() != null) {
                            try {
                                PlanResponse plan = paymentApiClient.getPlan(ativa.getPlanId());
                                planoCodigo = plan.getCodigo();
                            } catch (Exception e) {
                                log.debug("Falha ao buscar plano {} para org {}: {}", ativa.getPlanId(), org.getId(), e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Falha ao buscar assinatura Payment API para org {}: {}", org.getId(), e.getMessage());
                }
            }

            return AdminOrganizacaoListDTO.builder()
                    .id(org.getId())
                    .nomeFantasia(org.getNomeFantasia())
                    .razaoSocial(org.getRazaoSocial())
                    .cnpj(org.getCnpj())
                    .emailPrincipal(org.getEmailPrincipal())
                    .telefone1(org.getTelefone1())
                    .slug(org.getSlug())
                    .ativo(org.getAtivo())
                    .planoNome(planoNome)
                    .planoCodigo(planoCodigo)
                    .dtCadastro(org.getDtCadastro())
                    .totalAgendamentos(totalAgendamentos)
                    .totalClientes(totalClientes)
                    .totalFuncionarios(totalFuncionarios)
                    .totalServicos(totalServicos)
                    .totalInstancias((long) instancias.size())
                    .build();
        }).collect(Collectors.toList());
    }

    public AdminOrganizacaoDetalheDTO detalharOrganizacao(Long organizacaoId) {
        Organizacao org = adminQueryRepository.findOrganizacaoComDetalhesById(organizacaoId)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada: " + organizacaoId));

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioMes = agora.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime inicio7Dias = agora.minusDays(7);
        LocalDateTime inicio30Dias = agora.minusDays(30);
        LocalDateTime inicio12Meses = agora.minusMonths(12).withDayOfMonth(1).with(LocalTime.MIN);

        // ── Instancias ──
        List<Instance> instanciasEntities = instanceRepository.findByOrganizacaoIdAndDeletadoFalse(organizacaoId);
        List<InstanciaResumoDTO> instanciasList = instanciasEntities.stream()
                .map(i -> InstanciaResumoDTO.builder()
                        .id(i.getId())
                        .instanceName(i.getInstanceName())
                        .instanceId(i.getInstanceId())
                        .status(i.getStatus() != null ? i.getStatus().name() : null)
                        .ativo(i.isAtivo())
                        .build())
                .collect(Collectors.toList());
        boolean instanciaConectada = instanciasEntities.stream()
                .anyMatch(i -> i.getStatus() != null && STATUS_CONECTADO.equals(i.getStatus().name()));

        // ── Contagens base ──
        long totalAgendamentos = nz(adminQueryRepository.countAgendamentosByOrganizacao(organizacaoId));
        long agendamentosNoMes = nz(adminQueryRepository.countAgendamentosByOrganizacaoNoPeriodo(organizacaoId, inicioMes, agora));
        long totalClientes = nz(adminQueryRepository.countClientesByOrganizacao(organizacaoId));
        long clientesAtivos = nz(adminQueryRepository.countClientesAtivosByOrganizacao(organizacaoId));
        long totalFuncionarios = nz(adminQueryRepository.countFuncionariosByOrganizacao(organizacaoId));
        long funcionariosAtivos = nz(adminQueryRepository.countFuncionariosAtivosByOrganizacao(organizacaoId));
        long totalServicos = nz(adminQueryRepository.countServicosByOrganizacao(organizacaoId));
        long servicosAtivos = nz(adminQueryRepository.countServicosAtivosByOrganizacao(organizacaoId));
        BigDecimal faturamentoTotal = nz(adminQueryRepository.calcularFaturamentoByOrganizacao(organizacaoId));
        BigDecimal faturamentoMes = nz(adminQueryRepository.calcularFaturamentoByOrganizacaoNoPeriodo(organizacaoId, inicioMes, agora));

        // ── Engajamento ──
        LocalDateTime ultimoAgendamentoCriado = adminQueryRepository.findUltimoAgendamentoCriadoEmByOrganizacao(organizacaoId);
        LocalDateTime ultimoAgendamentoRealizado = adminQueryRepository.findUltimoAgendamentoRealizadoEmByOrganizacao(organizacaoId, agora);
        LocalDateTime proximoAgendamento = adminQueryRepository.findProximoAgendamentoEmByOrganizacao(organizacaoId, agora);
        LocalDateTime ultimoClienteCadastrado = adminQueryRepository.findUltimoClienteCadastradoEmByOrganizacao(organizacaoId);
        long agendamentosFuturos = nz(adminQueryRepository.countAgendamentosFuturosByOrganizacao(organizacaoId, agora));
        long agendamentos7d = nz(adminQueryRepository.countAgendamentosByOrganizacaoNoPeriodo(organizacaoId, inicio7Dias, agora));
        long agendamentos30d = nz(adminQueryRepository.countAgendamentosByOrganizacaoNoPeriodo(organizacaoId, inicio30Dias, agora));
        long clientesNovos30d = nz(adminQueryRepository.countClientesByOrganizacaoNoPeriodo(organizacaoId, inicio30Dias, agora));

        LocalDateTime ultimaAtividade = maxData(ultimoAgendamentoCriado, ultimoClienteCadastrado);
        Long diasDesdeCadastro = org.getDtCadastro() != null ? ChronoUnit.DAYS.between(org.getDtCadastro(), agora) : null;
        Long diasSemAtividade = ultimaAtividade != null ? ChronoUnit.DAYS.between(ultimaAtividade, agora) : null;
        String statusUso = derivarStatusUso(diasDesdeCadastro, diasSemAtividade);

        // ── Onboarding ──
        boolean configPreenchida = org.getConfigSistema() != null;
        boolean temAssinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId).isPresent();
        boolean[] itensOnboarding = {
                totalFuncionarios > 0,
                totalServicos > 0,
                totalClientes > 0,
                instanciaConectada,
                totalAgendamentos > 0,
                configPreenchida,
                temAssinatura
        };
        int itensConcluidos = 0;
        for (boolean b : itensOnboarding) if (b) itensConcluidos++;
        int totalItensOnboarding = itensOnboarding.length;
        int percentualOnboarding = (int) Math.round(itensConcluidos * 100.0 / totalItensOnboarding);

        OnboardingDTO onboarding = OnboardingDTO.builder()
                .temFuncionario(totalFuncionarios > 0)
                .temServico(totalServicos > 0)
                .temCliente(totalClientes > 0)
                .temInstanciaConectada(instanciaConectada)
                .temAgendamento(totalAgendamentos > 0)
                .configPreenchida(configPreenchida)
                .temAssinatura(temAssinatura)
                .itensConcluidos(itensConcluidos)
                .totalItens(totalItensOnboarding)
                .percentualConcluido(percentualOnboarding)
                .build();

        EngajamentoDTO engajamento = EngajamentoDTO.builder()
                .diasDesdeCadastro(diasDesdeCadastro)
                .statusUso(statusUso)
                .healthScore(calcularHealthScore(diasSemAtividade, instanciaConectada, percentualOnboarding, faturamentoMes))
                .ultimaAtividadeEm(ultimaAtividade)
                .diasSemAtividade(diasSemAtividade)
                .ultimoAgendamentoCriadoEm(ultimoAgendamentoCriado)
                .ultimoAgendamentoRealizadoEm(ultimoAgendamentoRealizado)
                .proximoAgendamentoEm(proximoAgendamento)
                .ultimoClienteCadastradoEm(ultimoClienteCadastrado)
                .agendamentosUltimos7Dias(agendamentos7d)
                .agendamentosUltimos30Dias(agendamentos30d)
                .clientesNovosUltimos30Dias(clientesNovos30d)
                .instanciaConectada(instanciaConectada)
                .build();

        // ── Agendamentos detalhe ──
        Map<String, Long> agendamentosPorStatus = new LinkedHashMap<>();
        long totalAgStatus = 0;
        for (Object[] row : adminQueryRepository.countAgendamentosAgrupadosPorStatusByOrganizacao(organizacaoId)) {
            long qtd = asLong(row[1]);
            agendamentosPorStatus.merge(row[0] != null ? row[0].toString() : "DESCONHECIDO", qtd, Long::sum);
            totalAgStatus += qtd;
        }
        long concluidos = agendamentosPorStatus.getOrDefault("CONCLUIDO", 0L);
        long cancelados = agendamentosPorStatus.getOrDefault("CANCELADO", 0L);
        long naoCompareceu = agendamentosPorStatus.getOrDefault("NAO_COMPARECEU", 0L);

        List<SerieMensalAgendamento> evolAgendamentos = new ArrayList<>();
        for (Object[] row : adminQueryRepository.countAgendamentosMensaisByOrganizacao(organizacaoId, inicio12Meses)) {
            evolAgendamentos.add(SerieMensalAgendamento.builder()
                    .mes((String) row[0])
                    .total(asLong(row[1]))
                    .concluidos(asLong(row[2]))
                    .cancelados(asLong(row[3]))
                    .build());
        }

        AgendamentosDetalheDTO agendamentos = AgendamentosDetalheDTO.builder()
                .total(totalAgendamentos)
                .noMes(agendamentosNoMes)
                .futuros(agendamentosFuturos)
                .porStatus(agendamentosPorStatus)
                .taxaConclusao(percentual(concluidos, totalAgStatus))
                .taxaCancelamento(percentual(cancelados, totalAgStatus))
                .taxaNoShow(percentual(naoCompareceu, totalAgStatus))
                .evolucaoMensal(evolAgendamentos)
                .build();

        // ── Clientes detalhe ──
        long clientesNovosNoMes = nz(adminQueryRepository.countClientesByOrganizacaoNoPeriodo(organizacaoId, inicioMes, agora));
        List<SerieMensalCliente> evolClientes = new ArrayList<>();
        long acumuladoCli = 0;
        for (Object[] row : adminQueryRepository.countClientesMensaisByOrganizacao(organizacaoId, inicio12Meses)) {
            long novos = asLong(row[1]);
            acumuladoCli += novos;
            evolClientes.add(SerieMensalCliente.builder()
                    .mes((String) row[0])
                    .novos(novos)
                    .acumulado(acumuladoCli)
                    .build());
        }

        ClientesDetalheDTO clientes = ClientesDetalheDTO.builder()
                .total(totalClientes)
                .ativos(clientesAtivos)
                .inativos(totalClientes - clientesAtivos)
                .cadastroIncompleto(nz(adminQueryRepository.countClientesCadastroIncompletoByOrganizacao(organizacaoId)))
                .novosNoMes(clientesNovosNoMes)
                .novosUltimos30Dias(clientesNovos30d)
                .mediaAgendamentosPorCliente(media(totalAgendamentos, totalClientes))
                .evolucaoMensal(evolClientes)
                .build();

        // ── Funcionarios detalhe ──
        Map<String, Long> funcionariosPorSituacao = new LinkedHashMap<>();
        for (Object[] row : adminQueryRepository.countFuncionariosPorSituacaoByOrganizacao(organizacaoId)) {
            funcionariosPorSituacao.merge(row[0] != null ? row[0].toString() : "(sem situacao)", asLong(row[1]), Long::sum);
        }
        FuncionariosDetalheDTO funcionarios = FuncionariosDetalheDTO.builder()
                .total(totalFuncionarios)
                .ativos(funcionariosAtivos)
                .inativos(totalFuncionarios - funcionariosAtivos)
                .porSituacao(funcionariosPorSituacao)
                .build();

        // ── Servicos detalhe ──
        List<ContagemPorCategoria> servicosPorCategoria = new ArrayList<>();
        for (Object[] row : adminQueryRepository.countServicosPorCategoriaByOrganizacao(organizacaoId)) {
            servicosPorCategoria.add(ContagemPorCategoria.builder()
                    .categoria(row[0] != null ? row[0].toString() : "(sem categoria)")
                    .quantidade(asLong(row[1]))
                    .build());
        }
        BigDecimal precoMedio = adminQueryRepository.calcularPrecoMedioServicosByOrganizacao(organizacaoId);
        ServicosDetalheDTO servicos = ServicosDetalheDTO.builder()
                .total(totalServicos)
                .ativos(servicosAtivos)
                .inativos(totalServicos - servicosAtivos)
                .precoMedio(precoMedio != null ? precoMedio.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .porCategoria(servicosPorCategoria)
                .build();

        // ── Financeiro detalhe ──
        Map<String, Long> cobrancasPorStatus = new LinkedHashMap<>();
        BigDecimal valorPendente = BigDecimal.ZERO;
        BigDecimal valorVencido = BigDecimal.ZERO;
        long totalCobrancas = 0;
        for (Object[] row : adminQueryRepository.countECalcularCobrancasPorStatusByOrganizacao(organizacaoId)) {
            String status = row[0] != null ? row[0].toString() : "DESCONHECIDO";
            long qtd = asLong(row[1]);
            BigDecimal soma = asBigDecimal(row[2]);
            cobrancasPorStatus.merge(status, qtd, Long::sum);
            totalCobrancas += qtd;
            if ("PENDENTE".equals(status) || "PARCIALMENTE_PAGO".equals(status)) valorPendente = valorPendente.add(soma);
            if ("VENCIDA".equals(status)) valorVencido = valorVencido.add(soma);
        }

        Map<String, Long> cobrancasPorTipo = new LinkedHashMap<>();
        for (Object[] row : adminQueryRepository.countCobrancasPorTipoByOrganizacao(organizacaoId)) {
            cobrancasPorTipo.merge(row[0] != null ? row[0].toString() : "DESCONHECIDO", asLong(row[1]), Long::sum);
        }

        Map<String, Long> pagamentosPorStatus = new LinkedHashMap<>();
        for (Object[] row : adminQueryRepository.countPagamentosPorStatusByOrganizacao(organizacaoId)) {
            pagamentosPorStatus.merge(row[0] != null ? row[0].toString() : "DESCONHECIDO", asLong(row[1]), Long::sum);
        }

        long pagamentosConfirmados = nz(adminQueryRepository.countPagamentosConfirmadosByOrganizacao(organizacaoId));
        BigDecimal ticketMedio = pagamentosConfirmados > 0
                ? faturamentoTotal.divide(BigDecimal.valueOf(pagamentosConfirmados), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<SerieMensalFaturamento> evolFaturamento = new ArrayList<>();
        for (Object[] row : adminQueryRepository.calcularFaturamentoMensalByOrganizacao(organizacaoId, inicio12Meses)) {
            evolFaturamento.add(SerieMensalFaturamento.builder()
                    .mes((String) row[0])
                    .valor(asBigDecimal(row[1]))
                    .quantidade(asLong(row[2]))
                    .build());
        }

        FinanceiroDetalheDTO financeiro = FinanceiroDetalheDTO.builder()
                .faturamentoTotal(faturamentoTotal)
                .faturamentoMes(faturamentoMes)
                .ticketMedio(ticketMedio)
                .valorPendente(valorPendente)
                .valorVencido(valorVencido)
                .totalCobrancas(totalCobrancas)
                .cobrancasPorStatus(cobrancasPorStatus)
                .cobrancasPorTipo(cobrancasPorTipo)
                .pagamentosPorStatus(pagamentosPorStatus)
                .evolucaoMensal(evolFaturamento)
                .build();

        // ── Endereco ──
        EnderecoDTO enderecoDTO = null;
        Endereco end = org.getEnderecoPrincipal();
        if (end != null) {
            enderecoDTO = EnderecoDTO.builder()
                    .logradouro(end.getLogradouro())
                    .numero(end.getNumero())
                    .complemento(end.getComplemento())
                    .bairro(end.getBairro())
                    .cidade(end.getCidade())
                    .uf(end.getUf())
                    .cep(end.getCep())
                    .build();
        }

        // ── Redes Sociais ──
        RedesSociaisDTO redesDTO = null;
        RedesSociais redes = org.getRedesSociais();
        if (redes != null) {
            redesDTO = RedesSociaisDTO.builder()
                    .instagram(redes.getInstagram())
                    .facebook(redes.getFacebook())
                    .whatsapp(redes.getWhatsapp())
                    .linkedin(redes.getLinkedin())
                    .youtube(redes.getYoutube())
                    .site(redes.getSite())
                    .build();
        }

        // ── Payment API (fail-safe; chamadas diretas — retornam o shape completo esperado pelo front) ──
        Long paymentCustomerId = null;
        Long paymentSubscriptionId = null;
        SubscriptionResponse assinaturaAtiva = null;
        AccessStatusResponse accessStatus = null;
        PlanResponse planoDetalhado = null;

        Optional<Assinatura> assinaturaOpt = assinaturaRepository.findByOrganizacaoId(organizacaoId);
        if (assinaturaOpt.isPresent()) {
            Assinatura assinatura = assinaturaOpt.get();
            paymentCustomerId = assinatura.getPaymentApiCustomerId();
            paymentSubscriptionId = assinatura.getPaymentApiSubscriptionId();

            if (paymentSubscriptionId != null) {
                try {
                    assinaturaAtiva = paymentApiClient.getSubscription(paymentSubscriptionId);
                } catch (Exception e) {
                    log.warn("Falha ao buscar assinatura {} da Payment API: {}", paymentSubscriptionId, e.getMessage());
                }
            }
            if (paymentCustomerId != null) {
                try {
                    accessStatus = paymentApiClient.getAccessStatus(paymentCustomerId);
                } catch (Exception e) {
                    log.warn("Falha ao buscar access-status do customer {}: {}", paymentCustomerId, e.getMessage());
                }
            }
            if (assinaturaAtiva != null && assinaturaAtiva.getPlanId() != null) {
                try {
                    planoDetalhado = paymentApiClient.getPlan(assinaturaAtiva.getPlanId());
                } catch (Exception e) {
                    log.warn("Falha ao buscar plano {} da Payment API: {}", assinaturaAtiva.getPlanId(), e.getMessage());
                }
            }
        }

        return AdminOrganizacaoDetalheDTO.builder()
                .geradoEm(agora)
                .id(org.getId())
                .nomeFantasia(org.getNomeFantasia())
                .razaoSocial(org.getRazaoSocial())
                .cnpj(org.getCnpj())
                .inscricaoEstadual(org.getInscricaoEstadual())
                .publicoAlvo(org.getPublicoAlvo())
                .emailPrincipal(org.getEmailPrincipal())
                .telefone1(org.getTelefone1())
                .telefone2(org.getTelefone2())
                .whatsapp(org.getWhatsapp())
                .slug(org.getSlug())
                .ativo(org.getAtivo())
                .logoUrl(org.getLogoUrl())
                .bannerUrl(org.getBannerUrl())
                .dtCadastro(org.getDtCadastro())
                .dtAtualizacao(org.getDtAtualizacao())
                .responsavelNome(org.getResponsavel() != null ? org.getResponsavel().getNome() : null)
                .responsavelEmail(org.getResponsavel() != null ? org.getResponsavel().getEmail() : null)
                .responsavelTelefone(org.getResponsavel() != null ? org.getResponsavel().getTelefone() : null)
                .endereco(enderecoDTO)
                .redesSociais(redesDTO)
                .paymentApiCustomerId(paymentCustomerId)
                .paymentApiSubscriptionId(paymentSubscriptionId)
                .assinaturaAtiva(assinaturaAtiva)
                .accessStatus(accessStatus)
                .planoDetalhado(planoDetalhado)
                .configSistema(buildConfigSistemaDTO(org.getConfigSistema()))
                .instancias(instanciasList)
                .engajamento(engajamento)
                .onboarding(onboarding)
                .agendamentos(agendamentos)
                .clientes(clientes)
                .funcionarios(funcionarios)
                .servicos(servicos)
                .financeiro(financeiro)
                .build();
    }

    private AdminOrganizacaoDetalheDTO.ConfigSistemaDTO buildConfigSistemaDTO(ConfigSistema cs) {
        if (cs == null) return null;
        var ag = cs.getConfigAgendamento();
        var sv = cs.getConfigServico();
        var cl = cs.getConfigCliente();
        var co = cs.getConfigColaborador();
        var no = cs.getConfigNotificacao();
        return AdminOrganizacaoDetalheDTO.ConfigSistemaDTO.builder()
                .usaEcommerce(cs.isUsaEcommerce())
                .usaGestaoProdutos(cs.isUsaGestaoProdutos())
                .usaPlanosParaClientes(cs.isUsaPlanosParaClientes())
                .disparaNotificacoesPush(cs.isDisparaNotificacoesPush())
                .urlAcesso(cs.getUrlAcesso())
                // Agendamento
                .toleranciaAgendamento(ag != null ? ag.getToleranciaAgendamento() : null)
                .minDiasAgendamento(ag != null ? ag.getMinDiasAgendamento() : null)
                .maxDiasAgendamento(ag != null ? ag.getMaxDiasAgendamento() : null)
                .cancelamentoCliente(ag != null ? ag.getCancelamentoCliente() : null)
                .mostrarAgendamentoCancelado(ag != null ? ag.getMostrarAgendamentoCancelado() : null)
                .tempoCancelamentoCliente(ag != null ? ag.getTempoCancelamentoCliente() : null)
                .aprovarAgendamento(ag != null ? ag.getAprovarAgendamento() : null)
                .aprovarAgendamentoAgente(ag != null ? ag.getAprovarAgendamentoAgente() : null)
                .ocultarFimSemana(ag != null ? ag.getOcultarFimSemana() : null)
                .ocultarDomingo(ag != null ? ag.getOcultarDomingo() : null)
                .cobrarSinal(ag != null ? ag.getCobrarSinal() : null)
                .porcentSinal(ag != null ? ag.getPorcentSinal() : null)
                .cobrarSinalAgente(ag != null ? ag.getCobrarSinalAgente() : null)
                .porcentSinalAgente(ag != null ? ag.getPorcentSinalAgente() : null)
                .modoVizualizacao(ag != null ? ag.getModoVizualizacao() : null)
                .usarFilaEspera(ag != null ? ag.getUsarFilaEspera() : null)
                .filaMaxCascata(ag != null ? ag.getFilaMaxCascata() : null)
                .filaTimeoutMinutos(ag != null ? ag.getFilaTimeoutMinutos() : null)
                .filaAntecedenciaHoras(ag != null ? ag.getFilaAntecedenciaHoras() : null)
                // Servico
                .mostrarValorAgendamento(sv != null ? sv.getMostrarValorAgendamento() : null)
                .unicoServicoAgendamento(sv != null ? sv.getUnicoServicoAgendamento() : null)
                .mostrarAvaliacao(sv != null ? sv.getMostrarAvaliacao() : null)
                // Cliente
                .precisaCadastroAgendar(cl != null ? cl.getPrecisaCadastroAgendar() : null)
                .programaFidelidade(cl != null ? cl.getProgramaFidelidade() : null)
                .valorGastoUmPonto(cl != null ? cl.getValorGastoUmPonto() : null)
                // Colaborador
                .selecionarColaboradorAgendamento(co != null ? co.getSelecionarColaboradorAgendamento() : null)
                .mostrarNotasComentarioColaborador(co != null ? co.getMostrarNotasComentarioColaborador() : null)
                .comissaoPadrao(co != null ? co.getComissaoPadrao() : null)
                // Notificacao
                .enviarConfirmacaoWhatsapp(no != null ? no.getEnviarConfirmacaoWhatsapp() : null)
                .enviarLembreteWhatsapp(no != null ? no.getEnviarLembreteWhatsapp() : null)
                .enviarLembreteSms(no != null ? no.getEnviarLembreteSMS() : null)
                .enviarLembreteEmail(no != null ? no.getEnviarLembreteEmail() : null)
                .enviarConfirmacaoForaHorario(no != null ? no.getEnviarConfirmacaoForaHorario() : null)
                .tempoParaConfirmacao(no != null ? no.getTempoParaConfirmacao() : null)
                .tempoLembretePosConfirmacao(no != null ? no.getTempoLembretePosConfirmacao() : null)
                .mensagemTemplateConfirmacao(no != null ? no.getMensagemTemplateConfirmacao() : null)
                .mensagemTemplateLembrete(no != null ? no.getMensagemTemplateLembrete() : null)
                .build();
    }

    // ==================== DERIVACOES ====================

    /**
     * NOVO: conta com menos de 30 dias. INATIVO: nunca teve atividade ou > 60 dias sem.
     * EM_RISCO: > 30 dias sem atividade. ATIVO: caso contrario.
     */
    private static String derivarStatusUso(Long diasDesdeCadastro, Long diasSemAtividade) {
        if (diasDesdeCadastro != null && diasDesdeCadastro < 30) return "NOVO";
        if (diasSemAtividade == null) return "INATIVO";
        if (diasSemAtividade > 60) return "INATIVO";
        if (diasSemAtividade > 30) return "EM_RISCO";
        return "ATIVO";
    }

    /**
     * Score 0-100: atividade recente (40), instancia conectada (20), onboarding (ate 25), faturamento no mes (15).
     */
    private static Integer calcularHealthScore(Long diasSemAtividade, boolean instanciaConectada,
                                               int percentualOnboarding, BigDecimal faturamentoMes) {
        int score = 0;
        if (diasSemAtividade != null) {
            if (diasSemAtividade <= 7) score += 40;
            else if (diasSemAtividade <= 30) score += 25;
            else if (diasSemAtividade <= 60) score += 10;
        }
        if (instanciaConectada) score += 20;
        score += (int) Math.round(percentualOnboarding * 0.25);
        if (faturamentoMes != null && faturamentoMes.compareTo(BigDecimal.ZERO) > 0) score += 15;
        return Math.max(0, Math.min(100, score));
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

    private static LocalDateTime maxData(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
