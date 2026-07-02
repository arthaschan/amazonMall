package cn.iocoder.yudao.module.amazon.listing.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonListingVersionMapper extends BaseMapperX<AmazonListingVersionDO> {

    default List<AmazonListingVersionDO> selectByProductId(Long productId) {
        return selectList(AmazonListingVersionDO::getProductId, productId);
    }
}
