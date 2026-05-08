package org.exemplo.belloryadmin.service.lead;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.lead.admin.LeadStatusCreateDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.LeadStatusDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.LeadStatusUpdateDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.ReorderItemDTO;
import org.exemplo.belloryadmin.model.entity.lead.LeadStatus;
import org.exemplo.belloryadmin.model.repository.lead.LeadRepository;
import org.exemplo.belloryadmin.model.repository.lead.LeadStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadStatusService {

    private final LeadStatusRepository statusRepository;
    private final LeadRepository leadRepository;

    @Transactional(readOnly = true)
    public List<LeadStatusDTO> listarTodos() {
        return statusRepository.findAllByOrderByOrdemAsc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadStatusDTO> listarAtivos() {
        return statusRepository.findByAtivoTrueOrderByOrdemAsc().stream().map(this::toDTO).toList();
    }

    @Transactional
    public LeadStatusDTO criar(LeadStatusCreateDTO dto) {
        if (statusRepository.findByCodigo(dto.getCodigo()).isPresent()) {
            throw new IllegalArgumentException("Ja existe um status com o codigo: " + dto.getCodigo());
        }

        boolean ehInicial = Boolean.TRUE.equals(dto.getEhStatusInicial());
        if (ehInicial) {
            // Desmarca o anterior antes de marcar este (constraint parcial unique no DB)
            statusRepository.findByEhStatusInicialTrue().ifPresent(s -> {
                s.setEhStatusInicial(false);
                statusRepository.save(s);
            });
        }

        LeadStatus entity = LeadStatus.builder()
                .codigo(dto.getCodigo())
                .nome(dto.getNome())
                .cor(dto.getCor() != null ? dto.getCor() : "#6B7280")
                .ordem(dto.getOrdem() != null ? dto.getOrdem() : proximaOrdem())
                .ativo(true)
                .ehStatusInicial(ehInicial)
                .ehStatusFinal(Boolean.TRUE.equals(dto.getEhStatusFinal()))
                .build();

        LeadStatus salvo = statusRepository.save(entity);
        log.info("LeadStatus criado: {} ({})", salvo.getNome(), salvo.getCodigo());
        return toDTO(salvo);
    }

    @Transactional
    public LeadStatusDTO atualizar(Long id, LeadStatusUpdateDTO dto) {
        LeadStatus entity = statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status nao encontrado: " + id));

        if (dto.getNome() != null) entity.setNome(dto.getNome());
        if (dto.getCor() != null) entity.setCor(dto.getCor());
        if (dto.getOrdem() != null) entity.setOrdem(dto.getOrdem());
        if (dto.getAtivo() != null) entity.setAtivo(dto.getAtivo());
        if (dto.getEhStatusFinal() != null) entity.setEhStatusFinal(dto.getEhStatusFinal());

        if (dto.getEhStatusInicial() != null && dto.getEhStatusInicial() && !entity.isEhStatusInicial()) {
            statusRepository.findByEhStatusInicialTrue().ifPresent(s -> {
                if (!s.getId().equals(id)) {
                    s.setEhStatusInicial(false);
                    statusRepository.save(s);
                }
            });
            entity.setEhStatusInicial(true);
        } else if (Boolean.FALSE.equals(dto.getEhStatusInicial())) {
            // Bloqueia desmarcar sem ter outro inicial — sempre precisa existir um.
            throw new IllegalArgumentException("Defina outro status como inicial antes de desmarcar este.");
        }

        LeadStatus salvo = statusRepository.save(entity);
        log.info("LeadStatus atualizado: {} ({})", salvo.getNome(), salvo.getCodigo());
        return toDTO(salvo);
    }

    @Transactional
    public List<LeadStatusDTO> reordenar(List<ReorderItemDTO> itens) {
        Map<Long, Integer> mapa = new HashMap<>();
        itens.forEach(i -> mapa.put(i.getId(), i.getOrdem()));

        List<LeadStatus> all = statusRepository.findAllById(mapa.keySet());
        if (all.size() != mapa.size()) {
            throw new IllegalArgumentException("Algum dos status informados nao existe.");
        }
        all.forEach(s -> s.setOrdem(mapa.get(s.getId())));
        statusRepository.saveAll(all);

        return statusRepository.findAllByOrderByOrdemAsc().stream().map(this::toDTO).toList();
    }

    /**
     * Inativa o status. Se houver leads ativos nele, exige {@code moveToStatusId}
     * para realocar antes de inativar.
     */
    @Transactional
    public void inativar(Long id, Long moveToStatusId) {
        LeadStatus entity = statusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Status nao encontrado: " + id));

        if (entity.isEhStatusInicial()) {
            throw new IllegalArgumentException("Nao eh possivel inativar o status inicial.");
        }

        long count = leadRepository.countByStatusIdAndDeletedAtIsNull(id);
        if (count > 0) {
            if (moveToStatusId == null) {
                throw new IllegalArgumentException(
                        "Existem " + count + " leads neste status. Informe moveTo=<outroStatusId> para realocar.");
            }
            LeadStatus destino = statusRepository.findById(moveToStatusId)
                    .orElseThrow(() -> new IllegalArgumentException("Status destino nao encontrado: " + moveToStatusId));
            if (!destino.isAtivo()) {
                throw new IllegalArgumentException("Status destino nao esta ativo.");
            }
            // Realoca os leads em massa via JPA query — feito no LeadService para registrar atividades.
            // Aqui apenas validamos. A reatribuicao em massa eh trigger de UI: o front pode chamar
            // PATCH /leads/{id}/status para cada lead, ou criar um endpoint bulk depois.
            throw new IllegalArgumentException(
                    "Existem " + count + " leads neste status. Mova-os manualmente para "
                            + destino.getNome() + " antes de inativar (endpoint bulk fica para fase posterior).");
        }

        entity.setAtivo(false);
        statusRepository.save(entity);
        log.info("LeadStatus inativado: {} ({})", entity.getNome(), entity.getCodigo());
    }

    // ===== helpers =====

    private int proximaOrdem() {
        return statusRepository.findAllByOrderByOrdemAsc().stream()
                .mapToInt(LeadStatus::getOrdem)
                .max()
                .orElse(0) + 10;
    }

    public LeadStatusDTO toDTO(LeadStatus s) {
        return LeadStatusDTO.builder()
                .id(s.getId())
                .codigo(s.getCodigo())
                .nome(s.getNome())
                .cor(s.getCor())
                .ordem(s.getOrdem())
                .ativo(s.isAtivo())
                .ehStatusInicial(s.isEhStatusInicial())
                .ehStatusFinal(s.isEhStatusFinal())
                .build();
    }
}
