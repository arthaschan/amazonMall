package com.yudao.module.amazon.research.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Financial Projection Service covering startup costs, revenue projections,
 * advertising decay models, cash flow with payout delays, break-even calculations,
 * scenario analysis, and brand valuation.
 *
 * Ported from amazon-omniscient/backend/app/services/financial_report.py
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Financial Projection Service Tests")
class FinancialProjectionServiceTest {

    @Mock
    private SupplierCostService supplierCostService;

    @Mock
    private FBAFeeCalculator fbaFeeCalculator;

    private FinancialProjectionService projectionService;

    // Constants matching the Python implementation
    private static final double REFERRAL_FEE_PCT = 0.15;
    private static final double RETURNS_RATE = 0.03;
    private static final double VINE_ENROLLMENT_FEE = 200.0;
    private static final int AMAZON_PAYOUT_DELAY_DAYS = 14;

    @BeforeEach
    void setUp() {
        projectionService = new FinancialProjectionService(supplierCostService, fbaFeeCalculator);
    }

    // -----------------------------------------------------------------------
    // Startup Cost Calculation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Startup Cost Calculation Tests")
    class StartupCostTests {

        @Test
        @DisplayName("All cost items are included in startup capital calculation")
        void testStartupCostCalculation() {
            // Given: Product with known costs
            FinancialProjectionInput input = FinancialProjectionInput.builder()
                    .sellingPrice(29.99)
                    .unitCostFOB(5.00)
                    .orderQuantity(500)
                    .weightKgPerUnit(0.5)
                    .launchPPCDaily(30.0)
                    .vineUnits(30)
                    .photographyBudget(500.0)
                    .aPlusDesignBudget(300.0)
                    .couponPct(0.10)
                    .couponBudgetUnits(100)
                    .build();

            SupplierLandedCost landedCost = new SupplierLandedCost(
                    5.00,   // unit cost
                    2.50,   // shipping per unit
                    0.75,   // customs duty
                    1.25,   // section 301 tariff
                    0.10,   // insurance
                    0.05,   // inspection
                    0.30,   // freight forwarding
                    0.50,   // FBA prep
                    0.40,   // FBA inbound
                    10.85   // total landed cost per unit to Amazon
            );

            when(supplierCostService.calculateLandedCost(any())).thenReturn(landedCost);

            // When: Computing startup capital
            StartupCapitalResult result = projectionService.calculateStartupCapital(input, landedCost);

            // Then: All cost components are included
            assertThat(result.getInventoryCost())
                    .as("Inventory cost = landed cost * quantity")
                    .isEqualTo(10.85 * 500);

            assertThat(result.getPpc90Day())
                    .as("90 days of PPC at daily rate")
                    .isEqualTo(30.0 * 90);

            assertThat(result.getCouponBudget())
                    .as("Coupon budget = price * discount * units")
                    .isEqualTo(29.99 * 0.10 * 100);

            assertThat(result.getVineCost())
                    .as("Vine = enrollment fee + unit cost * vine units")
                    .isGreaterThan(VINE_ENROLLMENT_FEE);

            assertThat(result.getPhotographyBudget()).isEqualTo(500.0);
            assertThat(result.getaPlusDesignBudget()).isEqualTo(300.0);

            // And: Total includes buffer
            assertThat(result.getTotalWithBuffer())
                    .as("Total with 15% buffer")
                    .isGreaterThan(result.getTotalLaunchCapital());

            double expectedBuffer = result.getTotalLaunchCapital() * 0.15;
            assertThat(result.getRecommendedBuffer()).isCloseTo(expectedBuffer, within(0.01));
        }

        @Test
        @DisplayName("Startup capital includes samples and miscellaneous costs")
        void testStartupCostCalculation_WithFixedCosts() {
            FinancialProjectionInput input = FinancialProjectionInput.builder()
                    .sellingPrice(25.00)
                    .unitCostFOB(4.00)
                    .orderQuantity(300)
                    .launchPPCDaily(20.0)
                    .vineUnits(0)
                    .photographyBudget(400.0)
                    .aPlusDesignBudget(200.0)
                    .build();

            SupplierLandedCost landedCost = new SupplierLandedCost(
                    4.00, 2.00, 0.60, 1.00, 0.08, 0.04, 0.25, 0.40, 0.35, 8.72
            );
            when(supplierCostService.calculateLandedCost(any())).thenReturn(landedCost);

            StartupCapitalResult result = projectionService.calculateStartupCapital(input, landedCost);

            // Fixed costs
            assertThat(result.getSamplesCost()).isEqualTo(150.0);
            assertThat(result.getMiscCost()).isEqualTo(200.0);

            // Total should be sum of all components
            double expectedTotal = result.getInventoryCost()
                    + result.getPpc90Day()
                    + result.getCouponBudget()
                    + result.getVineCost()
                    + result.getPhotographyBudget()
                    + result.getaPlusDesignBudget()
                    + result.getSamplesCost()
                    + result.getMiscCost();

            assertThat(result.getTotalLaunchCapital())
                    .isCloseTo(expectedTotal, within(0.01));
        }
    }

    // -----------------------------------------------------------------------
    // Weekly Revenue Projection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Weekly revenue = daily sales * 7 * price with sales ramp applied")
    void testWeeklyRevenueProjection() {
        // Given: 200 monthly sales, $29.99 price
        int monthlySales = 200;
        double price = 29.99;
        double dailySalesAvg = monthlySales / 30.0;

        // Sales ramp: month 1 = 60% of target
        double[] salesRamp = {0.60, 0.75, 0.85, 0.90, 0.95, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00};

        // When: Computing week 1 revenue (month 1 ramp = 0.60)
        double week1Units = dailySalesAvg * 7 * salesRamp[0];
        double week1Revenue = week1Units * price;

        // Then: Week 1 revenue matches calculation
        double expectedWeek1Units = (200.0 / 30.0) * 7 * 0.60;
        assertThat(week1Units).isCloseTo(expectedWeek1Units, within(0.1));
        assertThat(week1Revenue).isCloseTo(expectedWeek1Units * 29.99, within(1.0));

        // And: Week 26 (month 7, ramp = 1.0) has full revenue
        double week26Units = dailySalesAvg * 7 * salesRamp[6]; // month index 6 = ramp 1.0
        double week26Revenue = week26Units * price;
        assertThat(week26Revenue).isGreaterThan(week1Revenue);
    }

    // -----------------------------------------------------------------------
    // Ad Decay Model (ACOS decreases over 52 weeks)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PPC spend tapers from 100% in month 1 to 45% in month 12")
    void testAdDecayModel() {
        // PPC taper matches the Python implementation
        double[] ppcTaper = {1.00, 0.95, 0.90, 0.85, 0.80, 0.75,
                             0.70, 0.65, 0.60, 0.55, 0.50, 0.45};

        double dailyPPCBudget = 30.0;

        // Month 1: full PPC spend
        double month1WeeklyPPC = dailyPPCBudget * 7 * ppcTaper[0];
        assertThat(month1WeeklyPPC).isEqualTo(210.0);

        // Month 6: 75% of original
        double month6WeeklyPPC = dailyPPCBudget * 7 * ppcTaper[5];
        assertThat(month6WeeklyPPC).isEqualTo(157.5);

        // Month 12: 45% of original
        double month12WeeklyPPC = dailyPPCBudget * 7 * ppcTaper[11];
        assertThat(month12WeeklyPPC).isEqualTo(94.5);

        // Verify taper is strictly decreasing
        for (int i = 1; i < ppcTaper.length; i++) {
            assertThat(ppcTaper[i])
                    .as("PPC taper month %d should be less than month %d", i + 1, i)
                    .isLessThan(ppcTaper[i - 1]);
        }
    }

    @Test
    @DisplayName("Total PPC spend over 12 months is less than 12x initial monthly spend")
    void testAdDecayModel_TotalSavings() {
        double dailyPPCBudget = 30.0;
        double[] ppcTaper = {1.00, 0.95, 0.90, 0.85, 0.80, 0.75,
                             0.70, 0.65, 0.60, 0.55, 0.50, 0.45};

        double totalYearPPC = 0;
        for (double taper : ppcTaper) {
            totalYearPPC += dailyPPCBudget * 30 * taper;
        }

        double fullYearWithoutTaper = dailyPPCBudget * 365;

        assertThat(totalYearPPC)
                .as("Tapered PPC should be less than constant full-year spend")
                .isLessThan(fullYearWithoutTaper);

        // Expected total: 30 * 30 * sum(taper) ≈ 30 * 30 * 8.75 = 7875
        assertThat(totalYearPPC).isBetween(7000.0, 9000.0);
    }

    // -----------------------------------------------------------------------
    // Cash Flow with Amazon Payout Delay
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("14-day Amazon payout delay shifts revenue by 2 weeks in cash flow")
    void testCashFlowWithPayoutDelay() {
        // Given: Product starts selling in week 1
        double price = 29.99;
        double referralFee = price * REFERRAL_FEE_PCT;
        double fulfillmentFee = 3.50;
        double netRevenuePerUnit = price - referralFee - fulfillmentFee;

        double weeklyUnits = 46.0; // ~200 monthly / 4.33
        double week1SalesRevenue = weeklyUnits * netRevenuePerUnit;

        // When: Building cash flow timeline
        CashFlowTimeline timeline = projectionService.buildCashFlowTimeline(
                price, weeklyUnits, netRevenuePerUnit, AMAZON_PAYOUT_DELAY_DAYS
        );

        // Then: Week 1 has sales but no revenue (14-day hold)
        CashFlowEntry week1 = timeline.getEntry(1);
        assertThat(week1.getCashIn())
                .as("Week 1 should have no cash in due to 14-day Amazon hold")
                .isEqualTo(0.0);

        // And: Week 3 receives payout from week 1 sales (2-week delay)
        CashFlowEntry week3 = timeline.getEntry(3);
        assertThat(week3.getCashIn())
                .as("Week 3 should receive payout from week 1 sales")
                .isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Cash flow correctly tracks cumulative balance through pre-launch costs")
    void testCashFlow_PreLaunchCosts() {
        // Given: Pre-launch timeline with supplier payments
        double inventoryCost = 5425.0; // 500 units * $10.85 landed
        double depositPct = 0.30;
        double balancePct = 0.70;

        CashFlowTimeline timeline = projectionService.buildFullCashFlow(
                FinancialProjectionInput.builder()
                        .sellingPrice(29.99)
                        .orderQuantity(500)
                        .supplierLeadTimeWeeks(4)
                        .shippingTransitWeeks(4)
                        .amazonInboundWeeks(1)
                        .launchPPCDaily(30.0)
                        .build(),
                new SupplierLandedCost(5.00, 2.50, 0.75, 1.25, 0.10, 0.05, 0.30, 0.50, 0.40, 10.85)
        );

        // Then: First entry is supplier deposit at negative week
        CashFlowEntry depositEntry = timeline.getFirstEntry();
        assertThat(depositEntry.getCashOut())
                .as("Supplier deposit should be 30% of inventory cost")
                .isCloseTo(inventoryCost * depositPct, within(1.0));

        // And: Balance goes deeply negative before revenue starts
        assertThat(timeline.getMinBalance())
                .as("Balance should be most negative during pre-launch phase")
                .isLessThan(-inventoryCost * 0.5);
    }

    // -----------------------------------------------------------------------
    // Break-Even Calculation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Break-even week is when cumulative profit turns positive")
    void testBreakEvenCalculation() {
        // Given: Product with known economics
        double sellingPrice = 29.99;
        double totalLandedCost = 10.85;
        double amazonFees = sellingPrice * (REFERRAL_FEE_PCT + RETURNS_RATE) + 3.50 + 0.10;
        double ppcCostPerUnit = 1.50 / 0.12; // CPC / CR = $12.50
        double profitPerUnit = sellingPrice - totalLandedCost - amazonFees - ppcCostPerUnit;
        double totalInvestment = 12000.0; // total startup capital with buffer

        // When: Computing break-even
        BreakEvenResult result = projectionService.calculateBreakEven(
                profitPerUnit, totalInvestment, 200, // monthly sales
                new double[]{0.60, 0.75, 0.85, 0.90, 0.95, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00},
                new double[]{1.00, 0.95, 0.90, 0.85, 0.80, 0.75, 0.70, 0.65, 0.60, 0.55, 0.50, 0.45},
                30.0 // daily PPC
        );

        // Then: Break-even week is identified
        assertThat(result.getBreakEvenWeek())
                .as("Break-even should occur within 52 weeks for profitable product")
                .isBetween(1, 52);

        // And: Cumulative profit at break-even is non-negative
        assertThat(result.getCumulativeProfitAtBreakEven())
                .isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Unprofitable product has null break-even week")
    void testBreakEvenCalculation_Unprofitable() {
        // Given: Product with negative unit economics
        double negativeProfitPerUnit = -2.50;
        double totalInvestment = 10000.0;

        BreakEvenResult result = projectionService.calculateBreakEven(
                negativeProfitPerUnit, totalInvestment, 200,
                new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
                new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
                30.0
        );

        assertThat(result.getBreakEvenWeek())
                .as("Unprofitable product should never break even")
                .isNull();
    }

    // -----------------------------------------------------------------------
    // Three Scenarios (Bull / Base / Bear)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Bull/Base/Bear scenarios produce distinct financial outcomes")
    void testThreeScenarios() {
        // Given: Base product metrics
        ScenarioInput scenarioInput = ScenarioInput.builder()
                .sellingPrice(29.99)
                .estimatedMonthlySales(200)
                .totalLandedCost(10.85)
                .referralFee(4.50)
                .fbaFee(3.50)
                .storageFeePerUnit(0.10)
                .launchPPCDaily(30.0)
                .launchCapitalTotal(12000.0)
                .build();

        // When: Computing scenarios
        ScenarioResults results = projectionService.computeScenarios(scenarioInput);

        // Then: Bull scenario (1.3x) > Base (1.0x) > Bear (0.7x)
        ScenarioResult bull = results.getBull();
        ScenarioResult base = results.getBase();
        ScenarioResult bear = results.getBear();

        assertThat(bull.getAnnualRevenue())
                .as("Bull scenario should have highest revenue")
                .isGreaterThan(base.getAnnualRevenue());

        assertThat(base.getAnnualRevenue())
                .as("Base scenario should have higher revenue than bear")
                .isGreaterThan(bear.getAnnualRevenue());

        assertThat(bull.getAnnualProfit())
                .as("Bull scenario should have highest profit")
                .isGreaterThan(base.getAnnualProfit());

        assertThat(base.getAnnualProfit())
                .isGreaterThan(bear.getAnnualProfit());

        // And: ROI follows same pattern
        assertThat(bull.getRoiPct())
                .isGreaterThan(base.getRoiPct());

        // And: Bear scenario break-even is later
        if (bear.getBreakEvenWeek() != null && base.getBreakEvenWeek() != null) {
            assertThat(bear.getBreakEvenWeek())
                    .as("Bear scenario should break even later than base")
                    .isGreaterThanOrEqualTo(base.getBreakEvenWeek());
        }
    }

    @Test
    @DisplayName("Scenario multipliers are correctly applied: 1.3x / 1.0x / 0.7x")
    void testThreeScenarios_Multipliers() {
        ScenarioInput input = ScenarioInput.builder()
                .sellingPrice(30.00)
                .estimatedMonthlySales(100)
                .totalLandedCost(10.00)
                .referralFee(4.50)
                .fbaFee(3.50)
                .storageFeePerUnit(0.10)
                .launchPPCDaily(20.0)
                .launchCapitalTotal(8000.0)
                .build();

        ScenarioResults results = projectionService.computeScenarios(input);

        // Steady-state monthly units at ramp=1.0
        double bullMonthlyUnits = 100 * 1.3;
        double baseMonthlyUnits = 100 * 1.0;
        double bearMonthlyUnits = 100 * 0.7;

        assertThat(bull.getAnnualUnits())
                .isCloseTo(Math.round(bullMonthlyUnits * 12 * 0.93), within(bullMonthlyUnits * 2));
        assertThat(base.getAnnualUnits())
                .isCloseTo(Math.round(baseMonthlyUnits * 12 * 0.93), within(baseMonthlyUnits * 2));
    }

    // -----------------------------------------------------------------------
    // Brand Valuation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Brand value = monthly profit * multiplier (24-36x typical)")
    void testBrandValuation() {
        // Given: Steady-state monthly profit
        double monthlyProfit = 3500.0;
        int multiplierLow = 24;
        int multiplierHigh = 36;

        // When: Computing brand valuation
        BrandValuation valuation = projectionService.calculateBrandValuation(monthlyProfit, multiplierLow, multiplierHigh);

        // Then: Valuation range is correct
        assertThat(valuation.getLowEstimate())
                .as("Low estimate = monthly profit * 24")
                .isEqualTo(monthlyProfit * multiplierLow);

        assertThat(valuation.getHighEstimate())
                .as("High estimate = monthly profit * 36")
                .isEqualTo(monthlyProfit * multiplierHigh);

        assertThat(valuation.getMidEstimate())
                .as("Mid estimate = monthly profit * 30")
                .isEqualTo(monthlyProfit * 30);

        // And: Annual revenue basis
        assertThat(valuation.getAnnualRevenue())
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Brand valuation with zero profit returns zero")
    void testBrandValuation_ZeroProfit() {
        BrandValuation valuation = projectionService.calculateBrandValuation(0.0, 24, 36);

        assertThat(valuation.getLowEstimate()).isEqualTo(0.0);
        assertThat(valuation.getHighEstimate()).isEqualTo(0.0);
        assertThat(valuation.getMidEstimate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Brand valuation with negative profit returns zero or negative")
    void testBrandValuation_NegativeProfit() {
        BrandValuation valuation = projectionService.calculateBrandValuation(-500.0, 24, 36);

        assertThat(valuation.getLowEstimate())
                .as("Negative profit should produce zero or negative valuation")
                .isLessThanOrEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // Monthly P&L Summary
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Monthly P&L applies sales ramp and PPC taper correctly")
    void testMonthlyPLSummary() {
        // Given: Product parameters
        double sellingPrice = 29.99;
        int monthlySales = 200;
        double landedCost = 10.85;
        double dailyPPC = 30.0;
        int orderQty = 500;

        double[] salesRamp = {0.60, 0.75, 0.85, 0.90, 0.95, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00};
        double[] ppcTaper = {1.00, 0.95, 0.90, 0.85, 0.80, 0.75, 0.70, 0.65, 0.60, 0.55, 0.50, 0.45};

        // When: Computing 12-month P&L
        List<MonthlyPnLEntry> monthlyPL = projectionService.buildMonthlySummary(
                sellingPrice, monthlySales, landedCost, dailyPPC, orderQty,
                salesRamp, ppcTaper
        );

        // Then: 12 months of data
        assertThat(monthlyPL).hasSize(12);

        // Month 1 has lower revenue (60% ramp)
        MonthlyPnLEntry month1 = monthlyPL.get(0);
        MonthlyPnLEntry month6 = monthlyPL.get(5);

        assertThat(month1.getRevenue())
                .as("Month 1 revenue should be less than month 6 (ramp effect)")
                .isLessThan(month6.getRevenue());

        // Month 1 has higher PPC spend (100% taper)
        assertThat(month1.getPpcSpend())
                .as("Month 1 PPC should be higher than month 12 (taper effect)")
                .isGreaterThan(monthlyPL.get(11).getPpcSpend());

        // Inventory decreases over time
        assertThat(month1.getInventoryRemaining())
                .isGreaterThanOrEqualTo(monthlyPL.get(11).getInventoryRemaining());
    }

    // -----------------------------------------------------------------------
    // Reorder Planning
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Reorder trigger = lead time days * daily sales")
    void testReorderPlanning() {
        int monthlySales = 200;
        int orderQuantity = 500;
        int leadTimeWeeks = 9; // 4 supplier + 4 shipping + 1 FBA inbound

        ReorderPlan plan = projectionService.buildReorderPlan(monthlySales, orderQuantity, leadTimeWeeks, 10.85);

        double dailySales = monthlySales / 30.0;
        int leadTimeDays = leadTimeWeeks * 7;
        int expectedReorderTrigger = (int) Math.round(leadTimeDays * dailySales);

        assertThat(plan.getReorderTriggerUnits())
                .as("Reorder trigger should be lead_time_days * daily_sales")
                .isEqualTo(expectedReorderTrigger);

        assertThat(plan.getDaysOfInventory())
                .as("Days of inventory = order qty / daily sales")
                .isCloseTo((int) Math.round(orderQuantity / dailySales), within(1.0));
    }

    // -----------------------------------------------------------------------
    // Edge Cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Zero monthly sales produces valid financial report without division errors")
    void testEdgeCases_ZeroSales() {
        ScenarioInput input = ScenarioInput.builder()
                .sellingPrice(29.99)
                .estimatedMonthlySales(0)
                .totalLandedCost(10.85)
                .referralFee(4.50)
                .fbaFee(3.50)
                .storageFeePerUnit(0.10)
                .launchPPCDaily(0.0)
                .launchCapitalTotal(5000.0)
                .build();

        ScenarioResults results = projectionService.computeScenarios(input);

        assertThat(results.getBase().getAnnualRevenue()).isEqualTo(0.0);
        assertThat(results.getBase().getAnnualProfit()).isLessThanOrEqualTo(0.0);
    }
}
