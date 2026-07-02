package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonReplenishAlertDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryMapper;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonReplenishAlertMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Function tool: Check inventory levels, days of supply, and restock alerts.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetInventoryStatusFunction implements Function<GetInventoryStatusFunction.Request, GetInventoryStatusFunction.Response> {

    @Resource
    private AmazonInventoryMapper amazonInventoryMapper;

    @Resource
    private AmazonReplenishAlertMapper amazonReplenishAlertMapper;

    @Resource
    private AmazonOrderMapper amazonOrderMapper;

    @Resource
    private AmazonOrderItemMapper amazonOrderItemMapper;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** Optional ASIN to check specific product */
        private String asin;
        /** Whether to include restock alerts */
        private Boolean includeAlerts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private String asin;
        private String title;
        private Integer availableUnits;
        private Integer inboundUnits;
        private Integer reservedUnits;
        private Double avgDailySales;
        private Integer daysOfSupply;
        private String status; // HEALTHY, LOW, CRITICAL, OUT_OF_STOCK
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestockAlert {
        private String asin;
        private String title;
        private Integer daysOfSupply;
        private Integer recommendedReorderQuantity;
        private String urgency;
        private String estimatedStockoutDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private List<InventoryItem> items;
        private List<RestockAlert> alerts;
        private String overallStatus; // HEALTHY, WARNING, CRITICAL
        private String summary;
    }

    @Override
    public Response apply(Request request) {
        log.info("getInventoryStatus called: asin={}, includeAlerts={}",
                request.getAsin(), request.getIncludeAlerts());

        try {
            // 1. Query inventory records
            List<AmazonInventoryDO> inventoryRecords;
            if (request.getAsin() != null && !request.getAsin().trim().isEmpty()) {
                // Query specific ASIN
                LambdaQueryWrapperX<AmazonInventoryDO> wrapper = new LambdaQueryWrapperX<>();
                wrapper.eq(AmazonInventoryDO::getAsin, request.getAsin().trim());
                wrapper.orderByDesc(AmazonInventoryDO::getSnapshotDate);
                inventoryRecords = amazonInventoryMapper.selectList(wrapper);
                log.info("Fetched {} inventory records for ASIN={}", inventoryRecords.size(), request.getAsin());
            } else {
                // Query all inventory for the latest snapshot date
                // First find the max snapshot date
                LambdaQueryWrapperX<AmazonInventoryDO> dateWrapper = new LambdaQueryWrapperX<>();
                dateWrapper.orderByDesc(AmazonInventoryDO::getSnapshotDate);
                dateWrapper.last("LIMIT 1");
                List<AmazonInventoryDO> latest = amazonInventoryMapper.selectList(dateWrapper);

                if (latest.isEmpty()) {
                    return Response.builder()
                            .items(Collections.emptyList())
                            .alerts(Collections.emptyList())
                            .overallStatus("HEALTHY")
                            .summary("No inventory records found.")
                            .build();
                }

                LocalDate latestDate = latest.get(0).getSnapshotDate();
                LambdaQueryWrapperX<AmazonInventoryDO> allWrapper = new LambdaQueryWrapperX<>();
                allWrapper.eq(AmazonInventoryDO::getSnapshotDate, latestDate);
                inventoryRecords = amazonInventoryMapper.selectList(allWrapper);
                log.info("Fetched {} inventory records for snapshot date={}", inventoryRecords.size(), latestDate);
            }

            if (inventoryRecords.isEmpty()) {
                return Response.builder()
                        .items(Collections.emptyList())
                        .alerts(Collections.emptyList())
                        .overallStatus("HEALTHY")
                        .summary("No inventory records found.")
                        .build();
            }

            // 2. Estimate avgDailySales from order history (last 30 days) per ASIN
            Map<String, Double> asinDailySales = estimateAvgDailySales(inventoryRecords);

            // 3. Aggregate inventory by ASIN (multiple fulfillment centers may exist)
            Map<String, Integer> asinAvailable = new HashMap<>();
            Map<String, Integer> asinInbound = new HashMap<>();
            Map<String, Integer> asinReserved = new HashMap<>();

            for (AmazonInventoryDO inv : inventoryRecords) {
                String asin = inv.getAsin();
                if (asin == null) {
                    continue;
                }
                Integer avail = asinAvailable.get(asin);
                asinAvailable.put(asin, (avail != null ? avail : 0) + (inv.getAvailableQty() != null ? inv.getAvailableQty() : 0));
                Integer inb = asinInbound.get(asin);
                asinInbound.put(asin, (inb != null ? inb : 0) + (inv.getInboundQty() != null ? inv.getInboundQty() : 0));
                Integer res = asinReserved.get(asin);
                asinReserved.put(asin, (res != null ? res : 0) + (inv.getReservedQty() != null ? inv.getReservedQty() : 0));
            }

            // 4. Build InventoryItem list
            List<InventoryItem> items = new ArrayList<>();
            String worstStatus = "HEALTHY";

            for (Map.Entry<String, Integer> entry : asinAvailable.entrySet()) {
                String asin = entry.getKey();
                int available = entry.getValue();
                int inbound = asinInbound.get(asin) != null ? asinInbound.get(asin) : 0;
                int reserved = asinReserved.get(asin) != null ? asinReserved.get(asin) : 0;
                double dailySales = asinDailySales.get(asin) != null ? asinDailySales.get(asin) : 1.0;

                int daysOfSupply;
                if (available <= 0) {
                    daysOfSupply = 0;
                } else if (dailySales <= 0) {
                    daysOfSupply = 999;
                } else {
                    daysOfSupply = (int) Math.floor(available / dailySales);
                }

                String status;
                if (available <= 0) {
                    status = "OUT_OF_STOCK";
                } else if (daysOfSupply <= 14) {
                    status = "CRITICAL";
                } else if (daysOfSupply <= 30) {
                    status = "LOW";
                } else {
                    status = "HEALTHY";
                }

                // Track worst status
                if ("OUT_OF_STOCK".equals(status) || "CRITICAL".equals(status)) {
                    worstStatus = "CRITICAL";
                } else if ("LOW".equals(status) && "HEALTHY".equals(worstStatus)) {
                    worstStatus = "WARNING";
                }

                items.add(InventoryItem.builder()
                        .asin(asin)
                        .title(asin)
                        .availableUnits(available)
                        .inboundUnits(inbound)
                        .reservedUnits(reserved)
                        .avgDailySales(Math.round(dailySales * 10.0) / 10.0)
                        .daysOfSupply(daysOfSupply)
                        .status(status)
                        .build());
            }

            // 5. Query replenishment alerts if requested
            List<RestockAlert> alerts = new ArrayList<>();
            if (Boolean.TRUE.equals(request.getIncludeAlerts())) {
                LambdaQueryWrapperX<AmazonReplenishAlertDO> alertWrapper = new LambdaQueryWrapperX<>();
                alertWrapper.eq(AmazonReplenishAlertDO::getAcknowledged, false);
                if (request.getAsin() != null && !request.getAsin().trim().isEmpty()) {
                    alertWrapper.eq(AmazonReplenishAlertDO::getAsin, request.getAsin().trim());
                }
                alertWrapper.orderByDesc(AmazonReplenishAlertDO::getId);
                List<AmazonReplenishAlertDO> alertRecords = amazonReplenishAlertMapper.selectList(alertWrapper);
                log.info("Fetched {} unacknowledged replenishment alerts", alertRecords.size());

                for (AmazonReplenishAlertDO alert : alertRecords) {
                    // Find the corresponding inventory item for daysOfSupply
                    int alertDaysOfSupply = 0;
                    for (InventoryItem item : items) {
                        if (alert.getAsin() != null && alert.getAsin().equals(item.getAsin())) {
                            alertDaysOfSupply = item.getDaysOfSupply();
                            break;
                        }
                    }

                    String urgency;
                    if ("OUT_OF_STOCK".equals(alert.getAlertType())) {
                        urgency = "CRITICAL";
                    } else if ("LOW_STOCK".equals(alert.getAlertType())) {
                        urgency = "HIGH";
                    } else {
                        urgency = "MEDIUM";
                    }

                    String stockoutDate;
                    if (alertDaysOfSupply > 0) {
                        stockoutDate = LocalDate.now().plusDays(alertDaysOfSupply).toString();
                    } else {
                        stockoutDate = "Imminent";
                    }

                    alerts.add(RestockAlert.builder()
                            .asin(alert.getAsin())
                            .title(alert.getAsin())
                            .daysOfSupply(alertDaysOfSupply)
                            .recommendedReorderQuantity(alert.getSuggestedQty())
                            .urgency(urgency)
                            .estimatedStockoutDate(stockoutDate)
                            .build());
                }
            }

            // 6. Build summary
            int healthyCount = 0;
            int attentionCount = 0;
            for (InventoryItem item : items) {
                if ("HEALTHY".equals(item.getStatus())) {
                    healthyCount++;
                } else {
                    attentionCount++;
                }
            }

            return Response.builder()
                    .items(items)
                    .alerts(alerts)
                    .overallStatus(worstStatus)
                    .summary(String.format("%d products tracked. %d healthy, %d need attention. %d alerts.",
                            items.size(), healthyCount, attentionCount, alerts.size()))
                    .build();

        } catch (Exception e) {
            log.error("getInventoryStatus failed: {}", e.getMessage(), e);
            return Response.builder()
                    .items(Collections.emptyList())
                    .alerts(Collections.emptyList())
                    .overallStatus("ERROR")
                    .summary("Error fetching inventory data: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Estimate average daily sales for each unique ASIN from order history in the last 30 days.
     */
    private Map<String, Double> estimateAvgDailySales(List<AmazonInventoryDO> inventoryRecords) {
        Map<String, Double> result = new HashMap<>();

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime thirtyDaysAgo = today.minusDays(30).atStartOfDay();
            LocalDateTime now = today.atTime(LocalTime.MAX);

            // Get recent orders
            LambdaQueryWrapperX<AmazonOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
            orderWrapper.ge(AmazonOrderDO::getPurchaseDate, thirtyDaysAgo);
            orderWrapper.le(AmazonOrderDO::getPurchaseDate, now);
            orderWrapper.ne(AmazonOrderDO::getOrderStatus, "Canceled");
            List<AmazonOrderDO> recentOrders = amazonOrderMapper.selectList(orderWrapper);

            if (recentOrders.isEmpty()) {
                log.info("No recent orders found for daily sales estimation, using default 1.0");
                return result;
            }

            List<Long> orderIds = new ArrayList<>();
            for (AmazonOrderDO order : recentOrders) {
                orderIds.add(order.getId());
            }

            // Get order items for those orders
            LambdaQueryWrapperX<AmazonOrderItemDO> itemWrapper = new LambdaQueryWrapperX<>();
            itemWrapper.in(AmazonOrderItemDO::getOrderId, orderIds);
            List<AmazonOrderItemDO> items = amazonOrderItemMapper.selectList(itemWrapper);

            // Sum quantity per ASIN
            Map<String, Integer> asinTotalQty = new HashMap<>();
            for (AmazonOrderItemDO item : items) {
                if (item.getAsin() == null) {
                    continue;
                }
                Integer current = asinTotalQty.get(item.getAsin());
                if (current == null) {
                    current = 0;
                }
                asinTotalQty.put(item.getAsin(), current + (item.getQuantity() != null ? item.getQuantity() : 0));
            }

            // Calculate average daily sales (total over 30 days)
            for (Map.Entry<String, Integer> entry : asinTotalQty.entrySet()) {
                result.put(entry.getKey(), entry.getValue().doubleValue() / 30.0);
            }
            log.info("Estimated daily sales for {} ASINs from {} order items", result.size(), items.size());

        } catch (Exception e) {
            log.warn("Failed to estimate daily sales, using defaults: {}", e.getMessage());
        }

        return result;
    }
}
