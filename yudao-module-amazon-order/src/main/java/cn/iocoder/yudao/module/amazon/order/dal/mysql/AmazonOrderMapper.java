package cn.iocoder.yudao.module.amazon.order.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonOrderMapper extends BaseMapperX<AmazonOrderDO> {

    default PageResult<AmazonOrderDO> selectPage(OrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonOrderDO>()
                .likeIfPresent(AmazonOrderDO::getAmazonOrderId, reqVO.getAmazonOrderId())
                .eqIfPresent(AmazonOrderDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonOrderDO::getMarketplaceId, reqVO.getMarketplaceId())
                .eqIfPresent(AmazonOrderDO::getOrderStatus, reqVO.getOrderStatus())
                .betweenIfPresent(AmazonOrderDO::getPurchaseDate, reqVO.getPurchaseDateStart(), reqVO.getPurchaseDateEnd())
                .orderByDesc(AmazonOrderDO::getPurchaseDate));
    }

    default AmazonOrderDO selectByAmazonOrderId(String amazonOrderId) {
        return selectOne(AmazonOrderDO::getAmazonOrderId, amazonOrderId);
    }
}
