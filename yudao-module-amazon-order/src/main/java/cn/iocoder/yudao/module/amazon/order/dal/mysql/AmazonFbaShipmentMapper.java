package cn.iocoder.yudao.module.amazon.order.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.FbaShipmentPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonFbaShipmentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AmazonFbaShipmentMapper extends BaseMapperX<AmazonFbaShipmentDO> {

    default PageResult<AmazonFbaShipmentDO> selectPage(FbaShipmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonFbaShipmentDO>()
                .likeIfPresent(AmazonFbaShipmentDO::getShipmentId, reqVO.getShipmentId())
                .eqIfPresent(AmazonFbaShipmentDO::getShopId, reqVO.getShopId())
                .eqIfPresent(AmazonFbaShipmentDO::getStatus, reqVO.getStatus())
                .orderByDesc(AmazonFbaShipmentDO::getId));
    }
}
