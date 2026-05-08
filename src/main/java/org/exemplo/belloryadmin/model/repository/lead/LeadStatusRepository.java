package org.exemplo.belloryadmin.model.repository.lead;

import org.exemplo.belloryadmin.model.entity.lead.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadStatusRepository extends JpaRepository<LeadStatus, Long> {

    Optional<LeadStatus> findByCodigo(String codigo);

    Optional<LeadStatus> findByEhStatusInicialTrue();

    List<LeadStatus> findAllByOrderByOrdemAsc();

    List<LeadStatus> findByAtivoTrueOrderByOrdemAsc();

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
