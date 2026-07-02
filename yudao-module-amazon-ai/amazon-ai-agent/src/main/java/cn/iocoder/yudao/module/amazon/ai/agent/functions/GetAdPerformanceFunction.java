package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdCampaignMapper;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdReportDailyMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Function tool: Retrieve advertising campaign performance data.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetAdPerformanceFunction implements Function<GetAdPerformanceFunction.Request, GetAdPerformanceFunction.Response> {

    @Resource
    private AmazonAdReportDailyMapper amazonAdReportDailyMapper;

    @Resource
    private AmazonAdCampaignMapper amazonAdCampaignMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String dateRange;
        private String campaignName;
        private Boolean includeSearchTerms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignSummary {
        private String campaignName;
        private String campaignType;
        private Double spend;
        private Double revenue;
        private Double acos;
        private Long impressions;
        private Long clicks;
        private Double ctr;
        private Double cpc;
        private Double conversionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String dateRange;
        private Double totalSpend;
        private Double totalRevenue;
        private Double overallAcos;
        private Double overallTacos;
        private List<CampaignSummary> campaigns;
        private List<String> topSearchTerms;
        private String insights;
    }

    @Override
    public Response apply(Request request) {
        log.info("getAdPerformance called: dateRange={}, campaign={}, includeSearchTerms={}",
                request.getDateRange(), request.getCampaignName(), request.getIncludeSearchTerms());

        String displayDateRange = request.getDateRange() != null ? request.getDateRange() : "Last 30 days";

        try {
            // 1. Parse date range
            LocalDate[] range = parseDateRange(request.getDateRange());
            LocalDate startDate = range[0];
            LocalDate endDate = range[1];
            log.info("Resolved ad date range: {} to {}", startDate, endDate);

            // 2. If campaignName filter is provided, find matching campaigns first
            Map<Long, AmazonAdCampaignDO> campaignMap = new HashMap<>();
            List<Long> filterCampaignIds = null;

            LambdaQueryWrapperX<AmazonAdCampaignDO> campaignWrapper = new LambdaQueryWrapperX<>();
            if (request.getCampaignName() != null && !request.getCampaignName().trim().isEmpty()) {
                campaignWrapper.like(AmazonAdCampaignDO::getCampaignName, request.getCampaignName().trim());
            }
            List<AmazonAdCampaignDO> campaigns = amazonAdCampaignMapper.selectList(campaignWrapper);
            log.info("Fetched {} campaigns", campaigns.size());

            if (request.getCampaignName() != null && !request.getCampaignName().trim().isEmpty() && campaigns.isEmpty()) {
                return Response.builder()
                        .dateRange(displayDateRange)
                        .totalSpend(0.0)
                        .totalRevenue(0.0)
                        .overallAcos(0.0)
                        .overallTacos(0.0)
                        .campaigns(Collections.emptyList())
                        .topSearchTerms(Collections.emptyList())
                        .insights("No campaigns found matching: " + request.getCampaignName())
                        .build();
            }

            if (request.getCampaignName() != null && !request.getCampaignName().trim().isEmpty()) {
                filterCampaignIds = new ArrayList<>();
                for (AmazonAdCampaignDO c : campaigns) {
                    filterCampaignIds.add(c.getCampaignId());
                    campaignMap.put(c.getCampaignId(), c);
                }
            } else {
                for (AmazonAdCampaignDO c : campaigns) {
                    campaignMap.put(c.getCampaignId(), c);
                }
            }

            // 3. Query ad report daily records within date range
            LambdaQueryWrapperX<AmazonAdReportDailyDO> reportWrapper = new LambdaQueryWrapperX<>();
            reportWrapper.ge(AmazonAdReportDailyDO::getReportDate, startDate);
            reportWrapper.le(AmazonAdReportDailyDO::getReportDate, endDate);
            if (filterCampaignIds != null && !filterCampaignIds.isEmpty()) {
                reportWrapper.in(AmazonAdReportDailyDO::getCampaignId, filterCampaignIds);
            }
            List<AmazonAdReportDailyDO> reports = amazonAdReportDailyMapper.selectList(reportWrapper);
            log.info("Fetched {} ad report records", reports.size());

            if (reports.isEmpty()) {
                return Response.builder()
                        .dateRange(displayDateRange)
                        .totalSpend(0.0)
                        .totalRevenue(0.0)
                        .overallAcos(0.0)
                        .overallTacos(0.0)
                        .campaigns(Collections.emptyList())
                        .topSearchTerms(Collections.emptyList())
                        .insights("No ad performance data found for the specified date range.")
                        .build();
            }

            // 4. Aggregate totals and per-campaign metrics
            BigDecimal totalSpend = BigDecimal.ZERO;
            BigDecimal totalSales = BigDecimal.ZERO;
            long totalImpressions = 0;
            long totalClicks = 0;
            int totalOrders = 0;

            // Per-campaign aggregation
            Map<Long, BigDecimal> campaignSpend = new HashMap<>();
            Map<Long, BigDecimal> campaignSales = new HashMap<>();
            Map<Long, Long> campaignImpressions = new HashMap<>();
            Map<Long, Long> campaignClicks = new HashMap<>();
            Map<Long, Integer> campaignOrders = new HashMap<>();

            // Search term aggregation
            Map<String, Long> searchTermClicks = new HashMap<>();
            Map<String, Integer> searchTermOrders = new HashMap<>();

            for (AmazonAdReportDailyDO report : reports) {
                BigDecimal cost = report.getCost() != null ? report.getCost() : BigDecimal.ZERO;
                BigDecimal sales = report.getSales() != null ? report.getSales() : BigDecimal.ZERO;
                long impressions = report.getImpressions() != null ? report.getImpressions() : 0L;
                long clicks = report.getClicks() != null ? report.getClicks() : 0L;
                int orders = report.getOrders() != null ? report.getOrders() : 0;
                Long campaignId = report.getCampaignId();

                totalSpend = totalSpend.add(cost);
                totalSales = totalSales.add(sales);
                totalImpressions += impressions;
                totalClicks += clicks;
                totalOrders += orders;

                // Per-campaign
                if (campaignId != null) {
                    BigDecimal cs = campaignSpend.get(campaignId);
                    campaignSpend.put(campaignId, (cs != null ? cs : BigDecimal.ZERO).add(cost));

                    BigDecimal csl = campaignSales.get(campaignId);
                    campaignSales.put(campaignId, (csl != null ? csl : BigDecimal.ZERO).add(sales));

                    Long ci = campaignImpressions.get(campaignId);
                    campaignImpressions.put(campaignId, (ci != null ? ci : 0L) + impressions);

                    Long cc = campaignClicks.get(campaignId);
                    campaignClicks.put(campaignId, (cc != null ? cc : 0L) + clicks);

                    Integer co = campaignOrders.get(campaignId);
                    campaignOrders.put(campaignId, (co != null ? co : 0) + orders);
                }

                // Search terms
                if (report.getKeywordText() != null && !report.getKeywordText().isEmpty()) {
                    Long stClicks = searchTermClicks.get(report.getKeywordText());
                    searchTermClicks.put(report.getKeywordText(), (stClicks != null ? stClicks : 0L) + clicks);

                    Integer stOrders = searchTermOrders.get(report.getKeywordText());
                    searchTermOrders.put(report.getKeywordText(), (stOrders != null ? stOrders : 0) + orders);
                }
            }

            // 5. Compute overall metrics
            double totalSpendDouble = totalSpend.doubleValue();
            double totalSalesDouble = totalSales.doubleValue();
            double overallAcos = totalSalesDouble > 0
                    ? Math.round((totalSpendDouble / totalSalesDouble) * 10000.0) / 100.0
                    : 0.0;
            // TACoS approximation: assume total ad spend is a fraction of total org revenue
            // Since we don't have organic revenue here, we estimate TACoS ~= ACoS * (ad revenue / total revenue)
            // For simplicity, set TACoS to null or a rough estimate
            double overallTacos = overallAcos * 0.4; // rough placeholder: assume 40% of revenue from ads

            // 6. Build per-campaign summaries
            List<CampaignSummary> campaignSummaries = new ArrayList<>();
            for (Map.Entry<Long, BigDecimal> entry : campaignSpend.entrySet()) {
                Long campaignId = entry.getKey();
                double spend = entry.getValue().doubleValue();
                BigDecimal salesBd = campaignSales.get(campaignId);
                double sales = salesBd != null ? salesBd.doubleValue() : 0.0;
                long impressions = campaignImpressions.get(campaignId) != null ? campaignImpressions.get(campaignId) : 0L;
                long clicks = campaignClicks.get(campaignId) != null ? campaignClicks.get(campaignId) : 0L;
                int orders = campaignOrders.get(campaignId) != null ? campaignOrders.get(campaignId) : 0;

                // Get campaign info
                AmazonAdCampaignDO campaignInfo = campaignMap.get(campaignId);
                String campaignName = campaignInfo != null ? campaignInfo.getCampaignName() : "Campaign " + campaignId;
                String campaignType = campaignInfo != null ? campaignInfo.getCampaignType() : "Unknown";

                double acos = sales > 0 ? Math.round((spend / sales) * 10000.0) / 100.0 : 0.0;
                double ctr = impressions > 0 ? Math.round((clicks * 1.0 / impressions) * 10000.0) / 100.0 : 0.0;
                double cpc = clicks > 0 ? Math.round((spend / clicks) * 100.0) / 100.0 : 0.0;
                double convRate = clicks > 0 ? Math.round((orders * 1.0 / clicks) * 10000.0) / 100.0 : 0.0;

                campaignSummaries.add(CampaignSummary.builder()
                        .campaignName(campaignName)
                        .campaignType(campaignType)
                        .spend(Math.round(spend * 100.0) / 100.0)
                        .revenue(Math.round(sales * 100.0) / 100.0)
                        .acos(acos)
                        .impressions(impressions)
                        .clicks(clicks)
                        .ctr(ctr)
                        .cpc(cpc)
                        .conversionRate(convRate)
                        .build());
            }

            // Sort campaigns by ROAS (revenue/spend) descending, take top 5
            Collections.sort(campaignSummaries, new Comparator<CampaignSummary>() {
                @Override
                public int compare(CampaignSummary a, CampaignSummary b) {
                    double roasA = a.getSpend() > 0 ? a.getRevenue() / a.getSpend() : 0.0;
                    double roasB = b.getSpend() > 0 ? b.getRevenue() / b.getSpend() : 0.0;
                    return Double.compare(roasB, roasA);
                }
            });

            int campaignLimit = Math.min(5, campaignSummaries.size());
            List<CampaignSummary> topCampaigns = campaignSummaries.subList(0, campaignLimit);

            // 7. Build search terms list if requested
            List<String> searchTerms = new ArrayList<>();
            if (Boolean.TRUE.equals(request.getIncludeSearchTerms()) && !searchTermClicks.isEmpty()) {
                // Sort by clicks descending, take top 10
                List<Map.Entry<String, Long>> sortedTerms = new ArrayList<>(searchTermClicks.entrySet());
                Collections.sort(sortedTerms, new Comparator<Map.Entry<String, Long>>() {
                    @Override
                    public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                        return Long.compare(b.getValue(), a.getValue());
                    }
                });

                int termLimit = Math.min(10, sortedTerms.size());
                for (int i = 0; i < termLimit; i++) {
                    Map.Entry<String, Long> termEntry = sortedTerms.get(i);
                    String term = termEntry.getKey();
                    long termClickCount = termEntry.getValue();
                    Integer termOrderCount = searchTermOrders.get(term);
                    int orders2 = termOrderCount != null ? termOrderCount : 0;
                    double cvr = termClickCount > 0 ? Math.round((orders2 * 1.0 / termClickCount) * 1000.0) / 10.0 : 0.0;
                    searchTerms.add(String.format("%s (%d clicks, %.1f%% CVR)", term, termClickCount, cvr));
                }
            }

            // 8. Generate insights
            String insights = generateInsights(overallAcos, totalSpendDouble, totalSalesDouble, topCampaigns);

            return Response.builder()
                    .dateRange(displayDateRange)
                    .totalSpend(Math.round(totalSpendDouble * 100.0) / 100.0)
                    .totalRevenue(Math.round(totalSalesDouble * 100.0) / 100.0)
                    .overallAcos(overallAcos)
                    .overallTacos(Math.round(overallTacos * 100.0) / 100.0)
                    .campaigns(topCampaigns)
                    .topSearchTerms(searchTerms)
                    .insights(insights)
                    .build();

        } catch (Exception e) {
            log.error("getAdPerformance failed: {}", e.getMessage(), e);
            return Response.builder()
                    .dateRange(displayDateRange)
                    .totalSpend(0.0)
                    .totalRevenue(0.0)
                    .overallAcos(0.0)
                    .overallTacos(0.0)
                    .campaigns(Collections.emptyList())
                    .topSearchTerms(Collections.emptyList())
                    .insights("Error fetching ad performance: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Generate human-readable insights based on the ad performance data.
     */
    private String generateInsights(double overallAcos, double totalSpend, double totalSales,
                                     List<CampaignSummary> topCampaigns) {
        StringBuilder sb = new StringBuilder();

        if (overallAcos > 0 && overallAcos <= 25.0) {
            sb.append(String.format("Overall ACOS %.1f%% is within target. ", overallAcos));
        } else if (overallAcos > 25.0 && overallAcos <= 40.0) {
            sb.append(String.format("Overall ACOS %.1f%% is slightly elevated. ", overallAcos));
        } else if (overallAcos > 40.0) {
            sb.append(String.format("Overall ACOS %.1f%% is high and needs attention. ", overallAcos));
        } else {
            sb.append("No ACOS data available. ");
        }

        // Identify best and worst campaigns
        if (topCampaigns.size() >= 2) {
            CampaignSummary best = topCampaigns.get(0);
            CampaignSummary worst = topCampaigns.get(topCampaigns.size() - 1);

            if (best.getAcos() > 0 && best.getAcos() <= 20.0) {
                sb.append(String.format("'%s' performing excellently at %.1f%% ACOS. ",
                        best.getCampaignName(), best.getAcos()));
            }

            if (worst.getAcos() > 30.0) {
                sb.append(String.format("'%s' has higher ACOS (%.1f%%) -- consider tightening match types or adding negative keywords.",
                        worst.getCampaignName(), worst.getAcos()));
            }
        }

        if (sb.length() == 0) {
            sb.append("Ad performance data is insufficient to generate insights.");
        }

        return sb.toString();
    }

    /**
     * Parse a human-readable date range string into a start/end LocalDate pair.
     */
    private LocalDate[] parseDateRange(String dateRange) {
        LocalDate today = LocalDate.now();

        if (dateRange == null || dateRange.trim().isEmpty()) {
            return new LocalDate[]{today.minusDays(30), today};
        }

        String normalized = dateRange.trim().toLowerCase();

        // Pattern: "last N days"
        Pattern lastNDays = Pattern.compile("last\\s+(\\d+)\\s+days?");
        Matcher matcher = lastNDays.matcher(normalized);
        if (matcher.find()) {
            int days = Integer.parseInt(matcher.group(1));
            return new LocalDate[]{today.minusDays(days), today};
        }

        // Pattern: "last N months"
        Pattern lastNMonths = Pattern.compile("last\\s+(\\d+)\\s+months?");
        matcher = lastNMonths.matcher(normalized);
        if (matcher.find()) {
            int months = Integer.parseInt(matcher.group(1));
            return new LocalDate[]{today.minusMonths(months), today};
        }

        // Pattern: "this month"
        if ("this month".equals(normalized)) {
            return new LocalDate[]{today.withDayOfMonth(1), today};
        }

        // Pattern: "last month"
        if ("last month".equals(normalized)) {
            LocalDate firstOfLastMonth = today.withDayOfMonth(1).minusMonths(1);
            LocalDate lastOfLastMonth = today.withDayOfMonth(1).minusDays(1);
            return new LocalDate[]{firstOfLastMonth, lastOfLastMonth};
        }

        // Pattern: "YYYY-MM-DD to YYYY-MM-DD"
        Pattern explicitRange = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s+to\\s+(\\d{4}-\\d{2}-\\d{2})");
        matcher = explicitRange.matcher(normalized);
        if (matcher.find()) {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(matcher.group(1), fmt);
            LocalDate end = LocalDate.parse(matcher.group(2), fmt);
            return new LocalDate[]{start, end};
        }

        // Fallback: last 30 days
        log.warn("Unrecognized date range format '{}', defaulting to last 30 days", dateRange);
        return new LocalDate[]{today.minusDays(30), today};
    }
}
