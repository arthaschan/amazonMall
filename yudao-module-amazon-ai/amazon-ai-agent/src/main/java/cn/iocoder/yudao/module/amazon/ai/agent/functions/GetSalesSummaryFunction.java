package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

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

        // TODO: Inject and query the actual order service:
        //   orderService.getSalesSummary(tenantId, dateRange, asin)
        //
        // For now, return a structured placeholder that demonstrates the contract.
        // In production, this queries amazon_order tables for real data.

        return Response.builder()
                .dateRange(request.getDateRange() != null ? request.getDateRange() : "last 30 days")
                .totalRevenue(12500.00)
                .totalUnits(250)
                .totalOrders(180)
                .avgOrderValue(69.44)
                .conversionRate(12.5)
                .comparisonToPreviousPeriod("Revenue up 8.3% vs previous period. Units up 5.1%.")
                .topPerformer("B08N5WRWNW - Wireless Earbuds ($4,200 revenue, 84 units)")
                .underperformer("B09XYZ1234 - Phone Case ($320 revenue, 16 units, declining)")
                .build();
    }
}
