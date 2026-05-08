package org.exemplo.belloryadmin.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.client.payment.PaymentApiClient;
import org.exemplo.belloryadmin.client.payment.dto.AccessStatusResponse;
import org.exemplo.belloryadmin.client.payment.dto.PlanResponse;
import org.exemplo.belloryadmin.client.payment.dto.SubscriptionResponse;
import org.exemplo.belloryadmin.model.dto.admin.AdminOrganizacaoDetalheDTO;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminOrganizacaoService {

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

        // ── Metricas ──
        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime fimMes = LocalDateTime.now();

        AdminOrganizacaoDetalheDTO.MetricasOrganizacao metricas = AdminOrganizacaoDetalheDTO.MetricasOrganizacao.builder()
                .totalAgendamentos(adminQueryRepository.countAgendamentosByOrganizacao(organizacaoId))
                .agendamentosNoMes(adminQueryRepository.countAgendamentosByOrganizacaoNoPeriodo(organizacaoId, inicioMes, fimMes))
                .agendamentosConcluidos(adminQueryRepository.countAgendamentosConcluidosByOrganizacao(organizacaoId))
                .agendamentosCancelados(adminQueryRepository.countAgendamentosCanceladosByOrganizacao(organizacaoId))
                .agendamentosPendentes(adminQueryRepository.countAgendamentosPendentesByOrganizacao(organizacaoId))
                .totalClientes(adminQueryRepository.countClientesByOrganizacao(organizacaoId))
                .clientesAtivos(adminQueryRepository.countClientesAtivosByOrganizacao(organizacaoId))
                .totalFuncionarios(adminQueryRepository.countFuncionariosByOrganizacao(organizacaoId))
                .funcionariosAtivos(adminQueryRepository.countFuncionariosAtivosByOrganizacao(organizacaoId))
                .totalServicos(adminQueryRepository.countServicosByOrganizacao(organizacaoId))
                .servicosAtivos(adminQueryRepository.countServicosAtivosByOrganizacao(organizacaoId))
                .faturamentoTotal(adminQueryRepository.calcularFaturamentoByOrganizacao(organizacaoId))
                .faturamentoMes(adminQueryRepository.calcularFaturamentoByOrganizacaoNoPeriodo(organizacaoId, inicioMes, fimMes))
                .totalCobrancas(adminQueryRepository.countCobrancasByOrganizacao(organizacaoId))
                .cobrancasPagas(adminQueryRepository.countCobrancasPagasByOrganizacao(organizacaoId))
                .cobrancasPendentes(adminQueryRepository.countCobrancasPendentesByOrganizacao(organizacaoId))
                .cobrancasVencidas(adminQueryRepository.countCobrancasVencidasByOrganizacao(organizacaoId))
                .build();

        // ── Instancias ──
        List<Instance> instancias = instanceRepository.findByOrganizacaoIdAndDeletadoFalse(organizacaoId);
        List<AdminOrganizacaoDetalheDTO.InstanciaResumoDTO> instanciasList = instancias.stream()
                .map(i -> AdminOrganizacaoDetalheDTO.InstanciaResumoDTO.builder()
                        .id(i.getId())
                        .instanceName(i.getInstanceName())
                        .instanceId(i.getInstanceId())
                        .status(i.getStatus() != null ? i.getStatus().name() : null)
                        .ativo(i.isAtivo())
                        .build())
                .collect(Collectors.toList());

        // ── Endereco ──
        AdminOrganizacaoDetalheDTO.EnderecoDTO enderecoDTO = null;
        Endereco end = org.getEnderecoPrincipal();
        if (end != null) {
            enderecoDTO = AdminOrganizacaoDetalheDTO.EnderecoDTO.builder()
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
        AdminOrganizacaoDetalheDTO.RedesSociaisDTO redesDTO = null;
        RedesSociais redes = org.getRedesSociais();
        if (redes != null) {
            redesDTO = AdminOrganizacaoDetalheDTO.RedesSociaisDTO.builder()
                    .instagram(redes.getInstagram())
                    .facebook(redes.getFacebook())
                    .whatsapp(redes.getWhatsapp())
                    .linkedin(redes.getLinkedin())
                    .youtube(redes.getYoutube())
                    .site(redes.getSite())
                    .build();
        }

        // ── Payment API (fail-safe) ──
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

            // Buscar assinatura ativa
            if (paymentSubscriptionId != null) {
                try {
                    assinaturaAtiva = paymentApiClient.getSubscription(paymentSubscriptionId);
                } catch (Exception e) {
                    log.warn("Falha ao buscar assinatura {} da Payment API: {}", paymentSubscriptionId, e.getMessage());
                }
            }

            // Buscar status de acesso
            if (paymentCustomerId != null) {
                try {
                    accessStatus = paymentApiClient.getAccessStatus(paymentCustomerId);
                } catch (Exception e) {
                    log.warn("Falha ao buscar access-status do customer {}: {}", paymentCustomerId, e.getMessage());
                }
            }

            // Buscar plano detalhado
            if (assinaturaAtiva != null && assinaturaAtiva.getPlanId() != null) {
                try {
                    planoDetalhado = paymentApiClient.getPlan(assinaturaAtiva.getPlanId());
                } catch (Exception e) {
                    log.warn("Falha ao buscar plano {} da Payment API: {}", assinaturaAtiva.getPlanId(), e.getMessage());
                }
            }
        }

        return AdminOrganizacaoDetalheDTO.builder()
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
                .metricas(metricas)
                .instancias(instanciasList)
                .build();
    }

    private AdminOrganizacaoDetalheDTO.ConfigSistemaDTO buildConfigSistemaDTO(ConfigSistema cs) {
        if (cs == null) return null;
        return AdminOrganizacaoDetalheDTO.ConfigSistemaDTO.builder()
                .usaEcommerce(cs.isUsaEcommerce())
                .usaGestaoProdutos(cs.isUsaGestaoProdutos())
                .usaPlanosParaClientes(cs.isUsaPlanosParaClientes())
                .disparaNotificacoesPush(cs.isDisparaNotificacoesPush())
                .urlAcesso(cs.getUrlAcesso())
                .toleranciaAgendamento(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getToleranciaAgendamento() : null)
                .minDiasAgendamento(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getMinDiasAgendamento() : null)
                .maxDiasAgendamento(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getMaxDiasAgendamento() : null)
                .cancelamentoCliente(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getCancelamentoCliente() : null)
                .aprovarAgendamento(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getAprovarAgendamento() : null)
                .ocultarFimSemana(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getOcultarFimSemana() : null)
                .cobrarSinal(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getCobrarSinal() : null)
                .porcentSinal(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getPorcentSinal() : null)
                .modoVizualizacao(cs.getConfigAgendamento() != null ? cs.getConfigAgendamento().getModoVizualizacao() : null)
                .mostrarValorAgendamento(cs.getConfigServico() != null ? cs.getConfigServico().getMostrarValorAgendamento() : null)
                .unicoServicoAgendamento(cs.getConfigServico() != null ? cs.getConfigServico().getUnicoServicoAgendamento() : null)
                .mostrarAvaliacao(cs.getConfigServico() != null ? cs.getConfigServico().getMostrarAvaliacao() : null)
                .precisaCadastroAgendar(cs.getConfigCliente() != null ? cs.getConfigCliente().getPrecisaCadastroAgendar() : null)
                .programaFidelidade(cs.getConfigCliente() != null ? cs.getConfigCliente().getProgramaFidelidade() : null)
                .selecionarColaboradorAgendamento(cs.getConfigColaborador() != null ? cs.getConfigColaborador().getSelecionarColaboradorAgendamento() : null)
                .comissaoPadrao(cs.getConfigColaborador() != null ? cs.getConfigColaborador().getComissaoPadrao() : null)
                .enviarConfirmacaoWhatsapp(cs.getConfigNotificacao() != null ? cs.getConfigNotificacao().getEnviarConfirmacaoWhatsapp() : null)
                .enviarLembreteWhatsapp(cs.getConfigNotificacao() != null ? cs.getConfigNotificacao().getEnviarLembreteWhatsapp() : null)
                .enviarLembreteEmail(cs.getConfigNotificacao() != null ? cs.getConfigNotificacao().getEnviarLembreteEmail() : null)
                .build();
    }
}
