package org.exemplo.belloryadmin.model.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Versao admin da TokenValidationResponseDTO.
 * Sem AssinaturaStatusDTO (esse e do fluxo do cliente).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponseDTO {
    private boolean valid;
    private String username;
    private Long userId;
    private String userType;
    private List<String> roles;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime expiresAt;
}
