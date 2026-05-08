package org.exemplo.belloryadmin.model.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrganizacaoListDTO {
    private Long id;
    private String nomeFantasia;
    private String razaoSocial;
    private String cnpj;
    private String emailPrincipal;
    private String telefone1;
    private String slug;
    private Boolean ativo;
    private String planoNome;
    private String planoCodigo;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dtCadastro;

    // Contadores resumidos
    private Long totalAgendamentos;
    private Long totalClientes;
    private Long totalFuncionarios;
    private Long totalServicos;
    private Long totalInstancias;
}
