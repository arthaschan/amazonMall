package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryForecastDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryForecastMapper;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderItemMapper;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存预测服务实现。
 *
 * <p>基于历史订单销量数据，采用加权移动平均法预测日均销量，
 * 并据此计算再订购点、安全库存和建议补货量。</p>
 *
 * <p>核心公式：
 * <ul>
 *   <li>日均销量 = 近 N 天总销量 / N</li>
 *   <li>安全库存 = Z × σ × √(LT)，Z=1.65（95% 服务水平），LT=备货周期</li>
 *   <li>再订购点 = 日均销量 × LT + 安全库存</li>
 *   <li>建议补货量 = 日均销量 × (LT + 覆盖天数) + 安全库存 - 当前库存</li>
 * </ul>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class InventoryForecastServiceImpl implements InventoryForecastService {

    /** 历史销量回溯天数 */
    private static final int LOOKBACK_DAYS = 30;
    /** 默认备货周期（天） */
    private static final int DEFAULT_LEAD_TIME_DAYS = 14;
    /** 补货覆盖天数（补一次货希望覆盖的销售天数） */
    private static final int COVERAGE_DAYS = 30;
    /** 服务水平 Z 值（95% → 1.65） */
    private static final double Z_SCORE_95 = 1.65;

    @Resource
    private AmazonInventoryForecastMapper forecastMapper;

    @Resource
    private AmazonOrderMapper orderMapper;

    @Resource
    private AmazonOrderItemMapper orderItemMapper;

    @Override
    public AmazonInventoryForecastDO generateForecast(Long shopId, String asin) {
        log.info("[InventoryForecast] 生成预测 shopId={}, asin={}", shopId, asin);

        // 1. 查询近 N 天该 ASIN 的每日销量
        Map<LocalDate, Integer> dailySales = queryDailySales(shopId, asin, LOOKBACK_DAYS);

        // 2. 计算日均销量和标准差
        double avgDailySales = calculateAverage(dailySales, LOOKBACK_DAYS);
        double stdDev = calculateStdDev(dailySales, LOOKBACK_DAYS, avgDailySales);

        // 3. 计算预测指标
        int leadTimeDays = DEFAULT_LEAD_TIME_DAYS;
        int safetyStock = (int) Math.ceil(Z_SCORE_95 * stdDev * Math.sqrt(leadTimeDays));
        int reorderPoint = (int) Math.ceil(avgDailySales * leadTimeDays + safetyStock);
        int suggestedReorderQty = (int) Math.ceil(
                avgDailySales * (leadTimeDays + COVERAGE_DAYS) + safetyStock);

        // 确保最小值
        safetyStock = Math.max(safetyStock, 1);
        reorderPoint = Math.max(reorderPoint, safetyStock);
        suggestedReorderQty = Math.max(suggestedReorderQty, reorderPoint);

        // 4. 计算置信度（基于数据充足性）
        BigDecimal confidence = calculateConfidence(dailySales, LOOKBACK_DAYS);

        // 5. 构建预测 DO
        AmazonInventoryForecastDO forecast = new AmazonInventoryForecastDO();
        forecast.setShopId(shopId);
        forecast.setAsin(asin);
        forecast.setForecastDate(LocalDate.now().plusDays(7));
        forecast.setPredictedDailySales(
                BigDecimal.valueOf(avgDailySales).setScale(2, RoundingMode.HALF_UP));
        forecast.setConfidence(confidence);
        forecast.setReorderPoint(reorderPoint);
        forecast.setSafetyStock(safetyStock);
        forecast.setSuggestedReorderQty(suggestedReorderQty);
        forecast.setLeadTimeDays(leadTimeDays);
        forecast.setGenerateDate(LocalDate.now());
        forecast.setTenantId(0L);

        forecastMapper.insert(forecast);
        log.info("[InventoryForecast] 预测完成 shopId={}, asin={}, avgDailySales={}, safetyStock={}, reorderPoint={}, suggestedQty={}",
                shopId, asin, avgDailySales, safetyStock, reorderPoint, suggestedReorderQty);

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

    /**
     * 查询指定 ASIN 在回溯天数内的每日销量。
     * <p>从 amazon_order + amazon_order_item 关联查询，按 purchaseDate 聚合。</p>
     */
    private Map<LocalDate, Integer> queryDailySales(Long shopId, String asin, int lookbackDays) {
        LocalDateTime startDate = LocalDate.now().minusDays(lookbackDays).atStartOfDay();

        // 1. 查询时间范围内的订单
        List<AmazonOrderDO> orders = orderMapper.selectList(
                new LambdaQueryWrapperX<AmazonOrderDO>()
                        .eq(AmazonOrderDO::getShopId, shopId)
                        .ge(AmazonOrderDO::getPurchaseDate, startDate)
                        .notIn(AmazonOrderDO::getOrderStatus,
                                Arrays.asList("Canceled", "Unfulfillable")));

        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 获取订单 ID 列表
        List<Long> orderIds = new ArrayList<Long>();
        Map<Long, LocalDate> orderIdToDate = new HashMap<Long, LocalDate>();
        for (AmazonOrderDO order : orders) {
            orderIds.add(order.getId());
            if (order.getPurchaseDate() != null) {
                orderIdToDate.put(order.getId(), order.getPurchaseDate().toLocalDate());
            }
        }

        // 3. 查询这些订单中该 ASIN 的明细
        List<AmazonOrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapperX<AmazonOrderItemDO>()
                        .in(AmazonOrderItemDO::getOrderId, orderIds)
                        .eq(AmazonOrderItemDO::getAsin, asin));

        // 4. 按日期聚合销量
        Map<LocalDate, Integer> dailySales = new HashMap<LocalDate, Integer>();
        for (AmazonOrderItemDO item : items) {
            LocalDate date = orderIdToDate.get(item.getOrderId());
            if (date != null && item.getQuantity() != null) {
                Integer current = dailySales.get(date);
                dailySales.put(date, (current != null ? current : 0) + item.getQuantity());
            }
        }

        return dailySales;
    }

    /**
     * 计算日均销量（含零销日）。
     */
    private double calculateAverage(Map<LocalDate, Integer> dailySales, int totalDays) {
        int totalQty = 0;
        for (Integer qty : dailySales.values()) {
            totalQty += qty;
        }
        return (double) totalQty / totalDays;
    }

    /**
     * 计算日销量标准差（含零销日）。
     */
    private double calculateStdDev(Map<LocalDate, Integer> dailySales, int totalDays, double mean) {
        double sumSqDiff = 0.0;
        LocalDate startDate = LocalDate.now().minusDays(totalDays);

        for (int i = 0; i < totalDays; i++) {
            LocalDate date = startDate.plusDays(i);
            Integer qty = dailySales.get(date);
            double dailyQty = (qty != null) ? qty.doubleValue() : 0.0;
            double diff = dailyQty - mean;
            sumSqDiff += diff * diff;
        }

        return Math.sqrt(sumSqDiff / totalDays);
    }

    /**
     * 计算预测置信度（0-1），基于数据充足性。
     * <ul>
     *   <li>有销售的天数占比越高，置信度越高</li>
     *   <li>30 天内有 15+ 天销售 → 0.85+</li>
     *   <li>不足 5 天销售 → 0.3</li>
     * </ul>
     */
    private BigDecimal calculateConfidence(Map<LocalDate, Integer> dailySales, int totalDays) {
        int activeDays = dailySales.size();
        double coverage = (double) activeDays / totalDays;

        double confidence;
        if (coverage >= 0.5) {
            confidence = 0.85 + coverage * 0.1; // 0.85 ~ 0.95
        } else if (coverage >= 0.2) {
            confidence = 0.5 + coverage * 0.7; // 0.5 ~ 0.64
        } else if (activeDays > 0) {
            confidence = 0.3;
        } else {
            confidence = 0.1;
        }

        return BigDecimal.valueOf(Math.min(confidence, 0.95)).setScale(2, RoundingMode.HALF_UP);
    }
}
