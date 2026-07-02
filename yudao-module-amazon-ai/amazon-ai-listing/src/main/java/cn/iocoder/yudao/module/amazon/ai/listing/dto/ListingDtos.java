package cn.iocoder.yudao.module.amazon.ai.listing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for the AI Listing module.
 *
 * @author AmazonOps AI
 */
public class ListingDtos {

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    /**
     * Input data required for listing generation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private String asin;
        private String brand;
        private String category;
        private String productType;
        private String marketplace;
        /** Target language code, e.g. "en", "de", "ja" */
        private String targetLanguage;
        /** Key selling points of the product */
        private List<String> sellingPoints;
        /** Existing title keywords (to avoid duplication in backend) */
        private List<String> existingTitleKeywords;
        /** Existing bullet keywords (to avoid duplication in backend) */
        private List<String> existingBulletKeywords;
        /** Competitor listing titles for context */
        private List<String> competitorListings;
    }

    // -----------------------------------------------------------------------
    // Output
    // -----------------------------------------------------------------------

    /**
     * The complete generated listing.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedListing {
        private String title;
        private List<String> bulletPoints;
        private String description;
        private String backendKeywords;
        private KeywordCoverageReport keywordCoverage;
        private ValidationResult validation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordCoverageReport {
        private List<String> keywordsUsedInTitle;
        private List<String> keywordsUsedInBullets;
        private List<String> keywordsInBackendOnly;
        private List<String> keywordsNotUsed;
        private double coveragePercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean titleLengthOk;
        private boolean bulletLengthOk;
        private boolean backendBytesOk;
        private List<String> forbiddenWordsFound;
        private int titleCharCount;
        private List<Integer> bulletCharCounts;
        private int backendByteCount;
        private boolean passed;
    }

    // -----------------------------------------------------------------------
    // Diagnosis
    // -----------------------------------------------------------------------

    /**
     * Input for listing diagnosis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagnosisInput {
        private String asin;
        private String title;
        private String brand;
        private List<String> bulletPoints;
        private String description;
        private String backendKeywords;
        private String category;
        private int imageCount;
        private String price;
        private String rating;
        private int reviewCount;
        private List<String> targetKeywords;
    }

    /**
     * Output of listing diagnosis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagnosisReport {
        private int overallScore;
        private String grade;
        private List<DimensionScore> dimensionScores;
        private List<String> criticalIssues;
        private List<String> quickWins;
        private List<String> keywordGaps;
        private List<String> competitiveGaps;
        private List<PriorityAction> priorityActions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScore {
        private String dimension;
        private int score;
        private String status; // PASS, WARN, FAIL
        private List<String> issues;
        private List<String> recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityAction {
        private String action;
        private String impact;   // HIGH, MEDIUM, LOW
        private String effort;   // HIGH, MEDIUM, LOW
        private String expectedImprovement;
    }
}
