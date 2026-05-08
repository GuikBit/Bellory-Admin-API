package org.exemplo.belloryadmin.model.repository.lead;

import org.exemplo.belloryadmin.model.entity.lead.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    /**
     * Lista todos os leads ativos (nao deletados) ordenados por data de criacao desc.
     * Usado pela tela de kanban para popular as colunas.
     */
    @Query("""
            SELECT l FROM Lead l
            JOIN FETCH l.status
            LEFT JOIN FETCH l.responsavel
            WHERE l.deletedAt IS NULL
            ORDER BY l.dtCriacao DESC
            """)
    List<Lead> findAllAtivosForKanban();

    /**
     * Listagem paginada com filtros opcionais (todos podem ser null).
     */
    @Query("""
            SELECT l FROM Lead l
            LEFT JOIN l.status s
            LEFT JOIN l.responsavel r
            WHERE l.deletedAt IS NULL
              AND (:statusId IS NULL OR s.id = :statusId)
              AND (:responsavelId IS NULL OR r.id = :responsavelId)
              AND (:prioridade IS NULL OR l.prioridade = :prioridade)
              AND (:from IS NULL OR l.dtCriacao >= :from)
              AND (:to   IS NULL OR l.dtCriacao <= :to)
              AND (:q IS NULL OR (
                    LOWER(l.nome)     LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(l.email)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(l.telefone) LIKE LOWER(CONCAT('%', :q, '%'))
              ))
            ORDER BY l.dtCriacao DESC
            """)
    Page<Lead> buscarComFiltros(
            @Param("statusId") Long statusId,
            @Param("responsavelId") Long responsavelId,
            @Param("prioridade") org.exemplo.belloryadmin.model.entity.lead.enums.PrioridadeLead prioridade,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("q") String q,
            Pageable pageable
    );

    /**
     * Conta leads (nao deletados) por status. Util para validar
     * inativacao de uma coluna do kanban.
     */
    long countByStatusIdAndDeletedAtIsNull(Long statusId);
}
