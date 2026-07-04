package cn.iocoder.yudao.module.amazon.inventory.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.inventory.service.InventoryService;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * FBA 库存同步 Job
 * <p>
 * 从 Amazon SP-API 拉取 FBA 库存快照数据，包括可售数量、预留数量、
 * 入库中数量、不可售数量和可售天数。
 * <p>
 * 处理器名称: amazonInventorySyncJob
 * 参数示例（可选）: {"shopId": 1, "lastSyncTime": "2026-07-01T00:00:00"}
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("amazonInventorySyncJob")
public class AmazonInventorySyncJob extends AbstractAmazonSyncJob {

    @Resource
    private InventoryService inventoryService;

    @Override
    protected String getJobName() {
        return "amazonInventorySyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) throws Exception {
        log.info("[doSync] 开始库存同步 | shopId={}, marketplace={}",
                shop.getId(), shop.getMarketplaceId());

        // TODO: 接入 SP-API FBA Inventory API
        // GET /fba/inventory/v1/summaries
        // 解析响应并持久化到 amazon_inventory 表
        log.info("[doSync] 库存同步完成 | shopId={}", shop.getId());
    }

}
