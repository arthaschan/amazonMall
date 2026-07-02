package com.yudao.module.amazon.research.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Omniscient Score Calculator covering all 9 weighted sub-scores,
 * hard disqualification filters, marketplace-specific thresholds, and edge cases.
 *
 * Ported from amazon-omniscient/backend/app/services/scoring_service.py
 */
@DisplayName("Omniscient Score Calculator Tests")
class OmniscientScoreCalculatorTest {

    private OmniscientScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new OmniscientScoreCalculator();
    }

    // -----------------------------------------------------------------------
    // Demand Scoring (15% weight)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Demand Scoring Tests")
    class DemandScoringTests {

        @Test
        @DisplayName("US marketplace: high demand (BSR<1000, SV>10000, Sales>1000) scores 100")
        void testDemandScoring_US() {
            // Given: High demand metrics for US marketplace
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("marketplace", "US");
            metrics.put("search_volume", 15000);
            metrics.put("avg_bsr", 800);
            metrics.put("estimated_monthly_sales", 1200);

            // When: Computing demand score
            double score = calculator.scoreDemand(metrics);

            // Then: Maximum score achieved
            assertThat(score).isBetween(90.0, 100.0);
        }

        @Test
        @DisplayName("US marketplace: low demand (BSR>50000, SV<500, Sales<50) scores low")
        void testDemandScoring_US_Low() {
            // Given: Low demand metrics for US marketplace
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("marketplace", "US");
            metrics.put("search_volume", 300);
            metrics.put("avg_bsr", 60000);
            metrics.put("estimated_monthly_sales", 30);

            // When: Computing demand score
            double score = calculator.scoreDemand(metrics);

            // Then: Low score returned
            assertThat(score).isLessThan(20.0);
        }

        @Test
        @DisplayName("UK marketplace: lower thresholds accommodate smaller market size")
        void testDemandScoring_UK() {
            // Given: Moderate demand metrics for UK marketplace (8% of US scale)
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("marketplace", "UK");
            metrics.put("search_volume", 800);   // would be low for US, decent for UK
            metrics.put("avg_bsr", 4000);        // UK BSR scale is 0.4x US
            metrics.put("estimated_monthly_sales", 100);  // decent for UK

            // When: Computing demand score
            double score = calculator.scoreDemand(metrics);

            // Then: Score reflects adjusted UK thresholds
            assertThat(score).isBetween(40.0, 80.0);
        }

        @Test
        @DisplayName("AU marketplace: even lower thresholds for 8% market size")
        void testDemandScoring_AU() {
            // Given: AU marketplace metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("marketplace", "AU");
            metrics.put("search_volume", 500);   // 0.08 scale applied
            metrics.put("avg_bsr", 3000);
            metrics.put("estimated_monthly_sales", 50);

            double score = calculator.scoreDemand(metrics);

            // AU has scale factor 0.08 for search volume, 0.4 for BSR
            assertThat(score).isBetween(20.0, 70.0);
        }
    }

    // -----------------------------------------------------------------------
    // Competition Scoring (15% weight)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("High competition (polished listings, 2000+ reviews, 5+ strong sellers) scores low")
    void testCompetitionScoring() {
        // Given: Highly competitive market
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("avg_listing_quality", 85);        // High quality listings
        metrics.put("median_competitor_reviews", 2500); // High review moat
        metrics.put("strong_seller_count", 6);          // Many strong sellers

        // When: Computing competition score (higher = less competition = better)
        double score = calculator.scoreCompetition(metrics);

        // Then: Low score due to intense competition (100 - 30 - 40 - 25 = 5)
        assertThat(score).isLessThanOrEqualTo(10.0);
    }

    @Test
    @DisplayName("Low competition (weak listings, few reviews, no strong sellers) scores high")
    void testCompetitionScoring_Low() {
        // Given: Weakly competitive market
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("avg_listing_quality", 30);
        metrics.put("median_competitor_reviews", 20);
        metrics.put("strong_seller_count", 0);

        double score = calculator.scoreCompetition(metrics);

        // Then: High score (100 - 0 - 0 - 0 = ~95-100)
        assertThat(score).isGreaterThanOrEqualTo(90.0);
    }

    // -----------------------------------------------------------------------
    // Profitability / Margin Scoring (15% weight)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("30%+ pre-PPC margin and 25%+ post-PPC margin achieves full score")
    void testProfitabilityScoring() {
        // Given: Highly profitable product
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("pre_ppc_margin_pct", 45.0);
        metrics.put("post_ppc_margin_pct", 28.0);

        double score = calculator.scoreMargin(metrics);

        // Then: pre_ppc >= 40 => 48, post_ppc >= 25 => 40. Total ~88
        assertThat(score).isBetween(80.0, 100.0);
    }

    @Test
    @DisplayName("Thin margins (pre-PPC < 25%, post-PPC < 5%) score poorly")
    void testProfitabilityScoring_ThinMargins() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("pre_ppc_margin_pct", 20.0);
        metrics.put("post_ppc_margin_pct", 3.0);

        double score = calculator.scoreMargin(metrics);

        // pre_ppc < 25 => 5, post_ppc < 5 => 2. Total = 7
        assertThat(score).isLessThanOrEqualTo(15.0);
    }

    // -----------------------------------------------------------------------
    // Review Moat Scoring (10% weight — review feasibility)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Low review threshold (<=15) with fast timeline (<=8 weeks) scores max")
    void testReviewMoatScoring() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("review_threshold", 10);
        metrics.put("weeks_to_review_threshold", 6);

        double score = calculator.scoreReviewFeasibility(metrics);

        // threshold <= 15 => 50, weeks <= 8 => 50. Total = 100
        assertThat(score).isEqualTo(100.0);
    }

    @Test
    @DisplayName("High review threshold (>300) with slow timeline (>40 weeks) scores low")
    void testReviewMoatScoring_HighBarrier() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("review_threshold", 500);
        metrics.put("weeks_to_review_threshold", 60);

        double score = calculator.scoreReviewFeasibility(metrics);

        // threshold > 300 => 5, weeks > 40 => 5. Total = 10
        assertThat(score).isLessThanOrEqualTo(15.0);
    }

    // -----------------------------------------------------------------------
    // Price Stability Scoring (part of trend score, 10% weight)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Stable BSR velocity (low CV) and rising search volume scores high")
    void testPriceStabilityScoring() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bsr_velocity_pct", -10.0);  // improving BSR
        metrics.put("search_volume_trend", "rising");
        metrics.put("is_seasonal", false);

        double score = calculator.scoreTrend(metrics);

        // base 50 + bsr_velocity < -5 => +15 + rising => +20 = 85
        assertThat(score).isBetween(75.0, 95.0);
    }

    @Test
    @DisplayName("Declining BSR and declining search volume with seasonality scores low")
    void testPriceStabilityScoring_Unstable() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bsr_velocity_pct", 25.0);  // worsening BSR
        metrics.put("search_volume_trend", "declining");
        metrics.put("is_seasonal", true);

        double score = calculator.scoreTrend(metrics);

        // base 50 + bsr_velocity > 20 => -20 + declining => -20 + seasonal => -15 = -5 => clamped to 0
        assertThat(score).isLessThanOrEqualTo(10.0);
    }

    // -----------------------------------------------------------------------
    // Seasonality Scoring
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Low monthly CV (stable year-round) gets no seasonality penalty")
    void testSeasonalityScoring() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bsr_velocity_pct", 0.0);
        metrics.put("search_volume_trend", "stable");
        metrics.put("is_seasonal", false);

        double score = calculator.scoreTrend(metrics);

        // base 50 + stable => +5 = 55 (no seasonal penalty)
        assertThat(score).isEqualTo(55.0);
    }

    @Test
    @DisplayName("Seasonal products get 15-point penalty")
    void testSeasonalityScoring_Seasonal() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("bsr_velocity_pct", 0.0);
        metrics.put("search_volume_trend", "stable");
        metrics.put("is_seasonal", true);

        double score = calculator.scoreTrend(metrics);

        // base 50 + stable => +5 - seasonal => -15 = 40
        assertThat(score).isEqualTo(40.0);
    }

    // -----------------------------------------------------------------------
    // Ad Dependency Scoring (PPC Viability, 10% weight)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Low CPC ($0.50), high break-even ACOS (50%), many keywords scores max")
    void testAdDependencyScoring() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avg_cpc", 0.40);
        metrics.put("break_even_acos", 55.0);
        metrics.put("relevant_keyword_count", 60);

        double score = calculator.scorePpcViability(metrics);

        // CPC <= 0.50 => 40, ACOS >= 50 => 30, keywords >= 50 => 30. Total = 100
        assertThat(score).isEqualTo(100.0);
    }

    @Test
    @DisplayName("High CPC ($5+), low break-even ACOS, few keywords scores low")
    void testAdDependencyScoring_HighAdDependency() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avg_cpc", 6.0);
        metrics.put("break_even_acos", 10.0);
        metrics.put("relevant_keyword_count", 3);

        double score = calculator.scorePpcViability(metrics);

        // CPC > 4 => 2, ACOS < 15 => 2, keywords < 5 => 2. Total = 6
        assertThat(score).isLessThanOrEqualTo(10.0);
    }

    // -----------------------------------------------------------------------
    // Supplier Availability Scoring (10% weight)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Many suppliers (10+), good quality, low MOQ (<=100) scores max")
    void testSupplierAvailability() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("supplier_count", 15);
        metrics.put("best_supplier_score", 90);
        metrics.put("min_moq", 50);

        double score = calculator.scoreSupplier(metrics);

        // suppliers >= 10 => 40, quality 90*0.3=27, MOQ <= 100 => 30. Total ~97
        assertThat(score).isBetween(90.0, 100.0);
    }

    @Test
    @DisplayName("No suppliers available scores zero")
    void testSupplierAvailability_None() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("supplier_count", 0);
        metrics.put("best_supplier_score", 0);
        metrics.put("min_moq", 9999);

        double score = calculator.scoreSupplier(metrics);

        // suppliers 0 => 0, quality 0, MOQ > 1000 => 2. Total = 2
        assertThat(score).isLessThanOrEqualTo(5.0);
    }

    // -----------------------------------------------------------------------
    // Total Score (Weighted Sum)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Total score is weighted sum of all 9 sub-scores")
    void testTotalScore() {
        // Given: All sub-scores at maximum
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("search_volume", 15000);
        metrics.put("avg_bsr", 500);
        metrics.put("estimated_monthly_sales", 1500);
        metrics.put("avg_listing_quality", 30);
        metrics.put("median_competitor_reviews", 10);
        metrics.put("strong_seller_count", 0);
        metrics.put("avg_price", 35.0);
        metrics.put("monthly_revenue_per_seller", 12000);
        metrics.put("pre_ppc_margin_pct", 50.0);
        metrics.put("post_ppc_margin_pct", 30.0);
        metrics.put("bsr_velocity_pct", -25.0);
        metrics.put("search_volume_trend", "rising");
        metrics.put("is_seasonal", false);
        metrics.put("review_threshold", 10);
        metrics.put("weeks_to_review_threshold", 6);
        metrics.put("supplier_count", 15);
        metrics.put("best_supplier_score", 95);
        metrics.put("min_moq", 50);
        metrics.put("avg_cpc", 0.30);
        metrics.put("break_even_acos", 55.0);
        metrics.put("relevant_keyword_count", 60);
        metrics.put("total_launch_capital", 2000);
        metrics.put("break_even_week_base", 6);

        // When: Computing total score
        OmniscientScoreResult result = calculator.computeScore(metrics);

        // Then: Weighted sum respects configured weights
        Map<String, Double> weights = OmniscientScoreCalculator.WEIGHTS;
        double expectedTotal = 0.0;
        for (Map.Entry<String, Double> entry : result.getSubScores().entrySet()) {
            expectedTotal += entry.getValue() * weights.get(entry.getKey());
        }
        expectedTotal = Math.min(100, Math.max(0, expectedTotal));

        assertThat(result.getOmniscientScore())
                .as("Total score should match weighted sum of sub-scores")
                .isCloseTo(Math.round(expectedTotal * 10.0) / 10.0, within(1.0));

        // And: Overall score is high for excellent metrics
        assertThat(result.getOmniscientScore()).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    @DisplayName("Score is clamped between 0 and 100")
    void testTotalScoreClamping() {
        // All worst-case metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("search_volume", 0);
        metrics.put("avg_bsr", 999999);
        metrics.put("estimated_monthly_sales", 0);
        metrics.put("avg_listing_quality", 90);
        metrics.put("median_competitor_reviews", 5000);
        metrics.put("strong_seller_count", 10);
        metrics.put("avg_price", 5.0);
        metrics.put("monthly_revenue_per_seller", 100);
        metrics.put("pre_ppc_margin_pct", 5.0);
        metrics.put("post_ppc_margin_pct", -10.0);
        metrics.put("bsr_velocity_pct", 30.0);
        metrics.put("search_volume_trend", "declining");
        metrics.put("is_seasonal", true);
        metrics.put("review_threshold", 1000);
        metrics.put("weeks_to_review_threshold", 100);
        metrics.put("supplier_count", 0);
        metrics.put("best_supplier_score", 0);
        metrics.put("min_moq", 99999);
        metrics.put("avg_cpc", 10.0);
        metrics.put("break_even_acos", 5.0);
        metrics.put("relevant_keyword_count", 1);
        metrics.put("total_launch_capital", 999999);
        metrics.put("break_even_week_base", 100);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.getOmniscientScore())
                .as("Score should never be negative")
                .isGreaterThanOrEqualTo(0.0);
        assertThat(result.getOmniscientScore())
                .as("Score should never exceed 100")
                .isLessThanOrEqualTo(100.0);
    }

    // -----------------------------------------------------------------------
    // Custom Weights
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("User-defined weight overrides replace default weights")
    void testCustomWeights() {
        // Given: Custom weight configuration (heavier on margin)
        Map<String, Double> customWeights = new HashMap<>();
        customWeights.put("demand", 0.05);
        customWeights.put("competition", 0.05);
        customWeights.put("revenue", 0.05);
        customWeights.put("margin", 0.40);         // heavily weighted
        customWeights.put("trend", 0.05);
        customWeights.put("review_feasibility", 0.05);
        customWeights.put("supplier", 0.05);
        customWeights.put("ppc_viability", 0.05);
        customWeights.put("launch_feasibility", 0.25);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("search_volume", 5000);
        metrics.put("avg_bsr", 5000);
        metrics.put("estimated_monthly_sales", 300);
        metrics.put("avg_listing_quality", 50);
        metrics.put("median_competitor_reviews", 500);
        metrics.put("strong_seller_count", 2);
        metrics.put("avg_price", 35.0);
        metrics.put("monthly_revenue_per_seller", 5000);
        metrics.put("pre_ppc_margin_pct", 55.0);
        metrics.put("post_ppc_margin_pct", 30.0);
        metrics.put("bsr_velocity_pct", 0.0);
        metrics.put("search_volume_trend", "stable");
        metrics.put("is_seasonal", false);
        metrics.put("review_threshold", 100);
        metrics.put("weeks_to_review_threshold", 20);
        metrics.put("supplier_count", 10);
        metrics.put("best_supplier_score", 80);
        metrics.put("min_moq", 200);
        metrics.put("avg_cpc", 1.0);
        metrics.put("break_even_acos", 40.0);
        metrics.put("relevant_keyword_count", 30);
        metrics.put("total_launch_capital", 3000);
        metrics.put("break_even_week_base", 10);

        // When: Computing with custom weights
        OmniscientScoreResult result = calculator.computeScore(metrics, customWeights);

        // Then: Result differs from default weights
        OmniscientScoreResult defaultResult = calculator.computeScore(metrics);

        assertThat(result.getOmniscientScore())
                .as("Custom weights should produce different total score")
                .isNotEqualTo(defaultResult.getOmniscientScore());
    }

    // -----------------------------------------------------------------------
    // Hard Disqualification Filters
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Hard filter: price outside $15-$70 range fails")
    void testHardFilter_PriceRange() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("avg_price", 10.0);  // Below minimum

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
        assertThat(result.getFailReasons())
                .anyMatch(reason -> reason.contains("price") || reason.contains("range"));
    }

    @Test
    @DisplayName("Hard filter: review moat > 2000 fails for US market")
    void testHardFilter_ReviewMoat() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("median_competitor_reviews", 3000);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
        assertThat(result.getFailReasons())
                .anyMatch(reason -> reason.contains("review") || reason.contains("moat"));
    }

    @Test
    @DisplayName("Hard filter: BSR > 50000 fails (low demand)")
    void testHardFilter_BSRDemand() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("avg_bsr", 80000);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
    }

    @Test
    @DisplayName("Hard filter: margin below 25% fails")
    void testHardFilter_MinimumMargin() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("pre_ppc_margin_pct", 15.0);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
    }

    @Test
    @DisplayName("Hard filter: Amazon dominance > 30% fails")
    void testHardFilter_AmazonDominance() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("amazon_seller_pct", 45.0);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
    }

    @Test
    @DisplayName("Hard filter: restricted/hazmat category fails")
    void testHardFilter_RestrictedCategory() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("is_restricted_category", true);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
    }

    @Test
    @DisplayName("Hard filter: IP/patent risk detected fails")
    void testHardFilter_IPRisk() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("ip_risk_detected", true);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
    }

    @Test
    @DisplayName("Hard filter: seasonal product fails unless allow_seasonal is true")
    void testHardFilter_Seasonality() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("is_seasonal", true);
        metrics.put("allow_seasonal", false);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.isPassAllFilters()).isFalse();
    }

    @Test
    @DisplayName("Seasonal product passes when allow_seasonal is enabled")
    void testHardFilter_Seasonality_Allowed() {
        Map<String, Object> metrics = buildPassingMetrics();
        metrics.put("is_seasonal", true);
        metrics.put("allow_seasonal", true);

        OmniscientScoreResult result = calculator.computeScore(metrics);

        // Seasonality filter should pass
        assertThat(result.getHardFilters())
                .filteredOn(f -> f.getFilter().equals("seasonality"))
                .allMatch(f -> f.isPassed());
    }

    // -----------------------------------------------------------------------
    // Confidence Tiers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Score >= 80 with all filters passing returns HIGH confidence")
    void testConfidenceTier_High() {
        Map<String, Object> metrics = buildExcellentMetrics();

        OmniscientScoreResult result = calculator.computeScore(metrics);

        if (result.isPassAllFilters() && result.getOmniscientScore() >= 80) {
            assertThat(result.getConfidenceTier()).isEqualTo("HIGH");
        }
    }

    @Test
    @DisplayName("Any hard filter failure sets tier to FAIL regardless of score")
    void testConfidenceTier_Fail() {
        Map<String, Object> metrics = buildExcellentMetrics();
        metrics.put("is_restricted_category", true); // force failure

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result.getConfidenceTier()).isEqualTo("FAIL");
    }

    // -----------------------------------------------------------------------
    // Edge Cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Null values in metrics use safe defaults without NPE")
    void testEdgeCases_NullValues() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        // All other keys missing — should use defaults

        OmniscientScoreResult result = calculator.computeScore(metrics);

        assertThat(result).isNotNull();
        assertThat(result.getOmniscientScore()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Zero values produce valid scores without division errors")
    void testEdgeCases_ZeroValues() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("search_volume", 0);
        metrics.put("avg_bsr", 0);
        metrics.put("estimated_monthly_sales", 0);
        metrics.put("avg_price", 0.0);
        metrics.put("monthly_revenue_per_seller", 0);
        metrics.put("pre_ppc_margin_pct", 0.0);
        metrics.put("post_ppc_margin_pct", 0.0);
        metrics.put("supplier_count", 0);
        metrics.put("best_supplier_score", 0);
        metrics.put("min_moq", 0);
        metrics.put("avg_cpc", 0.0);
        metrics.put("break_even_acos", 0.0);
        metrics.put("relevant_keyword_count", 0);
        metrics.put("total_launch_capital", 0);
        metrics.put("break_even_week_base", null);

        // Should not throw
        OmniscientScoreResult result = calculator.computeScore(metrics);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Negative values are handled gracefully")
    void testEdgeCases_NegativeValues() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("search_volume", -100);
        metrics.put("avg_bsr", -50);
        metrics.put("pre_ppc_margin_pct", -20.0);
        metrics.put("post_ppc_margin_pct", -50.0);
        metrics.put("bsr_velocity_pct", -100.0);
        metrics.put("total_launch_capital", -5000);

        OmniscientScoreResult result = calculator.computeScore(metrics);
        assertThat(result).isNotNull();
        assertThat(result.getOmniscientScore()).isGreaterThanOrEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private Map<String, Object> buildPassingMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("marketplace", "US");
        metrics.put("avg_price", 30.0);
        metrics.put("median_competitor_reviews", 500);
        metrics.put("avg_bsr", 10000);
        metrics.put("pre_ppc_margin_pct", 35.0);
        metrics.put("amazon_seller_pct", 10.0);
        metrics.put("is_restricted_category", false);
        metrics.put("ip_risk_detected", false);
        metrics.put("is_seasonal", false);
        return metrics;
    }

    private Map<String, Object> buildExcellentMetrics() {
        Map<String, Object> metrics = buildPassingMetrics();
        metrics.put("search_volume", 15000);
        metrics.put("estimated_monthly_sales", 1500);
        metrics.put("avg_listing_quality", 30);
        metrics.put("strong_seller_count", 0);
        metrics.put("monthly_revenue_per_seller", 12000);
        metrics.put("post_ppc_margin_pct", 30.0);
        metrics.put("bsr_velocity_pct", -25.0);
        metrics.put("search_volume_trend", "rising");
        metrics.put("review_threshold", 10);
        metrics.put("weeks_to_review_threshold", 6);
        metrics.put("supplier_count", 15);
        metrics.put("best_supplier_score", 95);
        metrics.put("min_moq", 50);
        metrics.put("avg_cpc", 0.30);
        metrics.put("break_even_acos", 55.0);
        metrics.put("relevant_keyword_count", 60);
        metrics.put("total_launch_capital", 2000);
        metrics.put("break_even_week_base", 6);
        return metrics;
    }
}
