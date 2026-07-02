package cn.iocoder.yudao.module.amazon.inventory.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.InventoryPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonInventoryMapper extends BaseMapperX<AmazonInventoryDO> {

    default PageResult<AmazonInventoryDO> selectPage(InventoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AmazonInventoryDO>()
                .eqIfPresent(AmazonInventoryDO::getShopId, reqVO.getShopId())
                .likeIfPresent(AmazonInventoryDO::getAsin, reqVO.getAsin())
                .likeIfPresent(AmazonInventoryDO::getSku, reqVO.getSku())
                .eqIfPresent(AmazonInventoryDO::getSnapshotDate, reqVO.getSnapshotDate())
                .orderByAsc(AmazonInventoryDO::getDaysOfSupply));
    }

    default List<AmazonInventoryDO> selectByAsin(Long shopId, String asin) {
        return selectList(new LambdaQueryWrapperX<AmazonInventoryDO>()
                .eq(AmazonInventoryDO::getShopId, shopId)
                .eq(AmazonInventoryDO::getAsin, asin));
    }
}
