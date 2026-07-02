package cn.iocoder.yudao.module.amazon.ai.research.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTOs for the AI Research module (Product Blueprint & Recommendation Engine).
 *
 * @author AmazonOps AI
 */
public class ResearchDtos {

    // -----------------------------------------------------------------------
    // Product Blueprint
    // -----------------------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetail {
        private String asin;
        private String title;
        private String category;
        private Double price;
        private Double rating;
        private Integer reviewCount;
        private Long bsr;
        private String brand;
        private List<String> features;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierInfo {
        private String supplierName;
        private String location;
        private Double minOrderQuantity;
        private Double unitPriceRange;
        private String manufacturingCapabilities;
        private Integer leadTimeWeeks;
        private String certifications;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductBlueprint {
        private ProductConcept productConcept;
        private TargetEconomics targetEconomics;
        private ManufacturingSpecs manufacturingSpecs;
        private BomCost bomCost;
        private List<QualityCheckpoint> qualityCheckpoints;
        private List<Risk> risks;
        private List<String> supplierTalkingPoints;
        private Integer launchTimelineEstimateWeeks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductConcept {
        private TargetCustomer targetCustomer;
        private String valueProposition;
        private List<String> keyDifferentiators;
        private String competitiveAdvantage;
        private String positioningStatement;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetCustomer {
        private String primaryPersona;
        private String demographics;
        private String psychographics;
        private String purchaseContext;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetEconomics {
        private Double targetPrice;
        private Double targetCogs;
        private FbaFeeBreakdown fbaFeesBreakdown;
        private Double targetGrossMarginPct;
        private Integer breakEvenUnitsPerMonth;
        private Double ppcBudgetMonthlyEstimate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FbaFeeBreakdown {
        private Double referralFee;
        private Double fulfillmentFee;
        private Double storageFeeMonthly;
        private Double inboundPlacementFee;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManufacturingSpecs {
        private List<MaterialSpec> materials;
        private String assemblyComplexity;
        private Integer estimatedAssemblyTimeMinutes;
        private List<String> criticalToleranceRequirements;
        private Integer manufacturingLeadTimeWeeks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialSpec {
        private String component;
        private String material;
        private String specifications;
        private String finish;
        private String supplierType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomCost {
        private List<BomComponent> components;
        private Double toolingCost;
        private Double packagingCost;
        private Double qcTestingCostPerBatch;
        private Double totalLandedCost1000Moq;
        private Double totalLandedCost3000Moq;
        private Double totalLandedCost5000Moq;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomComponent {
        private String component;
        private String material;
        private Double costPerUnitAt1000Moq;
        private Double costPerUnitAt3000Moq;
        private Double costPerUnitAt5000Moq;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityCheckpoint {
        private String stage;
        private String checkpoint;
        private String method;
        private String passCriteria;
        private String sampleSize;
        private Double costPerCheck;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Risk {
        private String risk;
        private String category;
        private String probability;
        private String impact;
        private String mitigation;
        private Double mitigationCost;
    }

    // -----------------------------------------------------------------------
    // Niche / Recommendation
    // -----------------------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Niche {
        private String keyword;
        private Double avgPrice;
        private Integer avgMonthlySales;
        private Double avgRating;
        private Integer avgReviewCount;
        private Integer numberOfCompetitors;
        private Double estimatedRevenue;
        private Double estimatedProfit;
        private String competitionLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String nicheKeyword;
        private Double overallScore;
        private String recommendation;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> actionItems;
        private String riskLevel;
        private Double estimatedMonthlyProfit;
        private Integer estimatedStartupCost;
        private String timeToLaunch;
    }
}
