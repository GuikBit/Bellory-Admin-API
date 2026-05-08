package org.exemplo.belloryadmin.model.dto.lead.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveStatusDTO {

    @NotNull
    private Long statusId;

    /** Comentario opcional registrado junto da mudanca. */
    @Size(max = 500)
    private String comentario;
}
