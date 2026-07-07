package cn.iocoder.yudao.module.amazon.common.sync;

import org.springframework.context.ApplicationEvent;

/**
 * 店铺数据同步触发事件。
 * <p>当 {@code AmazonShopServiceImpl.syncShopData()} 被调用时发布此事件，
 * 各业务模块通过 {@code @EventListener} 监听并执行各自的数据同步逻辑。</p>
 *
 * <p>使用示例（在监听模块中）：
 * <pre>
 * &#64;EventListener
 * public void onShopSync(ShopSyncTriggeredEvent event) {
 *     orderSyncService.syncOrders(event.getShopId(), event.getMarketplaceId());
 * }
 * </pre>
 *
 * @author AmazonOps AI
 */
public class ShopSyncTriggeredEvent extends ApplicationEvent {

    private final Long shopId;
    private final String marketplaceId;
    private final String sellerId;

    public ShopSyncTriggeredEvent(Object source, Long shopId, String marketplaceId, String sellerId) {
        super(source);
        this.shopId = shopId;
        this.marketplaceId = marketplaceId;
        this.sellerId = sellerId;
    }

    public Long getShopId() {
        return shopId;
    }

    public String getMarketplaceId() {
        return marketplaceId;
    }

    public String getSellerId() {
        return sellerId;
    }
}
