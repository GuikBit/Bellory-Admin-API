package org.exemplo.belloryadmin.model.dto.tracking.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsRealtimeDTO {

    private long activeVisitors;
    private List<ActivePage> activePages;
    private List<Map<String, Object>> recentEvents;
    private Last30Minutes last30Minutes;

    @Data
    @Builder
    public static class ActivePage {
        private String path;
        private long visitors;
    }

    @Data
    @Builder
    public static class Last30Minutes {
        private long visitors;
        private long pageViews;
        private long conversions;
    }
}
