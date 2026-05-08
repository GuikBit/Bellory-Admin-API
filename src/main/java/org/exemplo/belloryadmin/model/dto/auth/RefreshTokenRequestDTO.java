package org.exemplo.belloryadmin.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {
    @NotBlank(message = "Token e obrigatorio")
    private String token;
}
