package cn.iocoder.yudao.module.amazon.listing.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 利润计算器实现。
 *
 * <p>综合 FBA 费用、佣金、头程运费、采购成本计算单件利润和利润率。</p>
 *
 * <p>FBA 费用按 Amazon 美国站 2024 年费用标准，根据商品重量阶梯计算：
 * <ul>
 *   <li>≤ 2 oz（56g）: $3.22</li>
 *   <li>2-4 oz（56-113g）: $3.40</li>
 *   <li>4-6 oz（113-170g）: $3.58</li>
 *   <li>6-8 oz（170-227g）: $3.77</li>
 *   <li>8-10 oz（227-283g）: $4.00</li>
 *   <li>10-12 oz（283-340g）: $4.17</li>
 *   <li>12-14 oz（340-397g）: $4.37</li>
 *   <li>14-16 oz（397-454g）: $4.55</li>
 *   <li>1-1.5 lb（454-680g）: $5.07</li>
 *   <li>1.5-2 lb（680-907g）: $5.40</li>
 *   <li>2-2.5 lb（907-1134g）: $5.69</li>
 *   <li>2.5-3 lb（1134-1361g）: $5.93</li>
 *   <li>> 3 lb（1361g+）: $5.93 + $0.32/half-lb</li>
 * </ul>
 *
 * <p>非美国站按汇率折算。</p>
 *
 * @author AmazonOps AI
 */
@Service
public class ProfitCalculatorServiceImpl implements ProfitCalculatorService {

    /** Amazon 佣金比例（大多数品类 15%） */
    private static final BigDecimal REFERRAL_FEE_RATE = new BigDecimal("0.15");

    /** 重量→FBA费用阶梯表（重量上限 oz → 费用 USD） */
    private static final BigDecimal[] FBA_FEE_TIERS_OZ = {
            new BigDecimal("3.22"),   // ≤ 2 oz
            new BigDecimal("3.40"),   // 2-4 oz
            new BigDecimal("3.58"),   // 4-6 oz
            new BigDecimal("3.77"),   // 6-8 oz
            new BigDecimal("4.00"),   // 8-10 oz
            new BigDecimal("4.17"),   // 10-12 oz
            new BigDecimal("4.37"),   // 12-14 oz
            new BigDecimal("4.55")    // 14-16 oz (= 1 lb)
    };

    /** 每阶梯的重量上限（oz） */
    private static final int[] TIER_UPPER_BOUNDS_OZ = { 2, 4, 6, 8, 10, 12, 14, 16 };

    /** 超过 1 lb 后的基础费用 */
    private static final BigDecimal LB_BASE_FEE = new BigDecimal("5.07");
    /** 超过 1 lb 后每半磅增加的费用 */
    private static final BigDecimal LB_INCREMENT_FEE = new BigDecimal("0.32");
    /** 超过 1 lb 后的基础重量（lb） */
    private static final BigDecimal LB_BASE_WEIGHT = new BigDecimal("1.0");

    /** 默认 FBA 费用（无重量信息时的估算值：标准尺寸中位数） */
    private static final BigDecimal DEFAULT_FBA_FEE_USD = new BigDecimal("5.50");

    /** 各站点币种相对美元的汇率估算（用于 FBA 费用折算） */
    private static final Map<String, BigDecimal> MARKETPLACE_FX = new HashMap<String, BigDecimal>();
    static {
        MARKETPLACE_FX.put("ATVPDKIKX0DER", BigDecimal.ONE);           // US: USD
        MARKETPLACE_FX.put("A2EUQ1WTGCTBG2", new BigDecimal("1.35"));  // CA: CAD
        MARKETPLACE_FX.put("A1F83G8C2ARO7P", new BigDecimal("0.79"));  // UK: GBP
        MARKETPLACE_FX.put("A1PA6795UKMFR9", new BigDecimal("0.92"));  // DE: EUR
        MARKETPLACE_FX.put("A13V1IB3VIYZZH", new BigDecimal("0.92"));  // FR: EUR
        MARKETPLACE_FX.put("APJ6JRA9NG5V4", new BigDecimal("0.92"));   // IT: EUR
        MARKETPLACE_FX.put("A1RKKUPIHCS9HS", new BigDecimal("0.92"));  // ES: EUR
        MARKETPLACE_FX.put("A1VC38T7YXB528", new BigDecimal("149.0")); // JP: JPY
        MARKETPLACE_FX.put("A39IBJ37TR1ESG", new BigDecimal("1.52"));  // AU: AUD
        MARKETPLACE_FX.put("A21TJRUUN4KGV", new BigDecimal("83.0"));   // IN: INR
    }

    @Override
    public ProfitDetail calculateProfit(BigDecimal sellingPrice, BigDecimal purchaseCost,
                                        BigDecimal shippingCost, String marketplaceId) {
        return calculateProfit(sellingPrice, purchaseCost, shippingCost, marketplaceId, null);
    }

    /**
     * 扩展计算方法：支持传入商品重量（克）进行精确 FBA 费用计算。
     *
     * @param sellingPrice  售价（本地币种）
     * @param purchaseCost  采购成本
     * @param shippingCost  头程运费
     * @param marketplaceId 站点 ID
     * @param weightGrams   商品重量（克），null 时使用默认估算
     * @return 利润明细
     */
    public ProfitDetail calculateProfit(BigDecimal sellingPrice, BigDecimal purchaseCost,
                                        BigDecimal shippingCost, String marketplaceId,
                                        BigDecimal weightGrams) {
        // 1. 佣金
        BigDecimal referralFee = sellingPrice.multiply(REFERRAL_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        // 2. FBA 费用
        BigDecimal fbaFeeUsd = calculateFbaFeeUsd(weightGrams);
        BigDecimal fbaFee = convertToLocalCurrency(fbaFeeUsd, marketplaceId);

        // 3. 总成本 = 采购 + 头程 + 佣金 + FBA
        BigDecimal totalCost = purchaseCost.add(shippingCost).add(referralFee).add(fbaFee);

        // 4. 利润 = 售价 - 总成本
        BigDecimal profit = sellingPrice.subtract(totalCost);

        // 5. 利润率 = (利润 / 售价) × 100
        BigDecimal profitRate = sellingPrice.compareTo(BigDecimal.ZERO) > 0
                ? profit.divide(sellingPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new ProfitDetail(sellingPrice, fbaFee, referralFee, shippingCost, purchaseCost, profit, profitRate);
    }

    /**
     * 根据重量（克）计算 FBA 费用（USD）。
     * <p>基于 Amazon 美国站 2024 年 FBA 配送费标准。</p>
     */
    private BigDecimal calculateFbaFeeUsd(BigDecimal weightGrams) {
        if (weightGrams == null || weightGrams.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_FBA_FEE_USD;
        }

        // 克 → 盎司 (1 oz = 28.3495g)
        double weightOz = weightGrams.doubleValue() / 28.3495;

        // 标准尺寸阶梯（≤ 16 oz）
        for (int i = 0; i < TIER_UPPER_BOUNDS_OZ.length; i++) {
            if (weightOz <= TIER_UPPER_BOUNDS_OZ[i]) {
                return FBA_FEE_TIERS_OZ[i];
            }
        }

        // 超过 1 lb 的部分，按每半磅 $0.32 递增
        double weightLb = weightOz / 16.0;
        double extraHalfLbs = Math.ceil((weightLb - 1.0) / 0.5);
        double extraFee = extraHalfLbs * 0.32;

        return LB_BASE_FEE.add(BigDecimal.valueOf(extraFee)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 将 USD FBA 费用转换为站点本地币种。
     */
    private BigDecimal convertToLocalCurrency(BigDecimal usdAmount, String marketplaceId) {
        if (marketplaceId == null) {
            return usdAmount;
        }

        BigDecimal fxRate = MARKETPLACE_FX.get(marketplaceId);
        if (fxRate == null) {
            return usdAmount; // 未知站点保持 USD
        }

        return usdAmount.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);
    }
}
