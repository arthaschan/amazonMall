package cn.iocoder.yudao.module.amazon.research.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonFinancialProjectionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonFinancialProjectionMapper extends BaseMapperX<AmazonFinancialProjectionDO> {

    default AmazonFinancialProjectionDO selectByOpportunityId(Long opportunityId) {
        return selectOne(AmazonFinancialProjectionDO::getOpportunityId, opportunityId);
    }
}
