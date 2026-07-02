package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryForecastDO;

import java.util.List;

/**
 * 库存预测 Service。
 * <p>基于历史销量和趋势模型预测库存需求。</p>
 *
 * @author AmazonOps AI
 */
public interface InventoryForecastService {

    /** 为指定 ASIN 生成库存预测 */
    AmazonInventoryForecastDO generateForecast(Long shopId, String asin);

    List<AmazonInventoryForecastDO> getForecasts(Long shopId, String asin);

    AmazonInventoryForecastDO getLatestForecast(Long shopId, String asin);
}
