package org.exemplo.belloryadmin.model.dto.tracking.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsContextDTO {

    private Map<String, DeviceInfo> devices;
    private List<BrowserInfo> browsers;
    private List<OsInfo> osList;
    private GeoInfo geo;
    private PerformanceInfo performance;
    private ErrorInfo errors;

    @Data
    @Builder
    public static class DeviceInfo {
        private long visitors;
        private double percentage;
        private double conversionRate;
    }

    @Data
    @Builder
    public static class BrowserInfo {
        private String browser;
        private long visitors;
        private double percentage;
    }

    @Data
    @Builder
    public static class OsInfo {
        private String os;
        private long visitors;
        private double percentage;
    }

    @Data
    @Builder
    public static class GeoInfo {
        private List<GeoEntry> countries;
        private List<GeoEntry> states;
        private List<GeoEntry> cities;
    }

    @Data
    @Builder
    public static class GeoEntry {
        private String name;
        private long visitors;
        private double percentage;
    }

    @Data
    @Builder
    public static class PerformanceInfo {
        private Map<String, Object> averages;
        private Map<String, Map<String, Object>> byDevice;
        private Map<String, Map<String, Object>> percentiles;
    }

    @Data
    @Builder
    public static class ErrorInfo {
        private long total;
        private List<ErrorTypeCount> byType;
        private List<TopError> topErrors;
    }

    @Data
    @Builder
    public static class ErrorTypeCount {
        private String type;
        private long count;
    }

    @Data
    @Builder
    public static class TopError {
        private String message;
        private long count;
        private String lastSeen;
    }
}
