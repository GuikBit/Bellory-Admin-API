package org.exemplo.belloryadmin.model.repository.config;

import org.exemplo.belloryadmin.model.entity.config.ApiKeyAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyAdminRepository extends JpaRepository<ApiKeyAdmin, Long> {

    Optional<ApiKeyAdmin> findByKeyHashAndAtivoTrue(String keyHash);

    List<ApiKeyAdmin> findByUsuarioAdminIdAndAtivoTrue(Long usuarioAdminId);

    List<ApiKeyAdmin> findAllByAtivoTrueOrderByCreatedAtDesc();
}
