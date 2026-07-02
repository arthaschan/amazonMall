package cn.iocoder.yudao.module.amazon.listing.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 利润计算器实现。
 *
 * @author AmazonOps AI
 */
@Service
public class ProfitCalculatorServiceImpl implements ProfitCalculatorService {

    private static final BigDecimal REFERRAL_FEE_RATE = new BigDecimal("0.15");
    private static final BigDecimal DEFAULT_FBA_FEE = new BigDecimal("5.00");

    @Override
    public ProfitDetail calculateProfit(BigDecimal sellingPrice, BigDecimal purchaseCost,
                                        BigDecimal shippingCost, String marketplaceId) {
        var referralFee = sellingPrice.multiply(REFERRAL_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        var fbaFee = DEFAULT_FBA_FEE; // TODO: 根据尺寸/重量动态计算
        var totalCost = purchaseCost.add(shippingCost).add(referralFee).add(fbaFee);
        var profit = sellingPrice.subtract(totalCost);
        var profitRate = sellingPrice.compareTo(BigDecimal.ZERO) > 0
                ? profit.divide(sellingPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new ProfitDetail(sellingPrice, fbaFee, referralFee, shippingCost, purchaseCost, profit, profitRate);
    }
}
