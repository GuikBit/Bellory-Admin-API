package org.exemplo.belloryadmin.model.repository.users;

import org.exemplo.belloryadmin.model.entity.users.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    long countByOrganizacao_IdAndAtivoTrue(Long organizacaoId);
}
