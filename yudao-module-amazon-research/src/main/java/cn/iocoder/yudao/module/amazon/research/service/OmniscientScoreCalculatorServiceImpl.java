package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonNicheDO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 全知评分计算器实现。
 * <p>采用加权平均算法，十维评分各有权重（默认等权 0.1）。</p>
 *
 * @author AmazonOps AI
 */
@Service
public class OmniscientScoreCalculatorServiceImpl implements OmniscientScoreCalculatorService {

    private static final BigDecimal DEFAULT_WEIGHT = new BigDecimal("0.1");

    @Override
    public BigDecimal calculate(AmazonNicheDO niche) {
        Map<String, BigDecimal> weights = niche.getScoreWeights();

        BigDecimal total = BigDecimal.ZERO;
        total = addWeighted(total, niche.getDemandScore(), weightOf(weights, "demand"));
        total = addWeighted(total, niche.getCompetitionScore(), weightOf(weights, "competition"));
        total = addWeighted(total, niche.getProfitabilityScore(), weightOf(weights, "profitability"));
        total = addWeighted(total, niche.getReviewMoatScore(), weightOf(weights, "reviewMoat"));
        total = addWeighted(total, niche.getPriceStabilityScore(), weightOf(weights, "priceStability"));
        total = addWeighted(total, niche.getSeasonalityScore(), weightOf(weights, "seasonality"));
        total = addWeighted(total, niche.getOrganicRankScore(), weightOf(weights, "organicRank"));
        total = addWeighted(total, niche.getAdDependencyScore(), weightOf(weights, "adDependency"));
        total = addWeighted(total, niche.getSupplierScore(), weightOf(weights, "supplier"));

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal addWeighted(BigDecimal acc, BigDecimal score, BigDecimal weight) {
        if (score == null) return acc;
        return acc.add(score.multiply(weight));
    }

    private BigDecimal weightOf(Map<String, BigDecimal> weights, String key) {
        if (weights == null) return DEFAULT_WEIGHT;
        return weights.getOrDefault(key, DEFAULT_WEIGHT);
    }
}
