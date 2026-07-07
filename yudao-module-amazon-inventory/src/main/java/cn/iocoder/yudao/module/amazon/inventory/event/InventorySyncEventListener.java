package cn.iocoder.yudao.module.amazon.inventory.event;

import cn.iocoder.yudao.module.amazon.common.sync.ShopSyncTriggeredEvent;
import cn.iocoder.yudao.module.amazon.inventory.service.InventorySyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 库存模块同步事件监听器。
 * <p>监听 {@link ShopSyncTriggeredEvent}，触发 FBA 库存同步。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class InventorySyncEventListener {

    @Resource
    private InventorySyncService inventorySyncService;

    @EventListener
    public void onShopSync(ShopSyncTriggeredEvent event) {
        log.info("[InventoryEventListener] 收到店铺同步事件 shopId={}, marketplaceId={}",
                event.getShopId(), event.getMarketplaceId());
        try {
            inventorySyncService.syncInventory(event.getShopId(), event.getMarketplaceId());
        } catch (Exception e) {
            log.error("[InventoryEventListener] 库存同步失败 shopId={}", event.getShopId(), e);
        }
    }
}
