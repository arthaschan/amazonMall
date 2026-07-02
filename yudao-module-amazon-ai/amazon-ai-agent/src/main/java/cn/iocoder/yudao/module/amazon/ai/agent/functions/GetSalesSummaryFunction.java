package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderItemMapper;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Function tool: Retrieve sales performance summary.
 *
 * <p>Called by the OpsAgent when the seller asks about sales data.
 * Queries the order module's service layer for actual sales metrics.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetSalesSummaryFunction implements Function<GetSalesSummaryFunction.Request, GetSalesSummaryFunction.Response> {

    @Resource
    private AmazonOrderMapper amazonOrderMapper;

    @Resource
    private AmazonOrderItemMapper amazonOrderItemMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** Date range, e.g. "last 7 days", "this month", "2025-01-01 to 2025-01-31" */
        private String dateRange;
        /** Optional ASIN to filter by specific product */
        private String asin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String dateRange;
        private Double totalRevenue;
        private Integer totalUnits;
        private Integer totalOrders;
        private Double avgOrderValue;
        private Double conversionRate;
        private String comparisonToPreviousPeriod;
        private String topPerformer;
        private String underperformer;
    }

    @Override
    public Response apply(Request request) {
        log.info("getSalesSummary called: dateRange={}, asin={}", request.getDateRange(), request.getAsin());

        String displayDateRange = request.getDateRange() != null ? request.getDateRange() : "last 30 days";

        try {
            // 1. Parse date range
            LocalDateTime[] range = parseDateRange(request.getDateRange());
            LocalDateTime startDate = range[0];
            LocalDateTime endDate = range[1];
            log.info("Resolved date range: {} to {}", startDate, endDate);

            // 2. Query orders within date range
            LambdaQueryWrapperX<AmazonOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
            orderWrapper.ge(AmazonOrderDO::getPurchaseDate, startDate);
            orderWrapper.le(AmazonOrderDO::getPurchaseDate, endDate);
            orderWrapper.ne(AmazonOrderDO::getOrderStatus, "Canceled");
            List<AmazonOrderDO> orders = amazonOrderMapper.selectList(orderWrapper);
            log.info("Fetched {} orders for date range", orders.size());

            if (orders.isEmpty()) {
                return Response.builder()
                        .dateRange(displayDateRange)
                        .totalRevenue(0.0)
                        .totalUnits(0)
                        .totalOrders(0)
                        .avgOrderValue(0.0)
                        .conversionRate(0.0)
                        .comparisonToPreviousPeriod("No orders found in the specified date range.")
                        .topPerformer("N/A")
                        .underperformer("N/A")
                        .build();
            }

            // 3. Compute order-level metrics
            BigDecimal totalRevenue = BigDecimal.ZERO;
            int totalUnits = 0;
            int totalOrders = orders.size();

            List<Long> orderIds = new ArrayList<>();
            for (AmazonOrderDO order : orders) {
                if (order.getOrderTotal() != null) {
                    totalRevenue = totalRevenue.add(order.getOrderTotal());
                }
                if (order.getItemCount() != null) {
                    totalUnits += order.getItemCount();
                }
                orderIds.add(order.getId());
            }

            double totalRevenueDouble = totalRevenue.doubleValue();
            double avgOrderValue = totalOrders > 0 ? totalRevenueDouble / totalOrders : 0.0;

            // 4. Query order items for ASIN-level analysis (topPerformer / underperformer)
            String topPerformer = "N/A";
            String underperformer = "N/A";

            if (!orderIds.isEmpty()) {
                List<AmazonOrderItemDO> allItems = new ArrayList<>();
                // Batch query items by orderId (use IN clause for efficiency)
                LambdaQueryWrapperX<AmazonOrderItemDO> itemWrapper = new LambdaQueryWrapperX<>();
                itemWrapper.in(AmazonOrderItemDO::getOrderId, orderIds);
                allItems = amazonOrderItemMapper.selectList(itemWrapper);
                log.info("Fetched {} order items for ASIN analysis", allItems.size());

                // Filter by ASIN if specified
                if (request.getAsin() != null && !request.getAsin().isEmpty()) {
                    List<AmazonOrderItemDO> filtered = new ArrayList<>();
                    for (AmazonOrderItemDO item : allItems) {
                        if (request.getAsin().equals(item.getAsin())) {
                            filtered.add(item);
                        }
                    }
                    allItems = filtered;
                }

                // Group by ASIN, sum revenue and units
                Map<String, BigDecimal> asinRevenue = new HashMap<>();
                Map<String, Integer> asinUnits = new HashMap<>();

                for (AmazonOrderItemDO item : allItems) {
                    String asin = item.getAsin();
                    if (asin == null || asin.isEmpty()) {
                        continue;
                    }
                    BigDecimal itemRevenue = BigDecimal.ZERO;
                    if (item.getPrice() != null && item.getQuantity() != null) {
                        itemRevenue = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    }
                    BigDecimal current = asinRevenue.get(asin);
                    if (current == null) {
                        current = BigDecimal.ZERO;
                    }
                    asinRevenue.put(asin, current.add(itemRevenue));

                    Integer currentUnits = asinUnits.get(asin);
                    if (currentUnits == null) {
                        currentUnits = 0;
                    }
                    asinUnits.put(asin, currentUnits + (item.getQuantity() != null ? item.getQuantity() : 0));
                }

                // Find top performer (highest revenue)
                if (!asinRevenue.isEmpty()) {
                    Map.Entry<String, BigDecimal> topEntry = null;
                    Map.Entry<String, BigDecimal> bottomEntry = null;
                    for (Map.Entry<String, BigDecimal> entry : asinRevenue.entrySet()) {
                        if (topEntry == null || entry.getValue().compareTo(topEntry.getValue()) > 0) {
                            topEntry = entry;
                        }
                        if (bottomEntry == null || entry.getValue().compareTo(bottomEntry.getValue()) < 0) {
                            bottomEntry = entry;
                        }
                    }
                    if (topEntry != null) {
                        int topUnits = asinUnits.get(topEntry.getKey()) != null ? asinUnits.get(topEntry.getKey()) : 0;
                        topPerformer = String.format("%s ($%.2f revenue, %d units)",
                                topEntry.getKey(), topEntry.getValue().doubleValue(), topUnits);
                    }
                    if (bottomEntry != null && asinRevenue.size() > 1) {
                        int bottomUnits = asinUnits.get(bottomEntry.getKey()) != null ? asinUnits.get(bottomEntry.getKey()) : 0;
                        underperformer = String.format("%s ($%.2f revenue, %d units)",
                                bottomEntry.getKey(), bottomEntry.getValue().doubleValue(), bottomUnits);
                    }
                }
            }

            // 5. Compare with previous period
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());
            LocalDateTime prevStartDate = startDate.minusDays(daysBetween);
            LocalDateTime prevEndDate = startDate;

            LambdaQueryWrapperX<AmazonOrderDO> prevWrapper = new LambdaQueryWrapperX<>();
            prevWrapper.ge(AmazonOrderDO::getPurchaseDate, prevStartDate);
            prevWrapper.lt(AmazonOrderDO::getPurchaseDate, prevEndDate);
            prevWrapper.ne(AmazonOrderDO::getOrderStatus, "Canceled");
            List<AmazonOrderDO> prevOrders = amazonOrderMapper.selectList(prevWrapper);

            BigDecimal prevRevenue = BigDecimal.ZERO;
            int prevUnits = 0;
            for (AmazonOrderDO order : prevOrders) {
                if (order.getOrderTotal() != null) {
                    prevRevenue = prevRevenue.add(order.getOrderTotal());
                }
                if (order.getItemCount() != null) {
                    prevUnits += order.getItemCount();
                }
            }

            String comparison;
            if (prevOrders.isEmpty()) {
                comparison = "No data for previous period to compare.";
            } else {
                double prevRevenueDouble = prevRevenue.doubleValue();
                double revChange = prevRevenueDouble > 0
                        ? ((totalRevenueDouble - prevRevenueDouble) / prevRevenueDouble) * 100.0
                        : 0.0;
                double unitChange = prevUnits > 0
                        ? ((double)(totalUnits - prevUnits) / prevUnits) * 100.0
                        : 0.0;
                comparison = String.format("Revenue %s %.1f%% vs previous period. Units %s %.1f%%.",
                        revChange >= 0 ? "up" : "down", Math.abs(revChange),
                        unitChange >= 0 ? "up" : "down", Math.abs(unitChange));
            }

            return Response.builder()
                    .dateRange(displayDateRange)
                    .totalRevenue(Math.round(totalRevenueDouble * 100.0) / 100.0)
                    .totalUnits(totalUnits)
                    .totalOrders(totalOrders)
                    .avgOrderValue(Math.round(avgOrderValue * 100.0) / 100.0)
                    .conversionRate(null)
                    .comparisonToPreviousPeriod(comparison)
                    .topPerformer(topPerformer)
                    .underperformer(underperformer)
                    .build();

        } catch (Exception e) {
            log.error("getSalesSummary failed: {}", e.getMessage(), e);
            return Response.builder()
                    .dateRange(displayDateRange)
                    .totalRevenue(0.0)
                    .totalUnits(0)
                    .totalOrders(0)
                    .avgOrderValue(0.0)
                    .conversionRate(0.0)
                    .comparisonToPreviousPeriod("Error fetching sales data: " + e.getMessage())
                    .topPerformer("N/A")
                    .underperformer("N/A")
                    .build();
        }
    }

    /**
     * Parse a human-readable date range string into a start/end LocalDateTime pair.
     * Supports: "last N days", "this month", "YYYY-MM-DD to YYYY-MM-DD", or null (defaults to last 30 days).
     */
    private LocalDateTime[] parseDateRange(String dateRange) {
        LocalDate today = LocalDate.now();
        LocalDateTime endDateTime = today.atTime(LocalTime.MAX);

        if (dateRange == null || dateRange.trim().isEmpty()) {
            // Default: last 30 days
            return new LocalDateTime[]{today.minusDays(30).atStartOfDay(), endDateTime};
        }

        String normalized = dateRange.trim().toLowerCase();

        // Pattern: "last N days"
        Pattern lastNDays = Pattern.compile("last\\s+(\\d+)\\s+days?");
        Matcher matcher = lastNDays.matcher(normalized);
        if (matcher.find()) {
            int days = Integer.parseInt(matcher.group(1));
            return new LocalDateTime[]{today.minusDays(days).atStartOfDay(), endDateTime};
        }

        // Pattern: "last N months"
        Pattern lastNMonths = Pattern.compile("last\\s+(\\d+)\\s+months?");
        matcher = lastNMonths.matcher(normalized);
        if (matcher.find()) {
            int months = Integer.parseInt(matcher.group(1));
            return new LocalDateTime[]{today.minusMonths(months).atStartOfDay(), endDateTime};
        }

        // Pattern: "this month"
        if ("this month".equals(normalized)) {
            return new LocalDateTime[]{today.withDayOfMonth(1).atStartOfDay(), endDateTime};
        }

        // Pattern: "last month"
        if ("last month".equals(normalized)) {
            LocalDate firstOfLastMonth = today.withDayOfMonth(1).minusMonths(1);
            LocalDate lastOfLastMonth = today.withDayOfMonth(1).minusDays(1);
            return new LocalDateTime[]{firstOfLastMonth.atStartOfDay(), lastOfLastMonth.atTime(LocalTime.MAX)};
        }

        // Pattern: "YYYY-MM-DD to YYYY-MM-DD"
        Pattern explicitRange = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s+to\\s+(\\d{4}-\\d{2}-\\d{2})");
        matcher = explicitRange.matcher(normalized);
        if (matcher.find()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(matcher.group(1), fmt);
            LocalDate end = LocalDate.parse(matcher.group(2), fmt);
            return new LocalDateTime[]{start.atStartOfDay(), end.atTime(LocalTime.MAX)};
        }

        // Fallback: last 30 days
        log.warn("Unrecognized date range format '{}', defaulting to last 30 days", dateRange);
        return new LocalDateTime[]{today.minusDays(30).atStartOfDay(), endDateTime};
    }
}
