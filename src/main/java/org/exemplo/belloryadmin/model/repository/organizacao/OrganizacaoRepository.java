package org.exemplo.belloryadmin.model.repository.organizacao;

import org.exemplo.belloryadmin.model.entity.organizacao.Organizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OrganizacaoRepository - versao admin (read-mostly).
 *
 * <p>Subset do repository do Bellory-API com apenas os metodos que admin
 * realmente usa (sobretudo via WebhookEventProcessor).</p>
 */
@Repository
public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {

    Optional<Organizacao> findByNomeFantasia(String nome);

    @Query("SELECT DISTINCT o FROM Organizacao o " +
            "LEFT JOIN FETCH o.enderecoPrincipal " +
            "LEFT JOIN FETCH o.configSistema " +
            "WHERE o.ativo = true")
    List<Organizacao> findAllByAtivoTrueWithDetails();

    @Query("SELECT o FROM Organizacao o " +
            "LEFT JOIN FETCH o.enderecoPrincipal " +
            "LEFT JOIN FETCH o.configSistema " +
            "LEFT JOIN FETCH o.dadosFaturamento " +
            "WHERE o.id = :id AND o.ativo = true")
    Optional<Organizacao> findByIdWithDetails(@Param("id") Long id);

    Optional<Organizacao> findByCnpj(String cnpj);

    Optional<Organizacao> findBySlug(String slug);
}
