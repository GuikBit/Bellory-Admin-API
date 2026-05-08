package org.exemplo.belloryadmin.model.entity.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "tb_roles", schema = "app")
@Getter
@Setter
@NoArgsConstructor
public class Role implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    public Role(String nome) {
        this.nome = nome;
    }

    @Override
    public String getAuthority() {
        return this.nome;
    }
}
