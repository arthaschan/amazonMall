package cn.iocoder.yudao.module.amazon.report.service;

import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.AiChatRespVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 数据对话实现。
 * <p>将用户自然语言问题转换为数据查询，并生成可读回答。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private AmazonShopService amazonShopService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiChatRespVO chat(Long shopId, String question) {
        AiChatRespVO resp = new AiChatRespVO();
        if (question == null || question.trim().isEmpty()) {
            resp.setAnswer("请输入您想查询的问题。");
            resp.setDataReference(null);
            return resp;
        }

        String q = question.toLowerCase();
        Map<String, Object> data;
        String answer;

        if (containsAny(q, "sales", "revenue", "收入", "销售")) {
            data = queryOrderStats(shopId);
            answer = formatOrderAnswer(data);
        } else if (containsAny(q, "order", "订单", "单量")) {
            data = queryOrderStats(shopId);
            answer = formatOrderAnswer(data);
        } else if (containsAny(q, "ad", "广告", "acos", "spend", "花费")) {
            data = queryAdStats(shopId);
            answer = formatAdAnswer(data);
        } else if (containsAny(q, "review", "评论", "评价", "rating", "星级")) {
            data = queryReviewStats(shopId);
            answer = formatReviewAnswer(data);
        } else if (containsAny(q, "inventory", "库存", "stock", "补货")) {
            data = queryInventoryStats(shopId);
            answer = formatInventoryAnswer(data);
        } else if (containsAny(q, "profit", "利润", "margin")) {
            data = queryProfitStats(shopId);
            answer = formatProfitAnswer(data);
        } else if (containsAny(q, "shop", "店铺", "store")) {
            data = queryShopInfo(shopId);
            answer = formatShopAnswer(data);
        } else {
            data = queryOverview(shopId);
            answer = formatOverviewAnswer(data);
        }

        resp.setAnswer(answer);
        try {
            resp.setDataReference(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.warn("Failed to serialize data reference", e);
            resp.setDataReference("{}");
        }
        return resp;
    }

    // ======================== keyword helper ========================

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // ======================== query methods ========================

    private Map<String, Object> queryOrderStats(Long shopId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> month = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) as cnt, COALESCE(SUM(order_total),0) as total " +
                    "FROM amazon_order WHERE shop_id=? AND deleted=0 " +
                    "AND purchase_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                    shopId);
            result.put("month_orders", toLong(month.get("cnt")));
            result.put("month_sales", toDouble(month.get("total")));
        } catch (Exception e) {
            log.warn("queryOrderStats month failed for shopId={}", shopId, e);
        }
        try {
            Map<String, Object> today = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) as cnt FROM amazon_order " +
                    "WHERE shop_id=? AND deleted=0 AND DATE(purchase_date)=CURDATE()",
                    shopId);
            result.put("today_orders", toLong(today.get("cnt")));
        } catch (Exception e) {
            log.warn("queryOrderStats today failed for shopId={}", shopId, e);
        }
        return result;
    }

    private Map<String, Object> queryAdStats(Long shopId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(cost),0) as cost, COALESCE(SUM(sales),0) as sales, " +
                    "COALESCE(SUM(clicks),0) as clicks, COALESCE(SUM(impressions),0) as imp " +
                    "FROM amazon_ad_report_daily WHERE shop_id=? " +
                    "AND report_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                    shopId);
            double cost = toDouble(row.get("cost"));
            double sales = toDouble(row.get("sales"));
            long clicks = toLong(row.get("clicks"));
            long impressions = toLong(row.get("imp"));
            double acos = sales > 0 ? (cost / sales * 100.0) : 0.0;
            double ctr = impressions > 0 ? (clicks * 100.0 / impressions) : 0.0;
            double cpc = clicks > 0 ? (cost / clicks) : 0.0;

            result.put("cost", cost);
            result.put("sales", sales);
            result.put("clicks", clicks);
            result.put("impressions", impressions);
            result.put("acos", acos);
            result.put("ctr", ctr);
            result.put("cpc", cpc);
        } catch (Exception e) {
            log.warn("queryAdStats failed for shopId={}", shopId, e);
        }
        return result;
    }

    private Map<String, Object> queryReviewStats(Long shopId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) as cnt, COALESCE(AVG(rating),0) as avg_rating " +
                    "FROM amazon_review WHERE shop_id=? AND deleted=0",
                    shopId);
            result.put("review_count", toLong(row.get("cnt")));
            result.put("avg_rating", toDouble(row.get("avg_rating")));
        } catch (Exception e) {
            log.warn("queryReviewStats failed for shopId={}", shopId, e);
        }
        return result;
    }

    private Map<String, Object> queryInventoryStats(Long shopId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT COUNT(DISTINCT asin) as asin_count " +
                    "FROM amazon_inventory WHERE shop_id=? AND deleted=0 AND quantity_available > 0",
                    shopId);
            result.put("in_stock_asin_count", toLong(row.get("asin_count")));
        } catch (Exception e) {
            log.warn("queryInventoryStats failed for shopId={}", shopId, e);
        }
        return result;
    }

    private Map<String, Object> queryProfitStats(Long shopId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(order_total),0) as total " +
                    "FROM amazon_order WHERE shop_id=? AND deleted=0 " +
                    "AND purchase_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                    shopId);
            double totalSales = toDouble(orderRow.get("total"));
            result.put("total_sales", totalSales);

            double estimatedMargin = totalSales * 0.30;
            result.put("estimated_margin", estimatedMargin);

            double adCost = 0.0;
            try {
                Map<String, Object> adRow = jdbcTemplate.queryForMap(
                        "SELECT COALESCE(SUM(cost),0) as cost " +
                        "FROM amazon_ad_report_daily WHERE shop_id=? " +
                        "AND report_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                        shopId);
                adCost = toDouble(adRow.get("cost"));
            } catch (Exception ex) {
                log.warn("queryProfitStats ad cost failed for shopId={}", shopId, ex);
            }
            result.put("ad_cost", adCost);
            result.put("estimated_profit", estimatedMargin - adCost);
        } catch (Exception e) {
            log.warn("queryProfitStats failed for shopId={}", shopId, e);
        }
        return result;
    }

    private Map<String, Object> queryShopInfo(Long shopId) {
        Map<String, Object> result = new HashMap<>();
        try {
            AmazonShopDO shop = amazonShopService.getShopById(shopId);
            if (shop != null) {
                result.put("shop_name", shop.getShopName());
                result.put("marketplace_id", shop.getMarketplaceId());
                result.put("country_code", shop.getCountryCode());
                result.put("seller_id", shop.getSellerId());
                result.put("status", shop.getStatus());
            }
        } catch (Exception e) {
            log.warn("queryShopInfo failed for shopId={}", shopId, e);
        }
        return result;
    }

    private Map<String, Object> queryOverview(Long shopId) {
        Map<String, Object> result = new HashMap<>();
        // order stats
        try {
            Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) as cnt, COALESCE(SUM(order_total),0) as total " +
                    "FROM amazon_order WHERE shop_id=? AND deleted=0 " +
                    "AND purchase_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                    shopId);
            result.put("month_orders", toLong(orderRow.get("cnt")));
            result.put("month_sales", toDouble(orderRow.get("total")));
        } catch (Exception e) {
            log.warn("queryOverview order failed for shopId={}", shopId, e);
        }
        // ad stats
        try {
            Map<String, Object> adRow = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(cost),0) as cost, COALESCE(SUM(sales),0) as sales " +
                    "FROM amazon_ad_report_daily WHERE shop_id=? " +
                    "AND report_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                    shopId);
            result.put("ad_cost", toDouble(adRow.get("cost")));
            result.put("ad_sales", toDouble(adRow.get("sales")));
        } catch (Exception e) {
            log.warn("queryOverview ad failed for shopId={}", shopId, e);
        }
        // review stats
        try {
            Map<String, Object> reviewRow = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) as cnt, COALESCE(AVG(rating),0) as avg_rating " +
                    "FROM amazon_review WHERE shop_id=? AND deleted=0",
                    shopId);
            result.put("review_count", toLong(reviewRow.get("cnt")));
            result.put("avg_rating", toDouble(reviewRow.get("avg_rating")));
        } catch (Exception e) {
            log.warn("queryOverview review failed for shopId={}", shopId, e);
        }
        return result;
    }

    // ======================== format methods ========================

    private String formatOrderAnswer(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "暂未查询到订单数据，请确认店铺已同步订单。";
        }
        long monthOrders = toLong(data.get("month_orders"));
        double monthSales = toDouble(data.get("month_sales"));
        long todayOrders = toLong(data.get("today_orders"));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("近30天订单概况：累计 %d 单，销售总额 %.2f 。", monthOrders, monthSales));
        sb.append(String.format("今日新增订单 %d 单。", todayOrders));
        if (monthOrders > 0) {
            sb.append(String.format("客单价约 %.2f 。", monthSales / monthOrders));
        }
        return sb.toString();
    }

    private String formatAdAnswer(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "暂未查询到广告数据，请确认广告报表已同步。";
        }
        double cost = toDouble(data.get("cost"));
        double sales = toDouble(data.get("sales"));
        long clicks = toLong(data.get("clicks"));
        long impressions = toLong(data.get("impressions"));
        double acos = toDouble(data.get("acos"));
        double ctr = toDouble(data.get("ctr"));
        double cpc = toDouble(data.get("cpc"));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("近30天广告概况：花费 %.2f ，广告销售额 %.2f ，ACOS %.1f%% 。", cost, sales, acos));
        sb.append(String.format("展示量 %d 次，点击量 %d 次，点击率(CTR) %.2f%% ，平均点击花费(CPC) %.2f 。",
                impressions, clicks, ctr, cpc));
        if (acos > 30) {
            sb.append("ACOS偏高，建议优化关键词竞价或否定低效搜索词。");
        }
        return sb.toString();
    }

    private String formatReviewAnswer(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "暂未查询到评论数据，请确认评论已同步。";
        }
        long reviewCount = toLong(data.get("review_count"));
        double avgRating = toDouble(data.get("avg_rating"));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("评论概况：共 %d 条评论，平均评分 %.1f 星。", reviewCount, avgRating));
        if (avgRating < 4.0 && reviewCount > 0) {
            sb.append("平均评分偏低，建议关注差评反馈并改进产品质量。");
        } else if (avgRating >= 4.5) {
            sb.append("评分表现优秀，继续保持！");
        }
        return sb.toString();
    }

    private String formatInventoryAnswer(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "暂未查询到库存数据，请确认库存已同步。";
        }
        long asinCount = toLong(data.get("in_stock_asin_count"));
        return String.format("当前有库存的ASIN数量：%d 个。如需查看缺货明细，请使用库存管理模块。", asinCount);
    }

    private String formatProfitAnswer(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "暂未查询到足够数据来计算利润估算。";
        }
        double totalSales = toDouble(data.get("total_sales"));
        double estimatedMargin = toDouble(data.get("estimated_margin"));
        double adCost = toDouble(data.get("ad_cost"));
        double estimatedProfit = toDouble(data.get("estimated_profit"));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("近30天利润估算(按30%%毛利率)：销售总额 %.2f ，预估毛利 %.2f ，广告花费 %.2f ，预估净利润 %.2f 。",
                totalSales, estimatedMargin, adCost, estimatedProfit));
        if (estimatedProfit < 0) {
            sb.append("当前预估净利润为负，建议检查广告投入产出比和产品定价策略。");
        }
        return sb.toString();
    }

    private String formatShopAnswer(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "未查询到店铺信息，请确认店铺ID正确。";
        }
        String shopName = String.valueOf(data.get("shop_name"));
        String marketplaceId = String.valueOf(data.get("marketplace_id"));
        String countryCode = String.valueOf(data.get("country_code"));
        String sellerId = String.valueOf(data.get("seller_id"));
        Object statusObj = data.get("status");
        String statusText;
        if (statusObj != null) {
            int status = ((Number) statusObj).intValue();
            switch (status) {
                case 0: statusText = "已禁用"; break;
                case 1: statusText = "正常"; break;
                case 2: statusText = "授权过期"; break;
                case 3: statusText = "授权中"; break;
                default: statusText = "未知"; break;
            }
        } else {
            statusText = "未知";
        }
        return String.format("店铺信息：%s | 站点: %s (%s) | 卖家ID: %s | 状态: %s",
                shopName, countryCode, marketplaceId, sellerId, statusText);
    }

    private String formatOverviewAnswer(Map<String, Object> data) {
        if (data.isEmpty()) {
            return "暂未查询到经营数据，请确认各模块数据已同步。";
        }
        long monthOrders = toLong(data.get("month_orders"));
        double monthSales = toDouble(data.get("month_sales"));
        double adCost = toDouble(data.get("ad_cost"));
        double adSales = toDouble(data.get("ad_sales"));
        long reviewCount = toLong(data.get("review_count"));
        double avgRating = toDouble(data.get("avg_rating"));

        StringBuilder sb = new StringBuilder();
        sb.append("【经营概览 - 近30天】\n");
        sb.append(String.format("订单：%d 单，销售额：%.2f\n", monthOrders, monthSales));

        if (adCost > 0 || adSales > 0) {
            double acos = adSales > 0 ? (adCost / adSales * 100.0) : 0.0;
            sb.append(String.format("广告：花费 %.2f ，广告销售额 %.2f ，ACOS %.1f%%\n", adCost, adSales, acos));
        }

        if (reviewCount > 0) {
            sb.append(String.format("评论：共 %d 条，平均 %.1f 星\n", reviewCount, avgRating));
        }

        double estimatedProfit = monthSales * 0.30 - adCost;
        sb.append(String.format("预估净利润(30%%毛利-广告)：%.2f", estimatedProfit));

        return sb.toString();
    }

    // ======================== number helpers ========================

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
