package org.exemplo.belloryadmin.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionDTO {

    private Long startedAt;
    private Long lastActiveAt;
    private Integer pageCount;
    private String entryPage;
    private String currentPage;
    private Long duration;
    private String trafficSource;
    private Map<String, String> utmParams;
    private String referrer;
}
