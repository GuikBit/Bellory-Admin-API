package org.exemplo.belloryadmin.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginRequestDTO {
    @NotBlank(message = "Username e obrigatorio")
    @Size(min = 3, max = 50, message = "Username deve ter entre 3 e 50 caracteres")
    String username;

    @NotBlank(message = "Password e obrigatorio")
    @Size(min = 6, message = "Password deve ter no minimo 6 caracteres")
    String password;
}
