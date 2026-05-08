package org.exemplo.belloryadmin.model.repository.assinatura;

import org.exemplo.belloryadmin.model.entity.assinatura.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    @Query("SELECT a FROM Assinatura a LEFT JOIN FETCH a.organizacao WHERE a.organizacao.id = :organizacaoId")
    Optional<Assinatura> findByOrganizacaoId(@Param("organizacaoId") Long organizacaoId);

    Optional<Assinatura> findByPaymentApiCustomerId(Long paymentApiCustomerId);

    Optional<Assinatura> findByPaymentApiSubscriptionId(Long paymentApiSubscriptionId);

    @Query("SELECT a FROM Assinatura a LEFT JOIN FETCH a.organizacao WHERE a.id = :id")
    Optional<Assinatura> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT a FROM Assinatura a LEFT JOIN FETCH a.organizacao")
    List<Assinatura> findAllWithDetails();
}
