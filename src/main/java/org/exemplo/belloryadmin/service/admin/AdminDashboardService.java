package org.exemplo.belloryadmin.service.admin;

import lombok.RequiredArgsConstructor;
import org.exemplo.belloryadmin.model.dto.admin.AdminDashboardDTO;
import org.exemplo.belloryadmin.model.repository.admin.AdminQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminQueryRepository adminQueryRepository;

    public AdminDashboardDTO getDashboard() {
        Long totalOrganizacoes = adminQueryRepository.count();
        Long organizacoesAtivas = adminQueryRepository.countOrganizacoesAtivas();
        Long organizacoesInativas = adminQueryRepository.countOrganizacoesInativas();

        Long totalAgendamentos = adminQueryRepository.countTotalAgendamentos();
        Long totalClientes = adminQueryRepository.countTotalClientes();
        Long totalFuncionarios = adminQueryRepository.countTotalFuncionarios();
        Long totalServicos = adminQueryRepository.countTotalServicos();
        Long totalInstancias = adminQueryRepository.countTotalInstancias();
        Long instanciasAtivas = adminQueryRepository.countInstanciasAtivas();

        BigDecimal faturamentoTotal = adminQueryRepository.calcularFaturamentoTotal();
        Long totalCobrancas = adminQueryRepository.countTotalCobrancas();
        Long cobrancasPendentes = adminQueryRepository.countCobrancasPendentes();
        Long cobrancasPagas = adminQueryRepository.countCobrancasPagas();

        // Localizacoes para o mapa
        List<AdminDashboardDTO.OrgLocationDTO> localizacoes = buildLocalizacoes(
                adminQueryRepository.findLocalizacoesOrganizacoes());

        return AdminDashboardDTO.builder()
                .totalOrganizacoes(totalOrganizacoes)
                .organizacoesAtivas(organizacoesAtivas)
                .organizacoesInativas(organizacoesInativas)
                .totalAgendamentos(totalAgendamentos)
                .totalClientes(totalClientes)
                .totalFuncionarios(totalFuncionarios)
                .totalServicos(totalServicos)
                .totalInstancias(totalInstancias)
                .instanciasConectadas(instanciasAtivas)
                .instanciasDesconectadas(totalInstancias - instanciasAtivas)
                .faturamentoTotal(faturamentoTotal)
                .totalCobrancas(totalCobrancas)
                .cobrancasPendentes(cobrancasPendentes)
                .cobrancasPagas(cobrancasPagas)
                .localizacoes(localizacoes)
                .build();
    }

    private List<AdminDashboardDTO.OrgLocationDTO> buildLocalizacoes(List<Object[]> dados) {
        List<AdminDashboardDTO.OrgLocationDTO> localizacoes = new ArrayList<>();

        for (Object[] row : dados) {
            String cidade = (String) row[0];
            String uf = (String) row[1];
            String latStr = (String) row[2];
            String lngStr = (String) row[3];

            try {
                localizacoes.add(AdminDashboardDTO.OrgLocationDTO.builder()
                        .cidade(cidade != null ? cidade.trim() : null)
                        .estado(uf != null ? uf.trim().toUpperCase() : null)
                        .latitude(Double.parseDouble(latStr))
                        .longitude(Double.parseDouble(lngStr))
                        .quantidade(1L)
                        .build());
            } catch (NumberFormatException ignored) {
            }
        }

        return localizacoes;
    }

}
