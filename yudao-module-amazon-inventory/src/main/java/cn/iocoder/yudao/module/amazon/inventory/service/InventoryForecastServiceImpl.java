package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryForecastDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryForecastMapper;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InventoryForecastServiceImpl implements InventoryForecastService {

    @Resource
    private AmazonInventoryForecastMapper forecastMapper;

    @Override
    public AmazonInventoryForecastDO generateForecast(Long shopId, String asin) {
        // TODO: 基于历史销量数据进行时间序列预测
        AmazonInventoryForecastDO forecast = new AmazonInventoryForecastDO();
        forecast.setShopId(shopId);
        forecast.setAsin(asin);
        forecast.setForecastDate(LocalDate.now().plusDays(7));
        forecast.setPredictedDailySales(BigDecimal.ZERO);
        forecast.setConfidence(new BigDecimal("0.8"));
        forecast.setReorderPoint(50);
        forecast.setSafetyStock(20);
        forecast.setSuggestedReorderQty(100);
        forecast.setLeadTimeDays(14);
        forecast.setGenerateDate(LocalDate.now());
        forecast.setTenantId(0L);
        forecastMapper.insert(forecast);
        return forecast;
    }

    @Override
    public List<AmazonInventoryForecastDO> getForecasts(Long shopId, String asin) {
        return forecastMapper.selectByAsin(shopId, asin);
    }

    @Override
    public AmazonInventoryForecastDO getLatestForecast(Long shopId, String asin) {
        return forecastMapper.selectLatest(shopId, asin);
    }
}
