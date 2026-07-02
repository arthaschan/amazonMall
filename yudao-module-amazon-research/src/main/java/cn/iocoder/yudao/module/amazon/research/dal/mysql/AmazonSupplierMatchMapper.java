package cn.iocoder.yudao.module.amazon.research.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonSupplierMatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonSupplierMatchMapper extends BaseMapperX<AmazonSupplierMatchDO> {

    default List<AmazonSupplierMatchDO> selectByOpportunityId(Long opportunityId) {
        return selectList(AmazonSupplierMatchDO::getOpportunityId, opportunityId);
    }
}
