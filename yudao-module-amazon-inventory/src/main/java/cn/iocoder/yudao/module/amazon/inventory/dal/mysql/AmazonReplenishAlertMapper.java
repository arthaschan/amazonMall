package cn.iocoder.yudao.module.amazon.inventory.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.ReplenishAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonReplenishAlertDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonReplenishAlertMapper extends BaseMapperX<AmazonReplenishAlertDO> {

    default PageResult<AmazonReplenishAlertDO> selectPage(ReplenishAlertPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonReplenishAlertDO>()
                .eqIfPresent(AmazonReplenishAlertDO::getShopId, reqVO.getShopId())
                .likeIfPresent(AmazonReplenishAlertDO::getAsin, reqVO.getAsin())
                .eqIfPresent(AmazonReplenishAlertDO::getAlertType, reqVO.getAlertType())
                .eqIfPresent(AmazonReplenishAlertDO::getAcknowledged, reqVO.getAcknowledged())
                .orderByDesc(AmazonReplenishAlertDO::getId));
    }

    default List<AmazonReplenishAlertDO> selectUnacknowledged(Long shopId) {
        return selectList(new LambdaQueryWrapperX<AmazonReplenishAlertDO>()
                .eq(AmazonReplenishAlertDO::getShopId, shopId)
                .eq(AmazonReplenishAlertDO::getAcknowledged, false));
    }
}
