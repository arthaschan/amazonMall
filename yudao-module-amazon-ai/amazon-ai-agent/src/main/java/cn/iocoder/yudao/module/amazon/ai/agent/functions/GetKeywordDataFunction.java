package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdSearchTermDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdSearchTermMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/**
 * Function tool: Retrieve keyword ranking and search volume data.
 *
 * <p>Queries the amazon_ad_search_term table for matching search terms and
 * aggregates performance metrics (impressions, clicks, cost, sales, conversion rate).
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetKeywordDataFunction implements Function<GetKeywordDataFunction.Request, GetKeywordDataFunction.Response> {

    @Resource
    private AmazonAdSearchTermMapper amazonAdSearchTermMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** The keyword to look up */
        private String keyword;
        /** Optional ASIN for rank tracking */
        private String asin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordData {
        private String keyword;
        private Long monthlySearchVolume;
        private Double searchVolumeTrend; // percentage change vs last month
        private Integer organicRank;
        private Integer sponsoredRank;
        private Double clickShare;
        private Double conversionShare;
        private String competitionLevel; // high, medium, low
        private Double suggestedBid;
        private List<String> relatedKeywords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private KeywordData keywordData;
        private List<KeywordData> relatedKeywords;
        private String insights;
    }

    @Override
    public Response apply(Request request) {
        log.info("getKeywordData called: keyword={}, asin={}", request.getKeyword(), request.getAsin());

        String keyword = request.getKeyword() != null ? request.getKeyword() : "";

        if (keyword.isEmpty()) {
            return Response.builder()
                    .insights("No keyword specified. Please provide a keyword to search.")
                    .relatedKeywords(Collections.<KeywordData>emptyList())
                    .build();
        }

        try {
            // Query search terms matching the keyword pattern (LIKE match)
            List<AmazonAdSearchTermDO> searchTerms = amazonAdSearchTermMapper.selectList(
                    new LambdaQueryWrapperX<AmazonAdSearchTermDO>()
                            .like(AmazonAdSearchTermDO::getSearchTerm, keyword));

            if (searchTerms.isEmpty()) {
                return buildNoDataResponse(keyword);
            }

            // Aggregate metrics per unique search term
            Map<String, SearchTermMetrics> metricsByTerm = new LinkedHashMap<String, SearchTermMetrics>();
            long totalImpressions = 0;
            long totalClicks = 0;
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalSales = BigDecimal.ZERO;
            int totalOrders = 0;

            for (AmazonAdSearchTermDO term : searchTerms) {
                String termText = term.getSearchTerm() != null ? term.getSearchTerm().trim().toLowerCase() : "";
                if (termText.isEmpty()) {
                    continue;
                }

                SearchTermMetrics metrics = metricsByTerm.get(termText);
                if (metrics == null) {
                    metrics = new SearchTermMetrics();
                    metrics.searchTerm = termText;
                    metricsByTerm.put(termText, metrics);
                }

                if (term.getImpressions() != null) {
                    metrics.impressions += term.getImpressions();
                    totalImpressions += term.getImpressions();
                }
                if (term.getClicks() != null) {
                    metrics.clicks += term.getClicks();
                    totalClicks += term.getClicks();
                }
                if (term.getCost() != null) {
                    metrics.cost = metrics.cost.add(term.getCost());
                    totalCost = totalCost.add(term.getCost());
                }
                if (term.getSales() != null) {
                    metrics.sales = metrics.sales.add(term.getSales());
                    totalSales = totalSales.add(term.getSales());
                }
                if (term.getOrders() != null) {
                    metrics.orders += term.getOrders();
                    totalOrders += term.getOrders();
                }
                if (term.getAiTag() != null) {
                    metrics.aiTag = term.getAiTag();
                }
            }

            if (metricsByTerm.isEmpty()) {
                return buildNoDataResponse(keyword);
            }

            // Compute derived metrics for each search term
            List<SearchTermMetrics> sortedMetrics = new ArrayList<SearchTermMetrics>(metricsByTerm.values());
            for (SearchTermMetrics m : sortedMetrics) {
                m.conversionRate = m.clicks > 0 ? (m.orders * 100.0 / m.clicks) : 0.0;
                m.cpc = m.clicks > 0 ? m.cost.doubleValue() / m.clicks : 0.0;
                m.ctr = m.impressions > 0 ? (m.clicks * 100.0 / m.impressions) : 0.0;
                m.acos = m.sales.doubleValue() > 0
                        ? (m.cost.doubleValue() * 100.0 / m.sales.doubleValue()) : 0.0;
            }

            // Sort by clicks descending (most traffic first)
            Collections.sort(sortedMetrics, new Comparator<SearchTermMetrics>() {
                @Override
                public int compare(SearchTermMetrics a, SearchTermMetrics b) {
                    return Long.compare(b.clicks, a.clicks);
                }
            });

            // Build main keyword data (aggregated across all matching terms)
            double overallConversionRate = totalClicks > 0 ? (totalOrders * 100.0 / totalClicks) : 0.0;
            double overallCpc = totalClicks > 0 ? totalCost.doubleValue() / totalClicks : 0.0;
            double overallCtr = totalImpressions > 0 ? (totalClicks * 100.0 / totalImpressions) : 0.0;
            double overallAcos = totalSales.doubleValue() > 0
                    ? (totalCost.doubleValue() * 100.0 / totalSales.doubleValue()) : 0.0;

            // Determine competition level based on metrics
            String competitionLevel;
            if (totalImpressions > 10000 && sortedMetrics.size() > 20) {
                competitionLevel = "high";
            } else if (totalImpressions > 3000 || sortedMetrics.size() > 10) {
                competitionLevel = "medium";
            } else {
                competitionLevel = "low";
            }

            // Estimate monthly search volume from impressions (rough estimate)
            long estimatedMonthlyVolume = totalImpressions;

            // Build main KeywordData
            List<String> relatedKeywordStrings = new ArrayList<String>();
            int relatedLimit = Math.min(5, sortedMetrics.size());
            for (int i = 0; i < relatedLimit; i++) {
                relatedKeywordStrings.add(sortedMetrics.get(i).searchTerm);
            }

            KeywordData mainKeyword = KeywordData.builder()
                    .keyword(keyword)
                    .monthlySearchVolume(estimatedMonthlyVolume)
                    .searchVolumeTrend(0.0) // Trend requires historical comparison
                    .organicRank(null) // Organic rank not available from ad data
                    .sponsoredRank(null) // Sponsored rank not available from search term data
                    .clickShare(totalImpressions > 0 ? overallCtr : null)
                    .conversionShare(overallConversionRate)
                    .competitionLevel(competitionLevel)
                    .suggestedBid(Math.round(overallCpc * 1.2 * 100.0) / 100.0) // 20% above avg CPC
                    .relatedKeywords(relatedKeywordStrings)
                    .build();

            // Build related keywords (top performing individual search terms)
            List<KeywordData> relatedKeywords = new ArrayList<KeywordData>();
            int topLimit = Math.min(10, sortedMetrics.size());
            for (int i = 0; i < topLimit; i++) {
                SearchTermMetrics m = sortedMetrics.get(i);
                String termCompetition;
                if (m.impressions > 5000) {
                    termCompetition = "high";
                } else if (m.impressions > 1000) {
                    termCompetition = "medium";
                } else {
                    termCompetition = "low";
                }

                KeywordData kd = KeywordData.builder()
                        .keyword(m.searchTerm)
                        .monthlySearchVolume(m.impressions)
                        .searchVolumeTrend(0.0)
                        .organicRank(null)
                        .sponsoredRank(null)
                        .clickShare(m.ctr)
                        .conversionShare(m.conversionRate)
                        .competitionLevel(termCompetition)
                        .suggestedBid(Math.round(m.cpc * 1.2 * 100.0) / 100.0)
                        .relatedKeywords(Collections.<String>emptyList())
                        .build();
                relatedKeywords.add(kd);
            }

            // Build insights
            StringBuilder insightsBuilder = new StringBuilder();
            insightsBuilder.append(String.format("Found %d unique search terms matching '%s'. ", sortedMetrics.size(), keyword));
            insightsBuilder.append(String.format("Aggregate performance: %d impressions, %d clicks, $%.2f cost, $%.2f sales, %d orders. ",
                    totalImpressions, totalClicks, totalCost.doubleValue(), totalSales.doubleValue(), totalOrders));
            insightsBuilder.append(String.format("Overall CTR: %.2f%%, Conversion rate: %.2f%%, Avg CPC: $%.2f, ACoS: %.1f%%. ",
                    overallCtr, overallConversionRate, overallCpc, overallAcos));

            // AI tag distribution
            int opportunityCount = 0;
            int wasteCount = 0;
            int keepCount = 0;
            int negativeCount = 0;
            for (SearchTermMetrics m : sortedMetrics) {
                if ("OPPORTUNITY".equals(m.aiTag)) {
                    opportunityCount++;
                } else if ("WASTE".equals(m.aiTag)) {
                    wasteCount++;
                } else if ("KEEP".equals(m.aiTag)) {
                    keepCount++;
                } else if ("NEGATIVE".equals(m.aiTag)) {
                    negativeCount++;
                }
            }
            if (opportunityCount + wasteCount + keepCount + negativeCount > 0) {
                insightsBuilder.append(String.format("AI tags: %d opportunities, %d keep, %d waste, %d negative candidates.",
                        opportunityCount, keepCount, wasteCount, negativeCount));
            }

            return Response.builder()
                    .keywordData(mainKeyword)
                    .relatedKeywords(relatedKeywords)
                    .insights(insightsBuilder.toString())
                    .build();

        } catch (Exception e) {
            log.error("Failed to query keyword data for '{}': {}", keyword, e.getMessage(), e);
            return Response.builder()
                    .insights("Error querying keyword data: " + e.getMessage())
                    .relatedKeywords(Collections.<KeywordData>emptyList())
                    .build();
        }
    }

    /**
     * Build response when no ad data exists for the keyword.
     */
    private Response buildNoDataResponse(String keyword) {
        String insights = "No ad search term data found for keyword '" + keyword + "'. "
                + "Keyword performance data is pending ad campaign sync. "
                + "Please ensure your Amazon Advertising campaigns are active and data has been synced.";

        return Response.builder()
                .keywordData(KeywordData.builder()
                        .keyword(keyword)
                        .monthlySearchVolume(0L)
                        .searchVolumeTrend(0.0)
                        .competitionLevel("unknown")
                        .relatedKeywords(Collections.<String>emptyList())
                        .build())
                .relatedKeywords(Collections.<KeywordData>emptyList())
                .insights(insights)
                .build();
    }

    /**
     * Internal holder for per-search-term aggregated metrics.
     */
    private static class SearchTermMetrics {
        String searchTerm;
        long impressions;
        long clicks;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
        int orders;
        String aiTag;
        double conversionRate;
        double cpc;
        double ctr;
        double acos;
    }
}
