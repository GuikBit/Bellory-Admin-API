package org.exemplo.belloryadmin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyAdminUserInfo {
    private Long usuarioAdminId;
    private String username;
    private String nomeCompleto;
    private String email;
    private String role;
}
