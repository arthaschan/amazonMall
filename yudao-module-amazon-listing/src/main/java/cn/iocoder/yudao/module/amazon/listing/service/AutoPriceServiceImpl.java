package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonAutoPriceRuleDO;
import cn.iocoder.yudao.module.amazon.listing.dal.mysql.AmazonAutoPriceRuleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自动调价服务实现。
 *
 * @author AmazonOps AI
 */
@Service
public class AutoPriceServiceImpl implements AutoPriceService {

    @Resource
    private AmazonAutoPriceRuleMapper ruleMapper;

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
        // TODO: 扫描所有启用的规则，评估条件并执行调价
    }
}
