package cn.iocoder.yudao.module.amazon.inventory.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryForecastDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AmazonInventoryForecastMapper extends BaseMapperX<AmazonInventoryForecastDO> {

    default List<AmazonInventoryForecastDO> selectByAsin(Long shopId, String asin) {
        return selectList(new LambdaQueryWrapperX<AmazonInventoryForecastDO>()
                .eq(AmazonInventoryForecastDO::getShopId, shopId)
                .eq(AmazonInventoryForecastDO::getAsin, asin)
                .orderByAsc(AmazonInventoryForecastDO::getForecastDate));
    }

    default AmazonInventoryForecastDO selectLatest(Long shopId, String asin) {
        return selectOne(new LambdaQueryWrapperX<AmazonInventoryForecastDO>()
                .eq(AmazonInventoryForecastDO::getShopId, shopId)
                .eq(AmazonInventoryForecastDO::getAsin, asin)
                .orderByDesc(AmazonInventoryForecastDO::getGenerateDate)
                .last("LIMIT 1"));
    }
}
