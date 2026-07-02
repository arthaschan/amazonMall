package cn.iocoder.yudao.module.amazon.listing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.listing.controller.admin.vo.ProductPageReqVO;
import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonProductDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonProductMapper extends BaseMapperX<AmazonProductDO> {

    default PageResult<AmazonProductDO> selectPage(ProductPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonProductDO>()
                .likeIfPresent(AmazonProductDO::getAsin, reqVO.getAsin())
                .likeIfPresent(AmazonProductDO::getTitle, reqVO.getTitle())
                .eqIfPresent(AmazonProductDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonProductDO::getMarketplaceId, reqVO.getMarketplaceId())
                .eqIfPresent(AmazonProductDO::getListingStatus, reqVO.getListingStatus())
                .orderByDesc(AmazonProductDO::getId));
    }
}
