package cn.iocoder.yudao.module.amazon.listing.event;

import cn.iocoder.yudao.module.amazon.common.sync.ShopSyncTriggeredEvent;
import cn.iocoder.yudao.module.amazon.listing.service.ListingSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Listing 模块同步事件监听器。
 * <p>监听 {@link ShopSyncTriggeredEvent}，触发商品 Listing 同步。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class ListingSyncEventListener {

    @Resource
    private ListingSyncService listingSyncService;

    @EventListener
    public void onShopSync(ShopSyncTriggeredEvent event) {
        log.info("[ListingEventListener] 收到店铺同步事件 shopId={}, marketplaceId={}",
                event.getShopId(), event.getMarketplaceId());
        try {
            listingSyncService.syncListings(event.getShopId(), event.getMarketplaceId());
        } catch (Exception e) {
            log.error("[ListingEventListener] Listing 同步失败 shopId={}", event.getShopId(), e);
        }
    }
}
