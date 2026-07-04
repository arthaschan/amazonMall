package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonFinancialProjectionDO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonProductOpportunityDO;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonFinancialProjectionMapper;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonProductOpportunityMapper;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务预测服务实现。
 * <p>基于产品机会数据生成 52 周现金流 / 利润预测模型，包括启动成本、月度收支、回本周期、12 个月累计利润及 ROI 计算。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class FinancialProjectionServiceImpl implements FinancialProjectionService {

    // ========================= 默认假设参数 =========================

    /** 单位成本占售价比例（默认 25%） */
    private static final BigDecimal UNIT_COST_RATIO = new BigDecimal("0.25");

    /** 最小起订量 */
    private static final int DEFAULT_MOQ = 500;

    /** 单位海运运费（美元） */
    private static final BigDecimal SHIPPING_PER_UNIT = new BigDecimal("2.00");

    /** 首批 FBA 入库费用（美元，一次性） */
    private static final BigDecimal FIRST_BATCH_FBA_FEE = new BigDecimal("300.00");

    /** 产品摄影费用（美元，一次性） */
    private static final BigDecimal PHOTOGRAPHY_COST = new BigDecimal("500.00");

    /** Listing 优化费用（美元，一次性） */
    private static final BigDecimal LISTING_OPTIMIZATION_COST = new BigDecimal("300.00");

    /** referral fee 比例（15%） */
    private static final BigDecimal REFERRAL_FEE_RATE = new BigDecimal("0.15");

    /** 广告支出占收入比例（15%） */
    private static final BigDecimal AD_SPEND_RATE = new BigDecimal("0.15");

    /** 月仓储费用（美元/单位/月） */
    private static final BigDecimal STORAGE_PER_UNIT_MONTH = new BigDecimal("0.10");

    /** 启动阶段（第 1-4 周）销量比例 */
    private static final BigDecimal LAUNCH_PHASE_RATIO = new BigDecimal("0.50");

    /** 增长阶段结束周 */
    private static final int GROWTH_END_WEEK = 12;

    /** 一年月数 */
    private static final int MONTHS_PER_YEAR = 12;

    /** 一年周数 */
    private static final int WEEKS_PER_YEAR = 52;

    /** 每月约 4.33 周 */
    private static final BigDecimal WEEKS_PER_MONTH = new BigDecimal("4.33");

    @Resource
    private AmazonFinancialProjectionMapper projectionMapper;

    @Resource
    private AmazonProductOpportunityMapper opportunityMapper;

    // ========================= interface methods =========================

    @Override
    public AmazonFinancialProjectionDO generateProjection(Long opportunityId) {
        return calculateProjection(opportunityId);
    }

    @Override
    public AmazonFinancialProjectionDO getProjection(Long opportunityId) {
        return projectionMapper.selectByOpportunityId(opportunityId);
    }

    // ========================= core business logic =========================

    /**
     * 为指定产品机会计算完整财务预测。
     *
     * @param opportunityId 产品机会 ID
     * @return 计算完成并持久化的预测记录
     */
    public AmazonFinancialProjectionDO calculateProjection(Long opportunityId) {
        AmazonProductOpportunityDO opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity == null) {
            throw new IllegalArgumentException("ProductOpportunity not found: " + opportunityId);
        }

        BigDecimal price = opportunity.getPrice();
        Integer estimatedMonthlySales = opportunity.getEstimatedMonthlySales();

        if (price == null || estimatedMonthlySales == null || estimatedMonthlySales <= 0) {
            log.warn("[FinancialProjection] Opportunity [id={}] has insufficient data (price={}, monthlySales={})",
                    opportunityId, price, estimatedMonthlySales);
            throw new IllegalArgumentException(
                    "Opportunity [id=" + opportunityId + "] missing price or estimatedMonthlySales");
        }

        // ---- Startup cost ----
        BigDecimal unitCost = price.multiply(UNIT_COST_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal moqCost = unitCost.multiply(BigDecimal.valueOf(DEFAULT_MOQ));
        BigDecimal shippingCost = SHIPPING_PER_UNIT.multiply(BigDecimal.valueOf(DEFAULT_MOQ));
        BigDecimal startupCost = moqCost
                .add(shippingCost)
                .add(FIRST_BATCH_FBA_FEE)
                .add(PHOTOGRAPHY_COST)
                .add(LISTING_OPTIMIZATION_COST);

        log.info("[FinancialProjection] Opportunity [id={}]: startupCost={}, unitCost={}, moq={}",
                opportunityId, startupCost, unitCost, DEFAULT_MOQ);

        // ---- Monthly financials (steady-state) ----
        BigDecimal monthlyRevenue = price.multiply(BigDecimal.valueOf(estimatedMonthlySales));
        BigDecimal fbaFeePerUnit = estimateFBAFees(price, opportunity.getAsin());
        BigDecimal monthlyFbaFees = fbaFeePerUnit.multiply(BigDecimal.valueOf(estimatedMonthlySales));
        BigDecimal referralFeePerUnit = estimateReferralFee(price);
        BigDecimal monthlyReferralFee = referralFeePerUnit.multiply(BigDecimal.valueOf(estimatedMonthlySales));
        BigDecimal monthlyStorage = STORAGE_PER_UNIT_MONTH.multiply(BigDecimal.valueOf(estimatedMonthlySales));
        BigDecimal monthlyAdSpend = estimateMonthlyAdSpend(monthlyRevenue);
        BigDecimal monthlyCogs = unitCost.multiply(BigDecimal.valueOf(estimatedMonthlySales));

        BigDecimal monthlyTotalCost = monthlyFbaFees
                .add(monthlyReferralFee)
                .add(monthlyStorage)
                .add(monthlyAdSpend)
                .add(monthlyCogs);

        BigDecimal monthlyProfit = monthlyRevenue.subtract(monthlyTotalCost);

        log.info("[FinancialProjection] Monthly: revenue={}, cost={}, profit={}",
                monthlyRevenue, monthlyTotalCost, monthlyProfit);

        // ---- Break-even (in months, mapped to weeks) ----
        int breakEvenMonth;
        if (monthlyProfit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal breakEvenMonthsBd = startupCost.divide(monthlyProfit, 2, RoundingMode.HALF_UP);
            breakEvenMonth = breakEvenMonthsBd.setScale(0, RoundingMode.CEILING).intValue();
        } else {
            // Profit is zero or negative: never breaks even within reasonable timeframe
            breakEvenMonth = -1;
            log.warn("[FinancialProjection] Opportunity [id={}] monthly profit <= 0, will not break even",
                    opportunityId);
        }
        int breakEvenWeek = breakEvenMonth > 0 ? breakEvenMonth * 4 : -1;

        // ---- 12-month profit & ROI ----
        BigDecimal twelveMonthProfit = monthlyProfit.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR))
                .subtract(startupCost);

        BigDecimal roi = BigDecimal.ZERO;
        if (startupCost.compareTo(BigDecimal.ZERO) > 0) {
            roi = twelveMonthProfit.multiply(new BigDecimal("100"))
                    .divide(startupCost, 2, RoundingMode.HALF_UP);
        }

        log.info("[FinancialProjection] 12-month: profit={}, ROI={}%, breakEvenWeek={}",
                twelveMonthProfit, roi, breakEvenWeek);

        // ---- Brand valuation (simple: 3x annual profit) ----
        BigDecimal annualProfit = twelveMonthProfit;
        BigDecimal brandValuation = annualProfit.compareTo(BigDecimal.ZERO) > 0
                ? annualProfit.multiply(new BigDecimal("3")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ---- Build weekly projections ----
        List<AmazonFinancialProjectionDO.WeeklyProjection> weeklyProjections =
                buildWeeklyProjections(opportunity, startupCost, unitCost);

        // ---- Save or update ----
        AmazonFinancialProjectionDO projection = new AmazonFinancialProjectionDO();
        projection.setOpportunityId(opportunityId);
        projection.setStartupCost(startupCost);
        projection.setBreakEvenWeek(breakEvenWeek);
        projection.setTwelveMonthProfit(twelveMonthProfit);
        projection.setBrandValuation(brandValuation);
        projection.setProjectionData(weeklyProjections);

        AmazonFinancialProjectionDO existing = projectionMapper.selectByOpportunityId(opportunityId);
        if (existing != null) {
            projection.setId(existing.getId());
            projectionMapper.updateById(projection);
            log.info("[FinancialProjection] Updated existing projection [id={}] for opportunity [id={}]",
                    existing.getId(), opportunityId);
        } else {
            projectionMapper.insert(projection);
            log.info("[FinancialProjection] Created new projection [id={}] for opportunity [id={}]",
                    projection.getId(), opportunityId);
        }

        return projection;
    }

    // ========================= estimation helpers =========================

    /**
     * 估算单件 FBA 费用。
     * <p>简化模型：按售价的 15% 估算（Amazon FBA 费率因品类/尺寸而异，此为通用估算）。</p>
     *
     * @param price    售价
     * @param category 品类标识（当前未使用，预留按品类差异化费率）
     * @return 每件 FBA 费用
     */
    public BigDecimal estimateFBAFees(BigDecimal price, String category) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // Default: ~15% of price as FBA fee (picking, packing, shipping, customer service)
        return price.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 估算单件 referral fee（佣金）。
     * <p>Amazon 标准 referral fee 为 15%。</p>
     *
     * @param price 售价
     * @return 每件 referral fee
     */
    public BigDecimal estimateReferralFee(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return price.multiply(REFERRAL_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 估算月度广告支出。
     * <p>按收入的 15% 估算（Amazon 卖家典型广告投入比例）。</p>
     *
     * @param monthlyRevenue 月收入
     * @return 月广告支出
     */
    public BigDecimal estimateMonthlyAdSpend(BigDecimal monthlyRevenue) {
        if (monthlyRevenue == null || monthlyRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return monthlyRevenue.multiply(AD_SPEND_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    // ========================= internal =========================

    /**
     * 构建 52 周逐周财务预测。
     * <p>模型：
     * <ul>
     *   <li>第 1-4 周（启动期）：销量为稳态的 50%</li>
     *   <li>第 5-12 周（增长期）：线性增长从 50% 到 100%</li>
     *   <li>第 13-52 周（稳态期）：100% 预计月销量 / 4.33</li>
     * </ul>
     * 第一周包含启动成本作为一次性现金流出。</p>
     */
    private List<AmazonFinancialProjectionDO.WeeklyProjection> buildWeeklyProjections(
            AmazonProductOpportunityDO opportunity,
            BigDecimal startupCost,
            BigDecimal unitCost) {

        List<AmazonFinancialProjectionDO.WeeklyProjection> list =
                new ArrayList<>();

        BigDecimal price = opportunity.getPrice();
        BigDecimal estimatedMonthlySales = BigDecimal.valueOf(opportunity.getEstimatedMonthlySales());
        BigDecimal weeklySteadyUnits = estimatedMonthlySales.divide(WEEKS_PER_MONTH, 2, RoundingMode.HALF_UP);

        BigDecimal cumulativeProfit = BigDecimal.ZERO;
        BigDecimal fbaFeePerUnit = estimateFBAFees(price, opportunity.getAsin());
        BigDecimal referralFeePerUnit = estimateReferralFee(price);
        BigDecimal adRatePerUnit = price.multiply(AD_SPEND_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal storagePerUnitWeek = STORAGE_PER_UNIT_MONTH
                .divide(WEEKS_PER_MONTH, 4, RoundingMode.HALF_UP);

        for (int week = 1; week <= WEEKS_PER_YEAR; week++) {
            AmazonFinancialProjectionDO.WeeklyProjection wp =
                    new AmazonFinancialProjectionDO.WeeklyProjection();
            wp.setWeek(week);

            // 计算本周销量比例
            BigDecimal salesRatio = computeSalesRatio(week);
            BigDecimal weeklyUnits = weeklySteadyUnits.multiply(salesRatio).setScale(2, RoundingMode.HALF_UP);

            // 收入
            BigDecimal weeklyRevenue = price.multiply(weeklyUnits).setScale(2, RoundingMode.HALF_UP);

            // 成本
            BigDecimal weeklyFba = fbaFeePerUnit.multiply(weeklyUnits).setScale(2, RoundingMode.HALF_UP);
            BigDecimal weeklyReferral = referralFeePerUnit.multiply(weeklyUnits).setScale(2, RoundingMode.HALF_UP);
            BigDecimal weeklyStorage = storagePerUnitWeek.multiply(weeklyUnits).setScale(2, RoundingMode.HALF_UP);
            BigDecimal weeklyAd = adRatePerUnit.multiply(weeklyUnits).setScale(2, RoundingMode.HALF_UP);
            BigDecimal weeklyCogs = unitCost.multiply(weeklyUnits).setScale(2, RoundingMode.HALF_UP);

            BigDecimal weeklyCost = weeklyFba.add(weeklyReferral).add(weeklyStorage)
                    .add(weeklyAd).add(weeklyCogs);

            // 第一周额外加上启动成本
            if (week == 1) {
                weeklyCost = weeklyCost.add(startupCost);
            }

            // 利润 & 现金流
            BigDecimal weeklyProfit = weeklyRevenue.subtract(weeklyCost).setScale(2, RoundingMode.HALF_UP);
            cumulativeProfit = cumulativeProfit.add(weeklyProfit);

            wp.setRevenue(weeklyRevenue);
            wp.setCost(weeklyCost);
            wp.setProfit(weeklyProfit);
            wp.setCashFlow(weeklyProfit);
            wp.setCumulativeProfit(cumulativeProfit);

            list.add(wp);
        }

        return list;
    }

    /**
     * 计算指定周的销售比例系数。
     *
     * @param week 周次（1-52）
     * @return 比例，0.0 ~ 1.0
     */
    private BigDecimal computeSalesRatio(int week) {
        if (week <= 4) {
            // 启动期: 50%
            return LAUNCH_PHASE_RATIO;
        } else if (week <= GROWTH_END_WEEK) {
            // 增长期: 从 50% 线性增长到 100%
            // ratio = 0.5 + (week - 4) / (12 - 4) * 0.5
            BigDecimal growthRange = BigDecimal.valueOf(GROWTH_END_WEEK - 4);
            BigDecimal elapsed = BigDecimal.valueOf(week - 4);
            return LAUNCH_PHASE_RATIO.add(
                    elapsed.divide(growthRange, 4, RoundingMode.HALF_UP)
                            .multiply(LAUNCH_PHASE_RATIO));
        } else {
            // 稳态期: 100%
            return BigDecimal.ONE;
        }
    }
}
