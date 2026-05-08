package org.exemplo.belloryadmin.model.dto.lead.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderItemDTO {

    @NotNull
    private Long id;

    @NotNull
    private Integer ordem;
}
