package org.exemplo.belloryadmin.model.repository.template;

import org.exemplo.belloryadmin.model.entity.template.CategoriaTemplate;
import org.exemplo.belloryadmin.model.entity.template.TemplateBellory;
import org.exemplo.belloryadmin.model.entity.template.TipoTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateBelloryRepository extends JpaRepository<TemplateBellory, Long> {

    Optional<TemplateBellory> findByCodigo(String codigo);

    List<TemplateBellory> findByAtivoTrueOrderByTipoAscCategoriaAsc();

    List<TemplateBellory> findByTipoAndAtivoTrueOrderByCategoriaAsc(TipoTemplate tipo);

    List<TemplateBellory> findByCategoriaAndAtivoTrueOrderByTipoAsc(CategoriaTemplate categoria);

    List<TemplateBellory> findByTipoAndCategoriaAndAtivoTrueOrderByNomeAsc(TipoTemplate tipo, CategoriaTemplate categoria);

    Optional<TemplateBellory> findByTipoAndCategoriaAndPadraoTrue(TipoTemplate tipo, CategoriaTemplate categoria);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    @Modifying
    @Query("UPDATE TemplateBellory t SET t.padrao = false WHERE t.tipo = :tipo AND t.categoria = :categoria AND t.id <> :id")
    void desmarcarPadrao(@Param("tipo") TipoTemplate tipo, @Param("categoria") CategoriaTemplate categoria, @Param("id") Long id);
}
