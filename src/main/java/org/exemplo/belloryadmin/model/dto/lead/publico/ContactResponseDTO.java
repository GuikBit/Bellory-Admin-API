package org.exemplo.belloryadmin.model.dto.lead.publico;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta do {@code POST /api/contato} no formato esperado pelo frontend
 * da landing page (campo {@code code} é o contrato; {@code message}
 * pode ser ignorado pelo cliente).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactResponseDTO {
    private boolean ok;
    private String id;
    private String code;
    private String message;

    public static ContactResponseDTO success(String id) {
        return ContactResponseDTO.builder().ok(true).id(id).build();
    }

    public static ContactResponseDTO error(String code, String message) {
        return ContactResponseDTO.builder().ok(false).code(code).message(message).build();
    }
}
