package org.exemplo.belloryadmin.model.dto.tracking.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsOverviewDTO {

    private Map<String, String> period;
    private VisitorStats visitors;
    private SessionStats sessions;
    private ConversionStats conversions;
    private List<PageStats> topPages;

    @Data
    @Builder
    public static class VisitorStats {
        private long total;
        private long newVisitors;
        private long returning;
    }

    @Data
    @Builder
    public static class SessionStats {
        private long total;
        private double averageDuration;
        private double averagePages;
        private double bounceRate;
    }

    @Data
    @Builder
    public static class ConversionStats {
        private long cadastroStarted;
        private long cadastroCompleted;
        private double conversionRate;
    }

    @Data
    @Builder
    public static class PageStats {
        private String path;
        private long views;
        private double avgTimeOnPage;
    }
}
