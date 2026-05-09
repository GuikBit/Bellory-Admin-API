package org.exemplo.belloryadmin.model.entity.config;

import jakarta.persistence.*;
import lombok.*;
import org.exemplo.belloryadmin.model.entity.users.UsuarioAdmin;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys", schema = "admin",
    indexes = {
        @Index(name = "idx_admin_apikey_usuario_ativo", columnList = "usuario_admin_id, ativo"),
        @Index(name = "idx_admin_apikey_ativo_expires", columnList = "ativo, expires_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_admin_id", nullable = false)
    private UsuarioAdmin usuarioAdmin;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "key_hash", nullable = false, unique = true, length = 128)
    private String keyHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
