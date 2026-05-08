package org.exemplo.belloryadmin.service.lead;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.context.TenantContext;
import org.exemplo.belloryadmin.model.dto.lead.admin.AtividadeDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.CreateAtividadeDTO;
import org.exemplo.belloryadmin.model.dto.lead.admin.ResponsavelMiniDTO;
import org.exemplo.belloryadmin.model.entity.lead.Lead;
import org.exemplo.belloryadmin.model.entity.lead.LeadAtividade;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoAtividade;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.exemplo.belloryadmin.model.repository.lead.LeadAtividadeRepository;
import org.exemplo.belloryadmin.model.repository.lead.LeadRepository;
import org.exemplo.belloryadmin.model.repository.users.UsuarioAdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service de historico do lead. Centraliza criacao tanto manual
 * (comentarios, ligacoes, etc.) quanto automatica (LEAD_CRIADO,
 * MUDANCA_STATUS, ATRIBUICAO).
 *
 * <p>Os outros services chamam {@code registrar*} sem se preocupar
 * em montar o payload — esta classe encapsula isso.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeadAtividadeService {

    private final LeadAtividadeRepository atividadeRepository;
    private final LeadRepository leadRepository;
    private final UsuarioAdminRepository usuarioAdminRepository;

    // ===== Auto =====

    @Transactional
    public void registrarLeadCriado(Lead lead, boolean viaSite) {
        Map<String, Object> dados = Map.of("viaSite", viaSite);
        save(lead, TipoAtividade.LEAD_CRIADO,
                viaSite ? "Lead chegou via formulario do site" : "Lead criado manualmente",
                dados, autorAtual());
    }

    @Transactional
    public void registrarMudancaStatus(Lead lead, String fromCodigo, String toCodigo, String comentario) {
        Map<String, Object> dados = Map.of(
                "fromStatus", fromCodigo == null ? "" : fromCodigo,
                "toStatus", toCodigo == null ? "" : toCodigo
        );
        String desc = "Status alterado: " + fromCodigo + " -> " + toCodigo
                + (comentario != null && !comentario.isBlank() ? " | " + comentario : "");
        save(lead, TipoAtividade.MUDANCA_STATUS, desc, dados, autorAtual());
    }

    @Transactional
    public void registrarAtribuicao(Lead lead, Long fromUserId, Long toUserId) {
        Map<String, Object> dados = Map.of(
                "fromUserId", fromUserId == null ? "" : fromUserId,
                "toUserId", toUserId == null ? "" : toUserId
        );
        String desc = toUserId == null
                ? "Lead desatribuido"
                : "Lead atribuido ao usuario " + toUserId;
        save(lead, TipoAtividade.ATRIBUICAO, desc, dados, autorAtual());
    }

    // ===== Manual =====

    @Transactional
    public AtividadeDTO criarManual(UUID leadId, CreateAtividadeDTO dto) {
        if (dto.getTipo().isAutomatico()) {
            throw new IllegalArgumentException(
                    "Tipo " + dto.getTipo() + " eh registrado automaticamente — nao crie manualmente.");
        }
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead nao encontrado: " + leadId));

        LeadAtividade salvo = save(lead, dto.getTipo(), dto.getDescricao(), null, autorAtual());
        return toDTO(salvo);
    }

    @Transactional
    public void apagarManual(Long atividadeId) {
        LeadAtividade entity = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new IllegalArgumentException("Atividade nao encontrada: " + atividadeId));
        if (entity.getTipo().isAutomatico()) {
            throw new IllegalArgumentException("Atividades automaticas nao podem ser apagadas.");
        }
        atividadeRepository.delete(entity);
    }

    // ===== Listagem =====

    @Transactional(readOnly = true)
    public List<AtividadeDTO> listar(UUID leadId, TipoAtividade tipo) {
        return atividadeRepository.listarPorLead(leadId, tipo).stream()
                .map(this::toDTO)
                .toList();
    }

    // ===== helpers =====

    private LeadAtividade save(Lead lead, TipoAtividade tipo, String descricao,
                               Map<String, Object> dados, UsuarioAdmin autor) {
        LeadAtividade entity = LeadAtividade.builder()
                .lead(lead)
                .tipo(tipo)
                .descricao(descricao)
                .dados(dados)
                .autor(autor)
                .build();
        return atividadeRepository.save(entity);
    }

    private UsuarioAdmin autorAtual() {
        Long userId = TenantContext.getCurrentUserId();
        if (userId == null) return null;
        return usuarioAdminRepository.findById(userId).orElse(null);
    }

    public AtividadeDTO toDTO(LeadAtividade a) {
        ResponsavelMiniDTO autor = null;
        if (a.getAutor() != null) {
            autor = ResponsavelMiniDTO.builder()
                    .id(a.getAutor().getId())
                    .username(a.getAutor().getUsername())
                    .nomeCompleto(a.getAutor().getNomeCompleto())
                    .build();
        }
        return AtividadeDTO.builder()
                .id(a.getId())
                .tipo(a.getTipo())
                .descricao(a.getDescricao())
                .dados(a.getDados())
                .autor(autor)
                .dtCriacao(a.getDtCriacao())
                .automatica(a.getTipo().isAutomatico())
                .build();
    }
}
