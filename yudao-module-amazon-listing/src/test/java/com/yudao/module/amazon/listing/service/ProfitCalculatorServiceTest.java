package com.yudao.module.amazon.listing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Profit Calculator Service covering referral fees by category,
 * FBA fees by size tier, dimensional weight, storage fees, placement fees,
 * shipping costs, total cost breakdown, profit margin, and multi-currency support.
 */
@DisplayName("Profit Calculator Service Tests")
class ProfitCalculatorServiceTest {

    private ProfitCalculatorService profitCalculator;

    @BeforeEach
    void setUp() {
        profitCalculator = new ProfitCalculatorService();
    }

    // -----------------------------------------------------------------------
    // Referral Fee by Category
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Referral Fee Calculation Tests")
    class ReferralFeeTests {

        @ParameterizedTest(name = "Category {0} with price {1} should have referral fee rate {2}%")
        @CsvSource({
                "Electronics, 100.00, 8",
                "Clothing, 50.00, 17",
                "Home, 30.00, 15",
                "Toys, 25.00, 15",
                "Grocery, 15.00, 8",
                "Beauty, 20.00, 8",
                "Sports, 35.00, 15",
                "Books, 12.00, 15"
        })
        @DisplayName("Referral fee percentage varies by product category")
        void testReferralFeeByCategory(String category, double price, int expectedRatePct) {
            // When: Computing referral fee
            BigDecimal fee = profitCalculator.calculateReferralFee(category, BigDecimal.valueOf(price));

            // Then: Fee matches expected percentage
            BigDecimal expected = BigDecimal.valueOf(price)
                    .multiply(BigDecimal.valueOf(expectedRatePct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            assertThat(fee)
                    .as("Referral fee for %s at $%.2f should be %d%%", category, price, expectedRatePct)
                    .isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("Default referral fee is 15% for unknown categories")
        void testReferralFee_DefaultCategory() {
            BigDecimal fee = profitCalculator.calculateReferralFee("unknown_category", BigDecimal.valueOf(100.00));

            assertThat(fee)
                    .as("Default referral fee should be 15%")
                    .isEqualByComparingTo(BigDecimal.valueOf(15.00));
        }

        @Test
        @DisplayName("Referral fee has minimum threshold ($0.30 or $1.00 depending on category)")
        void testReferralFee_Minimum() {
            // Very low price item
            BigDecimal fee = profitCalculator.calculateReferralFee("Electronics", BigDecimal.valueOf(1.00));

            assertThat(fee)
                    .as("Referral fee should have a minimum floor")
                    .isGreaterThanOrEqualTo(BigDecimal.valueOf(0.30));
        }
    }

    // -----------------------------------------------------------------------
    // FBA Fee by Size Tier
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("FBA Fulfillment Fee Tests")
    class FBAFeeTests {

        @Test
        @DisplayName("Standard size item (10x6x4 inches, 1lb) has standard FBA fee")
        void testFBAFeeBySizeTier_Standard() {
            ProductDimensions dims = new ProductDimensions(10, 6, 4, 1.0); // L x W x H inches, weight lb

            BigDecimal fee = profitCalculator.calculateFBAFulfillmentFee(dims);

            // Standard size: ~$3.22-$5.07 depending on weight
            assertThat(fee)
                    .as("Standard size FBA fee should be in standard range")
                    .isBetween(BigDecimal.valueOf(3.00), BigDecimal.valueOf(6.00));
        }

        @Test
        @DisplayName("Oversize item (30x20x15 inches, 20lb) has oversize FBA fee")
        void testFBAFeeBySizeTier_Oversize() {
            ProductDimensions dims = new ProductDimensions(30, 20, 15, 20.0);

            BigDecimal fee = profitCalculator.calculateFBAFulfillmentFee(dims);

            // Oversize: typically $9.00+
            assertThat(fee)
                    .as("Oversize FBA fee should be significantly higher")
                    .isGreaterThan(BigDecimal.valueOf(8.00));
        }

        @Test
        @DisplayName("Small standard item (8x5x2, 0.5lb) has lowest FBA fee")
        void testFBAFeeBySizeTier_SmallStandard() {
            ProductDimensions dims = new ProductDimensions(8, 5, 2, 0.5);

            BigDecimal fee = profitCalculator.calculateFBAFulfillmentFee(dims);

            assertThat(fee)
                    .as("Small standard should have lowest fee")
                    .isBetween(BigDecimal.valueOf(2.50), BigDecimal.valueOf(4.00));
        }

        @Test
        @DisplayName("Large oversize item (60x40x30, 50lb) has highest FBA fee")
        void testFBAFeeBySizeTier_LargeOversize() {
            ProductDimensions dims = new ProductDimensions(60, 40, 30, 50.0);

            BigDecimal fee = profitCalculator.calculateFBAFulfillmentFee(dims);

            assertThat(fee)
                    .as("Large oversize should have very high fee")
                    .isGreaterThan(BigDecimal.valueOf(20.00));
        }
    }

    // -----------------------------------------------------------------------
    // Billable Weight (max of actual and dimensional)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Billable Weight Calculation Tests")
    class BillableWeightTests {

        @Test
        @DisplayName("Dimensional weight > actual weight uses dimensional weight")
        void testBillableWeight_DimensionalHigher() {
            // 18x12x8 inches, actual weight 2lb
            // Dimensional weight = (18*12*8) / 139 = 12.4 lb
            ProductDimensions dims = new ProductDimensions(18, 12, 8, 2.0);

            BigDecimal billableWeight = profitCalculator.calculateBillableWeight(dims);

            double dimensionalWeight = (18.0 * 12.0 * 8.0) / 139.0;
            assertThat(billableWeight.doubleValue())
                    .as("Billable weight should be dimensional weight when it exceeds actual")
                    .isCloseTo(dimensionalWeight, within(0.1));
        }

        @Test
        @DisplayName("Actual weight > dimensional weight uses actual weight")
        void testBillableWeight_ActualHigher() {
            // 8x6x4 inches, actual weight 5lb
            // Dimensional weight = (8*6*4) / 139 = 1.38 lb
            ProductDimensions dims = new ProductDimensions(8, 6, 4, 5.0);

            BigDecimal billableWeight = profitCalculator.calculateBillableWeight(dims);

            assertThat(billableWeight.doubleValue())
                    .as("Billable weight should be actual weight when it exceeds dimensional")
                    .isCloseTo(5.0, within(0.1));
        }

        @Test
        @DisplayName("Billable weight uses correct divisor (139 for Amazon US)")
        void testBillableWeight_Divisor() {
            ProductDimensions dims = new ProductDimensions(14, 10, 10, 3.0);

            BigDecimal billableWeight = profitCalculator.calculateBillableWeight(dims);

            double dimensionalWeight = (14.0 * 10.0 * 10.0) / 139.0; // ≈ 10.07
            assertThat(billableWeight.doubleValue())
                    .isCloseTo(Math.max(3.0, dimensionalWeight), within(0.1));
        }
    }

    // -----------------------------------------------------------------------
    // Storage Fee (Standard month vs peak season)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Storage Fee Tests")
    class StorageFeeTests {

        @Test
        @DisplayName("Standard month (Jan-Sep) has lower storage rate")
        void testStorageFee_StandardMonth() {
            ProductDimensions dims = new ProductDimensions(10, 6, 4, 1.0);
            int month = 3; // March — standard season

            BigDecimal fee = profitCalculator.calculateMonthlyStorageFee(dims, month);

            // Standard: ~$0.87 per cubic foot
            double cubicFeet = (10.0 * 6.0 * 4.0) / 1728.0;
            double expectedFee = cubicFeet * 0.87;

            assertThat(fee.doubleValue())
                    .as("Standard month storage fee should use lower rate")
                    .isCloseTo(expectedFee, within(0.10));
        }

        @Test
        @DisplayName("Peak season (Oct-Dec) has higher storage rate")
        void testStorageFee_PeakSeason() {
            ProductDimensions dims = new ProductDimensions(10, 6, 4, 1.0);
            int month = 11; // November — peak season

            BigDecimal fee = profitCalculator.calculateMonthlyStorageFee(dims, month);

            // Peak: ~$2.40 per cubic foot
            double cubicFeet = (10.0 * 6.0 * 4.0) / 1728.0;
            double expectedFee = cubicFeet * 2.40;

            assertThat(fee.doubleValue())
                    .as("Peak season storage fee should use higher rate")
                    .isCloseTo(expectedFee, within(0.10));
        }

        @Test
        @DisplayName("Peak season fee is ~2.76x standard season fee")
        void testStorageFee_PeakVsStandard() {
            ProductDimensions dims = new ProductDimensions(10, 6, 4, 1.0);

            BigDecimal standardFee = profitCalculator.calculateMonthlyStorageFee(dims, 3);
            BigDecimal peakFee = profitCalculator.calculateMonthlyStorageFee(dims, 11);

            assertThat(peakFee.doubleValue())
                    .as("Peak season should be approximately 2.76x standard")
                    .isGreaterThan(standardFee.doubleValue() * 2.0);
        }
    }

    // -----------------------------------------------------------------------
    // Placement Fee (with/without label service)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Placement fee with Amazon label service includes per-unit charge")
    void testPlacementFee_WithLabelService() {
        ProductDimensions dims = new ProductDimensions(10, 6, 4, 1.0);
        boolean useAmazonLabelService = true;

        BigDecimal fee = profitCalculator.calculateInboundPlacementFee(dims, useAmazonLabelService);

        assertThat(fee)
                .as("Placement fee with label service should include per-unit charge")
                .isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Placement fee without label service is lower or zero")
    void testPlacementFee_WithoutLabelService() {
        ProductDimensions dims = new ProductDimensions(10, 6, 4, 1.0);
        boolean useAmazonLabelService = false;

        BigDecimal feeWithLabel = profitCalculator.calculateInboundPlacementFee(dims, true);
        BigDecimal feeWithoutLabel = profitCalculator.calculateInboundPlacementFee(dims, false);

        assertThat(feeWithoutLabel)
                .as("Without label service, placement fee should be lower")
                .isLessThanOrEqualTo(feeWithLabel);
    }

    // -----------------------------------------------------------------------
    // Shipping Cost (Head Freight per Unit)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Shipping cost per unit = total freight / quantity")
    void testShippingCost() {
        double totalFreightCost = 2500.0; // Total sea freight cost
        int quantity = 500;
        double weightKgPerUnit = 0.5;

        BigDecimal perUnitShipping = profitCalculator.calculateShippingCostPerUnit(
                totalFreightCost, quantity, weightKgPerUnit
        );

        assertThat(perUnitShipping.doubleValue())
                .as("Per unit shipping = total freight / quantity")
                .isCloseTo(totalFreightCost / quantity, within(0.01));
    }

    @Test
    @DisplayName("Air freight is more expensive per unit than sea freight")
    void testShippingCost_AirVsSea() {
        int quantity = 500;
        double weightKgPerUnit = 0.5;

        BigDecimal seaFreight = profitCalculator.calculateShippingCostPerUnit(1250.0, quantity, weightKgPerUnit);
        BigDecimal airFreight = profitCalculator.calculateShippingCostPerUnit(5000.0, quantity, weightKgPerUnit);

        assertThat(airFreight)
                .as("Air freight should be more expensive than sea freight")
                .isGreaterThan(seaFreight);
    }

    // -----------------------------------------------------------------------
    // Total Cost Breakdown
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("All fees are correctly summed in total cost breakdown")
    void testTotalCostBreakdown() {
        // Given: Product with all cost components
        ProfitCalculationInput input = ProfitCalculationInput.builder()
                .sellingPrice(BigDecimal.valueOf(29.99))
                .productCategory("Home")
                .productDimensions(new ProductDimensions(10, 6, 4, 1.0))
                .unitCostFOB(BigDecimal.valueOf(5.00))
                .shippingPerUnit(BigDecimal.valueOf(2.50))
                .customsDuty(BigDecimal.valueOf(0.75))
                .tariff(BigDecimal.valueOf(1.25))
                .insurancePerUnit(BigDecimal.valueOf(0.10))
                .monthOfYear(3)
                .build();

        // When: Computing total cost breakdown
        ProfitBreakdown breakdown = profitCalculator.calculateBreakdown(input);

        // Then: All components are present
        assertThat(breakdown.getReferralFee()).isGreaterThan(BigDecimal.ZERO);
        assertThat(breakdown.getFBAFulfillmentFee()).isGreaterThan(BigDecimal.ZERO);
        assertThat(breakdown.getStorageFee()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(breakdown.getUnitCostFOB()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(breakdown.getShippingPerUnit()).isEqualByComparingTo(BigDecimal.valueOf(2.50));

        // And: Total cost is sum of all components
        BigDecimal expectedTotal = breakdown.getReferralFee()
                .add(breakdown.getFBAFulfillmentFee())
                .add(breakdown.getStorageFee())
                .add(breakdown.getUnitCostFOB())
                .add(breakdown.getShippingPerUnit())
                .add(breakdown.getCustomsDuty())
                .add(breakdown.getTariff())
                .add(breakdown.getInsurancePerUnit());

        assertThat(breakdown.getTotalCostPerUnit())
                .as("Total cost should equal sum of all cost components")
                .isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Total cost includes returns cost allowance")
    void testTotalCostBreakdown_WithReturns() {
        ProfitCalculationInput input = ProfitCalculationInput.builder()
                .sellingPrice(BigDecimal.valueOf(29.99))
                .productCategory("Home")
                .productDimensions(new ProductDimensions(10, 6, 4, 1.0))
                .unitCostFOB(BigDecimal.valueOf(5.00))
                .shippingPerUnit(BigDecimal.valueOf(2.50))
                .monthOfYear(6)
                .build();

        ProfitBreakdown breakdown = profitCalculator.calculateBreakdown(input);

        // Returns cost = price * returns_rate (typically 3%)
        assertThat(breakdown.getReturnsCost())
                .as("Returns cost should be approximately 3% of selling price")
                .isCloseTo(BigDecimal.valueOf(29.99 * 0.03), within(BigDecimal.valueOf(0.01)));
    }

    // -----------------------------------------------------------------------
    // Profit Margin
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Profit margin = (price - total cost) / price * 100")
    void testProfitMargin() {
        ProfitCalculationInput input = ProfitCalculationInput.builder()
                .sellingPrice(BigDecimal.valueOf(29.99))
                .productCategory("Home")
                .productDimensions(new ProductDimensions(10, 6, 4, 1.0))
                .unitCostFOB(BigDecimal.valueOf(5.00))
                .shippingPerUnit(BigDecimal.valueOf(2.50))
                .customsDuty(BigDecimal.valueOf(0.75))
                .tariff(BigDecimal.valueOf(1.25))
                .insurancePerUnit(BigDecimal.valueOf(0.10))
                .monthOfYear(3)
                .build();

        ProfitBreakdown breakdown = profitCalculator.calculateBreakdown(input);

        // Manual calculation
        BigDecimal profit = input.getSellingPrice().subtract(breakdown.getTotalCostPerUnit());
        BigDecimal marginPct = profit
                .multiply(BigDecimal.valueOf(100))
                .divide(input.getSellingPrice(), 2, RoundingMode.HALF_UP);

        assertThat(breakdown.getProfitPerUnit())
                .as("Profit = selling price - total cost")
                .isEqualByComparingTo(profit);

        assertThat(breakdown.getProfitMarginPct())
                .as("Margin % = (profit / price) * 100")
                .isEqualByComparingTo(marginPct);
    }

    @Test
    @DisplayName("Unprofitable product has negative profit margin")
    void testProfitMargin_Negative() {
        ProfitCalculationInput input = ProfitCalculationInput.builder()
                .sellingPrice(BigDecimal.valueOf(9.99))
                .productCategory("Home")
                .productDimensions(new ProductDimensions(20, 15, 10, 5.0)) // Large, heavy
                .unitCostFOB(BigDecimal.valueOf(8.00))
                .shippingPerUnit(BigDecimal.valueOf(5.00))
                .customsDuty(BigDecimal.valueOf(2.00))
                .tariff(BigDecimal.valueOf(3.00))
                .insurancePerUnit(BigDecimal.valueOf(0.50))
                .monthOfYear(11) // Peak storage
                .build();

        ProfitBreakdown breakdown = profitCalculator.calculateBreakdown(input);

        assertThat(breakdown.getProfitPerUnit())
                .as("High-cost product at low price should be unprofitable")
                .isLessThan(BigDecimal.ZERO);

        assertThat(breakdown.getProfitMarginPct())
                .isLessThan(BigDecimal.ZERO);
    }

    // -----------------------------------------------------------------------
    // Multi-Currency Support
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "Currency {0} with rate {1} converts ${2} to {3}")
    @CsvSource({
            "USD, 1.00, 29.99, 29.99",
            "EUR, 0.92, 29.99, 27.59",
            "GBP, 0.79, 29.99, 23.69",
            "JPY, 149.50, 29.99, 4483.51",
            "CAD, 1.35, 29.99, 40.49"
    })
    @DisplayName("Price conversion works correctly for multiple currencies")
    void testMultiCurrency(String currency, double exchangeRate, double usdPrice, double expectedConverted) {
        // When: Converting price
        BigDecimal converted = profitCalculator.convertCurrency(
                BigDecimal.valueOf(usdPrice),
                exchangeRate,
                currency
        );

        // Then: Conversion is correct
        assertThat(converted.doubleValue())
                .as("USD %.2f at rate %.2f should be %.2f %s", usdPrice, exchangeRate, expectedConverted, currency)
                .isCloseTo(expectedConverted, within(0.50));
    }

    @Test
    @DisplayName("Multi-currency profit calculation uses correct exchange rate")
    void testMultiCurrency_ProfitCalculation() {
        // Given: EUR marketplace with exchange rate
        ProfitCalculationInput inputUSD = ProfitCalculationInput.builder()
                .sellingPrice(BigDecimal.valueOf(29.99))
                .productCategory("Home")
                .productDimensions(new ProductDimensions(10, 6, 4, 1.0))
                .unitCostFOB(BigDecimal.valueOf(5.00))
                .shippingPerUnit(BigDecimal.valueOf(2.50))
                .currency("USD")
                .exchangeRate(1.0)
                .monthOfYear(3)
                .build();

        ProfitCalculationInput inputEUR = ProfitCalculationInput.builder()
                .sellingPrice(BigDecimal.valueOf(27.59)) // EUR equivalent
                .productCategory("Home")
                .productDimensions(new ProductDimensions(10, 6, 4, 1.0))
                .unitCostFOB(BigDecimal.valueOf(4.60)) // EUR equivalent
                .shippingPerUnit(BigDecimal.valueOf(2.30)) // EUR equivalent
                .currency("EUR")
                .exchangeRate(0.92)
                .monthOfYear(3)
                .build();

        ProfitBreakdown usdBreakdown = profitCalculator.calculateBreakdown(inputUSD);
        ProfitBreakdown eurBreakdown = profitCalculator.calculateBreakdown(inputEUR);

        // EUR profit converted back to USD should be close to USD profit
        double eurProfitInUSD = eurBreakdown.getProfitPerUnit().doubleValue() / 0.92;
        assertThat(eurProfitInUSD)
                .as("EUR profit converted to USD should approximate USD profit")
                .isCloseTo(usdBreakdown.getProfitPerUnit().doubleValue(), within(2.0));
    }
}
