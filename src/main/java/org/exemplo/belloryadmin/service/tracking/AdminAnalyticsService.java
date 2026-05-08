package org.exemplo.belloryadmin.service.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exemplo.belloryadmin.model.dto.tracking.analytics.*;
import org.exemplo.belloryadmin.model.repository.tracking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminAnalyticsService {

    private final TrackingVisitorRepository visitorRepository;
    private final TrackingSessionRepository sessionRepository;
    private final TrackingPageViewRepository pageViewRepository;
    private final TrackingInteractionEventRepository interactionEventRepository;
    private final TrackingScrollEventRepository scrollEventRepository;
    private final TrackingConversionEventRepository conversionEventRepository;
    private final TrackingErrorEventRepository errorEventRepository;
    private final TrackingPerformanceSnapshotRepository performanceSnapshotRepository;

    public AnalyticsOverviewDTO getOverview(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        long totalVisitors = visitorRepository.countVisitorsInPeriod(start, end);
        long newVisitors = visitorRepository.countNewVisitors(start, end);
        long returningVisitors = visitorRepository.countReturningVisitors(start, end);

        long totalSessions = sessionRepository.countSessionsInPeriod(start, end);
        double avgDuration = sessionRepository.avgDurationInPeriod(start, end);
        double avgPages = sessionRepository.avgPagesInPeriod(start, end);
        long bounces = sessionRepository.countBouncesInPeriod(start, end);
        double bounceRate = totalSessions > 0 ? (double) bounces / totalSessions * 100 : 0;

        long cadastroStarted = conversionEventRepository.countByTypeInPeriod("cadastro_started", start, end);
        long cadastroCompleted = conversionEventRepository.countByTypeInPeriod("cadastro_completed", start, end);
        double conversionRate = totalVisitors > 0 ? (double) cadastroCompleted / totalVisitors * 100 : 0;

        List<Object[]> topPagesRaw = pageViewRepository.getTopPages(start, end, 10);
        List<AnalyticsOverviewDTO.PageStats> topPages = topPagesRaw.stream().map(row ->
                AnalyticsOverviewDTO.PageStats.builder()
                        .path((String) row[0])
                        .views(((Number) row[1]).longValue())
                        .avgTimeOnPage(((Number) row[2]).doubleValue())
                        .build()
        ).toList();

        return AnalyticsOverviewDTO.builder()
                .period(Map.of("start", startDate.toString(), "end", endDate.toString()))
                .visitors(AnalyticsOverviewDTO.VisitorStats.builder()
                        .total(totalVisitors)
                        .newVisitors(newVisitors)
                        .returning(returningVisitors)
                        .build())
                .sessions(AnalyticsOverviewDTO.SessionStats.builder()
                        .total(totalSessions)
                        .averageDuration(avgDuration)
                        .averagePages(round(avgPages, 1))
                        .bounceRate(round(bounceRate, 1))
                        .build())
                .conversions(AnalyticsOverviewDTO.ConversionStats.builder()
                        .cadastroStarted(cadastroStarted)
                        .cadastroCompleted(cadastroCompleted)
                        .conversionRate(round(conversionRate, 2))
                        .build())
                .topPages(topPages)
                .build();
    }

    public AnalyticsTrafficDTO getTraffic(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> sourcesRaw = sessionRepository.getTrafficSourceStats(start, end);
        List<AnalyticsTrafficDTO.TrafficSource> sources = sourcesRaw.stream().map(row ->
                AnalyticsTrafficDTO.TrafficSource.builder()
                        .source((String) row[0])
                        .visitors(((Number) row[1]).longValue())
                        .sessions(((Number) row[2]).longValue())
                        .conversionRate(((Number) row[3]).doubleValue())
                        .build()
        ).toList();

        List<Object[]> campaignsRaw = sessionRepository.getCampaignStats(start, end);
        List<AnalyticsTrafficDTO.Campaign> campaigns = campaignsRaw.stream().map(row ->
                AnalyticsTrafficDTO.Campaign.builder()
                        .campaign((String) row[0])
                        .source((String) row[1])
                        .medium((String) row[2])
                        .visitors(((Number) row[3]).longValue())
                        .conversions(((Number) row[4]).longValue())
                        .build()
        ).toList();

        List<Object[]> referrersRaw = sessionRepository.getTopReferrers(start, end);
        List<AnalyticsTrafficDTO.Referrer> referrers = referrersRaw.stream().map(row ->
                AnalyticsTrafficDTO.Referrer.builder()
                        .referrer((String) row[0])
                        .visitors(((Number) row[1]).longValue())
                        .build()
        ).toList();

        return AnalyticsTrafficDTO.builder()
                .sources(sources)
                .campaigns(campaigns)
                .topReferrers(referrers)
                .build();
    }

    public AnalyticsBehaviorDTO getBehavior(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        long totalSessions = sessionRepository.countSessionsInPeriod(start, end);

        // Top CTAs
        List<Object[]> ctasRaw = interactionEventRepository.getTopCTAs(start, end);
        List<AnalyticsBehaviorDTO.CTAStats> topCTAs = ctasRaw.stream().map(row ->
                AnalyticsBehaviorDTO.CTAStats.builder()
                        .elementId((String) row[0])
                        .label((String) row[1])
                        .section((String) row[2])
                        .clicks(((Number) row[3]).longValue())
                        .build()
        ).toList();

        // Scroll depth
        List<Object[]> scrollRaw = scrollEventRepository.getScrollDepthDistribution(start, end, totalSessions);
        Map<String, Double> scrollDepth = new LinkedHashMap<>();
        for (Object[] row : scrollRaw) {
            scrollDepth.put(String.valueOf(((Number) row[0]).intValue()), ((Number) row[1]).doubleValue());
        }

        // Section visibility
        List<Object[]> sectionRaw = scrollEventRepository.getSectionVisibility(start, end, totalSessions);
        List<AnalyticsBehaviorDTO.SectionVisibility> sectionVisibility = sectionRaw.stream().map(row ->
                AnalyticsBehaviorDTO.SectionVisibility.builder()
                        .section((String) row[0])
                        .viewRate(((Number) row[2]).doubleValue())
                        .build()
        ).toList();

        // Exit pages
        List<Object[]> exitRaw = pageViewRepository.getExitPages(start, end, totalSessions);
        List<AnalyticsBehaviorDTO.ExitPage> exitPages = exitRaw.stream().map(row ->
                AnalyticsBehaviorDTO.ExitPage.builder()
                        .path((String) row[0])
                        .exits(((Number) row[1]).longValue())
                        .exitRate(((Number) row[2]).doubleValue())
                        .build()
        ).toList();

        return AnalyticsBehaviorDTO.builder()
                .topCTAs(topCTAs)
                .scrollDepth(scrollDepth)
                .sectionVisibility(sectionVisibility)
                .flows(AnalyticsBehaviorDTO.FlowStats.builder().exitPages(exitPages).build())
                .build();
    }

    public AnalyticsConversionsDTO getConversions(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        long totalVisitors = visitorRepository.countVisitorsInPeriod(start, end);

        // Funnel
        List<Object[]> funnelRaw = conversionEventRepository.getFunnelData(start, end);
        Map<String, Long> funnel = new LinkedHashMap<>();
        funnel.put("totalVisitors", totalVisitors);
        if (!funnelRaw.isEmpty()) {
            Object[] row = funnelRaw.get(0);
            funnel.put("viewedPricing", ((Number) row[0]).longValue());
            funnel.put("startedCadastro", ((Number) row[1]).longValue());
            funnel.put("completedStep0_empresa", ((Number) row[2]).longValue());
            funnel.put("completedStep1_localizacao", ((Number) row[3]).longValue());
            funnel.put("completedStep2_acesso", ((Number) row[4]).longValue());
            funnel.put("completedStep3_tema", ((Number) row[5]).longValue());
            funnel.put("completedStep4_plano", ((Number) row[6]).longValue());
            funnel.put("completedCadastro", ((Number) row[7]).longValue());
        }

        // Plan distribution
        List<Object[]> planRaw = conversionEventRepository.getPlanDistribution(start, end);
        long totalCompleted = planRaw.stream().mapToLong(r -> ((Number) r[2]).longValue()).sum();
        List<AnalyticsConversionsDTO.PlanStats> planDistribution = planRaw.stream().map(row ->
                AnalyticsConversionsDTO.PlanStats.builder()
                        .planId((String) row[0])
                        .planName((String) row[1])
                        .count(((Number) row[2]).longValue())
                        .percentage(totalCompleted > 0 ? round((double) ((Number) row[2]).longValue() / totalCompleted * 100, 1) : 0)
                        .build()
        ).toList();

        // Billing preference
        List<Object[]> billingRaw = conversionEventRepository.getBillingPreference(start, end);
        long monthly = 0, annual = 0;
        for (Object[] row : billingRaw) {
            String cycle = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if ("monthly".equals(cycle)) monthly = count;
            else if ("annual".equals(cycle)) annual = count;
        }
        long totalBilling = monthly + annual;

        // Average time to convert
        List<Object[]> timeRaw = conversionEventRepository.getAverageTimeToConvert(start, end);
        double avgTime = 0, avgSessions = 0;
        if (!timeRaw.isEmpty()) {
            Object[] row = timeRaw.get(0);
            avgTime = ((Number) row[0]).doubleValue();
            avgSessions = ((Number) row[1]).doubleValue();
        }

        return AnalyticsConversionsDTO.builder()
                .funnel(funnel)
                .planDistribution(planDistribution)
                .billingPreference(AnalyticsConversionsDTO.BillingStats.builder()
                        .monthly(monthly)
                        .annual(annual)
                        .annualPercentage(totalBilling > 0 ? round((double) annual / totalBilling * 100, 1) : 0)
                        .build())
                .averageTimeToConvert(AnalyticsConversionsDTO.TimeToConvertStats.builder()
                        .fromFirstVisitMs(avgTime)
                        .averageSessions(round(avgSessions, 1))
                        .build())
                .build();
    }

    public AnalyticsContextDTO getContext(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        // Devices
        List<Object[]> deviceRaw = sessionRepository.getDeviceStats(start, end);
        long totalDeviceVisitors = deviceRaw.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        Map<String, AnalyticsContextDTO.DeviceInfo> devices = new LinkedHashMap<>();
        for (Object[] row : deviceRaw) {
            String type = (String) row[0];
            long visitors = ((Number) row[1]).longValue();
            double convRate = ((Number) row[2]).doubleValue();
            devices.put(type, AnalyticsContextDTO.DeviceInfo.builder()
                    .visitors(visitors)
                    .percentage(totalDeviceVisitors > 0 ? round((double) visitors / totalDeviceVisitors * 100, 1) : 0)
                    .conversionRate(convRate)
                    .build());
        }

        // Browsers
        List<Object[]> browserRaw = sessionRepository.getBrowserStats(start, end);
        long totalBrowserVisitors = browserRaw.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<AnalyticsContextDTO.BrowserInfo> browsers = browserRaw.stream().map(row -> {
            long visitors = ((Number) row[1]).longValue();
            return AnalyticsContextDTO.BrowserInfo.builder()
                    .browser((String) row[0])
                    .visitors(visitors)
                    .percentage(totalBrowserVisitors > 0 ? round((double) visitors / totalBrowserVisitors * 100, 1) : 0)
                    .build();
        }).toList();

        // OS
        List<Object[]> osRaw = sessionRepository.getOsStats(start, end);
        long totalOsVisitors = osRaw.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<AnalyticsContextDTO.OsInfo> osList = osRaw.stream().map(row -> {
            long visitors = ((Number) row[1]).longValue();
            return AnalyticsContextDTO.OsInfo.builder()
                    .os((String) row[0])
                    .visitors(visitors)
                    .percentage(totalOsVisitors > 0 ? round((double) visitors / totalOsVisitors * 100, 1) : 0)
                    .build();
        }).toList();

        // Geo
        List<AnalyticsContextDTO.GeoEntry> countries = buildGeoEntries(sessionRepository.getCountryStats(start, end));
        List<AnalyticsContextDTO.GeoEntry> states = buildGeoEntries(sessionRepository.getStateStats(start, end));
        List<AnalyticsContextDTO.GeoEntry> cities = buildGeoEntries(sessionRepository.getCityStats(start, end));

        // Performance
        Map<String, Object> perfAverages = new LinkedHashMap<>();
        List<Object[]> perfAvgRaw = performanceSnapshotRepository.getAveragePerformance(start, end);
        if (!perfAvgRaw.isEmpty()) {
            Object[] row = perfAvgRaw.get(0);
            perfAverages.put("pageLoadTime", toIntSafe(row[0]));
            perfAverages.put("fcp", toIntSafe(row[1]));
            perfAverages.put("lcp", toIntSafe(row[2]));
            perfAverages.put("fid", toIntSafe(row[3]));
            perfAverages.put("cls", row[4] != null ? ((Number) row[4]).doubleValue() : 0);
            perfAverages.put("ttfb", toIntSafe(row[5]));
        }

        Map<String, Map<String, Object>> byDevice = new LinkedHashMap<>();
        List<Object[]> perfDeviceRaw = performanceSnapshotRepository.getPerformanceByDevice(start, end);
        for (Object[] row : perfDeviceRaw) {
            Map<String, Object> devicePerf = new LinkedHashMap<>();
            devicePerf.put("pageLoadTime", toIntSafe(row[1]));
            devicePerf.put("lcp", toIntSafe(row[2]));
            byDevice.put((String) row[0], devicePerf);
        }

        Map<String, Map<String, Object>> percentiles = new LinkedHashMap<>();
        List<Object[]> percRaw = performanceSnapshotRepository.getPerformancePercentiles(start, end);
        if (!percRaw.isEmpty()) {
            Object[] row = percRaw.get(0);
            percentiles.put("p50", Map.of("pageLoadTime", toIntSafe(row[0]), "lcp", toIntSafe(row[1])));
            percentiles.put("p75", Map.of("pageLoadTime", toIntSafe(row[2]), "lcp", toIntSafe(row[3])));
            percentiles.put("p95", Map.of("pageLoadTime", toIntSafe(row[4]), "lcp", toIntSafe(row[5])));
        }

        // Errors
        long totalErrors = errorEventRepository.countErrorsInPeriod(start, end);
        List<Object[]> errorsByTypeRaw = errorEventRepository.getErrorsByType(start, end);
        List<AnalyticsContextDTO.ErrorTypeCount> errorsByType = errorsByTypeRaw.stream().map(row ->
                AnalyticsContextDTO.ErrorTypeCount.builder()
                        .type((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build()
        ).toList();

        List<Object[]> topErrorsRaw = errorEventRepository.getTopErrors(start, end);
        List<AnalyticsContextDTO.TopError> topErrors = topErrorsRaw.stream().map(row ->
                AnalyticsContextDTO.TopError.builder()
                        .message((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .lastSeen(row[2] != null ? row[2].toString() : null)
                        .build()
        ).toList();

        return AnalyticsContextDTO.builder()
                .devices(devices)
                .browsers(browsers)
                .osList(osList)
                .geo(AnalyticsContextDTO.GeoInfo.builder()
                        .countries(countries)
                        .states(states)
                        .cities(cities)
                        .build())
                .performance(AnalyticsContextDTO.PerformanceInfo.builder()
                        .averages(perfAverages)
                        .byDevice(byDevice)
                        .percentiles(percentiles)
                        .build())
                .errors(AnalyticsContextDTO.ErrorInfo.builder()
                        .total(totalErrors)
                        .byType(errorsByType)
                        .topErrors(topErrors)
                        .build())
                .build();
    }

    public AnalyticsRealtimeDTO getRealtime() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        long activeVisitors = sessionRepository.countActiveVisitorsSince(fiveMinutesAgo);

        List<Object[]> activePagesRaw = pageViewRepository.getActivePages(fiveMinutesAgo);
        List<AnalyticsRealtimeDTO.ActivePage> activePages = activePagesRaw.stream().map(row ->
                AnalyticsRealtimeDTO.ActivePage.builder()
                        .path((String) row[0])
                        .visitors(((Number) row[1]).longValue())
                        .build()
        ).toList();

        // Recent events (mix de diferentes tipos)
        List<Map<String, Object>> recentEvents = new ArrayList<>();

        List<Object[]> recentConversions = conversionEventRepository.getRecentConversions(thirtyMinutesAgo);
        for (Object[] row : recentConversions) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", row[0]);
            event.put("planId", row[1]);
            event.put("timestamp", row[2]);
            recentEvents.add(event);
        }

        List<Object[]> recentInteractions = interactionEventRepository.getRecentInteractions(thirtyMinutesAgo);
        for (Object[] row : recentInteractions) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", row[0]);
            event.put("elementId", row[1]);
            event.put("timestamp", row[2]);
            recentEvents.add(event);
        }

        // Sort by timestamp descending
        recentEvents.sort((a, b) -> {
            Object tsA = a.get("timestamp");
            Object tsB = b.get("timestamp");
            if (tsA instanceof Comparable && tsB instanceof Comparable) {
                return ((Comparable) tsB).compareTo(tsA);
            }
            return 0;
        });
        if (recentEvents.size() > 20) {
            recentEvents = recentEvents.subList(0, 20);
        }

        long last30Visitors = sessionRepository.countActiveVisitorsSince(thirtyMinutesAgo);
        long last30PageViews = pageViewRepository.countPageViewsInPeriod(thirtyMinutesAgo, LocalDateTime.now());
        long last30Conversions = conversionEventRepository.countConversionsSince(thirtyMinutesAgo);

        return AnalyticsRealtimeDTO.builder()
                .activeVisitors(activeVisitors)
                .activePages(activePages)
                .recentEvents(recentEvents)
                .last30Minutes(AnalyticsRealtimeDTO.Last30Minutes.builder()
                        .visitors(last30Visitors)
                        .pageViews(last30PageViews)
                        .conversions(last30Conversions)
                        .build())
                .build();
    }

    private List<AnalyticsContextDTO.GeoEntry> buildGeoEntries(List<Object[]> raw) {
        long total = raw.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        return raw.stream().map(row -> {
            long visitors = ((Number) row[1]).longValue();
            return AnalyticsContextDTO.GeoEntry.builder()
                    .name((String) row[0])
                    .visitors(visitors)
                    .percentage(total > 0 ? round((double) visitors / total * 100, 1) : 0)
                    .build();
        }).toList();
    }

    private double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private int toIntSafe(Object val) {
        return val != null ? ((Number) val).intValue() : 0;
    }
}
