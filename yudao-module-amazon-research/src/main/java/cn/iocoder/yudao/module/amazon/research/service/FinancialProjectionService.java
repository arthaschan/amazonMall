package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonFinancialProjectionDO;

/**
 * 财务预测服务。
 * <p>根据产品机会数据生成 52 周财务预测模型。</p>
 *
 * @author AmazonOps AI
 */
public interface FinancialProjectionService {

    /**
     * 为指定产品机会生成财务预测。
     *
     * @param opportunityId 产品机会 ID
     * @return 生成的预测记录
     */
    AmazonFinancialProjectionDO generateProjection(Long opportunityId);

    AmazonFinancialProjectionDO getProjection(Long opportunityId);
}
