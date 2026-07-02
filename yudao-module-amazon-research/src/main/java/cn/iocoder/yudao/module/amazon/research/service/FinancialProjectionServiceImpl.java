package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonFinancialProjectionDO;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonProductOpportunityDO;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonFinancialProjectionMapper;
import cn.iocoder.yudao.module.amazon.research.dal.mysql.AmazonProductOpportunityMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务预测服务实现。
 * <p>生成 52 周的现金流 / 利润预测。</p>
 *
 * @author AmazonOps AI
 */
@Service
public class FinancialProjectionServiceImpl implements FinancialProjectionService {

    @Resource
    private AmazonFinancialProjectionMapper projectionMapper;

    @Resource
    private AmazonProductOpportunityMapper opportunityMapper;

    @Override
    public AmazonFinancialProjectionDO generateProjection(Long opportunityId) {
        var opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity == null) {
            throw new IllegalArgumentException("Opportunity not found: " + opportunityId);
        }

        var projection = new AmazonFinancialProjectionDO();
        projection.setOpportunityId(opportunityId);
        projection.setStartupCost(new BigDecimal("5000")); // TODO: 动态计算
        projection.setBreakEvenWeek(12);                   // TODO: 动态计算
        projection.setTwelveMonthProfit(BigDecimal.ZERO);   // TODO: 动态计算
        projection.setBrandValuation(BigDecimal.ZERO);      // TODO: 动态计算
        projection.setProjectionData(buildWeeklyProjections(opportunity));

        // 保存或更新
        var existing = projectionMapper.selectByOpportunityId(opportunityId);
        if (existing != null) {
            projection.setId(existing.getId());
            projectionMapper.updateById(projection);
        } else {
            projectionMapper.insert(projection);
        }
        return projection;
    }

    @Override
    public AmazonFinancialProjectionDO getProjection(Long opportunityId) {
        return projectionMapper.selectByOpportunityId(opportunityId);
    }

    private List<AmazonFinancialProjectionDO.WeeklyProjection> buildWeeklyProjections(
            AmazonProductOpportunityDO opportunity) {
        var list = new ArrayList<AmazonFinancialProjectionDO.WeeklyProjection>();
        BigDecimal cumulativeProfit = BigDecimal.ZERO;

        for (int week = 1; week <= 52; week++) {
            var wp = new AmazonFinancialProjectionDO.WeeklyProjection();
            wp.setWeek(week);
            // 简单线性增长模型（后续替换为更复杂的模型）
            BigDecimal weeklyRevenue = opportunity.getPrice()
                    .multiply(BigDecimal.valueOf(opportunity.getEstimatedMonthlySales()))
                    .divide(BigDecimal.valueOf(4), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal weeklyCost = weeklyRevenue.multiply(BigDecimal.ONE.subtract(
                    opportunity.getProfitMargin() != null ? opportunity.getProfitMargin() : new BigDecimal("0.3")));
            BigDecimal weeklyProfit = weeklyRevenue.subtract(weeklyCost);
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
}
