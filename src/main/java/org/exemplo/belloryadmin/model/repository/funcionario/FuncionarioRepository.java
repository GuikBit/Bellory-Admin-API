package org.exemplo.belloryadmin.model.repository.funcionario;

import org.exemplo.belloryadmin.model.entity.funcionario.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository enxuto - apenas o que admin precisa para PlanoUsoService.
 */
@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    long countByOrganizacao_IdAndIsDeletadoFalse(Long organizacaoId);
}
