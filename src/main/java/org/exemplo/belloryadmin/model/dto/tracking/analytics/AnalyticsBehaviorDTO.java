package org.exemplo.belloryadmin.model.dto.tracking.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsBehaviorDTO {

    private List<CTAStats> topCTAs;
    private Map<String, Double> scrollDepth;
    private List<SectionVisibility> sectionVisibility;
    private FlowStats flows;

    @Data
    @Builder
    public static class CTAStats {
        private String elementId;
        private String label;
        private long clicks;
        private String section;
    }

    @Data
    @Builder
    public static class SectionVisibility {
        private String section;
        private double viewRate;
    }

    @Data
    @Builder
    public static class FlowStats {
        private List<ExitPage> exitPages;
    }

    @Data
    @Builder
    public static class ExitPage {
        private String path;
        private long exits;
        private double exitRate;
    }
}
