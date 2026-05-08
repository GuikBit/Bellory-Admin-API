package org.exemplo.belloryadmin.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingEventDTO {

    @NotBlank
    private String category;

    @NotBlank
    private String type;

    private Long timestamp;

    // Navigation events
    private String path;
    private String title;
    private String referrer;
    private Long timeOnPreviousPage;

    // Interaction events
    private String elementId;
    private String elementLabel;
    private String section;

    // Scroll events
    private Integer maxDepth;
    private String visibleSection;

    // Conversion/Error/General metadata
    private Map<String, Object> metadata;

    // Error events
    private String message;
    private String stack;
    private String url;
    private Integer statusCode;
}
