package cn.iocoder.yudao.module.amazon.report.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.WeeklyReportPageReqVO;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonWeeklyReportDO;
import cn.iocoder.yudao.module.amazon.report.dal.mysql.AmazonWeeklyReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 周报 Service 实现。
 * <p>自动汇总店铺经营数据，生成 AI 摘要和建议。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AiWeeklyReportServiceImpl implements AiWeeklyReportService {

    @Resource
    private AmazonWeeklyReportMapper reportMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public AmazonWeeklyReportDO generateReport(Long shopId, String reportWeek) {
        // 检查是否已存在已完成报告
        AmazonWeeklyReportDO existing = reportMapper.selectByWeek(shopId, reportWeek);
        if (existing != null && existing.getStatus() == 1) {
            return existing;
        }

        AmazonWeeklyReportDO report = existing != null ? existing : new AmazonWeeklyReportDO();
        report.setShopId(shopId);
        report.setTenantId(0L);
        report.setReportWeek(reportWeek);
        report.setStatus(0); // 生成中

        // 解析周起止日期
        LocalDate startDate;
        LocalDate endDate;
        try {
            String[] parts = reportWeek.split("-W");
            int year = Integer.parseInt(parts[0]);
            int weekNum = Integer.parseInt(parts[1]);
            // ISO week 1 always contains Jan 4
            LocalDate jan4 = LocalDate.of(year, 1, 4);
            int dayOfWeek = jan4.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
            LocalDate week1Monday = jan4.minusDays(dayOfWeek - 1);
            startDate = week1Monday.plusWeeks(weekNum - 1);
            endDate = startDate.plusDays(6);
        } catch (Exception e) {
            log.error("Failed to parse reportWeek: {}", reportWeek, e);
            startDate = LocalDate.now().minusDays(7);
            endDate = LocalDate.now();
        }

        // 查询真实数据
        AmazonWeeklyReportDO.ReportData data = new AmazonWeeklyReportDO.ReportData();
        double totalSales = 0.0;
        int totalOrders = 0;
        double adSpend = 0.0;
        int newReviews = 0;
        double avgRating = 0.0;
        int inventoryAlerts = 0;

        // totalSales + totalOrders
        try {
            Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(order_total),0) as total_sales, " +
                    "COUNT(CASE WHEN status != 'Canceled' THEN 1 END) as total_orders " +
                    "FROM amazon_order WHERE shop_id=? AND deleted=0 " +
                    "AND DATE(purchase_date) BETWEEN ? AND ?",
                    shopId, startDate, endDate);
            totalSales = toDouble(orderRow.get("total_sales"));
            totalOrders = toInt(orderRow.get("total_orders"));
        } catch (Exception e) {
            log.warn("Failed to query order stats for shopId={}, week={}", shopId, reportWeek, e);
        }

        // adSpend
        try {
            Map<String, Object> adRow = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(cost),0) as cost " +
                    "FROM amazon_ad_report_daily WHERE shop_id=? " +
                    "AND report_date BETWEEN ? AND ?",
                    shopId, startDate, endDate);
            adSpend = toDouble(adRow.get("cost"));
        } catch (Exception e) {
            log.warn("Failed to query ad stats for shopId={}, week={}", shopId, reportWeek, e);
        }

        // newReviews + avgRating
        try {
            Map<String, Object> reviewRow = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) as cnt, COALESCE(AVG(rating),0) as avg_rating " +
                    "FROM amazon_review WHERE shop_id=? AND deleted=0 " +
                    "AND DATE(create_time) BETWEEN ? AND ?",
                    shopId, startDate, endDate);
            newReviews = toInt(reviewRow.get("cnt"));
            avgRating = toDouble(reviewRow.get("avg_rating"));
        } catch (Exception e) {
            log.warn("Failed to query review stats for shopId={}, week={}", shopId, reportWeek, e);
        }

        // inventoryAlerts
        try {
            Map<String, Object> alertRow = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) as cnt FROM amazon_replenish_alert " +
                    "WHERE shop_id=? AND status='UNACKNOWLEDGED'",
                    shopId);
            inventoryAlerts = toInt(alertRow.get("cnt"));
        } catch (Exception e) {
            log.warn("Failed to query inventory alerts for shopId={}, week={} (table may not exist)",
                    shopId, reportWeek, e);
            inventoryAlerts = 0;
        }

        // profit: estimated 30% margin minus ad spend
        double profit = totalSales * 0.30 - adSpend;

        // conversionRate: placeholder (sessions not tracked)
        double conversionRate = (totalOrders / 100.0) * 100.0;

        data.setTotalSales(totalSales);
        data.setTotalOrders(totalOrders);
        data.setAdSpend(adSpend);
        data.setProfit(profit);
        data.setConversionRate(conversionRate);
        data.setNewReviews(newReviews);
        data.setAvgRating(avgRating);
        data.setInventoryAlerts(inventoryAlerts);
        report.setReportData(data);

        // 生成 AI 摘要
        String summary = generateAiSummary(totalSales, totalOrders, adSpend, avgRating, newReviews, reportWeek);
        report.setAiSummary(summary);

        // 生成 AI 建议
        List<String> recommendations = generateRecommendations(totalSales, totalOrders, adSpend, avgRating, inventoryAlerts);
        report.setAiRecommendations(recommendations);

        report.setStatus(1); // 已完成

        if (existing != null) {
            reportMapper.updateById(report);
        } else {
            reportMapper.insert(report);
        }

        return report;
    }

    @Override
    public AmazonWeeklyReportDO getReport(Long shopId, String reportWeek) {
        return reportMapper.selectByWeek(shopId, reportWeek);
    }

    @Override
    public PageResult<AmazonWeeklyReportDO> getReportPage(WeeklyReportPageReqVO reqVO) {
        return reportMapper.selectPage(reqVO);
    }

    // ======================== AI summary generation ========================

    private String generateAiSummary(double totalSales, int totalOrders, double adSpend,
                                     double avgRating, int newReviews, String reportWeek) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s 周报：本周销售额 %.2f ，共 %d 笔订单。",
                reportWeek, totalSales, totalOrders));

        if (totalSales > 0 && adSpend > totalSales * 0.2) {
            double adRatio = adSpend / totalSales * 100.0;
            sb.append(String.format("广告花费占比 %.1f%% ，投入偏高，需关注投产比。", adRatio));
        }

        if (avgRating > 0 && avgRating < 4.0) {
            sb.append(String.format("平均评分 %.1f 星，低于4星警戒线，建议重点跟进差评产品。", avgRating));
        }

        if (newReviews > 0) {
            sb.append(String.format("本周新增 %d 条评论。", newReviews));
        }

        if (totalOrders == 0) {
            sb.append("本周暂无订单，建议检查Listing流量和广告投放情况。");
        }

        return sb.toString();
    }

    private List<String> generateRecommendations(double totalSales, int totalOrders,
                                                  double adSpend, double avgRating,
                                                  int inventoryAlerts) {
        List<String> list = new ArrayList<>();

        if (totalSales > 0 && adSpend > 0) {
            double adRatio = adSpend / totalSales;
            if (adRatio > 0.2) {
                list.add(String.format("广告花费占比偏高(%.0f%%)，建议优化关键词竞价或否定低效搜索词", adRatio * 100));
            }
        }

        if (avgRating > 0 && avgRating < 4.0) {
            list.add(String.format("平均评分偏低(%.1f星)，建议关注差评反馈的产品问题并改进", avgRating));
        }

        if (inventoryAlerts > 0) {
            list.add(String.format("有%d个补货预警待处理，建议及时补货避免断货", inventoryAlerts));
        }

        if (totalOrders < 10) {
            list.add(String.format("本周订单量较少(%d单)，建议加大广告投入或优化Listing", totalOrders));
        }

        list.add("建议持续关注竞品动态和市场趋势变化");

        return list;
    }

    // ======================== number helpers ========================

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

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
