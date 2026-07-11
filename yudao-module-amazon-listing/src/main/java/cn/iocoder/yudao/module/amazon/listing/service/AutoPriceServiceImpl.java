package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonAutoPriceRuleDO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonAutoPriceRuleMapper;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonProductMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动调价服务实现。
 * <p>扫描所有启用的调价规则，根据规则类型（COMPETITIVE / PROFIT_BASED / BUY_BOX）计算目标价格，
 * 检查边界后记录调价日志（实际改价需通过 SP-API PUT /listings/2021-08-01/items 完成）。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AutoPriceServiceImpl implements AutoPriceService {

    /** 价格变动阈值：超过此百分比才触发调价 */
    private static final BigDecimal DEFAULT_CHANGE_THRESHOLD_PERCENT = new BigDecimal("3");

    /** 默认最大单次调价幅度（百分比） */
    private static final BigDecimal DEFAULT_MAX_CHANGE_PERCENT = new BigDecimal("10");

    /** 默认 referral fee 比例 15% */
    private static final BigDecimal REFERRAL_FEE_RATE = new BigDecimal("0.15");

    @Resource
    private AmazonAutoPriceRuleMapper ruleMapper;

    @Resource
    private AmazonProductMapper productMapper;

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonShopService amazonShopService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================= interface methods =========================

    @Override
    public Long createRule(AmazonAutoPriceRuleDO rule) {
        rule.setStatus(1);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateRule(AmazonAutoPriceRuleDO rule) {
        ruleMapper.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public List<AmazonAutoPriceRuleDO> getRulesByProductId(Long productId) {
        return ruleMapper.selectByProductId(productId);
    }

    @Override
    public void executeRules() {
        processAllEnabledRules();
    }

    // ========================= core business logic =========================

    /**
     * 处理所有启用的调价规则（status=1）。
     * <p>逐条规则处理，单条失败不影响其他规则。</p>
     */
    public void processAllEnabledRules() {
        List<AmazonAutoPriceRuleDO> rules = ruleMapper.selectList(
                new LambdaQueryWrapperX<AmazonAutoPriceRuleDO>()
                        .eq(AmazonAutoPriceRuleDO::getStatus, 1));

        log.info("[AutoPrice] Found {} enabled pricing rules to process", rules.size());

        int processedCount = 0;
        int changedCount = 0;
        int failCount = 0;

        for (AmazonAutoPriceRuleDO rule : rules) {
            try {
                boolean changed = processRule(rule);
                processedCount++;
                if (changed) {
                    changedCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("[AutoPrice] Failed to process rule [id={}, productId={}]: {}",
                        rule.getId(), rule.getProductId(), e.getMessage(), e);
            }
        }
        log.info("[AutoPrice] Processing complete. processed={}, changed={}, failed={}",
                processedCount, changedCount, failCount);
    }

    // ========================= helper methods =========================

    /**
     * 基于竞争策略计算目标价格。
     * <p>策略：以当前价格为基准，按 offsetPercent 下调（模拟"匹配最低价 - X%"逻辑）。</p>
     *
     * @param rule    调价规则
     * @param product 产品数据
     * @return 目标价格
     */
    public BigDecimal calculateCompetitivePrice(AmazonAutoPriceRuleDO rule, AmazonProductDO product) {
        BigDecimal currentPrice = product.getPrice();
        if (currentPrice == null) {
            log.warn("[AutoPrice] Product [id={}] has null price, skipping competitive calc", product.getId());
            return null;
        }

        BigDecimal offsetPercent = parseDecimalFromCondition(rule.getConditionJson(), "offsetPercent", "5");

        // target = currentPrice * (1 - offsetPercent/100)
        BigDecimal factor = BigDecimal.ONE.subtract(
                offsetPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        BigDecimal targetPrice = currentPrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);

        log.info("[AutoPrice][COMPETITIVE] productId={}, currentPrice={}, offsetPercent={}%, targetPrice={}",
                product.getId(), currentPrice, offsetPercent, targetPrice);
        return targetPrice;
    }

    /**
     * 基于利润策略计算目标价格。
     * <p>确保售价满足最低利润率：targetPrice = unitCost / (1 - minProfitMargin/100 - referralFeeRate)。</p>
     *
     * @param rule    调价规则
     * @param product 产品数据
     * @return 目标价格
     */
    public BigDecimal calculateProfitBasedPrice(AmazonAutoPriceRuleDO rule, AmazonProductDO product) {
        BigDecimal unitCost = parseDecimalFromCondition(rule.getConditionJson(), "unitCost", "0");
        BigDecimal minProfitMargin = parseDecimalFromCondition(rule.getConditionJson(), "minProfitMargin", "20");

        if (unitCost.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[AutoPrice] Rule [id={}] unitCost <= 0, cannot calculate profit-based price", rule.getId());
            return product.getPrice(); // fallback to current price
        }

        // targetPrice = unitCost / (1 - minProfitMargin% - referralFeeRate)
        BigDecimal marginFraction = minProfitMargin.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal denominator = BigDecimal.ONE.subtract(marginFraction).subtract(REFERRAL_FEE_RATE);

        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[AutoPrice] Rule [id={}] denominator <= 0 (margin+referral >= 100%), cannot calc",
                    rule.getId());
            return product.getPrice();
        }

        BigDecimal targetPrice = unitCost.divide(denominator, 2, RoundingMode.HALF_UP);

        log.info("[AutoPrice][PROFIT_BASED] productId={}, unitCost={}, minMargin={}%, targetPrice={}",
                product.getId(), unitCost, minProfitMargin, targetPrice);
        return targetPrice;
    }

    /**
     * 判断价格是否在 [minPrice, maxPrice] 区间内。
     */
    public boolean isWithinBounds(BigDecimal price, BigDecimal minPrice, BigDecimal maxPrice) {
        if (price == null) {
            return false;
        }
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    // ========================= internal =========================

    /**
     * 处理单条规则，返回 true 表示建议改价。
     */
    private boolean processRule(AmazonAutoPriceRuleDO rule) {
        Long productId = rule.getProductId();

        // 获取关联产品
        List<AmazonProductDO> products = getProductsForRule(rule);
        if (products.isEmpty()) {
            log.warn("[AutoPrice] No products found for rule [id={}, productId={}]", rule.getId(), productId);
            return false;
        }

        boolean anyChanged = false;
        for (AmazonProductDO product : products) {
            boolean changed = processRuleForProduct(rule, product);
            if (changed) {
                anyChanged = true;
            }
        }
        return anyChanged;
    }

    /**
     * 获取规则关联的产品列表。
     */
    private List<AmazonProductDO> getProductsForRule(AmazonAutoPriceRuleDO rule) {
        List<AmazonProductDO> products = new ArrayList<>();
        if (rule.getProductId() != null) {
            AmazonProductDO product = productMapper.selectById(rule.getProductId());
            if (product != null) {
                products.add(product);
            }
        } else {
            // No specific product: query all active products
            products = productMapper.selectList(
                    new LambdaQueryWrapperX<AmazonProductDO>()
                            .eq(AmazonProductDO::getListingStatus, "ACTIVE"));
        }
        return products;
    }

    /**
     * 对单个产品执行规则逻辑，返回 true 表示建议改价。
     */
    private boolean processRuleForProduct(AmazonAutoPriceRuleDO rule, AmazonProductDO product) {
        BigDecimal currentPrice = product.getPrice();
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("[AutoPrice] Product [id={}, asin={}] has no valid price, skipping",
                    product.getId(), product.getAsin());
            return false;
        }

        String ruleType = rule.getRuleType();
        BigDecimal targetPrice;

        if ("COMPETITIVE".equals(ruleType)) {
            targetPrice = calculateCompetitivePrice(rule, product);
        } else if ("PROFIT_BASED".equals(ruleType)) {
            targetPrice = calculateProfitBasedPrice(rule, product);
        } else if ("BUY_BOX".equals(ruleType)) {
            targetPrice = calculateBuyBoxPrice(rule, product);
        } else {
            log.warn("[AutoPrice] Unknown ruleType '{}' for rule [id={}]", ruleType, rule.getId());
            return false;
        }

        if (targetPrice == null) {
            return false;
        }

        // 读取价格边界
        BigDecimal minPrice = parseDecimalFromCondition(rule.getConditionJson(), "minPrice", null);
        BigDecimal maxPrice = parseDecimalFromCondition(rule.getConditionJson(), "maxPrice", null);

        // 边界裁剪
        if (minPrice != null && targetPrice.compareTo(minPrice) < 0) {
            log.info("[AutoPrice] Target price {} below minPrice {}, clamping", targetPrice, minPrice);
            targetPrice = minPrice;
        }
        if (maxPrice != null && targetPrice.compareTo(maxPrice) > 0) {
            log.info("[AutoPrice] Target price {} above maxPrice {}, clamping", targetPrice, maxPrice);
            targetPrice = maxPrice;
        }

        // 检查最大单次调价幅度
        BigDecimal maxChangePercent = parseDecimalFromAction(rule.getActionJson(), "maxChangePercent",
                DEFAULT_MAX_CHANGE_PERCENT.toPlainString());
        BigDecimal maxChangeAmount = currentPrice.multiply(maxChangePercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal diff = targetPrice.subtract(currentPrice).abs();
        if (diff.compareTo(maxChangeAmount) > 0) {
            // Clamp to max allowed change
            if (targetPrice.compareTo(currentPrice) > 0) {
                targetPrice = currentPrice.add(maxChangeAmount);
            } else {
                targetPrice = currentPrice.subtract(maxChangeAmount);
            }
            log.info("[AutoPrice] Price change capped to maxChangePercent={}%, adjusted target={}",
                    maxChangePercent, targetPrice);
        }

        // 判断是否超过阈值（避免微小变动频繁调价）
        BigDecimal changePercent = diff.multiply(new BigDecimal("100"))
                .divide(currentPrice, 2, RoundingMode.HALF_UP);
        if (changePercent.compareTo(DEFAULT_CHANGE_THRESHOLD_PERCENT) < 0) {
            log.debug("[AutoPrice] Price change {}% below threshold, no action for product [id={}]",
                    changePercent, product.getId());
            return false;
        }

        log.info("[AutoPrice] PRICE CHANGE: product [id={}, asin={}, sku={}], current={}, target={}, " +
                        "change={}% (SP-API PUT /listings/2021-08-01/items pending)",
                product.getId(), product.getAsin(), product.getSku(), currentPrice, targetPrice, changePercent);
        boolean pushed = pushPriceToAmazon(product, targetPrice);
        if (pushed) {
            product.setPrice(targetPrice);
            productMapper.updateById(product);
            log.info("[AutoPrice] Price updated in DB for product [id={}, asin={}] to {}", product.getId(), product.getAsin(), targetPrice);
        }
        return pushed;
    }

    /**
     * 计算 Buy Box 价格。
     * <p>策略：以当前价格为基准，下调 offsetAmount（模拟"最低 FBA 价格 - 小额差值"逻辑）。</p>
     */
    private BigDecimal calculateBuyBoxPrice(AmazonAutoPriceRuleDO rule, AmazonProductDO product) {
        BigDecimal currentPrice = product.getPrice();
        if (currentPrice == null) {
            return null;
        }

        BigDecimal offsetAmount = parseDecimalFromCondition(rule.getConditionJson(), "offsetAmount", "0.01");

        BigDecimal targetPrice = currentPrice.subtract(offsetAmount).setScale(2, RoundingMode.HALF_UP);

        // Ensure price does not go to zero or negative
        if (targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[AutoPrice][BUY_BOX] Calculated price <= 0 for product [id={}], returning current price",
                    product.getId());
            return currentPrice;
        }

        log.info("[AutoPrice][BUY_BOX] productId={}, currentPrice={}, offsetAmount={}, targetPrice={}",
                product.getId(), currentPrice, offsetAmount, targetPrice);
        return targetPrice;
    }

    // ========================= SP-API push =========================

    private boolean pushPriceToAmazon(AmazonProductDO product, BigDecimal targetPrice) {
        try {
            AmazonShopDO shop = amazonShopService.getShopById(product.getShopId());
            if (shop == null) {
                log.warn("[AutoPrice] Shop not found for product [id={}]", product.getId());
                return false;
            }
            String sellerId = shop.getSellerId();
            AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(product.getMarketplaceId());
            if (marketplace == null) {
                log.warn("[AutoPrice] Unknown marketplace for product [id={}]", product.getId());
                return false;
            }
            String awsRegion = marketplace.getAwsRegion();

            // Build request body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("productType", "PRODUCT");
            ObjectNode attributes = body.putObject("attributes");
            ObjectNode purchasableOffer = attributes.putObject("purchasable_offer");
            ObjectNode ourPrice = purchasableOffer.putObject("our_price");
            ObjectNode schedule = ourPrice.putObject("schedule");
            schedule.put("value", targetPrice.toPlainString());

            String path = "/listings/2021-08-01/items/" + sellerId + "/" + product.getSku();
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("marketplaceIds", product.getMarketplaceId());

            spApiClient.put(sellerId, awsRegion, path, queryParams, body);
            log.info("[AutoPrice] Price pushed to Amazon for SKU={}, newPrice={}", product.getSku(), targetPrice);
            return true;
        } catch (Exception e) {
            log.warn("[AutoPrice] Failed to push price for product [id={}]: {}", product.getId(), e.getMessage());
            return false;
        }
    }

    // ========================= JSON helpers =========================

    /**
     * 从 conditionJson 中解析 BigDecimal 参数。
     */
    private BigDecimal parseDecimalFromCondition(String json, String fieldName, String defaultValue) {
        if (json == null) {
            return defaultValue != null ? new BigDecimal(defaultValue) : null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return new BigDecimal(node.get(fieldName).asText());
            }
        } catch (Exception e) {
            log.warn("[AutoPrice] Failed to parse field '{}' from conditionJson [{}]: {}",
                    fieldName, json, e.getMessage());
        }
        return defaultValue != null ? new BigDecimal(defaultValue) : null;
    }

    /**
     * 从 actionJson 中解析 BigDecimal 参数。
     */
    private BigDecimal parseDecimalFromAction(String json, String fieldName, String defaultValue) {
        if (json == null) {
            return defaultValue != null ? new BigDecimal(defaultValue) : null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return new BigDecimal(node.get(fieldName).asText());
            }
        } catch (Exception e) {
            log.warn("[AutoPrice] Failed to parse field '{}' from actionJson [{}]: {}",
                    fieldName, json, e.getMessage());
        }
        return defaultValue != null ? new BigDecimal(defaultValue) : null;
    }
}
