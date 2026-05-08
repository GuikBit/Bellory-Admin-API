package org.exemplo.belloryadmin.model.dto.admin.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserInfoDTO {
    private Long id;
    private String username;
    private String nomeCompleto;
    private String email;
    private String role;
    private boolean ativo;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dtCriacao;
}
