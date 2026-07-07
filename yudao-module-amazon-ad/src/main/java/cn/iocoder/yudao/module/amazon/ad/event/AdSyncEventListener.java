package cn.iocoder.yudao.module.amazon.ad.event;

import cn.iocoder.yudao.module.amazon.ad.service.AdCampaignService;
import cn.iocoder.yudao.module.amazon.common.sync.ShopSyncTriggeredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 广告模块同步事件监听器。
 * <p>监听 {@link ShopSyncTriggeredEvent}，触发广告活动同步。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class AdSyncEventListener {

    @Resource
    private AdCampaignService adCampaignService;

    @EventListener
    public void onShopSync(ShopSyncTriggeredEvent event) {
        log.info("[AdEventListener] 收到店铺同步事件 shopId={}", event.getShopId());
        try {
            adCampaignService.syncCampaigns(event.getShopId());
        } catch (Exception e) {
            log.error("[AdEventListener] 广告活动同步失败 shopId={}", event.getShopId(), e);
        }
    }
}
