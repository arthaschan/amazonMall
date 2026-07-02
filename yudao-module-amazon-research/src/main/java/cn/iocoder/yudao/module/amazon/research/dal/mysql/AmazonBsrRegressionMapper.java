package cn.iocoder.yudao.module.amazon.research.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.research.dal.dataobject.AmazonBsrRegressionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonBsrRegressionMapper extends BaseMapperX<AmazonBsrRegressionDO> {

    default AmazonBsrRegressionDO selectByCategoryAndMarketplace(String categoryId, String marketplaceId) {
        return selectOne(new LambdaQueryWrapperX<AmazonBsrRegressionDO>()
                .eq(AmazonBsrRegressionDO::getCategoryId, categoryId)
                .eq(AmazonBsrRegressionDO::getMarketplaceId, marketplaceId));
    }
}
