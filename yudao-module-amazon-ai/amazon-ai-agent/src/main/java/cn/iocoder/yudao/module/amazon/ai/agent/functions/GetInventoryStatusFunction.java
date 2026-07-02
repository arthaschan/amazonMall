package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Function tool: Check inventory levels, days of supply, and restock alerts.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetInventoryStatusFunction implements Function<GetInventoryStatusFunction.Request, GetInventoryStatusFunction.Response> {

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

        // TODO: Inject and query the actual inventory service:
        //   inventoryService.getInventoryStatus(tenantId, asin)

        List<InventoryItem> items = List.of(
                InventoryItem.builder()
                        .asin("B08N5WRWNW").title("Wireless Earbuds Pro")
                        .availableUnits(450).inboundUnits(200).reservedUnits(12)
                        .avgDailySales(3.2).daysOfSupply(140)
                        .status("HEALTHY").build(),
                InventoryItem.builder()
                        .asin("B09ABC5678").title("Stainless Steel Water Bottle")
                        .availableUnits(85).inboundUnits(0).reservedUnits(8)
                        .avgDailySales(4.5).daysOfSupply(17)
                        .status("LOW").build(),
                InventoryItem.builder()
                        .asin("B09XYZ1234").title("Phone Case Ultra")
                        .availableUnits(12).inboundUnits(0).reservedUnits(3)
                        .avgDailySales(0.8).daysOfSupply(11)
                        .status("CRITICAL").build()
        );

        List<RestockAlert> alerts = List.of();
        if (Boolean.TRUE.equals(request.getIncludeAlerts())) {
            alerts = List.of(
                    RestockAlert.builder()
                            .asin("B09ABC5678").title("Stainless Steel Water Bottle")
                            .daysOfSupply(17)
                            .recommendedReorderQuantity(500)
                            .urgency("HIGH")
                            .estimatedStockoutDate("17 days from now")
                            .build(),
                    RestockAlert.builder()
                            .asin("B09XYZ1234").title("Phone Case Ultra")
                            .daysOfSupply(11)
                            .recommendedReorderQuantity(100)
                            .urgency("CRITICAL")
                            .estimatedStockoutDate("11 days from now")
                            .build()
            );
        }

        return Response.builder()
                .items(items)
                .alerts(alerts)
                .overallStatus(alerts.isEmpty() ? "HEALTHY" : "WARNING")
                .summary(String.format("%d products tracked. %d healthy, %d need attention. %d alerts.",
                        items.size(),
                        items.stream().filter(i -> "HEALTHY".equals(i.getStatus())).count(),
                        items.stream().filter(i -> !"HEALTHY".equals(i.getStatus())).count(),
                        alerts.size()))
                .build();
    }
}
