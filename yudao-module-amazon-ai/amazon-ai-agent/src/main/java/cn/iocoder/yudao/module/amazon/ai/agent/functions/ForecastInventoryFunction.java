package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryForecastDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryForecastMapper;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryMapper;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderItemMapper;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Function tool: Get inventory demand forecast and reorder recommendations.
 *
 * <p>Queries amazon_inventory for current stock, amazon_order + amazon_order_item
 * for sales velocity, and amazon_inventory_forecast for pre-computed predictions.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class ForecastInventoryFunction implements Function<ForecastInventoryFunction.Request, ForecastInventoryFunction.Response> {

    private static final int DEFAULT_LEAD_TIME_DAYS = 14;
    private static final int DEFAULT_SAFETY_DAYS = 7;
    private static final int SALES_HISTORY_DAYS = 30;

    @Resource
    private AmazonInventoryMapper amazonInventoryMapper;

    @Resource
    private AmazonInventoryForecastMapper amazonInventoryForecastMapper;

    @Resource
    private AmazonOrderMapper amazonOrderMapper;

    @Resource
    private AmazonOrderItemMapper amazonOrderItemMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String asin;
        /** Number of days to forecast */
        private Integer forecastDays;
        /** Confidence level (0-100, typically 80 or 95) */
        private Integer confidenceLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String asin;
        private Integer forecastDays;
        private Double avgDailyDemand;
        private Double predictedTotalDemand;
        private String reorderDate;
        private Integer reorderQuantity;
        private Integer daysOfSupplyRemaining;
        private String urgency;
        private String seasonalInsight;
        private String confidenceLevel;
        private String summary;
    }

    @Override
    public Response apply(Request request) {
        log.info("forecastInventory called: asin={}, forecastDays={}, confidence={}",
                request.getAsin(), request.getForecastDays(), request.getConfidenceLevel());

        int forecastDays = request.getForecastDays() != null ? request.getForecastDays() : 90;
        int confidence = request.getConfidenceLevel() != null ? request.getConfidenceLevel() : 80;
        String asin = request.getAsin() != null ? request.getAsin() : "";

        try {
            // Step 1: Get current inventory
            List<AmazonInventoryDO> inventoryRecords = amazonInventoryMapper.selectList(
                    new LambdaQueryWrapperX<AmazonInventoryDO>()
                            .eq(AmazonInventoryDO::getAsin, asin));

            int availableQty = 0;
            int inboundQty = 0;
            for (AmazonInventoryDO inv : inventoryRecords) {
                if (inv.getAvailableQty() != null) {
                    availableQty += inv.getAvailableQty();
                }
                if (inv.getInboundQty() != null) {
                    inboundQty += inv.getInboundQty();
                }
            }
            int totalAvailable = availableQty + inboundQty;

            // Step 2: Check for pre-computed forecast
            AmazonInventoryForecastDO preComputedForecast = null;
            try {
                List<AmazonInventoryForecastDO> forecasts = amazonInventoryForecastMapper.selectList(
                        new LambdaQueryWrapperX<AmazonInventoryForecastDO>()
                                .eq(AmazonInventoryForecastDO::getAsin, asin)
                                .orderByDesc(AmazonInventoryForecastDO::getGenerateDate)
                                .last("LIMIT 1"));
                if (!forecasts.isEmpty()) {
                    preComputedForecast = forecasts.get(0);
                }
            } catch (Exception e) {
                log.warn("Could not load pre-computed forecast for ASIN {}: {}", asin, e.getMessage());
            }

            // Step 3: Calculate average daily sales from order history
            double avgDailySales = calculateAvgDailySales(asin);

            // Use pre-computed forecast values if available, otherwise calculate
            double avgDailyDemand;
            int reorderPoint;
            int suggestedReorderQty;

            if (preComputedForecast != null && preComputedForecast.getPredictedDailySales() != null) {
                avgDailyDemand = preComputedForecast.getPredictedDailySales().doubleValue();
                reorderPoint = preComputedForecast.getReorderPoint() != null
                        ? preComputedForecast.getReorderPoint()
                        : (int) Math.ceil(avgDailyDemand * DEFAULT_LEAD_TIME_DAYS);
                suggestedReorderQty = preComputedForecast.getSuggestedReorderQty() != null
                        ? preComputedForecast.getSuggestedReorderQty()
                        : computeSuggestedReorderQty(avgDailyDemand, DEFAULT_LEAD_TIME_DAYS, DEFAULT_SAFETY_DAYS, totalAvailable);
            } else {
                avgDailyDemand = avgDailySales;
                reorderPoint = (int) Math.ceil(avgDailyDemand * DEFAULT_LEAD_TIME_DAYS);
                suggestedReorderQty = computeSuggestedReorderQty(avgDailyDemand, DEFAULT_LEAD_TIME_DAYS, DEFAULT_SAFETY_DAYS, totalAvailable);
            }

            // Step 4: Compute days of supply
            int daysOfSupply = avgDailyDemand > 0
                    ? (int) (totalAvailable / avgDailyDemand)
                    : (totalAvailable > 0 ? 999 : 0);

            // Step 5: Compute predicted total demand
            double predictedTotalDemand = avgDailyDemand * forecastDays;

            // Step 6: Determine reorder date
            String reorderDate;
            if (daysOfSupply <= DEFAULT_LEAD_TIME_DAYS + DEFAULT_SAFETY_DAYS) {
                reorderDate = "Immediate (today)";
            } else {
                int daysUntilReorder = daysOfSupply - DEFAULT_LEAD_TIME_DAYS - DEFAULT_SAFETY_DAYS;
                reorderDate = "In approximately " + daysUntilReorder + " days";
            }

            // Step 7: Determine urgency
            String urgency;
            if (totalAvailable <= 0 && avgDailyDemand > 0) {
                urgency = "CRITICAL";
            } else if (daysOfSupply <= DEFAULT_SAFETY_DAYS) {
                urgency = "CRITICAL";
            } else if (daysOfSupply <= DEFAULT_LEAD_TIME_DAYS) {
                urgency = "HIGH";
            } else if (daysOfSupply <= DEFAULT_LEAD_TIME_DAYS + DEFAULT_SAFETY_DAYS + 7) {
                urgency = "MEDIUM";
            } else {
                urgency = "LOW";
            }

            // Step 8: Confidence level
            String confidenceLevel;
            if (preComputedForecast != null && preComputedForecast.getConfidence() != null) {
                double conf = preComputedForecast.getConfidence().doubleValue();
                if (conf >= 0.8) {
                    confidenceLevel = "HIGH";
                } else if (conf >= 0.5) {
                    confidenceLevel = "MEDIUM";
                } else {
                    confidenceLevel = "LOW";
                }
            } else if (avgDailySales > 0) {
                confidenceLevel = "MEDIUM";
            } else {
                confidenceLevel = "LOW";
            }

            // Seasonal insight
            String seasonalInsight = "No pre-computed seasonal analysis available. "
                    + "Historical average based on last " + SALES_HISTORY_DAYS + " days of order data.";

            // Build summary
            String summary = String.format(
                    "Forecast: %.1f avg daily units over %d days. Total predicted demand: %.0f units. "
                            + "Current stock: %d available + %d inbound = %d total. "
                            + "Stock covers ~%d days. Reorder %d units %s (%s urgency).",
                    avgDailyDemand, forecastDays, predictedTotalDemand,
                    availableQty, inboundQty, totalAvailable,
                    daysOfSupply, Math.max(0, suggestedReorderQty), reorderDate, urgency);

            return Response.builder()
                    .asin(asin)
                    .forecastDays(forecastDays)
                    .avgDailyDemand(Math.round(avgDailyDemand * 10.0) / 10.0)
                    .predictedTotalDemand(Math.round(predictedTotalDemand * 10.0) / 10.0)
                    .reorderDate(reorderDate)
                    .reorderQuantity(Math.max(0, suggestedReorderQty))
                    .daysOfSupplyRemaining(daysOfSupply)
                    .urgency(urgency)
                    .seasonalInsight(seasonalInsight)
                    .confidenceLevel(confidenceLevel)
                    .summary(summary)
                    .build();

        } catch (Exception e) {
            log.error("Forecast failed for ASIN {}: {}", asin, e.getMessage(), e);
            return Response.builder()
                    .asin(asin)
                    .forecastDays(forecastDays)
                    .urgency("ERROR")
                    .confidenceLevel("UNKNOWN")
                    .summary("Forecast generation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Calculate average daily sales from order items in the last 30 days.
     */
    private double calculateAvgDailySales(String asin) {
        try {
            // Get orders from the last 30 days
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(SALES_HISTORY_DAYS);
            List<AmazonOrderDO> recentOrders = amazonOrderMapper.selectList(
                    new LambdaQueryWrapperX<AmazonOrderDO>()
                            .ge(AmazonOrderDO::getPurchaseDate, cutoffDate));

            if (recentOrders.isEmpty()) {
                log.info("No orders found in the last {} days", SALES_HISTORY_DAYS);
                return 0.0;
            }

            // Collect order IDs
            Set<Long> orderIds = new HashSet<Long>();
            for (AmazonOrderDO order : recentOrders) {
                orderIds.add(order.getId());
            }

            // Query order items for the ASIN within those orders
            // Process in batches to avoid overly large IN clauses
            int totalQuantity = 0;
            List<Long> orderIdList = new ArrayList<Long>(orderIds);
            int batchSize = 500;
            for (int i = 0; i < orderIdList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, orderIdList.size());
                List<Long> batch = orderIdList.subList(i, end);

                List<AmazonOrderItemDO> items = amazonOrderItemMapper.selectList(
                        new LambdaQueryWrapperX<AmazonOrderItemDO>()
                                .eq(AmazonOrderItemDO::getAsin, asin)
                                .in(AmazonOrderItemDO::getOrderId, batch));

                for (AmazonOrderItemDO item : items) {
                    if (item.getQuantity() != null) {
                        totalQuantity += item.getQuantity();
                    }
                }
            }

            return (double) totalQuantity / SALES_HISTORY_DAYS;

        } catch (Exception e) {
            log.warn("Failed to calculate avg daily sales for ASIN {}: {}", asin, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Compute suggested reorder quantity.
     * Formula: avg_daily * (lead_time + safety_days) - available
     */
    private int computeSuggestedReorderQty(double avgDailyDemand, int leadTimeDays,
                                           int safetyDays, int totalAvailable) {
        double targetStock = avgDailyDemand * (leadTimeDays + safetyDays + 30); // +30 days buffer
        double reorderQty = targetStock - totalAvailable;
        return (int) Math.ceil(Math.max(0, reorderQty));
    }
}
