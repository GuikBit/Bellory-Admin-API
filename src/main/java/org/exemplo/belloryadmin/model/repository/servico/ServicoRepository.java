package org.exemplo.belloryadmin.model.repository.servico;

import org.exemplo.belloryadmin.model.entity.servico.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    long countByOrganizacao_IdAndIsDeletadoFalse(Long organizacaoId);
}
