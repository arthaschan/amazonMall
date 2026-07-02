package cn.iocoder.yudao.module.amazon.listing.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonAutoPriceRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonAutoPriceRuleMapper extends BaseMapperX<AmazonAutoPriceRuleDO> {

    default List<AmazonAutoPriceRuleDO> selectByProductId(Long productId) {
        return selectList(AmazonAutoPriceRuleDO::getProductId, productId);
    }
}
