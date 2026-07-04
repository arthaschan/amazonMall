package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdRulePageReqVO;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdRuleSaveReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdRuleDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdRuleMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdRuleEngineServiceImpl implements AdRuleEngineService {

    @Resource
    private AmazonAdRuleMapper ruleMapper;

    @Resource
    private AdRuleEngine adRuleEngine;

    @Override
    public Long createRule(AdRuleSaveReqVO reqVO) {
        AmazonAdRuleDO rule = BeanUtils.toBean(reqVO, AmazonAdRuleDO.class);
        rule.setStatus(1);
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateRule(AdRuleSaveReqVO reqVO) {
        ruleMapper.updateById(BeanUtils.toBean(reqVO, AmazonAdRuleDO.class));
    }

    @Override
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public AmazonAdRuleDO getRule(Long id) {
        return ruleMapper.selectById(id);
    }

    @Override
    public PageResult<AmazonAdRuleDO> getRulePage(AdRulePageReqVO reqVO) {
        return ruleMapper.selectPage(reqVO);
    }

    @Override
    public void executeRules(Long shopId) {
        // 委托给 AdRuleEngine 执行完整的规则评估逻辑
        adRuleEngine.executeAllEnabledRules();
    }
}
