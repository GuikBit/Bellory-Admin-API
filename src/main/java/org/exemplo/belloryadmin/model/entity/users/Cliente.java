package org.exemplo.belloryadmin.model.entity.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cliente - versao admin (read-mostly).
 *
 * <p>Sem os {@code @OneToMany} de enderecos, cartoes, agendamentos, compras,
 * cobrancas, pagamentos. Admin so faz contagens via queries diretas.</p>
 */
@Entity
@Table(name = "cliente", schema = "app", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cliente_org_username", columnNames = {"organizacao_id", "username"}),
        @UniqueConstraint(name = "uk_cliente_org_email", columnNames = {"organizacao_id", "email"}),
        @UniqueConstraint(name = "uk_cliente_org_telefone", columnNames = {"organizacao_id", "telefone"})
}, indexes = {
        @Index(name = "idx_cliente_organizacao_id", columnList = "organizacao_id"),
        @Index(name = "idx_cliente_telefone", columnList = "telefone"),
        @Index(name = "idx_cliente_cpf", columnList = "cpf"),
        @Index(name = "idx_cliente_org_ativo", columnList = "organizacao_id, ativo"),
        @Index(name = "idx_cliente_dt_criacao", columnList = "dt_criacao")
})
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Cliente extends User {

    @Column(length = 15)
    private String telefone;

    private LocalDate dataNascimento;

    private Boolean isCadastroIncompleto;

    @Column(length = 14)
    private String cpf;

    private String role = "ROLE_CLIENTE";

    @Column(name = "dt_criacao", columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime dtCriacao;

    @Override
    public String getRole() {
        return this.role;
    }
}
