package org.exemplo.belloryadmin.model.repository.instance;

import org.exemplo.belloryadmin.model.entity.instancia.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * InstanceRepository - versao admin (read-only).
 *
 * <p>Sem queries que fazem JOIN FETCH em Tools/WebhookConfig/Settings/KnowledgeBase
 * (entities removidas no admin). Mantido apenas o que o AdminQueryRepository
 * e AdminOrganizacaoService consomem.</p>
 */
@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {

    List<Instance> findByOrganizacaoIdAndDeletadoFalse(Long organizacaoId);

    long countByOrganizacaoIdAndDeletadoFalse(Long organizacaoId);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Instance i
        WHERE i.organizacao.id = :orgId
          AND i.deletado = false
          AND i.status IN ('CONNECTED', 'OPEN')
        """)
    boolean existsConectadaByOrganizacaoId(@Param("orgId") Long organizacaoId);

    @Query("SELECT i.status, COUNT(i) FROM Instance i " +
            "WHERE i.organizacao.id = :organizacaoId " +
            "GROUP BY i.status")
    List<Object[]> countByStatusAndOrganizacao(@Param("organizacaoId") Long organizacaoId);
}
