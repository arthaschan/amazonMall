package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdRuleDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdCampaignMapper;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdReportDailyMapper;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdRuleMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 广告规则引擎。
 * <p>扫描所有启用的广告自动化规则，解析条件 JSON，评估广告报表数据，满足条件时执行动作（当前仅记录日志，实际调价需对接 Amazon Ads API）。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AdRuleEngine {

    /** 回溯天数：取最近 7 天报表数据做聚合判断 */
    private static final int LOOKBACK_DAYS = 7;

    @Resource
    private AmazonAdRuleMapper ruleMapper;

    @Resource
    private AmazonAdReportDailyMapper reportDailyMapper;

    @Resource
    private AmazonAdCampaignMapper campaignMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================= public API =========================

    /**
     * 执行所有启用的广告规则（status=1）。
     * <p>逐条规则执行，单条失败不影响其他规则。</p>
     */
    public void executeAllEnabledRules() {
        List<AmazonAdRuleDO> rules = ruleMapper.selectList(
                new LambdaQueryWrapperX<AmazonAdRuleDO>()
                        .eq(AmazonAdRuleDO::getStatus, 1));

        log.info("[AdRuleEngine] Found {} enabled ad rules to execute", rules.size());

        int successCount = 0;
        int failCount = 0;
        for (AmazonAdRuleDO rule : rules) {
            try {
                executeRule(rule);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("[AdRuleEngine] Failed to execute rule [id={}, name={}]: {}",
                        rule.getId(), rule.getRuleName(), e.getMessage(), e);
            }
        }
        log.info("[AdRuleEngine] Execution complete. success={}, failed={}", successCount, failCount);
    }

    /**
     * 评估条件是否满足。
     *
     * @param conditionJson 条件 JSON，格式：{"metric":"ACOS","operator":"GT","threshold":30.0}
     * @param actualValue   实际指标值
     * @return true 表示条件满足，应触发动作
     */
    public boolean evaluateCondition(String conditionJson, BigDecimal actualValue) {
        if (conditionJson == null || actualValue == null) {
            log.warn("[AdRuleEngine] evaluateCondition: conditionJson or actualValue is null");
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(conditionJson);
            String operator = node.has("operator") ? node.get("operator").asText() : null;
            BigDecimal threshold = node.has("threshold") && !node.get("threshold").isNull()
                    ? new BigDecimal(node.get("threshold").asText()) : null;

            if (operator == null || threshold == null) {
                log.warn("[AdRuleEngine] evaluateCondition: missing operator or threshold in JSON");
                return false;
            }

            int cmp = actualValue.compareTo(threshold);

            if ("GT".equals(operator)) {
                return cmp > 0;
            } else if ("LT".equals(operator)) {
                return cmp < 0;
            } else if ("GTE".equals(operator)) {
                return cmp >= 0;
            } else if ("LTE".equals(operator)) {
                return cmp <= 0;
            } else if ("EQ".equals(operator)) {
                return cmp == 0;
            } else {
                log.warn("[AdRuleEngine] Unknown operator: {}", operator);
                return false;
            }
        } catch (Exception e) {
            log.error("[AdRuleEngine] Failed to parse conditionJson [{}]: {}", conditionJson, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 执行广告动作（当前仅记录日志，实际调价需对接 Amazon Ads API）。
     *
     * @param actionJson 动作 JSON，格式：{"type":"DECREASE_BID","value":10} 或 {"type":"PAUSE","value":null}
     * @param campaignId 广告活动 ID
     */
    public void executeAction(String actionJson, Long campaignId) {
        if (actionJson == null) {
            log.warn("[AdRuleEngine] executeAction: actionJson is null for campaignId={}", campaignId);
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(actionJson);
            String type = node.has("type") ? node.get("type").asText() : null;

            BigDecimal value = null;
            if (node.has("value") && !node.get("value").isNull()) {
                value = new BigDecimal(node.get("value").asText());
            }

            if (type == null) {
                log.warn("[AdRuleEngine] executeAction: type is null for campaignId={}", campaignId);
                return;
            }

            if ("INCREASE_BID".equals(type)) {
                log.info("[AdRuleEngine][Action] INCREASE_BID: campaignId={}, value={}% (pending Ads API)",
                        campaignId, value);
            } else if ("DECREASE_BID".equals(type)) {
                log.info("[AdRuleEngine][Action] DECREASE_BID: campaignId={}, value={}% (pending Ads API)",
                        campaignId, value);
            } else if ("PAUSE".equals(type)) {
                log.info("[AdRuleEngine][Action] PAUSE: campaignId={} (pending Ads API)", campaignId);
            } else if ("INCREASE_BUDGET".equals(type)) {
                log.info("[AdRuleEngine][Action] INCREASE_BUDGET: campaignId={}, value={} (pending Ads API)",
                        campaignId, value);
            } else if ("DECREASE_BUDGET".equals(type)) {
                log.info("[AdRuleEngine][Action] DECREASE_BUDGET: campaignId={}, value={} (pending Ads API)",
                        campaignId, value);
            } else {
                log.warn("[AdRuleEngine] Unknown action type: {} for campaignId={}", type, campaignId);
            }
        } catch (Exception e) {
            log.error("[AdRuleEngine] Failed to parse/execute actionJson [{}] for campaignId={}: {}",
                    actionJson, campaignId, e.getMessage(), e);
        }
    }

    // ========================= internal =========================

    /**
     * 执行单条规则：解析条件 → 获取报表 → 评估 → 执行动作 → 更新执行时间。
     */
    private void executeRule(AmazonAdRuleDO rule) {
        String conditionJson = rule.getConditionJson();
        String actionJson = rule.getActionJson();

        if (conditionJson == null || actionJson == null) {
            log.warn("[AdRuleEngine] Rule [id={}] has null conditionJson or actionJson, skipping", rule.getId());
            return;
        }

        // 1. 解析条件 JSON 中的 metric
        JsonNode conditionNode;
        try {
            conditionNode = objectMapper.readTree(conditionJson);
        } catch (Exception e) {
            log.error("[AdRuleEngine] Failed to parse conditionJson for rule [id={}]: {}",
                    rule.getId(), e.getMessage());
            return;
        }

        String metric = conditionNode.has("metric") ? conditionNode.get("metric").asText() : null;
        if (metric == null) {
            log.warn("[AdRuleEngine] Rule [id={}] missing 'metric' in conditionJson", rule.getId());
            return;
        }

        // 2. 获取最近报表数据
        List<AmazonAdReportDailyDO> reports = getRecentReports(rule);
        if (reports.isEmpty()) {
            log.info("[AdRuleEngine] No report data for rule [id={}, shopId={}, scope={}], skipping",
                    rule.getId(), rule.getShopId(), rule.getScope());
            return;
        }

        // 3. 聚合指标
        BigDecimal actualValue = aggregateMetric(metric, reports);
        log.info("[AdRuleEngine] Rule [id={}, name={}]: metric={}, actualValue={}, reportCount={}",
                rule.getId(), rule.getRuleName(), metric, actualValue, reports.size());

        // 4. 评估条件
        if (evaluateCondition(conditionJson, actualValue)) {
            log.info("[AdRuleEngine] Rule [id={}] condition MET, executing action", rule.getId());
            Long campaignId = reports.get(0).getCampaignId();
            executeAction(actionJson, campaignId);
        } else {
            log.debug("[AdRuleEngine] Rule [id={}] condition NOT met, no action", rule.getId());
        }

        // 5. 更新最后执行时间
        rule.setLastExecutedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);
    }

    /**
     * 获取规则对应店铺最近 {@link #LOOKBACK_DAYS} 天的广告日报数据。
     */
    private List<AmazonAdReportDailyDO> getRecentReports(AmazonAdRuleDO rule) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(LOOKBACK_DAYS);

        LambdaQueryWrapperX<AmazonAdReportDailyDO> wrapper = new LambdaQueryWrapperX<AmazonAdReportDailyDO>()
                .eq(AmazonAdReportDailyDO::getShopId, rule.getShopId())
                .between(AmazonAdReportDailyDO::getReportDate, startDate, endDate);

        // 按 scope 添加额外过滤
        String scope = rule.getScope();
        if ("CAMPAIGN".equals(scope)) {
            // Campaign scope: no additional filter, aggregate across all campaigns
        } else if ("ADGROUP".equals(scope)) {
            // AdGroup scope: ensure adGroupId is not null
            wrapper.isNotNull(AmazonAdReportDailyDO::getAdGroupId);
        } else if ("KEYWORD".equals(scope)) {
            // Keyword scope: ensure keywordId is not null
            wrapper.isNotNull(AmazonAdReportDailyDO::getKeywordId);
        }

        return reportDailyMapper.selectList(wrapper);
    }

    /**
     * 按指标名称聚合报表数据。
     * <p>比率型指标（ACOS/ROAS/CPC/CTR）取平均值，累加型指标（SPEND/CLICKS/IMPRESSIONS/ORDERS）求和。</p>
     */
    private BigDecimal aggregateMetric(String metric, List<AmazonAdReportDailyDO> reports) {
        if ("ACOS".equals(metric)) {
            return averageField(reports, "acos");
        } else if ("SPEND".equals(metric)) {
            return sumField(reports, "cost");
        } else if ("CLICKS".equals(metric)) {
            return sumLongField(reports, "clicks");
        } else if ("IMPRESSIONS".equals(metric)) {
            return sumLongField(reports, "impressions");
        } else if ("ROAS".equals(metric)) {
            return averageField(reports, "roas");
        } else if ("CPC".equals(metric)) {
            return averageField(reports, "cpc");
        } else if ("CTR".equals(metric)) {
            return averageField(reports, "ctr");
        } else if ("ORDERS".equals(metric)) {
            return sumIntField(reports, "orders");
        }
        log.warn("[AdRuleEngine] Unknown metric: {}", metric);
        return BigDecimal.ZERO;
    }

    /** 对 BigDecimal 字段求平均 */
    private BigDecimal averageField(List<AmazonAdReportDailyDO> reports, String field) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (AmazonAdReportDailyDO report : reports) {
            BigDecimal val = getBigDecimalField(report, field);
            if (val != null) {
                sum = sum.add(val);
                count++;
            }
        }
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /** 对 BigDecimal 字段求和 */
    private BigDecimal sumField(List<AmazonAdReportDailyDO> reports, String field) {
        BigDecimal sum = BigDecimal.ZERO;
        for (AmazonAdReportDailyDO report : reports) {
            BigDecimal val = getBigDecimalField(report, field);
            if (val != null) {
                sum = sum.add(val);
            }
        }
        return sum;
    }

    /** 对 Long 字段求和并返回 BigDecimal */
    private BigDecimal sumLongField(List<AmazonAdReportDailyDO> reports, String field) {
        long total = 0L;
        for (AmazonAdReportDailyDO report : reports) {
            Long val = getLongField(report, field);
            if (val != null) {
                total += val;
            }
        }
        return BigDecimal.valueOf(total);
    }

    /** 对 Integer 字段求和并返回 BigDecimal */
    private BigDecimal sumIntField(List<AmazonAdReportDailyDO> reports, String field) {
        int total = 0;
        for (AmazonAdReportDailyDO report : reports) {
            Integer val = getIntField(report, field);
            if (val != null) {
                total += val;
            }
        }
        return BigDecimal.valueOf(total);
    }

    // ---------- field accessors (avoid reflection for JDK 8 friendliness) ----------

    private BigDecimal getBigDecimalField(AmazonAdReportDailyDO report, String field) {
        if ("acos".equals(field)) {
            return report.getAcos();
        } else if ("cost".equals(field)) {
            return report.getCost();
        } else if ("sales".equals(field)) {
            return report.getSales();
        } else if ("roas".equals(field)) {
            return report.getRoas();
        } else if ("cpc".equals(field)) {
            return report.getCpc();
        } else if ("ctr".equals(field)) {
            return report.getCtr();
        }
        return null;
    }

    private Long getLongField(AmazonAdReportDailyDO report, String field) {
        if ("clicks".equals(field)) {
            return report.getClicks();
        } else if ("impressions".equals(field)) {
            return report.getImpressions();
        }
        return null;
    }

    private Integer getIntField(AmazonAdReportDailyDO report, String field) {
        if ("orders".equals(field)) {
            return report.getOrders();
        }
        return null;
    }
}
