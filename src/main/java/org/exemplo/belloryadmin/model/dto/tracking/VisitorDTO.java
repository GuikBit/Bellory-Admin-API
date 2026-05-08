package org.exemplo.belloryadmin.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisitorDTO {

    @NotBlank
    private String visitorId;

    @NotBlank
    private String sessionId;

    private boolean isNewVisitor;
}
