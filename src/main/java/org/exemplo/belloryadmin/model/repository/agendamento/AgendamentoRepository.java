package org.exemplo.belloryadmin.model.repository.agendamento;

import org.exemplo.belloryadmin.model.entity.agendamento.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.organizacao.id = :orgId " +
            "AND a.dtAgendamento BETWEEN :inicio AND :fim")
    long countByOrganizacaoAndPeriodo(@Param("orgId") Long orgId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);
}
