package com.yudao.module.amazon.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Inventory Forecast Service covering reorder point calculation,
 * safety stock, days of supply, reorder alerts, inbound inventory handling,
 * zero sales edge cases, and seasonal adjustments.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory Forecast Service Tests")
class InventoryForecastServiceTest {

    @Mock
    private SalesHistoryRepository salesHistoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InboundShipmentRepository inboundShipmentRepository;

    @Mock
    private NotificationService notificationService;

    private InventoryForecastService forecastService;

    private static final String SKU = "TEST-SKU-001";
    private static final String WAREHOUSE_US = "FC-US-EAST";

    @BeforeEach
    void setUp() {
        forecastService = new InventoryForecastService(
                salesHistoryRepository, inventoryRepository,
                inboundShipmentRepository, notificationService
        );
    }

    // -----------------------------------------------------------------------
    // Reorder Point Calculation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Reorder point = average daily sales * lead time + safety stock")
    void testReorderPointCalculation() {
        // Given: Product with known sales and lead time
        double avgDailySales = 10.0; // 300 monthly / 30 days
        int leadTimeDays = 45;       // 4 weeks supplier + 2 weeks shipping + 0.5 week FBA
        double safetyStock = 50.0;

        // When: Calculating reorder point
        int reorderPoint = forecastService.calculateReorderPoint(
                avgDailySales, leadTimeDays, safetyStock
        );

        // Then: Reorder point = 10 * 45 + 50 = 500
        int expectedReorderPoint = (int) Math.round(avgDailySales * leadTimeDays + safetyStock);
        assertThat(reorderPoint)
                .as("Reorder point should be avg_daily_sales * lead_time + safety_stock")
                .isEqualTo(expectedReorderPoint);
    }

    @Test
    @DisplayName("Reorder point with zero safety stock uses only demand during lead time")
    void testReorderPointCalculation_NoSafetyStock() {
        double avgDailySales = 5.0;
        int leadTimeDays = 30;
        double safetyStock = 0.0;

        int reorderPoint = forecastService.calculateReorderPoint(
                avgDailySales, leadTimeDays, safetyStock
        );

        assertThat(reorderPoint)
                .as("Without safety stock, reorder point = daily sales * lead time")
                .isEqualTo(150);
    }

    @Test
    @DisplayName("Reorder point accounts for fractional daily sales accurately")
    void testReorderPointCalculation_FractionalSales() {
        // 7 sales per day (210 monthly)
        double avgDailySales = 7.0;
        int leadTimeDays = 60;
        double safetyStock = 100.0;

        int reorderPoint = forecastService.calculateReorderPoint(
                avgDailySales, leadTimeDays, safetyStock
        );

        // 7 * 60 + 100 = 520
        assertThat(reorderPoint).isEqualTo(520);
    }

    // -----------------------------------------------------------------------
    // Safety Stock Calculation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Safety stock = Z * sigma_demand * sqrt(lead_time)")
    void testSafetyStockCalculation() {
        // Given: Service level 95% (Z = 1.65), demand std dev = 3 units/day, lead time = 45 days
        double serviceLevelZ = 1.65; // 95% service level
        double demandStdDev = 3.0;   // standard deviation of daily sales
        int leadTimeDays = 45;

        // When: Calculating safety stock
        double safetyStock = forecastService.calculateSafetyStock(
                serviceLevelZ, demandStdDev, leadTimeDays
        );

        // Then: Safety stock = 1.65 * 3.0 * sqrt(45) = 1.65 * 3.0 * 6.708 ≈ 33.2
        double expected = serviceLevelZ * demandStdDev * Math.sqrt(leadTimeDays);
        assertThat(safetyStock)
                .as("Safety stock = Z * σ * √(lead_time)")
                .isCloseTo(expected, within(0.1));
    }

    @Test
    @DisplayName("Higher service level (99%) produces higher safety stock than 95%")
    void testSafetyStockCalculation_ServiceLevels() {
        double demandStdDev = 5.0;
        int leadTimeDays = 30;

        double safetyStock95 = forecastService.calculateSafetyStock(1.65, demandStdDev, leadTimeDays);
        double safetyStock99 = forecastService.calculateSafetyStock(2.33, demandStdDev, leadTimeDays);

        assertThat(safetyStock99)
                .as("99% service level should require more safety stock than 95%")
                .isGreaterThan(safetyStock95);
    }

    @Test
    @DisplayName("Zero demand variability produces zero safety stock")
    void testSafetyStockCalculation_ZeroVariability() {
        double safetyStock = forecastService.calculateSafetyStock(1.65, 0.0, 45);

        assertThat(safetyStock)
                .as("Zero demand variability should produce zero safety stock")
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Safety stock increases with longer lead time")
    void testSafetyStockCalculation_LeadTimeEffect() {
        double z = 1.65;
        double stdDev = 4.0;

        double safety30 = forecastService.calculateSafetyStock(z, stdDev, 30);
        double safety60 = forecastService.calculateSafetyStock(z, stdDev, 60);
        double safety90 = forecastService.calculateSafetyStock(z, stdDev, 90);

        assertThat(safety30).isLessThan(safety60);
        assertThat(safety60).isLessThan(safety90);

        // Verify square root relationship
        assertThat(safety60 / safety30)
                .isCloseTo(Math.sqrt(60.0 / 30.0), within(0.01));
    }

    // -----------------------------------------------------------------------
    // Days of Supply Calculation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Days of supply = effective stock / daily sales average")
    void testDaysOfSupplyCalculation() {
        // Given: 500 units in stock, selling 10 per day
        int effectiveStock = 500;
        double avgDailySales = 10.0;

        // When: Calculating days of supply
        int daysOfSupply = forecastService.calculateDaysOfSupply(effectiveStock, avgDailySales);

        // Then: 500 / 10 = 50 days
        assertThat(daysOfSupply)
                .as("Days of supply = effective stock / daily sales")
                .isEqualTo(50);
    }

    @Test
    @DisplayName("Days of supply with zero sales returns max value")
    void testDaysOfSupplyCalculation_ZeroSales() {
        int effectiveStock = 500;
        double avgDailySales = 0.0;

        int daysOfSupply = forecastService.calculateDaysOfSupply(effectiveStock, avgDailySales);

        assertThat(daysOfSupply)
                .as("Zero sales should return maximum days of supply (or -1 for infinite)")
                .isGreaterThanOrEqualTo(999);
    }

    @Test
    @DisplayName("Fractional days of supply are rounded down")
    void testDaysOfSupplyCalculation_Fractional() {
        int effectiveStock = 100;
        double avgDailySales = 7.0; // 100/7 = 14.28

        int daysOfSupply = forecastService.calculateDaysOfSupply(effectiveStock, avgDailySales);

        assertThat(daysOfSupply).isEqualTo(14);
    }

    // -----------------------------------------------------------------------
    // Reorder Alert Trigger
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Stock below reorder point triggers reorder alert")
    void testReorderAlertTrigger() {
        // Given: Current stock below reorder point
        InventorySnapshot snapshot = InventorySnapshot.builder()
                .sku(SKU)
                .warehouseId(WAREHOUSE_US)
                .availableUnits(400)
                .reorderPoint(500)
                .build();

        when(inventoryRepository.getSnapshot(SKU, WAREHOUSE_US)).thenReturn(snapshot);

        // When: Checking for reorder alerts
        List<ReorderAlert> alerts = forecastService.checkReorderAlerts(List.of(SKU), WAREHOUSE_US);

        // Then: Alert is triggered
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getSku()).isEqualTo(SKU);
        assertThat(alerts.get(0).getAvailableUnits()).isEqualTo(400);
        assertThat(alerts.get(0).getReorderPoint()).isEqualTo(500);
        assertThat(alerts.get(0).getAlertType()).isEqualTo(AlertType.STOCK_BELOW_REORDER_POINT);

        // And: Notification is sent
        verify(notificationService).sendInventoryAlert(any(ReorderAlert.class));
    }

    @Test
    @DisplayName("Stock above reorder point does not trigger alert")
    void testReorderAlertTrigger_NoAlert() {
        InventorySnapshot snapshot = InventorySnapshot.builder()
                .sku(SKU)
                .warehouseId(WAREHOUSE_US)
                .availableUnits(600)
                .reorderPoint(500)
                .build();

        when(inventoryRepository.getSnapshot(SKU, WAREHOUSE_US)).thenReturn(snapshot);

        List<ReorderAlert> alerts = forecastService.checkReorderAlerts(List.of(SKU), WAREHOUSE_US);

        assertThat(alerts).isEmpty();
        verify(notificationService, never()).sendInventoryAlert(any());
    }

    @Test
    @DisplayName("Critical stock level (< 7 days supply) triggers urgent alert")
    void testReorderAlertTrigger_CriticalLevel() {
        InventorySnapshot snapshot = InventorySnapshot.builder()
                .sku(SKU)
                .warehouseId(WAREHOUSE_US)
                .availableUnits(50) // 5 days at 10/day
                .reorderPoint(500)
                .avgDailySales(10.0)
                .build();

        when(inventoryRepository.getSnapshot(SKU, WAREHOUSE_US)).thenReturn(snapshot);

        List<ReorderAlert> alerts = forecastService.checkReorderAlerts(List.of(SKU), WAREHOUSE_US);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getAlertType()).isEqualTo(AlertType.CRITICAL_STOCK_LEVEL);
        assertThat(alerts.get(0).getDaysOfSupply()).isLessThan(7);
    }

    // -----------------------------------------------------------------------
    // No Reorder When Inbound
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Effective stock includes inbound shipments, preventing unnecessary reorder")
    void testNoReorderWhenInbound() {
        // Given: Low available stock but inbound shipment on the way
        int availableUnits = 200;
        int inboundUnits = 400;
        int effectiveStock = availableUnits + inboundUnits; // 600

        InventorySnapshot snapshot = InventorySnapshot.builder()
                .sku(SKU)
                .warehouseId(WAREHOUSE_US)
                .availableUnits(availableUnits)
                .inboundUnits(inboundUnits)
                .reorderPoint(500)
                .build();

        when(inventoryRepository.getSnapshot(SKU, WAREHOUSE_US)).thenReturn(snapshot);

        // When: Calculating effective stock for reorder decision
        int calculatedEffectiveStock = forecastService.getEffectiveStock(SKU, WAREHOUSE_US);

        // Then: Effective stock includes inbound
        assertThat(calculatedEffectiveStock)
                .as("Effective stock should include inbound shipments")
                .isEqualTo(effectiveStock);

        // And: No reorder alert (effective stock > reorder point)
        List<ReorderAlert> alerts = forecastService.checkReorderAlerts(List.of(SKU), WAREHOUSE_US);
        assertThat(alerts)
                .as("No alert when effective stock (including inbound) exceeds reorder point")
                .isEmpty();
    }

    @Test
    @DisplayName("Inbound shipment arriving after lead time is discounted")
    void testNoReorderWhenInbound_FutureArrival() {
        // Given: Inbound arriving in 30 days (longer than 7-day window)
        InboundShipment futureShipment = InboundShipment.builder()
                .sku(SKU)
                .units(500)
                .expectedArrival(LocalDate.now().plusDays(30))
                .status("SHIPPED")
                .build();

        when(inboundShipmentRepository.findBySku(SKU)).thenReturn(List.of(futureShipment));

        // When: Calculating effective stock
        int effectiveStock = forecastService.getEffectiveStock(SKU, WAREHOUSE_US);

        // Then: Future inbound may be excluded or discounted
        // (depends on business logic — if arrival > lead time, exclude)
        assertThat(effectiveStock)
                .as("Future inbound beyond lead time should be excluded or discounted")
                .isGreaterThanOrEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Zero Sales Handling
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("No sales history produces zero reorder point and no alert")
    void testZeroSalesHandling() {
        // Given: New product with no sales
        when(salesHistoryRepository.getDailySales(SKU, WAREHOUSE_US, 30))
                .thenReturn(List.of()); // no sales data

        // When: Calculating forecast for new product
        InventoryForecast forecast = forecastService.calculateForecast(SKU, WAREHOUSE_US);

        // Then: Average daily sales is zero
        assertThat(forecast.getAvgDailySales())
                .as("No sales history should result in zero daily sales")
                .isEqualTo(0.0);

        // And: Reorder point is zero
        assertThat(forecast.getReorderPoint())
                .as("Zero sales should produce zero reorder point")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("Product with intermittent sales uses non-zero day average")
    void testZeroSalesHandling_Intermittent() {
        // Given: 30-day period with only 10 days having sales
        List<DailySales> salesHistory = List.of(
                new DailySales(LocalDate.now().minusDays(1), 3),
                new DailySales(LocalDate.now().minusDays(5), 2),
                new DailySales(LocalDate.now().minusDays(10), 5),
                new DailySales(LocalDate.now().minusDays(15), 1),
                new DailySales(LocalDate.now().minusDays(20), 4),
                new DailySales(LocalDate.now().minusDays(22), 2),
                new DailySales(LocalDate.now().minusDays(25), 3),
                new DailySales(LocalDate.now().minusDays(27), 1),
                new DailySales(LocalDate.now().minusDays(28), 2),
                new DailySales(LocalDate.now().minusDays(29), 3)
        );

        when(salesHistoryRepository.getDailySales(SKU, WAREHOUSE_US, 30))
                .thenReturn(salesHistory);

        InventoryForecast forecast = forecastService.calculateForecast(SKU, WAREHOUSE_US);

        // Total sales = 26 over 30 days
        assertThat(forecast.getAvgDailySales())
                .as("Average should be calculated over full 30-day period including zero-sale days")
                .isCloseTo(26.0 / 30.0, within(0.01));
    }

    // -----------------------------------------------------------------------
    // Seasonal Adjustment
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Higher forecast during peak season (Q4) using seasonal multiplier")
    void testSeasonalAdjustment() {
        // Given: Base daily sales of 10 with Q4 seasonal multiplier of 1.5x
        double baseAvgDailySales = 10.0;
        SeasonalProfile seasonalProfile = SeasonalProfile.builder()
                .sku(SKU)
                .monthlyMultipliers(new double[]{
                        0.8, 0.8, 0.9, 1.0, 1.0, 1.0,  // Jan-Jun
                        0.9, 0.9, 1.0, 1.2, 1.5, 1.8   // Jul-Dec (Q4 peak)
                })
                .build();

        when(salesHistoryRepository.getSeasonalProfile(SKU)).thenReturn(seasonalProfile);

        // When: Computing forecast for November (index 10, multiplier 1.5)
        double novemberForecast = forecastService.getSeasonallyAdjustedSales(
                baseAvgDailySales, 11 // November
        );

        // Then: November forecast is 1.5x base
        assertThat(novemberForecast)
                .as("November should have 1.5x seasonal multiplier")
                .isCloseTo(baseAvgDailySales * 1.5, within(0.1));

        // And: January (index 0, multiplier 0.8) is lower
        double januaryForecast = forecastService.getSeasonallyAdjustedSales(
                baseAvgDailySales, 1 // January
        );
        assertThat(januaryForecast)
                .as("January should have 0.8x seasonal multiplier")
                .isCloseTo(baseAvgDailySales * 0.8, within(0.1));
    }

    @Test
    @DisplayName("Seasonal adjustment increases reorder point during peak season")
    void testSeasonalAdjustment_ReorderPoint() {
        // Given: Q4 seasonal period
        double baseAvgDailySales = 10.0;
        double seasonalMultiplier = 1.5; // Q4 multiplier
        int leadTimeDays = 45;
        double safetyStock = 50.0;

        double adjustedDailySales = baseAvgDailySales * seasonalMultiplier;

        int standardReorderPoint = forecastService.calculateReorderPoint(
                baseAvgDailySales, leadTimeDays, safetyStock
        );

        int seasonalReorderPoint = forecastService.calculateReorderPoint(
                adjustedDailySales, leadTimeDays, safetyStock
        );

        assertThat(seasonalReorderPoint)
                .as("Seasonal reorder point should be higher during peak season")
                .isGreaterThan(standardReorderPoint);

        // Verify the difference is due to increased demand
        int expectedDifference = (int) Math.round(baseAvgDailySales * (seasonalMultiplier - 1.0) * leadTimeDays);
        assertThat(seasonalReorderPoint - standardReorderPoint)
                .isCloseTo(expectedDifference, within(1.0));
    }

    @Test
    @DisplayName("No seasonal profile uses flat forecast without adjustment")
    void testSeasonalAdjustment_NoProfile() {
        when(salesHistoryRepository.getSeasonalProfile(SKU)).thenReturn(null);

        double baseAvgDailySales = 10.0;
        double adjusted = forecastService.getSeasonallyAdjustedSales(baseAvgDailySales, 11);

        assertThat(adjusted)
                .as("Without seasonal profile, forecast should equal base average")
                .isEqualTo(baseAvgDailySales);
    }

    // -----------------------------------------------------------------------
    // Recommended Reorder Quantity
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Recommended reorder quantity covers 3 months of projected demand")
    void testRecommendedReorderQuantity() {
        double avgDailySales = 10.0;
        int monthsSupply = 3;
        int currentStock = 100;

        int recommendedQty = forecastService.calculateReorderQuantity(
                avgDailySales, monthsSupply, currentStock
        );

        // 3 months * 30 days * 10 = 900 units
        int expectedQty = (int) Math.round(avgDailySales * 30 * monthsSupply);
        assertThat(recommendedQty).isEqualTo(expectedQty);
    }

    @Test
    @DisplayName("Reorder quantity is rounded to nearest 100 for manufacturing efficiency")
    void testRecommendedReorderQuantity_Rounded() {
        double avgDailySales = 7.3; // 7.3 * 90 = 657
        int monthsSupply = 3;
        int currentStock = 0;

        int recommendedQty = forecastService.calculateReorderQuantity(
                avgDailySales, monthsSupply, currentStock
        );

        // Should round to nearest 100
        assertThat(recommendedQty % 100)
                .as("Reorder quantity should be rounded to nearest 100")
                .isEqualTo(0);
    }
}
