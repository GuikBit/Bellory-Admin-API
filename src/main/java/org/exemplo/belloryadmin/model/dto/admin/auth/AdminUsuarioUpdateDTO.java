package org.exemplo.belloryadmin.model.dto.admin.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUsuarioUpdateDTO {

    @Size(min = 3, max = 50, message = "Username deve ter entre 3 e 50 caracteres")
    private String username;

    private String nomeCompleto;

    @Size(min = 6, message = "Password deve ter no mínimo 6 caracteres")
    private String password;

    @Email(message = "Email inválido")
    private String email;
}
