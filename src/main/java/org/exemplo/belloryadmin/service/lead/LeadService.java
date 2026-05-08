package org.exemplo.belloryadmin.service.lead;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.lead.admin.*;
import org.exemplo.belloryadmin.model.entity.lead.Lead;
import org.exemplo.belloryadmin.model.entity.lead.LeadStatus;
import org.exemplo.belloryadmin.model.entity.lead.enums.PrioridadeLead;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.exemplo.belloryadmin.model.repository.lead.LeadRepository;
import org.exemplo.belloryadmin.model.repository.lead.LeadStatusRepository;
import org.exemplo.belloryadmin.model.repository.users.UsuarioAdminRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service principal de leads — CRUD admin, kanban, atribuicoes, soft delete.
 *
 * <p>Toda mudanca relevante (status, atribuicao, criacao) dispara
 * {@link LeadAtividadeService} para registrar no historico.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadStatusRepository statusRepository;
    private final UsuarioAdminRepository usuarioAdminRepository;
    private final LeadStatusService leadStatusService;
    private final LeadAtividadeService atividadeService;

    // ===== Kanban =====

    @Transactional(readOnly = true)
    public List<LeadKanbanColumnDTO> kanban() {
        List<LeadStatus> colunas = statusRepository.findByAtivoTrueOrderByOrdemAsc();
        List<Lead> leads = leadRepository.findAllAtivosForKanban();

        Map<Long, List<Lead>> porStatus = new HashMap<>();
        for (Lead l : leads) {
            porStatus.computeIfAbsent(l.getStatus().getId(), k -> new ArrayList<>()).add(l);
        }

        List<LeadKanbanColumnDTO> resultado = new ArrayList<>();
        for (LeadStatus s : colunas) {
            List<Lead> dessaColuna = porStatus.getOrDefault(s.getId(), List.of());
            resultado.add(LeadKanbanColumnDTO.builder()
                    .status(leadStatusService.toDTO(s))
                    .leads(dessaColuna.stream().map(this::toListDTO).toList())
                    .total(dessaColuna.size())
                    .build());
        }
        return resultado;
    }

    // ===== Listagem paginada =====

    @Transactional(readOnly = true)
    public Page<LeadListDTO> listar(Long statusId, Long responsavelId, PrioridadeLead prioridade,
                                    LocalDateTime from, LocalDateTime to, String q,
                                    int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<Lead> resultado = leadRepository.buscarComFiltros(
                statusId, responsavelId, prioridade, from, to,
                (q == null || q.isBlank()) ? null : q.trim(),
                pageable);
        return resultado.map(this::toListDTO);
    }

    // ===== Detalhe =====

    @Transactional(readOnly = true)
    public LeadDetailDTO detalhar(UUID id) {
        Lead lead = buscar(id);
        return toDetailDTO(lead);
    }

    // ===== Criar manual =====

    @Transactional
    public LeadDetailDTO criarManual(LeadCreateDTO dto) {
        LeadStatus status;
        if (dto.getStatusId() != null) {
            status = statusRepository.findById(dto.getStatusId())
                    .orElseThrow(() -> new IllegalArgumentException("Status nao encontrado: " + dto.getStatusId()));
        } else {
            status = statusRepository.findByEhStatusInicialTrue()
                    .orElseThrow(() -> new IllegalStateException("Nenhum status marcado como inicial — configure pelo admin."));
        }

        UsuarioAdmin responsavel = null;
        if (dto.getResponsavelId() != null) {
            responsavel = usuarioAdminRepository.findById(dto.getResponsavelId())
                    .orElseThrow(() -> new IllegalArgumentException("Responsavel nao encontrado: " + dto.getResponsavelId()));
        }

        Lead lead = Lead.builder()
                .status(status)
                .nome(dto.getNome().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .telefone(dto.getTelefone().trim())
                .tipoNegocio(dto.getTipoNegocio())
                .mensagem(dto.getMensagem() != null ? dto.getMensagem().trim() : null)
                .origem(dto.getOrigem() != null ? dto.getOrigem() : "admin/manual")
                .prioridade(dto.getPrioridade() != null ? dto.getPrioridade() : PrioridadeLead.MEDIA)
                .tags(dto.getTags() != null ? dto.getTags().toArray(new String[0]) : new String[0])
                .valorEstimado(dto.getValorEstimado())
                .dataPrevistaFechamento(dto.getDataPrevistaFechamento())
                .responsavel(responsavel)
                .turnstileOk(false)
                .build();

        Lead salvo = leadRepository.save(lead);
        atividadeService.registrarLeadCriado(salvo, false);
        if (responsavel != null) {
            atividadeService.registrarAtribuicao(salvo, null, responsavel.getId());
        }
        log.info("Lead criado manualmente: {} ({})", salvo.getNome(), salvo.getEmail());
        return toDetailDTO(salvo);
    }

    // ===== Atualizar campos =====

    @Transactional
    public LeadDetailDTO atualizar(UUID id, LeadUpdateDTO dto) {
        Lead lead = buscar(id);

        if (dto.getNome() != null) lead.setNome(dto.getNome().trim());
        if (dto.getEmail() != null) lead.setEmail(dto.getEmail().trim().toLowerCase());
        if (dto.getTelefone() != null) lead.setTelefone(dto.getTelefone().trim());
        if (dto.getTipoNegocio() != null) lead.setTipoNegocio(dto.getTipoNegocio());
        if (dto.getMensagem() != null) lead.setMensagem(dto.getMensagem().trim());
        if (dto.getOrigem() != null) lead.setOrigem(dto.getOrigem());
        if (dto.getPrioridade() != null) lead.setPrioridade(dto.getPrioridade());
        if (dto.getTags() != null) lead.setTags(dto.getTags().toArray(new String[0]));
        if (dto.getValorEstimado() != null) lead.setValorEstimado(dto.getValorEstimado());
        if (dto.getDataPrevistaFechamento() != null) lead.setDataPrevistaFechamento(dto.getDataPrevistaFechamento());

        leadRepository.save(lead);
        return toDetailDTO(lead);
    }

    // ===== Mover de coluna (kanban) =====

    @Transactional
    public LeadDetailDTO moverStatus(UUID id, MoveStatusDTO dto) {
        Lead lead = buscar(id);
        LeadStatus destino = statusRepository.findById(dto.getStatusId())
                .orElseThrow(() -> new IllegalArgumentException("Status nao encontrado: " + dto.getStatusId()));
        if (!destino.isAtivo()) {
            throw new IllegalArgumentException("Nao eh possivel mover para um status inativo.");
        }
        if (lead.getStatus().getId().equals(destino.getId())) {
            return toDetailDTO(lead);
        }

        String fromCodigo = lead.getStatus().getCodigo();
        lead.setStatus(destino);
        leadRepository.save(lead);
        atividadeService.registrarMudancaStatus(lead, fromCodigo, destino.getCodigo(), dto.getComentario());
        return toDetailDTO(lead);
    }

    // ===== Atribuir =====

    @Transactional
    public LeadDetailDTO atribuir(UUID id, AssignDTO dto) {
        Lead lead = buscar(id);
        Long fromUserId = lead.getResponsavel() != null ? lead.getResponsavel().getId() : null;

        if (dto.getResponsavelId() == null) {
            lead.setResponsavel(null);
        } else {
            UsuarioAdmin u = usuarioAdminRepository.findById(dto.getResponsavelId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario admin nao encontrado: " + dto.getResponsavelId()));
            lead.setResponsavel(u);
        }
        leadRepository.save(lead);
        atividadeService.registrarAtribuicao(lead, fromUserId, dto.getResponsavelId());
        return toDetailDTO(lead);
    }

    // ===== Soft delete =====

    @Transactional
    public void softDelete(UUID id) {
        Lead lead = buscar(id);
        lead.setDeletedAt(LocalDateTime.now());
        leadRepository.save(lead);
        log.info("Lead soft-deleted: {}", id);
    }

    /**
     * Hard delete LGPD — apaga o lead e todo o historico (cascade).
     * Apenas para atender a pedido de exclusao do titular dos dados.
     */
    @Transactional
    public void hardDeleteLgpd(UUID id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead nao encontrado: " + id));
        leadRepository.delete(lead);
        log.warn("Lead HARD-deleted (LGPD): {} ({})", id, lead.getEmail());
    }

    // ===== helpers =====

    private Lead buscar(UUID id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lead nao encontrado: " + id));
        if (lead.getDeletedAt() != null) {
            throw new IllegalArgumentException("Lead esta excluido.");
        }
        return lead;
    }

    private LeadListDTO toListDTO(Lead l) {
        return LeadListDTO.builder()
                .id(l.getId())
                .nome(l.getNome())
                .email(l.getEmail())
                .telefone(l.getTelefone())
                .tipoNegocio(l.getTipoNegocio())
                .status(leadStatusService.toDTO(l.getStatus()))
                .prioridade(l.getPrioridade())
                .tags(l.getTags() == null ? List.of() : Arrays.asList(l.getTags()))
                .valorEstimado(l.getValorEstimado())
                .dataPrevistaFechamento(l.getDataPrevistaFechamento())
                .responsavel(toResponsavelMini(l.getResponsavel()))
                .origem(l.getOrigem())
                .dtCriacao(l.getDtCriacao())
                .build();
    }

    private LeadDetailDTO toDetailDTO(Lead l) {
        return LeadDetailDTO.builder()
                .id(l.getId())
                .status(leadStatusService.toDTO(l.getStatus()))
                .nome(l.getNome())
                .email(l.getEmail())
                .telefone(l.getTelefone())
                .tipoNegocio(l.getTipoNegocio())
                .mensagem(l.getMensagem())
                .origem(l.getOrigem())
                .prioridade(l.getPrioridade())
                .tags(l.getTags() == null ? List.of() : Arrays.asList(l.getTags()))
                .valorEstimado(l.getValorEstimado())
                .dataPrevistaFechamento(l.getDataPrevistaFechamento())
                .responsavel(toResponsavelMini(l.getResponsavel()))
                .turnstileOk(l.isTurnstileOk())
                .fillTimeMs(l.getFillTimeMs())
                .policyVersion(l.getPolicyVersion())
                .dtCriacao(l.getDtCriacao())
                .dtAtualizacao(l.getDtAtualizacao())
                .build();
    }

    private ResponsavelMiniDTO toResponsavelMini(UsuarioAdmin u) {
        if (u == null) return null;
        return ResponsavelMiniDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nomeCompleto(u.getNomeCompleto())
                .build();
    }
}
