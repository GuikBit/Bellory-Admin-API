package org.exemplo.belloryadmin.model.entity.endereco;

import jakarta.persistence.*;
import lombok.*;

/**
 * Endereco - versao admin (read-mostly).
 *
 * <p>Sem o relacionamento reverso com Cliente: o admin nunca le enderecos pelo
 * cliente, apenas via Organizacao.enderecoPrincipal. Reduz grafo carregado.</p>
 */
@Entity
@Table(name = "endereco", schema = "app", indexes = {
    @Index(name = "idx_endereco_cliente_id", columnList = "cliente_id"),
    @Index(name = "idx_endereco_tipo", columnList = "tipo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK cliente_id existe no banco mas o admin nao mapeia o relacionamento
    // (read-only, e admin nao usa enderecos de cliente).
    @Column(name = "cliente_id", insertable = false, updatable = false)
    private Long clienteId;

    @Column(nullable = false, length = 255)
    private String logradouro;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 10)
    private String cep;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(columnDefinition = "TEXT")
    private String referencia;

    @Column(length = 255)
    private String complemento;

    @Column(nullable = false)
    private boolean isPrincipal = false;

    @Embedded
    private Coordenadas coordenadas;

    @Column(nullable = false)
    private boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoEndereco tipo;

    public enum TipoEndereco {
        RESIDENCIAL,
        COMERCIAL,
        CORRESPONDENCIA,
        OUTRO
    }
}
