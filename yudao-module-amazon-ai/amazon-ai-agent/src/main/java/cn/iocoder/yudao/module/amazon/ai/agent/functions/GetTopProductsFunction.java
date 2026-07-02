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
 * Function tool: Get top/bottom performing products by various metrics.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetTopProductsFunction implements Function<GetTopProductsFunction.Request, GetTopProductsFunction.Response> {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        /** Metric to sort by: "revenue", "units", "profit", "conversion" */
        private String metric;
        /** Number of products to return */
        private Integer limit;
        /** Sort order: "asc" (worst first) or "desc" (best first) */
        private String order;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPerformance {
        private String asin;
        private String title;
        private Double revenue;
        private Integer units;
        private Double profit;
        private Double conversionRate;
        private String trend; // up, down, stable
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String metric;
        private String order;
        private List<ProductPerformance> products;
        private String summary;
    }

    @Override
    public Response apply(Request request) {
        log.info("getTopProducts called: metric={}, limit={}, order={}",
                request.getMetric(), request.getLimit(), request.getOrder());

        // TODO: Inject and query the actual listing/order service:
        //   listingService.getProductPerformance(tenantId, metric, limit, order)

        int limit = request.getLimit() != null ? request.getLimit() : 5;
        String metric = request.getMetric() != null ? request.getMetric() : "revenue";

        List<ProductPerformance> products = List.of(
                ProductPerformance.builder()
                        .asin("B08N5WRWNW").title("Wireless Earbuds Pro")
                        .revenue(4200.0).units(84).profit(1260.0)
                        .conversionRate(15.2).trend("up").build(),
                ProductPerformance.builder()
                        .asin("B09ABC5678").title("Stainless Steel Water Bottle")
                        .revenue(3800.0).units(95).profit(1140.0)
                        .conversionRate(13.8).trend("stable").build(),
                ProductPerformance.builder()
                        .asin("B09XYZ1234").title("Phone Case Ultra")
                        .revenue(320.0).units(16).profit(48.0)
                        .conversionRate(4.2).trend("down").build()
        );

        return Response.builder()
                .metric(metric)
                .order(request.getOrder() != null ? request.getOrder() : "desc")
                .products(products.subList(0, Math.min(limit, products.size())))
                .summary(String.format("Top %d products by %s. Total portfolio revenue: $8,320 across %d active ASINs.",
                        Math.min(limit, products.size()), metric, products.size()))
                .build();
    }
}
