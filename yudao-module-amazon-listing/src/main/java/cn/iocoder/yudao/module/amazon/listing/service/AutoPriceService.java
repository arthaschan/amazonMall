package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonAutoPriceRuleDO;

import java.util.List;

/**
 * 自动调价服务。
 *
 * @author AmazonOps AI
 */
public interface AutoPriceService {

    Long createRule(AmazonAutoPriceRuleDO rule);

    void updateRule(AmazonAutoPriceRuleDO rule);

    void deleteRule(Long id);

    List<AmazonAutoPriceRuleDO> getRulesByProductId(Long productId);

    /** 执行所有启用的调价规则 */
    void executeRules();
}
