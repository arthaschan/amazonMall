package cn.iocoder.yudao.module.amazon.order.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonOrderItemMapper extends BaseMapperX<AmazonOrderItemDO> {

    default List<AmazonOrderItemDO> selectByOrderId(Long orderId) {
        return selectList(AmazonOrderItemDO::getOrderId, orderId);
    }
}
