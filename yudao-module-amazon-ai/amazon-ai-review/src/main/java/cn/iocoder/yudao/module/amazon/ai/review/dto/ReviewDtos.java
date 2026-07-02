package cn.iocoder.yudao.module.amazon.ai.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for the AI Review Analysis module.
 *
 * @author AmazonOps AI
 */
public class ReviewDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private String reviewId;
        private String asin;
        private Integer rating;
        private String title;
        private String body;
        private Boolean verifiedPurchase;
        private Integer helpfulVotes;
        private String date;
        private String reviewerProfile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewAnalysisReport {
        private SentimentAnalysis sentiment;
        private List<TopicAnalysis> topics;
        private List<PainPoint> painPoints;
        private List<SellingPoint> sellingPoints;
        private List<UserPersona> userPersonas;
        private List<String> commonUseCases;
        private List<String> competitorMentions;
        private List<ImprovementOpportunity> improvementOpportunities;
        private String sampleSizeWarning;
        private Integer totalReviewsAnalyzed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentAnalysis {
        private Integer overallScore;
        private Map<String, Double> distribution; // positive_pct, neutral_pct, negative_pct
        private String trend;
        private Integer fakeReviewRisk;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicAnalysis {
        private String topic;
        private String category;
        private Integer frequency;
        private Double avgRatingWhenMentioned;
        private String sentiment;
        private List<String> sampleQuotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PainPoint {
        private String issue;
        private String severity;
        private Integer frequency;
        private Boolean isProductDefect;
        private Boolean fixableInManufacturing;
        private List<String> sampleQuotes;
        private List<String> affectedPersonas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellingPoint {
        private String feature;
        private Integer frequency;
        private Boolean isUniqueDifferentiator;
        private String credibility;
        private List<String> sampleQuotes;
        private String listingCopySuggestion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPersona {
        private String persona;
        private Integer estimatedPercentage;
        private List<String> needs;
        private String priceSensitivity;
        private String brandLoyalty;
        private String purchaseMotivation;
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImprovementOpportunity {
        private String opportunity;
        private String addressesPainPoint;
        private String estimatedImpact;
        private String feasibility;
    }
}
