package cn.iocoder.yudao.module.amazon.research.service;

import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonSupplierMatchDO;

import java.util.List;

/**
 * 供应商匹配服务。
 * <p>根据产品机会从供应平台搜索匹配供应商。</p>
 *
 * @author AmazonOps AI
 */
public interface SupplierMatchService {

    /**
     * 为指定产品机会搜索供应商。
     *
     * @param opportunityId 产品机会 ID
     * @return 匹配的供应商列表
     */
    List<AmazonSupplierMatchDO> matchSuppliers(Long opportunityId);

    List<AmazonSupplierMatchDO> getByOpportunityId(Long opportunityId);
}
