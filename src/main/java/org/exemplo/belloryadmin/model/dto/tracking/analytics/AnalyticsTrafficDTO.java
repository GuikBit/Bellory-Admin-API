package org.exemplo.belloryadmin.model.dto.tracking.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsTrafficDTO {

    private List<TrafficSource> sources;
    private List<Campaign> campaigns;
    private List<Referrer> topReferrers;

    @Data
    @Builder
    public static class TrafficSource {
        private String source;
        private long visitors;
        private long sessions;
        private double conversionRate;
    }

    @Data
    @Builder
    public static class Campaign {
        private String campaign;
        private String source;
        private String medium;
        private long visitors;
        private long conversions;
    }

    @Data
    @Builder
    public static class Referrer {
        private String referrer;
        private long visitors;
    }
}
