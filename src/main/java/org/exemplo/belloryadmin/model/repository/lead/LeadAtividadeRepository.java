package org.exemplo.belloryadmin.model.repository.lead;

import org.exemplo.belloryadmin.model.entity.lead.LeadAtividade;
import org.exemplo.belloryadmin.model.entity.lead.enums.TipoAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadAtividadeRepository extends JpaRepository<LeadAtividade, Long> {

    @Query("""
            SELECT a FROM LeadAtividade a
            LEFT JOIN FETCH a.autor
            WHERE a.lead.id = :leadId
              AND (:tipo IS NULL OR a.tipo = :tipo)
            ORDER BY a.dtCriacao DESC, a.id DESC
            """)
    List<LeadAtividade> listarPorLead(
            @Param("leadId") UUID leadId,
            @Param("tipo") TipoAtividade tipo
    );
}
