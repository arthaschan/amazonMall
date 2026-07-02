package cn.iocoder.yudao.module.amazon.ai.ad;

import cn.iocoder.yudao.module.amazon.ai.ad.dto.AdDtos.*;
import cn.iocoder.yudao.module.amazon.ai.core.AiModelRouter;
import cn.iocoder.yudao.module.amazon.ai.core.AiResponseParser;
import cn.iocoder.yudao.module.amazon.ai.core.AiTokenTracker;
import cn.iocoder.yudao.module.amazon.ai.core.PromptTemplateEngine;
import cn.iocoder.yudao.module.amazon.ai.core.enums.AiTaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI-powered Amazon PPC advertising optimization service.
 *
 * <p>Analyzes advertising campaign data and generates specific, prioritized
 * optimization recommendations across five categories:
 * <ol>
 *   <li><b>Negative Keywords</b> - Identify wasted spend on irrelevant search terms</li>
 *   <li><b>Bid Adjustments</b> - Increase/decrease bids based on performance data</li>
 *   <li><b>Budget Allocation</b> - Reallocate budget to high-performing campaigns</li>
 *   <li><b>New Keywords</b> - Discover converting search terms not yet targeted</li>
 *   <li><b>Campaign Structure</b> - Improve campaign organization</li>
 * </ol>
 *
 * <p>Suggestions are prioritized by expected ROI with HIGH/MEDIUM/LOW priority.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAdOptimizerService {

    private static final String TEMPLATE_NAME = "ad_optimization";

    private final AiModelRouter modelRouter;
    private final PromptTemplateEngine templateEngine;
    private final AiTokenTracker tokenTracker;
    private final AiResponseParser responseParser;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Analyze ad campaign data and generate optimization suggestions.
     *
     * @param reportData the advertising report data
     * @return list of optimization suggestions, sorted by priority
     */
    public List<AdSuggestion> analyzeAdData(AdReportData reportData) {
        log.info("Analyzing ad data for ASIN: {}", reportData.getAsin());

        AdOptimizationReport fullReport = generateFullReport(reportData);
        if (fullReport == null || fullReport.getSuggestions() == null) {
            return List.of();
        }

        // Sort by priority: HIGH > MEDIUM > LOW
        List<AdSuggestion> sorted = fullReport.getSuggestions().stream()
                .sorted((a, b) -> {
                    int pa = priorityOrdinal(a.getPriority());
                    int pb = priorityOrdinal(b.getPriority());
                    return Integer.compare(pa, pb);
                })
                .toList();

        log.info("Generated {} optimization suggestions ({} HIGH, {} MEDIUM, {} LOW)",
                sorted.size(),
                sorted.stream().filter(s -> "HIGH".equals(s.getPriority())).count(),
                sorted.stream().filter(s -> "MEDIUM".equals(s.getPriority())).count(),
                sorted.stream().filter(s -> "LOW".equals(s.getPriority())).count());

        return sorted;
    }

    /**
     * Generate a full optimization report with all categories.
     */
    public AdOptimizationReport generateFullReport(AdReportData reportData) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("asin", reportData.getAsin() != null ? reportData.getAsin() : "N/A");
        vars.put("dateRange", reportData.getDateRange() != null ? reportData.getDateRange() : "Last 30 days");
        vars.put("totalSpend", reportData.getTotalSpend() != null ? String.format("$%.2f", reportData.getTotalSpend()) : "N/A");
        vars.put("totalRevenue", reportData.getTotalRevenue() != null ? String.format("$%.2f", reportData.getTotalRevenue()) : "N/A");
        vars.put("overallAcos", reportData.getOverallAcos() != null ? String.format("%.1f%%", reportData.getOverallAcos()) : "N/A");
        vars.put("overallTacos", reportData.getOverallTacos() != null ? String.format("%.1f%%", reportData.getOverallTacos()) : "N/A");
        vars.put("totalImpressions", reportData.getTotalImpressions() != null ? String.valueOf(reportData.getTotalImpressions()) : "N/A");
        vars.put("totalClicks", reportData.getTotalClicks() != null ? String.valueOf(reportData.getTotalClicks()) : "N/A");
        vars.put("overallCtr", reportData.getOverallCtr() != null ? String.format("%.2f%%", reportData.getOverallCtr()) : "N/A");
        vars.put("overallCpc", reportData.getOverallCpc() != null ? String.format("$%.2f", reportData.getOverallCpc()) : "N/A");
        vars.put("overallConversionRate", reportData.getOverallConversionRate() != null
                ? String.format("%.1f%%", reportData.getOverallConversionRate()) : "N/A");
        vars.put("campaignData", reportData.getCampaignData() != null ? reportData.getCampaignData() : "N/A");
        vars.put("searchTermReport", reportData.getSearchTermReport() != null ? reportData.getSearchTermReport() : "N/A");
        vars.put("keywordPerformance", reportData.getKeywordPerformance() != null ? reportData.getKeywordPerformance() : "N/A");
        vars.put("price", reportData.getPrice() != null ? reportData.getPrice() : "N/A");
        vars.put("rating", reportData.getRating() != null ? reportData.getRating() : "N/A");
        vars.put("organicRank", reportData.getOrganicRank() != null ? reportData.getOrganicRank() : "N/A");

        String prompt = templateEngine.render(TEMPLATE_NAME, vars);

        String response = callLlm(AiTaskType.AD_OPTIMIZATION, prompt,
                "You are an Amazon PPC advertising expert. Provide specific, data-driven recommendations. Return valid JSON.");

        try {
            return responseParser.parse(response, AdOptimizationReport.class);
        } catch (AiResponseParser.AiResponseParseException e) {
            log.error("Failed to parse ad optimization response: {}", e.getMessage());
            return buildRuleBasedReport(reportData);
        }
    }

    /**
     * Quick analysis: identify negative keyword candidates from search term data.
     */
    public List<NegativeKeyword> findNegativeKeywords(AdReportData reportData) {
        String prompt = """
                Analyze this search term data and identify negative keyword candidates.

                SEARCH TERM DATA:
                %s

                ASIN: %s | Price: %s

                RULES for negative keywords:
                - Search terms with 10+ clicks and 0 conversions
                - Search terms with spend > $5 and 0 conversions
                - Search terms clearly irrelevant to the product
                - Search terms with conversion rate below 2%% and high spend

                Return JSON:
                {
                    "negative_keywords": [
                        {
                            "search_term": "<the search term>",
                            "match_type": "<negative_exact|negative_phrase>",
                            "current_spend": <amount>,
                            "clicks": <number>,
                            "conversions": <number>,
                            "reason": "<why this should be negatived>"
                        }
                    ]
                }
                """.formatted(
                reportData.getSearchTermReport() != null ? reportData.getSearchTermReport() : "N/A",
                reportData.getAsin(),
                reportData.getPrice()
        );

        String response = callLlm(AiTaskType.AD_OPTIMIZATION, prompt,
                "You are an Amazon PPC expert. Identify wasted spend. Return valid JSON.");

        try {
            Map<String, Object> parsed = responseParser.parseToMap(response);
            Object negKws = parsed.get("negative_keywords");
            if (negKws instanceof List<?> list) {
                return list.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> responseParser.parse(
                                serializeObject(item), NegativeKeyword.class))
                        .toList();
            }
        } catch (Exception e) {
            log.error("Failed to parse negative keywords: {}", e.getMessage());
        }

        return List.of();
    }

    /**
     * Suggest bid adjustments based on keyword performance data.
     */
    public List<BidAdjustment> suggestBidAdjustments(AdReportData reportData) {
        String prompt = """
                Analyze this keyword performance data and suggest bid adjustments.

                KEYWORD PERFORMANCE:
                %s

                OVERALL METRICS:
                - ACOS: %s
                - CPC: %s
                - Conversion Rate: %s
                - ASIN Price: %s

                RULES:
                - Increase bids: high conversion rate keywords with low impression share
                - Decrease bids: low conversion rate keywords with high spend
                - Target ACOS: 25-35%% for most categories
                - Max bid change: +/- 30%% of current bid

                Return JSON:
                {
                    "bid_adjustments": [
                        {
                            "keyword": "<keyword>",
                            "current_bid": <current>,
                            "recommended_bid": <recommended>,
                            "direction": "<increase|decrease>",
                            "reason": "<data-backed reason>",
                            "expected_impact": "<estimated impact>"
                        }
                    ]
                }
                """.formatted(
                reportData.getKeywordPerformance() != null ? reportData.getKeywordPerformance() : "N/A",
                reportData.getOverallAcos() != null ? String.format("%.1f%%", reportData.getOverallAcos()) : "N/A",
                reportData.getOverallCpc() != null ? String.format("$%.2f", reportData.getOverallCpc()) : "N/A",
                reportData.getOverallConversionRate() != null ? String.format("%.1f%%", reportData.getOverallConversionRate()) : "N/A",
                reportData.getPrice()
        );

        String response = callLlm(AiTaskType.AD_OPTIMIZATION, prompt,
                "You are an Amazon PPC expert. Suggest specific bid changes. Return valid JSON.");

        try {
            Map<String, Object> parsed = responseParser.parseToMap(response);
            Object adjustments = parsed.get("bid_adjustments");
            if (adjustments instanceof List<?> list) {
                return list.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> responseParser.parse(
                                serializeObject(item), BidAdjustment.class))
                        .toList();
            }
        } catch (Exception e) {
            log.error("Failed to parse bid adjustments: {}", e.getMessage());
        }

        return List.of();
    }

    // -----------------------------------------------------------------------
    // Rule-based fallback
    // -----------------------------------------------------------------------

    private AdOptimizationReport buildRuleBasedReport(AdReportData data) {
        List<AdSuggestion> suggestions = new ArrayList<>();

        // Check ACOS
        if (data.getOverallAcos() != null) {
            if (data.getOverallAcos() > 50) {
                suggestions.add(AdSuggestion.builder()
                        .category("bid_adjustment")
                        .priority("HIGH")
                        .action("Reduce bids across underperforming keywords")
                        .rationale("ACOS of " + String.format("%.1f%%", data.getOverallAcos()) + " is significantly above target")
                        .expectedImpact("Reduce ACOS by 10-20%")
                        .confidence(80)
                        .build());
            }
        }

        // Check CTR
        if (data.getOverallCtr() != null && data.getOverallCtr() < 0.3) {
            suggestions.add(AdSuggestion.builder()
                    .category("campaign_structure")
                    .priority("MEDIUM")
                    .action("Improve ad relevance and targeting")
                    .rationale("CTR of " + String.format("%.2f%%", data.getOverallCtr()) + " is below average (target: 0.3-0.7%)")
                    .expectedImpact("Improve CTR by 20-50%")
                    .confidence(70)
                    .build());
        }

        // Check conversion rate
        if (data.getOverallConversionRate() != null && data.getOverallConversionRate() < 5) {
            suggestions.add(AdSuggestion.builder()
                    .category("new_keywords")
                    .priority("HIGH")
                    .action("Review listing quality and keyword relevance")
                    .rationale("Conversion rate of " + String.format("%.1f%%", data.getOverallConversionRate())
                            + " suggests listing or targeting issues")
                    .expectedImpact("Improve conversion rate to 8-12%")
                    .confidence(65)
                    .build());
        }

        return AdOptimizationReport.builder()
                .summary(Summary.builder()
                        .currentPerformanceGrade(data.getOverallAcos() != null
                                ? (data.getOverallAcos() < 25 ? "B" : data.getOverallAcos() < 40 ? "C" : "D")
                                : "N/A")
                        .top3Actions(suggestions.stream()
                                .limit(3).map(AdSuggestion::getAction).toList())
                        .build())
                .suggestions(suggestions)
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private int priorityOrdinal(String priority) {
        return switch (priority != null ? priority.toUpperCase() : "") {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }

    private String callLlm(AiTaskType taskType, String prompt, String systemMessage) {
        ChatClient client = modelRouter.getChatClient(taskType);
        try {
            ChatResponse response = client.prompt()
                    .system(systemMessage)
                    .user(prompt)
                    .call()
                    .chatResponse();
            trackUsage(taskType, response);
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("LLM call failed for [{}], trying fallback: {}", taskType.getCode(), e.getMessage());
            ChatClient fallback = modelRouter.getFallbackChatClient(taskType);
            ChatResponse response = fallback.prompt()
                    .system(systemMessage)
                    .user(prompt)
                    .call()
                    .chatResponse();
            trackUsage(taskType, response);
            return response.getResult().getOutput().getText();
        }
    }

    private void trackUsage(AiTaskType taskType, ChatResponse response) {
        try {
            var metadata = response.getMetadata();
            if (metadata != null && metadata.getUsage() != null) {
                tokenTracker.track(taskType,
                        modelRouter.resolveModel(taskType),
                        (int) metadata.getUsage().getPromptTokens(),
                        (int) metadata.getUsage().getCompletionTokens());
            }
        } catch (Exception e) {
            log.warn("Failed to track token usage: {}", e.getMessage());
        }
    }

    private String serializeObject(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
