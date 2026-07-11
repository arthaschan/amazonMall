package cn.iocoder.yudao.module.amazon.inventory.service;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryDO;
import cn.iocoder.yudao.module.amazon.inventory.dal.mysql.AmazonInventoryMapper;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderItemMapper;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 滞销检测服务实现。
 *
 * <p>检测逻辑：
 * <ol>
 *   <li>获取店铺下所有有库存的 ASIN（availableQty > 0）</li>
 *   <li>查询近 noSaleThreshold 天内有销售的 ASIN 集合</li>
 *   <li>差集即为滞销品</li>
 * </ol>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class SlowMovingDetectorServiceImpl implements SlowMovingDetectorService {

    @Resource
    private AmazonInventoryMapper inventoryMapper;

    @Resource
    private AmazonOrderMapper orderMapper;

    @Resource
    private AmazonOrderItemMapper orderItemMapper;

    @Override
    public List<String> detectSlowMoving(Long shopId, int noSaleThreshold) {
        log.info("[SlowMoving] 开始滞销检测 shopId={}, noSaleThreshold={}天", shopId, noSaleThreshold);

        LocalDate today = LocalDate.now();

        // 1. 获取店铺下所有有库存的 ASIN（取最新快照日期）
        LocalDate latestSnapshot = findLatestSnapshotDate(shopId);
        if (latestSnapshot == null) {
            log.warn("[SlowMoving] 无库存快照数据 shopId={}", shopId);
            return Collections.emptyList();
        }

        List<AmazonInventoryDO> inventoryRecords = inventoryMapper.selectList(
                new LambdaQueryWrapperX<AmazonInventoryDO>()
                        .eq(AmazonInventoryDO::getShopId, shopId)
                        .eq(AmazonInventoryDO::getSnapshotDate, latestSnapshot)
                        .gt(AmazonInventoryDO::getAvailableQty, 0));

        if (inventoryRecords.isEmpty()) {
            log.info("[SlowMoving] 无可用库存记录 shopId={}", shopId);
            return Collections.emptyList();
        }

        // 收集所有有库存的 ASIN
        Set<String> inStockAsins = new HashSet<String>();
        for (AmazonInventoryDO inv : inventoryRecords) {
            if (inv.getAsin() != null && !inv.getAsin().trim().isEmpty()) {
                inStockAsins.add(inv.getAsin());
            }
        }

        log.info("[SlowMoving] 有库存 ASIN 数量: {}", inStockAsins.size());

        // 2. 查询近 noSaleThreshold 天内有销售的 ASIN
        LocalDateTime startDate = today.minusDays(noSaleThreshold).atStartOfDay();

        List<AmazonOrderDO> recentOrders = orderMapper.selectList(
                new LambdaQueryWrapperX<AmazonOrderDO>()
                        .eq(AmazonOrderDO::getShopId, shopId)
                        .ge(AmazonOrderDO::getPurchaseDate, startDate)
                        .notIn(AmazonOrderDO::getOrderStatus,
                                Arrays.asList("Canceled", "Unfulfillable")));

        Set<String> soldAsins = new HashSet<String>();
        if (!recentOrders.isEmpty()) {
            List<Long> orderIds = new ArrayList<Long>();
            for (AmazonOrderDO order : recentOrders) {
                orderIds.add(order.getId());
            }

            // 分批查询 order items（避免 IN 子句过大）
            int batchSize = 500;
            for (int i = 0; i < orderIds.size(); i += batchSize) {
                int end = Math.min(i + batchSize, orderIds.size());
                List<Long> batchIds = orderIds.subList(i, end);

                List<AmazonOrderItemDO> items = orderItemMapper.selectList(
                        new LambdaQueryWrapperX<AmazonOrderItemDO>()
                                .in(AmazonOrderItemDO::getOrderId, batchIds));

                for (AmazonOrderItemDO item : items) {
                    if (item.getAsin() != null && !item.getAsin().trim().isEmpty()) {
                        soldAsins.add(item.getAsin());
                    }
                }
            }
        }

        log.info("[SlowMoving] {}天内有销售的 ASIN 数量: {}", noSaleThreshold, soldAsins.size());

        // 3. 差集 = 有库存但无销售 → 滞销品
        List<String> slowMovingAsins = new ArrayList<String>();
        for (String asin : inStockAsins) {
            if (!soldAsins.contains(asin)) {
                slowMovingAsins.add(asin);
            }
        }

        Collections.sort(slowMovingAsins);
        log.info("[SlowMoving] 滞销检测完成 shopId={}, 滞销ASIN数={}, 总库存ASIN数={}",
                shopId, slowMovingAsins.size(), inStockAsins.size());

        return slowMovingAsins;
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
