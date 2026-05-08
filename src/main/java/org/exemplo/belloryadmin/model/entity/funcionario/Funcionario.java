package org.exemplo.belloryadmin.model.entity.funcionario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.exemplo.belloryadmin.model.entity.servico.Servico;
import org.exemplo.belloryadmin.model.entity.users.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Funcionario - versao admin (read-mostly).
 *
 * <p>Mantem so os campos usados pelas queries do AdminQueryRepository:
 * organizacao_id (herdado), situacao, cargo (ManyToOne), servicos (ManyToMany,
 * usado em SIZE(f.servicos)), dataCriacao, isDeletado.</p>
 *
 * <p>Removido todo o detalhe de RH/PJ (cpf, rg, ctps, salario, endereco, etc).
 * Os campos existem no banco mas a entity admin nao os mapeia.</p>
 */
@Entity
@Table(name = "funcionario", schema = "app", uniqueConstraints = {
        @UniqueConstraint(name = "uk_funcionario_org_username", columnNames = {"organizacao_id", "username"}),
        @UniqueConstraint(name = "uk_funcionario_org_email", columnNames = {"organizacao_id", "email"})
}, indexes = {
        @Index(name = "idx_funcionario_organizacao_id", columnList = "organizacao_id"),
        @Index(name = "idx_funcionario_cpf", columnList = "cpf"),
        @Index(name = "idx_funcionario_cargo_id", columnList = "cargo_id"),
        @Index(name = "idx_funcionario_org_ativo", columnList = "organizacao_id, ativo"),
        @Index(name = "idx_funcionario_org_visivel", columnList = "organizacao_id, ativo, isVisivelExterno")
})
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Funcionario extends User {

    @Column(length = 50)
    private String situacao;

    @Column(name = "is_deletado", nullable = false)
    private boolean isDeletado = false;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @ManyToMany
    @JoinTable(
            name = "funcionario_servico",
            schema = "app",
            joinColumns = @JoinColumn(name = "funcionario_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private List<Servico> servicos;

    private String role;

    @Override
    public String getRole() {
        return this.role;
    }
}
