package org.exemplo.belloryadmin.model.repository.users;

import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, Long> {

    Optional<UsuarioAdmin> findByUsername(String username);

    Optional<UsuarioAdmin> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<UsuarioAdmin> findAllByAtivoTrue();
}
