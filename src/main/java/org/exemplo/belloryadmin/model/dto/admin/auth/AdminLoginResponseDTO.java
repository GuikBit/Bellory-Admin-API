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
public class AdminLoginResponseDTO {
    private boolean success;
    private String message;
    private String token;
    private AdminUserInfoDTO user;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime expiresAt;
}
