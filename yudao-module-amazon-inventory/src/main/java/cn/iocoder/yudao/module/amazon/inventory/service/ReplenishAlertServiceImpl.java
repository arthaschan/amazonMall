package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.ReplenishAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryForecastDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonReplenishAlertDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryForecastMapper;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryMapper;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonReplenishAlertMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;

/**
 * 补货预警服务实现。
 *
 * <p>扫描逻辑：
 * <ol>
 *   <li>获取店铺最新库存快照中所有 ASIN 的当前可售库存</li>
 *   <li>获取每个 ASIN 的库存预测（再订购点、安全库存）</li>
 *   <li>对比当前库存与再订购点，生成预警</li>
 *   <li>预警类型：OUT_OF_STOCK（库存=0）、LOW_STOCK（库存 < 再订购点）</li>
 * </ol>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class ReplenishAlertServiceImpl implements ReplenishAlertService {

    @Resource
    private AmazonReplenishAlertMapper alertMapper;

    @Resource
    private AmazonInventoryMapper inventoryMapper;

    @Resource
    private AmazonInventoryForecastMapper forecastMapper;

    @Override
    public PageResult<AmazonReplenishAlertDO> getAlertPage(ReplenishAlertPageReqVO reqVO) {
        return alertMapper.selectPage(reqVO);
    }

    @Override
    public List<AmazonReplenishAlertDO> getUnacknowledgedAlerts(Long shopId) {
        return alertMapper.selectUnacknowledged(shopId);
    }

    @Override
    public void acknowledgeAlert(Long alertId) {
        AmazonReplenishAlertDO alert = alertMapper.selectById(alertId);
        if (alert != null) {
            alert.setAcknowledged(true);
            alertMapper.updateById(alert);
        }
    }

    @Override
    public void scanAndAlert(Long shopId) {
        log.info("[ReplenishAlert] 开始补货预警扫描 shopId={}", shopId);

        // 1. 获取最新库存快照
        LocalDate latestSnapshot = findLatestSnapshotDate(shopId);
        if (latestSnapshot == null) {
            log.warn("[ReplenishAlert] 无库存快照数据 shopId={}", shopId);
            return;
        }

        List<AmazonInventoryDO> inventoryRecords = inventoryMapper.selectList(
                new LambdaQueryWrapperX<AmazonInventoryDO>()
                        .eq(AmazonInventoryDO::getShopId, shopId)
                        .eq(AmazonInventoryDO::getSnapshotDate, latestSnapshot));

        if (inventoryRecords.isEmpty()) {
            log.info("[ReplenishAlert] 无库存记录 shopId={}", shopId);
            return;
        }

        // 2. 按 ASIN 汇总库存（可能多仓库）
        Map<String, Integer> asinTotalQty = new HashMap<String, Integer>();
        for (AmazonInventoryDO inv : inventoryRecords) {
            String asin = inv.getAsin();
            if (asin == null || asin.trim().isEmpty()) continue;
            int qty = inv.getAvailableQty() != null ? inv.getAvailableQty() : 0;
            Integer current = asinTotalQty.get(asin);
            asinTotalQty.put(asin, (current != null ? current : 0) + qty);
        }

        // 3. 清除当天该店铺的旧预警（保证幂等）
        int deleted = alertMapper.delete(
                new LambdaQueryWrapperX<AmazonReplenishAlertDO>()
                        .eq(AmazonReplenishAlertDO::getShopId, shopId)
                        .eq(AmazonReplenishAlertDO::getAcknowledged, false));
        if (deleted > 0) {
            log.info("[ReplenishAlert] 已清除 {} 条旧预警", deleted);
        }

        // 4. 逐 ASIN 检查并生成预警
        int alertCount = 0;
        for (Map.Entry<String, Integer> entry : asinTotalQty.entrySet()) {
            String asin = entry.getKey();
            int currentQty = entry.getValue();

            // 获取该 ASIN 的最新预测（再订购点）
            AmazonInventoryForecastDO forecast = forecastMapper.selectLatest(shopId, asin);
            int reorderPoint = (forecast != null && forecast.getReorderPoint() != null)
                    ? forecast.getReorderPoint() : 10; // 默认再订购点
            int suggestedQty = (forecast != null && forecast.getSuggestedReorderQty() != null)
                    ? forecast.getSuggestedReorderQty() : 50;

            String alertType = null;
            if (currentQty <= 0) {
                alertType = "OUT_OF_STOCK";
            } else if (currentQty < reorderPoint) {
                alertType = "LOW_STOCK";
            }

            if (alertType != null) {
                AmazonReplenishAlertDO alert = new AmazonReplenishAlertDO();
                alert.setShopId(shopId);
                alert.setTenantId(0L);
                alert.setAsin(asin);
                alert.setAlertType(alertType);
                alert.setCurrentQty(currentQty);
                alert.setReorderPoint(reorderPoint);
                alert.setSuggestedQty(Math.max(suggestedQty - currentQty, 0));
                alert.setAcknowledged(false);
                alertMapper.insert(alert);
                alertCount++;
            }
        }

        log.info("[ReplenishAlert] 补货预警扫描完成 shopId={}, 总ASIN={}, 预警数={}",
                shopId, asinTotalQty.size(), alertCount);
    }

    /**
     * 查找店铺最新的库存快照日期。
     */
    private LocalDate findLatestSnapshotDate(Long shopId) {
        List<AmazonInventoryDO> latest = inventoryMapper.selectList(
                new LambdaQueryWrapperX<AmazonInventoryDO>()
                        .eq(AmazonInventoryDO::getShopId, shopId)
                        .orderByDesc(AmazonInventoryDO::getSnapshotDate)
                        .last("LIMIT 1"));

        return (latest != null && !latest.isEmpty()) ? latest.get(0).getSnapshotDate() : null;
    }
}
