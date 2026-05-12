package org.exemplo.belloryadmin.model.repository.admin;

import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminQueryRepository extends JpaRepository<Organizacao, Long> {

    // === DASHBOARD GERAL ===

    @Query("SELECT COUNT(o) FROM Organizacao o WHERE o.ativo = true")
    Long countOrganizacoesAtivas();

    @Query("SELECT COUNT(o) FROM Organizacao o WHERE o.ativo = false")
    Long countOrganizacoesInativas();

    // === AGENDAMENTOS ===

    @Query("SELECT COUNT(a) FROM Agendamento a")
    Long countTotalAgendamentos();

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.dtCriacao >= :inicio AND a.dtCriacao <= :fim")
    Long countAgendamentosNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.status = 'CONCLUIDO'")
    Long countAgendamentosConcluidos();

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.status = 'CANCELADO'")
    Long countAgendamentosCancelados();

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.status = 'PENDENTE'")
    Long countAgendamentosPendentes();

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.status = 'AGENDADO'")
    Long countAgendamentosAgendados();

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.status = 'NAO_COMPARECEU'")
    Long countAgendamentosNaoCompareceu();

    @Query("SELECT a.organizacao.id, a.organizacao.nomeFantasia, COUNT(a), " +
            "SUM(CASE WHEN a.status = 'CONCLUIDO' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.status = 'CANCELADO' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.status = 'PENDENTE' THEN 1 ELSE 0 END) " +
            "FROM Agendamento a GROUP BY a.organizacao.id, a.organizacao.nomeFantasia ORDER BY COUNT(a) DESC")
    List<Object[]> countAgendamentosPorOrganizacao();

    @Query("SELECT FUNCTION('TO_CHAR', a.dtCriacao, 'YYYY-MM') as mes, COUNT(a), " +
            "SUM(CASE WHEN a.status = 'CONCLUIDO' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.status = 'CANCELADO' THEN 1 ELSE 0 END) " +
            "FROM Agendamento a WHERE a.dtCriacao >= :inicio " +
            "GROUP BY FUNCTION('TO_CHAR', a.dtCriacao, 'YYYY-MM') " +
            "ORDER BY FUNCTION('TO_CHAR', a.dtCriacao, 'YYYY-MM') ASC")
    List<Object[]> countAgendamentosMensais(@Param("inicio") LocalDateTime inicio);

    // === CLIENTES ===

    @Query("SELECT COUNT(c) FROM Cliente c")
    Long countTotalClientes();

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.ativo = true")
    Long countClientesAtivos();

    @Query("SELECT c.organizacao.id, c.organizacao.nomeFantasia, COUNT(c), " +
            "SUM(CASE WHEN c.ativo = true THEN 1 ELSE 0 END) " +
            "FROM Cliente c GROUP BY c.organizacao.id, c.organizacao.nomeFantasia ORDER BY COUNT(c) DESC")
    List<Object[]> countClientesPorOrganizacao();

    @Query("SELECT FUNCTION('TO_CHAR', c.dtCriacao, 'YYYY-MM') as mes, COUNT(c) " +
            "FROM Cliente c WHERE c.dtCriacao >= :inicio " +
            "GROUP BY FUNCTION('TO_CHAR', c.dtCriacao, 'YYYY-MM') " +
            "ORDER BY FUNCTION('TO_CHAR', c.dtCriacao, 'YYYY-MM') ASC")
    List<Object[]> countClientesMensais(@Param("inicio") LocalDateTime inicio);

    // === FUNCIONARIOS ===

    @Query("SELECT COUNT(f) FROM Funcionario f")
    Long countTotalFuncionarios();

    @Query("SELECT COUNT(f) FROM Funcionario f WHERE f.situacao = 'Ativo'")
    Long countFuncionariosAtivos();

    @Query("SELECT f.organizacao.id, f.organizacao.nomeFantasia, COUNT(f), " +
            "SUM(CASE WHEN f.situacao = 'Ativo' THEN 1 ELSE 0 END), " +
            "SIZE(f.servicos) " +
            "FROM Funcionario f GROUP BY f.organizacao.id, f.organizacao.nomeFantasia ORDER BY COUNT(f) DESC")
    List<Object[]> countFuncionariosPorOrganizacao();

    // === SERVICOS ===

    @Query("SELECT COUNT(s) FROM Servico s WHERE s.isDeletado = false")
    Long countTotalServicos();

    @Query("SELECT COUNT(s) FROM Servico s WHERE s.ativo = true AND s.isDeletado = false")
    Long countServicosAtivos();

    @Query("SELECT AVG(s.preco) FROM Servico s WHERE s.ativo = true AND s.isDeletado = false")
    BigDecimal calcularPrecoMedioServicos();

    @Query("SELECT s.organizacao.id, s.organizacao.nomeFantasia, COUNT(s), " +
            "SUM(CASE WHEN s.ativo = true THEN 1 ELSE 0 END) " +
            "FROM Servico s WHERE s.isDeletado = false " +
            "GROUP BY s.organizacao.id, s.organizacao.nomeFantasia ORDER BY COUNT(s) DESC")
    List<Object[]> countServicosPorOrganizacao();

    // === COBRANCAS E FATURAMENTO ===

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.statusPagamento = 'CONFIRMADO'")
    BigDecimal calcularFaturamentoTotal();

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.statusPagamento = 'CONFIRMADO' AND p.dtPagamento >= :inicio AND p.dtPagamento <= :fim")
    BigDecimal calcularFaturamentoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(p) FROM Pagamento p")
    Long countTotalPagamentos();

    @Query("SELECT COUNT(p) FROM Pagamento p WHERE p.statusPagamento = 'CONFIRMADO'")
    Long countPagamentosConfirmados();

    @Query("SELECT COUNT(c) FROM Cobranca c")
    Long countTotalCobrancas();

    @Query("SELECT COUNT(c) FROM Cobranca c WHERE c.statusCobranca = 'PENDENTE'")
    Long countCobrancasPendentes();

    @Query("SELECT COUNT(c) FROM Cobranca c WHERE c.statusCobranca = 'PAGO'")
    Long countCobrancasPagas();

    @Query("SELECT COUNT(c) FROM Cobranca c WHERE c.statusCobranca = 'VENCIDA'")
    Long countCobrancasVencidas();

    @Query("SELECT p.organizacao.id, p.organizacao.nomeFantasia, " +
            "COALESCE(SUM(p.valor), 0), " +
            "COUNT(p) " +
            "FROM Pagamento p WHERE p.statusPagamento = 'CONFIRMADO' " +
            "GROUP BY p.organizacao.id, p.organizacao.nomeFantasia ORDER BY SUM(p.valor) DESC")
    List<Object[]> calcularFaturamentoPorOrganizacao();

    @Query("SELECT p.organizacao.id, p.organizacao.nomeFantasia, " +
            "COALESCE(SUM(p.valor), 0), " +
            "COUNT(p) " +
            "FROM Pagamento p WHERE p.statusPagamento = 'CONFIRMADO' " +
            "AND p.dtPagamento >= :inicio AND p.dtPagamento <= :fim " +
            "GROUP BY p.organizacao.id, p.organizacao.nomeFantasia ORDER BY SUM(p.valor) DESC")
    List<Object[]> calcularFaturamentoPorOrganizacaoNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT FUNCTION('TO_CHAR', p.dtPagamento, 'YYYY-MM') as mes, COALESCE(SUM(p.valor), 0), COUNT(p) " +
            "FROM Pagamento p WHERE p.statusPagamento = 'CONFIRMADO' AND p.dtPagamento >= :inicio " +
            "GROUP BY FUNCTION('TO_CHAR', p.dtPagamento, 'YYYY-MM') " +
            "ORDER BY FUNCTION('TO_CHAR', p.dtPagamento, 'YYYY-MM') ASC")
    List<Object[]> calcularFaturamentoMensal(@Param("inicio") LocalDateTime inicio);

    // === INSTANCIAS ===

    @Query("SELECT COUNT(i) FROM Instance i WHERE i.deletado = false")
    Long countTotalInstancias();

    @Query("SELECT COUNT(i) FROM Instance i WHERE i.ativo = true AND i.deletado = false")
    Long countInstanciasAtivas();

    @Query("SELECT i.organizacao.id, i.organizacao.nomeFantasia, COUNT(i), " +
            "SUM(CASE WHEN i.ativo = true THEN 1 ELSE 0 END) " +
            "FROM Instance i WHERE i.deletado = false " +
            "GROUP BY i.organizacao.id, i.organizacao.nomeFantasia ORDER BY COUNT(i) DESC")
    List<Object[]> countInstanciasPorOrganizacao();

    // === METRICAS POR ORGANIZACAO ESPECIFICA ===

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId")
    Long countAgendamentosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId AND a.dtCriacao >= :inicio AND a.dtCriacao <= :fim")
    Long countAgendamentosByOrganizacaoNoPeriodo(@Param("orgId") Long orgId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId AND a.status = 'CONCLUIDO'")
    Long countAgendamentosConcluidosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId AND a.status = 'CANCELADO'")
    Long countAgendamentosCanceladosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId AND a.status = 'PENDENTE'")
    Long countAgendamentosPendentesByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.organizacao.id = :orgId")
    Long countClientesByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.organizacao.id = :orgId AND c.ativo = true")
    Long countClientesAtivosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(f) FROM Funcionario f WHERE f.organizacao.id = :orgId")
    Long countFuncionariosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(f) FROM Funcionario f WHERE f.organizacao.id = :orgId AND f.situacao = 'Ativo'")
    Long countFuncionariosAtivosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(s) FROM Servico s WHERE s.organizacao.id = :orgId AND s.isDeletado = false")
    Long countServicosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(s) FROM Servico s WHERE s.organizacao.id = :orgId AND s.ativo = true AND s.isDeletado = false")
    Long countServicosAtivosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.organizacao.id = :orgId AND p.statusPagamento = 'CONFIRMADO'")
    BigDecimal calcularFaturamentoByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.organizacao.id = :orgId AND p.statusPagamento = 'CONFIRMADO' AND p.dtPagamento >= :inicio AND p.dtPagamento <= :fim")
    BigDecimal calcularFaturamentoByOrganizacaoNoPeriodo(@Param("orgId") Long orgId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT COUNT(c) FROM Cobranca c WHERE c.organizacao.id = :orgId")
    Long countCobrancasByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(c) FROM Cobranca c WHERE c.organizacao.id = :orgId AND c.statusCobranca = 'PAGO'")
    Long countCobrancasPagasByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(c) FROM Cobranca c WHERE c.organizacao.id = :orgId AND c.statusCobranca = 'PENDENTE'")
    Long countCobrancasPendentesByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(c) FROM Cobranca c WHERE c.organizacao.id = :orgId AND c.statusCobranca = 'VENCIDA'")
    Long countCobrancasVencidasByOrganizacao(@Param("orgId") Long orgId);

    // === LOCALIZACOES (MAPA) ===

    @Query("SELECT e.cidade, e.uf, e.coordenadas.latitude, e.coordenadas.longitude " +
            "FROM Organizacao o JOIN o.enderecoPrincipal e " +
            "WHERE o.ativo = true " +
            "AND e.coordenadas.latitude IS NOT NULL AND e.coordenadas.longitude IS NOT NULL")
    List<Object[]> findLocalizacoesOrganizacoes();

    // === ORGANIZACOES COM DETALHES ===

    @Query("SELECT DISTINCT o FROM Organizacao o " +
            "LEFT JOIN FETCH o.configSistema " +
            "ORDER BY o.dtCadastro DESC")
    List<Organizacao> findAllOrganizacoesComPlano();

    @Query("SELECT DISTINCT o FROM Organizacao o " +
            "LEFT JOIN FETCH o.configSistema " +
            "LEFT JOIN FETCH o.enderecoPrincipal " +
            "LEFT JOIN FETCH o.dadosFaturamento " +
            "WHERE o.id = :id")
    Optional<Organizacao> findOrganizacaoComDetalhesById(@Param("id") Long id);

    // ============================================================
    // === DASHBOARD V2 (agregacoes adicionais) ===
    // ============================================================

    // --- Organizacoes ---

    @Query("SELECT COUNT(o) FROM Organizacao o WHERE o.dtCadastro >= :inicio AND o.dtCadastro <= :fim")
    Long countOrganizacoesNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT UPPER(TRIM(e.uf)), COUNT(o) FROM Organizacao o JOIN o.enderecoPrincipal e " +
            "WHERE o.ativo = true AND e.uf IS NOT NULL AND TRIM(e.uf) <> '' " +
            "GROUP BY UPPER(TRIM(e.uf)) ORDER BY COUNT(o) DESC")
    List<Object[]> countOrganizacoesAtivasPorUf();

    @Query("SELECT " +
            "SUM(CASE WHEN cs.usaEcommerce = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN cs.usaGestaoProdutos = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN cs.usaPlanosParaClientes = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN cs.disparaNotificacoesPush = true THEN 1 ELSE 0 END) " +
            "FROM ConfigSistema cs JOIN cs.organizacao o WHERE o.ativo = true")
    List<Object[]> countAdocaoFeaturesOrganizacoesAtivas();

    @Query("SELECT COUNT(o) FROM Organizacao o WHERE o.ativo = true " +
            "AND NOT EXISTS (SELECT a FROM Agendamento a WHERE a.organizacao.id = o.id)")
    Long countOrganizacoesAtivasSemAgendamento();

    @Query("SELECT COUNT(o) FROM Organizacao o WHERE o.ativo = true " +
            "AND NOT EXISTS (SELECT i FROM Instance i WHERE i.organizacao.id = o.id AND i.deletado = false)")
    Long countOrganizacoesAtivasSemInstancia();

    // --- Agendamentos ---

    @Query("SELECT a.status, COUNT(a) FROM Agendamento a GROUP BY a.status")
    List<Object[]> countAgendamentosAgrupadosPorStatus();

    // --- Clientes ---

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.isCadastroIncompleto = true")
    Long countClientesCadastroIncompleto();

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.dtCriacao >= :inicio AND c.dtCriacao <= :fim")
    Long countClientesNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // --- Instancias ---

    @Query("SELECT i.status, COUNT(i) FROM Instance i WHERE i.deletado = false GROUP BY i.status")
    List<Object[]> countInstanciasAgrupadasPorStatus();

    // --- Cobrancas / Pagamentos ---

    @Query("SELECT c.statusCobranca, COUNT(c), COALESCE(SUM(c.valor), 0) FROM Cobranca c GROUP BY c.statusCobranca")
    List<Object[]> countECalcularCobrancasPorStatus();

    @Query("SELECT c.tipoCobranca, COUNT(c) FROM Cobranca c GROUP BY c.tipoCobranca")
    List<Object[]> countCobrancasPorTipo();

    @Query("SELECT p.statusPagamento, COUNT(p) FROM Pagamento p GROUP BY p.statusPagamento")
    List<Object[]> countPagamentosPorStatus();

    // --- Localizacoes agregadas ---

    @Query("SELECT e.cidade, e.uf, e.coordenadas.latitude, e.coordenadas.longitude, COUNT(o) " +
            "FROM Organizacao o JOIN o.enderecoPrincipal e " +
            "WHERE o.ativo = true " +
            "AND e.coordenadas.latitude IS NOT NULL AND e.coordenadas.longitude IS NOT NULL " +
            "GROUP BY e.cidade, e.uf, e.coordenadas.latitude, e.coordenadas.longitude")
    List<Object[]> findLocalizacoesOrganizacoesAgregadas();

    // ============================================================
    // === DETALHE DA ORGANIZACAO (engajamento / desempenho) ===
    // ============================================================

    // --- Engajamento / ultimas atividades ---

    @Query("SELECT MAX(a.dtCriacao) FROM Agendamento a WHERE a.organizacao.id = :orgId")
    LocalDateTime findUltimoAgendamentoCriadoEmByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT MAX(a.dtAgendamento) FROM Agendamento a WHERE a.organizacao.id = :orgId " +
            "AND a.status = 'CONCLUIDO' AND a.dtAgendamento <= :agora")
    LocalDateTime findUltimoAgendamentoRealizadoEmByOrganizacao(@Param("orgId") Long orgId, @Param("agora") LocalDateTime agora);

    @Query("SELECT MIN(a.dtAgendamento) FROM Agendamento a WHERE a.organizacao.id = :orgId " +
            "AND a.dtAgendamento >= :agora AND a.status NOT IN ('CANCELADO', 'CONCLUIDO', 'NAO_COMPARECEU')")
    LocalDateTime findProximoAgendamentoEmByOrganizacao(@Param("orgId") Long orgId, @Param("agora") LocalDateTime agora);

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId " +
            "AND a.dtAgendamento >= :agora AND a.status NOT IN ('CANCELADO', 'CONCLUIDO', 'NAO_COMPARECEU')")
    Long countAgendamentosFuturosByOrganizacao(@Param("orgId") Long orgId, @Param("agora") LocalDateTime agora);

    @Query("SELECT MAX(c.dtCriacao) FROM Cliente c WHERE c.organizacao.id = :orgId")
    LocalDateTime findUltimoClienteCadastradoEmByOrganizacao(@Param("orgId") Long orgId);

    // --- Agendamentos detalhados ---

    @Query("SELECT a.status, COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId GROUP BY a.status")
    List<Object[]> countAgendamentosAgrupadosPorStatusByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT FUNCTION('TO_CHAR', a.dtCriacao, 'YYYY-MM'), COUNT(a), " +
            "SUM(CASE WHEN a.status = 'CONCLUIDO' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.status = 'CANCELADO' THEN 1 ELSE 0 END) " +
            "FROM Agendamento a WHERE a.organizacao.id = :orgId AND a.dtCriacao >= :inicio " +
            "GROUP BY FUNCTION('TO_CHAR', a.dtCriacao, 'YYYY-MM') " +
            "ORDER BY FUNCTION('TO_CHAR', a.dtCriacao, 'YYYY-MM') ASC")
    List<Object[]> countAgendamentosMensaisByOrganizacao(@Param("orgId") Long orgId, @Param("inicio") LocalDateTime inicio);

    // --- Clientes detalhados ---

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.organizacao.id = :orgId AND c.isCadastroIncompleto = true")
    Long countClientesCadastroIncompletoByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.organizacao.id = :orgId AND c.dtCriacao >= :inicio AND c.dtCriacao <= :fim")
    Long countClientesByOrganizacaoNoPeriodo(@Param("orgId") Long orgId, @Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT FUNCTION('TO_CHAR', c.dtCriacao, 'YYYY-MM'), COUNT(c) " +
            "FROM Cliente c WHERE c.organizacao.id = :orgId AND c.dtCriacao >= :inicio " +
            "GROUP BY FUNCTION('TO_CHAR', c.dtCriacao, 'YYYY-MM') " +
            "ORDER BY FUNCTION('TO_CHAR', c.dtCriacao, 'YYYY-MM') ASC")
    List<Object[]> countClientesMensaisByOrganizacao(@Param("orgId") Long orgId, @Param("inicio") LocalDateTime inicio);

    // --- Servicos / Funcionarios detalhados ---

    @Query("SELECT AVG(s.preco) FROM Servico s WHERE s.organizacao.id = :orgId AND s.ativo = true AND s.isDeletado = false")
    BigDecimal calcularPrecoMedioServicosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT s.categoria.label, COUNT(s) FROM Servico s WHERE s.organizacao.id = :orgId AND s.isDeletado = false " +
            "GROUP BY s.categoria.label ORDER BY COUNT(s) DESC")
    List<Object[]> countServicosPorCategoriaByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT f.situacao, COUNT(f) FROM Funcionario f WHERE f.organizacao.id = :orgId GROUP BY f.situacao ORDER BY COUNT(f) DESC")
    List<Object[]> countFuncionariosPorSituacaoByOrganizacao(@Param("orgId") Long orgId);

    // --- Financeiro detalhado ---

    @Query("SELECT c.statusCobranca, COUNT(c), COALESCE(SUM(c.valor), 0) FROM Cobranca c WHERE c.organizacao.id = :orgId GROUP BY c.statusCobranca")
    List<Object[]> countECalcularCobrancasPorStatusByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT c.tipoCobranca, COUNT(c) FROM Cobranca c WHERE c.organizacao.id = :orgId GROUP BY c.tipoCobranca")
    List<Object[]> countCobrancasPorTipoByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT p.statusPagamento, COUNT(p) FROM Pagamento p WHERE p.organizacao.id = :orgId GROUP BY p.statusPagamento")
    List<Object[]> countPagamentosPorStatusByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(p) FROM Pagamento p WHERE p.organizacao.id = :orgId AND p.statusPagamento = 'CONFIRMADO'")
    Long countPagamentosConfirmadosByOrganizacao(@Param("orgId") Long orgId);

    @Query("SELECT FUNCTION('TO_CHAR', p.dtPagamento, 'YYYY-MM'), COALESCE(SUM(p.valor), 0), COUNT(p) " +
            "FROM Pagamento p WHERE p.organizacao.id = :orgId AND p.statusPagamento = 'CONFIRMADO' AND p.dtPagamento >= :inicio " +
            "GROUP BY FUNCTION('TO_CHAR', p.dtPagamento, 'YYYY-MM') " +
            "ORDER BY FUNCTION('TO_CHAR', p.dtPagamento, 'YYYY-MM') ASC")
    List<Object[]> calcularFaturamentoMensalByOrganizacao(@Param("orgId") Long orgId, @Param("inicio") LocalDateTime inicio);
}
