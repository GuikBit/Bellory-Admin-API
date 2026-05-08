package org.exemplo.belloryadmin.model.dto.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerformanceDTO {

    private Integer pageLoadTime;
    private Integer fcp;
    private Integer lcp;
    private Integer fid;
    private BigDecimal cls;
    private Integer ttfb;
}
