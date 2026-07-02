package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdRulePageReqVO;
import cn.iocoder.yudao.module.amazon.ad.controller.admin.vo.AdRuleSaveReqVO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdRuleDO;

/**
 * 广告规则引擎 Service。
 *
 * @author AmazonOps AI
 */
public interface AdRuleEngineService {

    Long createRule(AdRuleSaveReqVO reqVO);

    void updateRule(AdRuleSaveReqVO reqVO);

    void deleteRule(Long id);

    AmazonAdRuleDO getRule(Long id);

    PageResult<AmazonAdRuleDO> getRulePage(AdRulePageReqVO reqVO);

    /** 执行所有启用的规则 */
    void executeRules(Long shopId);
}
