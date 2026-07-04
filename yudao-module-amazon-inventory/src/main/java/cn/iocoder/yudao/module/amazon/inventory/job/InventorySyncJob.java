package cn.iocoder.yudao.module.amazon.inventory.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.inventory.service.InventorySyncService;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 库存同步定时任务。
 *
 * <p>遍历所有已启用店铺，从 SP-API 拉取最新 FBA 库存数据并持久化。
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("inventorySyncJob")
public class InventorySyncJob extends AbstractAmazonSyncJob {

    @Resource
    private InventorySyncService inventorySyncService;

    @Override
    protected String getJobName() {
        return "inventorySyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) {
        inventorySyncService.syncInventory(shop.getId(), shop.getMarketplaceId());
    }
}
