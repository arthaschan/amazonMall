package cn.iocoder.yudao.module.amazon.review.event;

import cn.iocoder.yudao.module.amazon.common.sync.ShopSyncTriggeredEvent;
import cn.iocoder.yudao.module.amazon.review.service.ReviewSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 评论模块同步事件监听器。
 * <p>监听 {@link ShopSyncTriggeredEvent}，触发评论同步。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class ReviewSyncEventListener {

    @Resource
    private ReviewSyncService reviewSyncService;

    @EventListener
    public void onShopSync(ShopSyncTriggeredEvent event) {
        log.info("[ReviewEventListener] 收到店铺同步事件 shopId={}, marketplaceId={}",
                event.getShopId(), event.getMarketplaceId());
        try {
            reviewSyncService.syncReviews(event.getShopId(), event.getMarketplaceId());
        } catch (Exception e) {
            log.error("[ReviewEventListener] 评论同步失败 shopId={}", event.getShopId(), e);
        }
    }
}
