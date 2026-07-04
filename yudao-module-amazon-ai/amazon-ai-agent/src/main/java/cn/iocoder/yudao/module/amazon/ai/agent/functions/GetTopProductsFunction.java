package cn.iocoder.yudao.module.amazon.ai.agent.functions;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderItemMapper;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import javax.annotation.Resource;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Function tool: Get top/bottom performing products by various metrics.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class GetTopProductsFunction implements Function<GetTopProductsFunction.Request, GetTopProductsFunction.Response> {

    @Resource
    private AmazonOrderItemMapper amazonOrderItemMapper;

    @Resource
    private AmazonOrderMapper amazonOrderMapper;

    @Resource
    private AmazonProductMapper amazonProductMapper;

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

        int limit = request.getLimit() != null ? request.getLimit() : 10;
        String metric = request.getMetric() != null ? request.getMetric() : "revenue";
        String sortOrder = request.getOrder() != null ? request.getOrder() : "desc";

        try {
            // 1. Query orders for the last 30 days to get order IDs
            LocalDate today = LocalDate.now();
            LocalDateTime thirtyDaysAgo = today.minusDays(30).atStartOfDay();
            LocalDateTime now = today.atTime(LocalTime.MAX);

            LambdaQueryWrapperX<AmazonOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
            orderWrapper.ge(AmazonOrderDO::getPurchaseDate, thirtyDaysAgo);
            orderWrapper.le(AmazonOrderDO::getPurchaseDate, now);
            orderWrapper.ne(AmazonOrderDO::getOrderStatus, "Canceled");
            List<AmazonOrderDO> orders = amazonOrderMapper.selectList(orderWrapper);
            log.info("Fetched {} orders for top products analysis", orders.size());

            if (orders.isEmpty()) {
                return Response.builder()
                        .metric(metric)
                        .order(sortOrder)
                        .products(Collections.emptyList())
                        .summary("No orders found in the last 30 days.")
                        .build();
            }

            List<Long> orderIds = new ArrayList<>();
            for (AmazonOrderDO order : orders) {
                orderIds.add(order.getId());
            }

            // 2. Query order items
            LambdaQueryWrapperX<AmazonOrderItemDO> itemWrapper = new LambdaQueryWrapperX<>();
            itemWrapper.in(AmazonOrderItemDO::getOrderId, orderIds);
            List<AmazonOrderItemDO> allItems = amazonOrderItemMapper.selectList(itemWrapper);
            log.info("Fetched {} order items for top products analysis", allItems.size());

            // 3. Group by ASIN, sum quantity and revenue
            Map<String, BigDecimal> asinRevenue = new HashMap<>();
            Map<String, Integer> asinUnits = new HashMap<>();

            for (AmazonOrderItemDO item : allItems) {
                String asin = item.getAsin();
                if (asin == null || asin.isEmpty()) {
                    continue;
                }

                BigDecimal itemRevenue = BigDecimal.ZERO;
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                if (item.getPrice() != null) {
                    itemRevenue = item.getPrice().multiply(BigDecimal.valueOf(qty));
                }

                BigDecimal current = asinRevenue.get(asin);
                asinRevenue.put(asin, (current != null ? current : BigDecimal.ZERO).add(itemRevenue));

                Integer currentUnits = asinUnits.get(asin);
                asinUnits.put(asin, (currentUnits != null ? currentUnits : 0) + qty);
            }

            if (asinRevenue.isEmpty()) {
                return Response.builder()
                        .metric(metric)
                        .order(sortOrder)
                        .products(Collections.emptyList())
                        .summary("No order items found in the last 30 days.")
                        .build();
            }

            // 4. Fetch product titles from AmazonProductMapper
            Map<String, String> asinTitles = new HashMap<>();
            List<String> asinList = new ArrayList<>(asinRevenue.keySet());
            LambdaQueryWrapperX<AmazonProductDO> productWrapper = new LambdaQueryWrapperX<>();
            productWrapper.in(AmazonProductDO::getAsin, asinList);
            List<AmazonProductDO> products = amazonProductMapper.selectList(productWrapper);
            for (AmazonProductDO product : products) {
                if (product.getAsin() != null && product.getTitle() != null) {
                    asinTitles.put(product.getAsin(), product.getTitle());
                }
            }
            log.info("Fetched {} product titles for ASIN enrichment", asinTitles.size());

            // 5. Build ProductPerformance list
            List<ProductPerformance> performances = new ArrayList<>();
            BigDecimal totalPortfolioRevenue = BigDecimal.ZERO;

            for (Map.Entry<String, BigDecimal> entry : asinRevenue.entrySet()) {
                String asin = entry.getKey();
                BigDecimal revenue = entry.getValue();
                int units = asinUnits.get(asin) != null ? asinUnits.get(asin) : 0;
                String title = asinTitles.get(asin);
                if (title == null) {
                    title = asin;
                }

                totalPortfolioRevenue = totalPortfolioRevenue.add(revenue);

                performances.add(ProductPerformance.builder()
                        .asin(asin)
                        .title(title)
                        .revenue(Math.round(revenue.doubleValue() * 100.0) / 100.0)
                        .units(units)
                        .profit(null)
                        .conversionRate(null)
                        .trend(null)
                        .build());
            }

            // 6. Sort by metric
            Comparator<ProductPerformance> comparator;
            if ("units".equals(metric)) {
                comparator = new Comparator<ProductPerformance>() {
                    @Override
                    public int compare(ProductPerformance a, ProductPerformance b) {
                        return Integer.compare(a.getUnits(), b.getUnits());
                    }
                };
            } else {
                // Default: sort by revenue
                comparator = new Comparator<ProductPerformance>() {
                    @Override
                    public int compare(ProductPerformance a, ProductPerformance b) {
                        return Double.compare(a.getRevenue(), b.getRevenue());
                    }
                };
            }

            if ("asc".equals(sortOrder)) {
                Collections.sort(performances, comparator);
            } else {
                Collections.sort(performances, Collections.reverseOrder(comparator));
            }

            // 7. Apply limit
            int resultSize = Math.min(limit, performances.size());
            List<ProductPerformance> topProducts = performances.subList(0, resultSize);

            return Response.builder()
                    .metric(metric)
                    .order(sortOrder)
                    .products(topProducts)
                    .summary(String.format("Top %d products by %s. Total portfolio revenue: $%.2f across %d active ASINs.",
                            resultSize, metric, totalPortfolioRevenue.doubleValue(), asinRevenue.size()))
                    .build();

        } catch (Exception e) {
            log.error("getTopProducts failed: {}", e.getMessage(), e);
            return Response.builder()
                    .metric(metric)
                    .order(sortOrder)
                    .products(Collections.emptyList())
                    .summary("Error fetching top products: " + e.getMessage())
                    .build();
        }
    }
}
