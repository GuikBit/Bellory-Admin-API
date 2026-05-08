package org.exemplo.belloryadmin.model.dto.tracking.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsConversionsDTO {

    private Map<String, Long> funnel;
    private List<PlanStats> planDistribution;
    private BillingStats billingPreference;
    private TimeToConvertStats averageTimeToConvert;

    @Data
    @Builder
    public static class PlanStats {
        private String planId;
        private String planName;
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    public static class BillingStats {
        private long monthly;
        private long annual;
        private double annualPercentage;
    }

    @Data
    @Builder
    public static class TimeToConvertStats {
        private double fromFirstVisitMs;
        private double averageSessions;
    }
}
