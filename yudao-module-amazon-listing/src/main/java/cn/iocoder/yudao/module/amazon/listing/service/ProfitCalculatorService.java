package cn.iocoder.yudao.module.amazon.listing.service;

import java.math.BigDecimal;

/**
 * 利润计算服务。
 * <p>综合 FBA 费用、佣金、头程运费、采购成本计算单件利润和利润率。</p>
 *
 * @author AmazonOps AI
 */
public interface ProfitCalculatorService {

    /**
     * 计算单件利润。
     *
     * @param sellingPrice  售价
     * @param purchaseCost  采购成本
     * @param shippingCost  头程运费
     * @param marketplaceId 站点 ID
     * @return 利润明细
     */
    ProfitDetail calculateProfit(BigDecimal sellingPrice, BigDecimal purchaseCost,
                                 BigDecimal shippingCost, String marketplaceId);

    record ProfitDetail(
            BigDecimal revenue,
            BigDecimal fbaFee,
            BigDecimal referralFee,
            BigDecimal shippingCost,
            BigDecimal purchaseCost,
            BigDecimal profit,
            BigDecimal profitRate
    ) {}
}
