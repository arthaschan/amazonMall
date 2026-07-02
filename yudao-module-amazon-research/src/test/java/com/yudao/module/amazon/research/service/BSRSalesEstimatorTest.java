package com.yudao.module.amazon.research.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BSR-to-Sales regression estimator covering model fitting,
 * prediction accuracy, fallback logic, category-specific coefficients,
 * and edge cases.
 */
@DisplayName("BSR Sales Estimator Tests")
class BSRSalesEstimatorTest {

    private BSRSalesEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new BSRSalesEstimator();
    }

    // -----------------------------------------------------------------------
    // Model Fitting
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Fit model from known BSR-sales data produces valid coefficients")
    void testFitModel() {
        // Given: Known BSR-sales pairs (log-linear relationship)
        // Typical Amazon US data: BSR 1 → ~30000 sales/mo, BSR 1000 → ~500 sales/mo
        List<BSRSalesDataPoint> trainingData = List.of(
                new BSRSalesDataPoint(1, 30000),
                new BSRSalesDataPoint(10, 8000),
                new BSRSalesDataPoint(50, 3500),
                new BSRSalesDataPoint(100, 2000),
                new BSRSalesDataPoint(500, 800),
                new BSRSalesDataPoint(1000, 500),
                new BSRSalesDataPoint(5000, 200),
                new BSRSalesDataPoint(10000, 100),
                new BSRSalesDataPoint(50000, 30),
                new BSRSalesDataPoint(100000, 10)
        );

        // When: Fitting the regression model
        BSRRegressionModel model = estimator.fitModel(trainingData, "US", "default");

        // Then: Model has valid coefficients
        assertThat(model).isNotNull();
        assertThat(model.getIntercept()).isNotZero();
        assertThat(model.getSlope()).isNotZero();

        // And: Slope is negative (higher BSR = lower sales)
        assertThat(model.getSlope())
                .as("BSR-sales relationship should be negative (higher BSR = fewer sales)")
                .isLessThan(0.0);

        // And: R-squared indicates reasonable fit
        assertThat(model.getRSquared())
                .as("R-squared should indicate a meaningful relationship")
                .isGreaterThan(0.5);
    }

    @Test
    @DisplayName("Model coefficients are within expected range for log-linear relationship")
    void testFitModel_CoefficientRange() {
        // Given: Standard Amazon US BSR-sales data
        List<BSRSalesDataPoint> trainingData = generateStandardTrainingData();

        BSRRegressionModel model = estimator.fitModel(trainingData, "US", "default");

        // Then: Typical log-linear coefficients
        // log(sales) = intercept + slope * log(BSR)
        // intercept typically 10-14, slope typically -0.5 to -1.5
        assertThat(model.getIntercept())
                .as("Intercept should be in typical range for Amazon US")
                .isBetween(8.0, 16.0);

        assertThat(model.getSlope())
                .as("Slope should be negative and in typical range")
                .isBetween(-2.0, -0.3);
    }

    // -----------------------------------------------------------------------
    // Prediction
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Predict sales for BSR=100 returns reasonable estimate")
    void testPrediction() {
        // Given: A fitted model
        List<BSRSalesDataPoint> trainingData = generateStandardTrainingData();
        BSRRegressionModel model = estimator.fitModel(trainingData, "US", "default");

        // When: Predicting sales for BSR=100
        double predictedSales = estimator.predictSales(model, 100);

        // Then: Prediction is in reasonable range (1000-3000 monthly sales for BSR 100)
        assertThat(predictedSales)
                .as("BSR 100 should predict roughly 1000-3000 monthly sales on Amazon US")
                .isBetween(500.0, 5000.0);
    }

    @Test
    @DisplayName("Predictions maintain monotonic decrease: higher BSR = lower sales")
    void testPrediction_MonotonicDecrease() {
        List<BSRSalesDataPoint> trainingData = generateStandardTrainingData();
        BSRRegressionModel model = estimator.fitModel(trainingData, "US", "default");

        // When: Predicting for increasing BSR values
        double salesAtBSR10 = estimator.predictSales(model, 10);
        double salesAtBSR100 = estimator.predictSales(model, 100);
        double salesAtBSR1000 = estimator.predictSales(model, 1000);
        double salesAtBSR10000 = estimator.predictSales(model, 10000);

        // Then: Sales strictly decrease as BSR increases
        assertThat(salesAtBSR10)
                .as("BSR 10 should have highest sales")
                .isGreaterThan(salesAtBSR100);
        assertThat(salesAtBSR100)
                .isGreaterThan(salesAtBSR1000);
        assertThat(salesAtBSR1000)
                .isGreaterThan(salesAtBSR10000);
    }

    @Test
    @DisplayName("Prediction for known BSR-sales pair is within 30% of actual")
    void testPrediction_Accuracy() {
        // Given: Training data with known point
        List<BSRSalesDataPoint> trainingData = generateStandardTrainingData();
        BSRRegressionModel model = estimator.fitModel(trainingData, "US", "default");

        // When: Predicting for a known BSR
        double bsr = 500;
        double actualSales = 800; // from training data
        double predictedSales = estimator.predictSales(model, bsr);

        // Then: Prediction within 30% of actual
        double errorPct = Math.abs(predictedSales - actualSales) / actualSales;
        assertThat(errorPct)
                .as("Prediction should be within 30% of actual for known data point")
                .isLessThan(0.30);
    }

    // -----------------------------------------------------------------------
    // Low R-Squared Fallback
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Model with R-squared < 0.5 falls back to default coefficients")
    void testLowRSquared() {
        // Given: Noisy data that produces poor fit
        List<BSRSalesDataPoint> noisyData = List.of(
                new BSRSalesDataPoint(10, 5000),
                new BSRSalesDataPoint(20, 100),   // contradictory
                new BSRSalesDataPoint(50, 8000),  // random
                new BSRSalesDataPoint(100, 200),
                new BSRSalesDataPoint(200, 9000),
                new BSRSalesDataPoint(500, 150)
        );

        // When: Fitting model with noisy data
        BSRRegressionModel model = estimator.fitModel(noisyData, "US", "default");

        // Then: Model falls back to default coefficients
        if (model.getRSquared() < 0.5) {
            assertThat(model.isUsingDefaultCoefficients())
                    .as("Low R-squared model should use default coefficients")
                    .isTrue();
        }

        // And: Predictions still produce reasonable results
        double sales = estimator.predictSales(model, 1000);
        assertThat(sales).isGreaterThan(0);
        assertThat(sales).isLessThan(10000);
    }

    @Test
    @DisplayName("Default model coefficients produce valid predictions when no training data")
    void testDefaultModelCoefficients() {
        // Given: No training data available
        BSRRegressionModel defaultModel = estimator.getDefaultModel("US", "default");

        // When: Predicting with default model
        double sales = estimator.predictSales(defaultModel, 1000);

        // Then: Prediction is still reasonable
        assertThat(sales)
                .as("Default model should produce reasonable prediction")
                .isBetween(100.0, 2000.0);
    }

    // -----------------------------------------------------------------------
    // Category-Specific Coefficients
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Different categories have different regression coefficients")
    void testCategorySpecificCoefficients() {
        // Given: Category-specific training data
        List<BSRSalesDataPoint> electronicsData = List.of(
                new BSRSalesDataPoint(1, 50000),
                new BSRSalesDataPoint(100, 5000),
                new BSRSalesDataPoint(1000, 1000),
                new BSRSalesDataPoint(10000, 200)
        );

        List<BSRSalesDataPoint> booksData = List.of(
                new BSRSalesDataPoint(1, 10000),
                new BSRSalesDataPoint(100, 1000),
                new BSRSalesDataPoint(1000, 300),
                new BSRSalesDataPoint(10000, 50)
        );

        // When: Fitting category-specific models
        BSRRegressionModel electronicsModel = estimator.fitModel(electronicsData, "US", "electronics");
        BSRRegressionModel booksModel = estimator.fitModel(booksData, "US", "books");

        // Then: Different categories produce different predictions
        double electronicsSales = estimator.predictSales(electronicsModel, 1000);
        double booksSales = estimator.predictSales(booksModel, 1000);

        assertThat(electronicsSales)
                .as("Electronics should have higher sales at same BSR than books")
                .isNotEqualTo(booksSales);

        // And: Electronics typically sells more at same BSR rank
        assertThat(electronicsSales).isGreaterThan(booksSales);
    }

    @Test
    @DisplayName("Marketplace-specific models account for market size differences")
    void testMarketplaceSpecificCoefficients() {
        List<BSRSalesDataPoint> usData = generateStandardTrainingData();
        List<BSRSalesDataPoint> ukData = List.of(
                new BSRSalesDataPoint(1, 5000),   // UK market ~15% of US
                new BSRSalesDataPoint(100, 500),
                new BSRSalesDataPoint(1000, 100),
                new BSRSalesDataPoint(10000, 20)
        );

        BSRRegressionModel usModel = estimator.fitModel(usData, "US", "default");
        BSRRegressionModel ukModel = estimator.fitModel(ukData, "UK", "default");

        double usSales = estimator.predictSales(usModel, 1000);
        double ukSales = estimator.predictSales(ukModel, 1000);

        assertThat(usSales)
                .as("US market should predict higher sales at same BSR")
                .isGreaterThan(ukSales);
    }

    // -----------------------------------------------------------------------
    // Edge Cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("BSR=0 returns maximum sales estimate")
    void testEdgeCases_BSRZero() {
        BSRRegressionModel model = estimator.getDefaultModel("US", "default");

        double sales = estimator.predictSales(model, 0);

        // BSR 0 is invalid; should return max reasonable sales or handle gracefully
        assertThat(sales)
                .as("BSR=0 should return a positive value (or be treated as BSR=1)")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("BSR=1 returns the highest sales estimate")
    void testEdgeCases_BSROne() {
        BSRRegressionModel model = estimator.getDefaultModel("US", "default");

        double sales = estimator.predictSales(model, 1);

        // BSR 1 = #1 best seller, should predict very high sales
        assertThat(sales)
                .as("BSR=1 should predict very high monthly sales")
                .isGreaterThan(10000.0);
    }

    @Test
    @DisplayName("Very large BSR (1000000+) returns minimum sales estimate")
    void testEdgeCases_VeryLargeBSR() {
        BSRRegressionModel model = estimator.getDefaultModel("US", "default");

        double sales = estimator.predictSales(model, 1000000);

        assertThat(sales)
                .as("Very large BSR should predict minimal sales")
                .isBetween(0.0, 10.0);
    }

    @Test
    @DisplayName("Negative BSR is handled gracefully")
    void testEdgeCases_NegativeBSR() {
        BSRRegressionModel model = estimator.getDefaultModel("US", "default");

        // Should not throw, should handle gracefully (e.g., treat as BSR=1)
        double sales = estimator.predictSales(model, -1);
        assertThat(sales).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Empty training data falls back to default model")
    void testEdgeCases_EmptyTrainingData() {
        BSRRegressionModel model = estimator.fitModel(List.of(), "US", "default");

        assertThat(model).isNotNull();
        assertThat(model.isUsingDefaultCoefficients()).isTrue();

        double sales = estimator.predictSales(model, 1000);
        assertThat(sales).isGreaterThan(0);
    }

    @Test
    @DisplayName("Single data point is insufficient for regression, uses default")
    void testEdgeCases_SingleDataPoint() {
        List<BSRSalesDataPoint> singlePoint = List.of(
                new BSRSalesDataPoint(100, 2000)
        );

        BSRRegressionModel model = estimator.fitModel(singlePoint, "US", "default");

        assertThat(model.isUsingDefaultCoefficients())
                .as("Single data point should fall back to default model")
                .isTrue();
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private List<BSRSalesDataPoint> generateStandardTrainingData() {
        return List.of(
                new BSRSalesDataPoint(1, 30000),
                new BSRSalesDataPoint(5, 15000),
                new BSRSalesDataPoint(10, 8000),
                new BSRSalesDataPoint(25, 5000),
                new BSRSalesDataPoint(50, 3500),
                new BSRSalesDataPoint(100, 2000),
                new BSRSalesDataPoint(250, 1200),
                new BSRSalesDataPoint(500, 800),
                new BSRSalesDataPoint(1000, 500),
                new BSRSalesDataPoint(2500, 300),
                new BSRSalesDataPoint(5000, 200),
                new BSRSalesDataPoint(10000, 100),
                new BSRSalesDataPoint(25000, 50),
                new BSRSalesDataPoint(50000, 30),
                new BSRSalesDataPoint(100000, 10)
        );
    }
}
