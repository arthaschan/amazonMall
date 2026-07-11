package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdCampaignMapper;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdReportDailyMapper;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdSearchTermMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 广告优化服务实现。
 * <p>分析广告报表数据，生成竞价调整、否定关键词、预算调整等优化建议。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AiAdOptimizerServiceImpl implements AiAdOptimizerService {

    @Resource
    private AmazonAdReportDailyMapper reportDailyMapper;

    @Resource
    private AmazonAdCampaignMapper campaignMapper;

    @Resource
    private AmazonAdSearchTermMapper searchTermMapper;

    @Override
    public Map<String, Object> analyzeAndSuggest(Long shopId) {
        // 1. 查询近30天广告数据
        List<AmazonAdReportDailyDO> reports = reportDailyMapper.selectList(
                new LambdaQueryWrapperX<AmazonAdReportDailyDO>()
                        .eq(AmazonAdReportDailyDO::getShopId, shopId)
                        .ge(AmazonAdReportDailyDO::getReportDate, LocalDate.now().minusDays(30))
        );

        // 2. 无数据直接返回
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (reports == null || reports.isEmpty()) {
            result.put("summary", "暂无广告数据");
            return result;
        }

        // 3. 计算汇总指标
        BigDecimal totalSpend = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;
        long totalClicks = 0L;
        long totalImpressions = 0L;

        for (AmazonAdReportDailyDO r : reports) {
            if (r.getCost() != null) {
                totalSpend = totalSpend.add(r.getCost());
            }
            if (r.getSales() != null) {
                totalSales = totalSales.add(r.getSales());
            }
            if (r.getClicks() != null) {
                totalClicks += r.getClicks();
            }
            if (r.getImpressions() != null) {
                totalImpressions += r.getImpressions();
            }
        }

        BigDecimal overallACoS = BigDecimal.ZERO;
        if (totalSales.compareTo(BigDecimal.ZERO) > 0) {
            overallACoS = totalSpend.multiply(new BigDecimal("100"))
                    .divide(totalSales, 2, RoundingMode.HALF_UP);
        }

        BigDecimal avgCTR = BigDecimal.ZERO;
        if (totalImpressions > 0) {
            avgCTR = new BigDecimal(totalClicks).multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(totalImpressions), 2, RoundingMode.HALF_UP);
        }

        // 4. 按关键词聚合
        Map<String, KeywordMetrics> keywordMap = new HashMap<String, KeywordMetrics>();
        for (AmazonAdReportDailyDO r : reports) {
            String kw = r.getKeywordText();
            if (kw == null || kw.isEmpty()) {
                continue;
            }
            KeywordMetrics km = keywordMap.get(kw);
            if (km == null) {
                km = new KeywordMetrics();
                keywordMap.put(kw, km);
            }
            if (r.getCost() != null) {
                km.cost = km.cost.add(r.getCost());
            }
            if (r.getSales() != null) {
                km.sales = km.sales.add(r.getSales());
            }
            if (r.getClicks() != null) {
                km.clicks += r.getClicks();
            }
            if (r.getImpressions() != null) {
                km.impressions += r.getImpressions();
            }
            if (r.getOrders() != null) {
                km.orders += r.getOrders();
            }
        }

        // 5. 高花费无转化 → 否定关键词
        List<String> negativeKeywords = new ArrayList<String>();
        // 6. 高ACoS → 降低竞价
        List<Map<String, Object>> bidDown = new ArrayList<Map<String, Object>>();
        // 7. 低ACoS → 提高竞价
        List<Map<String, Object>> bidUp = new ArrayList<Map<String, Object>>();

        for (Map.Entry<String, KeywordMetrics> entry : keywordMap.entrySet()) {
            String kw = entry.getKey();
            KeywordMetrics km = entry.getValue();

            // 高花费无转化: cost > 5 AND orders == 0
            if (km.cost.compareTo(new BigDecimal("5")) > 0 && km.orders == 0) {
                negativeKeywords.add(kw);
            }

            // 计算关键词级别 ACoS
            BigDecimal kwAcos = BigDecimal.ZERO;
            if (km.sales.compareTo(BigDecimal.ZERO) > 0) {
                kwAcos = km.cost.multiply(new BigDecimal("100"))
                        .divide(km.sales, 2, RoundingMode.HALF_UP);
            }

            // 高ACoS: acos > 40 AND orders > 0
            if (kwAcos.compareTo(new BigDecimal("40")) > 0 && km.orders > 0) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("keyword", kw);
                item.put("currentAcos", kwAcos);
                item.put("suggestedChange", "-15%");
                bidDown.add(item);
            }

            // 低ACoS: acos < 20 AND acos > 0 AND orders >= 3
            if (kwAcos.compareTo(new BigDecimal("20")) < 0
                    && kwAcos.compareTo(BigDecimal.ZERO) > 0
                    && km.orders >= 3) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("keyword", kw);
                item.put("currentAcos", kwAcos);
                item.put("suggestedChange", "+10%");
                bidUp.add(item);
            }
        }

        // 合并竞价调整建议
        List<Map<String, Object>> bidAdjustments = new ArrayList<Map<String, Object>>();
        bidAdjustments.addAll(bidDown);
        bidAdjustments.addAll(bidUp);

        // 8. 活动级别预算建议
        List<AmazonAdCampaignDO> campaigns = campaignMapper.selectList(
                new LambdaQueryWrapperX<AmazonAdCampaignDO>()
                        .eq(AmazonAdCampaignDO::getShopId, shopId)
        );

        // 按 campaignId 聚合报表数据
        Map<Long, CampaignMetrics> campaignMetricsMap = new HashMap<Long, CampaignMetrics>();
        for (AmazonAdReportDailyDO r : reports) {
            Long cid = r.getCampaignId();
            if (cid == null) {
                continue;
            }
            CampaignMetrics cm = campaignMetricsMap.get(cid);
            if (cm == null) {
                cm = new CampaignMetrics();
                campaignMetricsMap.put(cid, cm);
            }
            if (r.getCost() != null) {
                cm.spend = cm.spend.add(r.getCost());
            }
            if (r.getSales() != null) {
                cm.sales = cm.sales.add(r.getSales());
            }
        }

        List<Map<String, Object>> budgetRecommendations = new ArrayList<Map<String, Object>>();
        for (AmazonAdCampaignDO campaign : campaigns) {
            CampaignMetrics cm = campaignMetricsMap.get(campaign.getCampaignId());
            if (cm == null) {
                continue;
            }
            BigDecimal campAcos = BigDecimal.ZERO;
            if (cm.sales.compareTo(BigDecimal.ZERO) > 0) {
                campAcos = cm.spend.multiply(new BigDecimal("100"))
                        .divide(cm.sales, 2, RoundingMode.HALF_UP);
            }

            // 高ACoS活动: acos > 40 → 减少预算
            if (campAcos.compareTo(new BigDecimal("40")) > 0) {
                Map<String, Object> rec = new HashMap<String, Object>();
                rec.put("campaignId", campaign.getCampaignId());
                rec.put("campaignName", campaign.getCampaignName());
                rec.put("currentAcos", campAcos);
                rec.put("currentBudget", campaign.getDailyBudget());
                rec.put("suggestedChange", "-20%");
                rec.put("reason", "HIGH_ACoS_CAMPAIGN");
                budgetRecommendations.add(rec);
            }

            // 低ACoS活动: acos < 15 AND spend > 0 → 增加预算
            if (campAcos.compareTo(new BigDecimal("15")) < 0
                    && cm.spend.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> rec = new HashMap<String, Object>();
                rec.put("campaignId", campaign.getCampaignId());
                rec.put("campaignName", campaign.getCampaignName());
                rec.put("currentAcos", campAcos);
                rec.put("currentBudget", campaign.getDailyBudget());
                rec.put("suggestedChange", "+20%");
                rec.put("reason", "LOW_ACoS_CAMPAIGN");
                budgetRecommendations.add(rec);
            }
        }

        // 9. 构建结果
        String summary = String.format(
                "近30天广告汇总: 花费 %.2f, 销售额 %.2f, ACoS %.2f%%, 点击 %d, 曝光 %d。"
                        + " 发现 %d 个否定关键词, %d 个竞价调整建议, %d 个预算调整建议。",
                totalSpend.doubleValue(), totalSales.doubleValue(), overallACoS.doubleValue(),
                totalClicks, totalImpressions,
                negativeKeywords.size(), bidAdjustments.size(), budgetRecommendations.size()
        );

        Map<String, Object> metrics = new HashMap<String, Object>();
        metrics.put("totalSpend", totalSpend);
        metrics.put("totalSales", totalSales);
        metrics.put("overallACoS", overallACoS);
        metrics.put("totalClicks", totalClicks);
        metrics.put("totalImpressions", totalImpressions);

        result.put("summary", summary);
        result.put("bidAdjustments", bidAdjustments);
        result.put("negativeKeywords", negativeKeywords);
        result.put("budgetRecommendations", budgetRecommendations);
        result.put("metrics", metrics);

        return result;
    }

    @Override
    public void autoOptimize(Long shopId) {
        Map<String, Object> analysis = analyzeAndSuggest(shopId);

        log.info("[autoOptimize] shopId={}, summary={}", shopId, analysis.get("summary"));

        List<String> negativeKeywords = castList(analysis.get("negativeKeywords"));
        if (negativeKeywords != null && !negativeKeywords.isEmpty()) {
            log.info("[autoOptimize] 否定关键词建议 ({}个): {}", negativeKeywords.size(), negativeKeywords);
        }

        List<?> bidAdjustments = castList(analysis.get("bidAdjustments"));
        if (bidAdjustments != null && !bidAdjustments.isEmpty()) {
            log.info("[autoOptimize] 竞价调整建议 ({}个): {}", bidAdjustments.size(), bidAdjustments);
        }

        List<?> budgetRecommendations = castList(analysis.get("budgetRecommendations"));
        if (budgetRecommendations != null && !budgetRecommendations.isEmpty()) {
            log.info("[autoOptimize] 预算调整建议 ({}个): {}", budgetRecommendations.size(), budgetRecommendations);
        }

        int totalSuggestions = 0;
        if (negativeKeywords != null) {
            totalSuggestions += negativeKeywords.size();
        }
        if (bidAdjustments != null) {
            totalSuggestions += bidAdjustments.size();
        }
        if (budgetRecommendations != null) {
            totalSuggestions += budgetRecommendations.size();
        }

        log.info("自动优化建议已生成，共 {} 条建议待审核执行", totalSuggestions);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object obj) {
        if (obj instanceof List) {
            return (List<T>) obj;
        }
        return null;
    }

    /**
     * 关键词聚合指标内部类
     */
    private static class KeywordMetrics {
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
        long clicks = 0L;
        long impressions = 0L;
        int orders = 0;
    }

    /**
     * 活动聚合指标内部类
     */
    private static class CampaignMetrics {
        BigDecimal spend = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
    }
}
