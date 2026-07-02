package cn.iocoder.yudao.module.amazon.ai.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTOs for the AI Ad Optimization module.
 *
 * @author AmazonOps AI
 */
public class AdDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdReportData {
        private String asin;
        private String dateRange;
        private Double totalSpend;
        private Double totalRevenue;
        private Double overallAcos;
        private Double overallTacos;
        private Long totalImpressions;
        private Long totalClicks;
        private Double overallCtr;
        private Double overallCpc;
        private Double overallConversionRate;
        private String campaignData;
        private String searchTermReport;
        private String keywordPerformance;
        private String price;
        private String rating;
        private String organicRank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdOptimizationReport {
        private Summary summary;
        private List<AdSuggestion> suggestions;
        private List<NegativeKeyword> negativeKeywords;
        private List<BidAdjustment> bidAdjustments;
        private List<NewKeywordOpportunity> newKeywordOpportunities;
        private List<BudgetRecommendation> budgetRecommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private String currentPerformanceGrade;
        private Double estimatedMonthlySavings;
        private Double estimatedRevenueUplift;
        private List<String> top3Actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdSuggestion {
        private String category;
        private String priority; // HIGH, MEDIUM, LOW
        private String action;
        private String rationale;
        private String expectedImpact;
        private List<String> keywordsAffected;
        private List<String> implementationSteps;
        private String risk;
        private Integer confidence; // 0-100
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NegativeKeyword {
        private String searchTerm;
        private String matchType; // negative_exact, negative_phrase
        private Double currentSpend;
        private Integer clicks;
        private Integer conversions;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidAdjustment {
        private String keyword;
        private Double currentBid;
        private Double recommendedBid;
        private String direction; // increase, decrease
        private String reason;
        private String expectedImpact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewKeywordOpportunity {
        private String keyword;
        private String source; // search_term, competitor, category
        private Long estimatedMonthlySearches;
        private Double estimatedCpc;
        private String competitionLevel; // high, medium, low
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetRecommendation {
        private String campaign;
        private Double currentDailyBudget;
        private Double recommendedDailyBudget;
        private String reason;
        private String expectedImpact;
    }
}
