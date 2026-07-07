package cn.iocoder.yudao.module.amazon.order.event;

import cn.iocoder.yudao.module.amazon.common.sync.ShopSyncTriggeredEvent;
import cn.iocoder.yudao.module.amazon.order.service.FbaShipmentService;
import cn.iocoder.yudao.module.amazon.order.service.OrderSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单模块同步事件监听器。
 * <p>监听 {@link ShopSyncTriggeredEvent}，触发订单和 FBA 货件同步。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component
public class OrderSyncEventListener {

    @Resource
    private OrderSyncService orderSyncService;

    @Resource
    private FbaShipmentService fbaShipmentService;

    @EventListener
    public void onShopSync(ShopSyncTriggeredEvent event) {
        log.info("[OrderEventListener] 收到店铺同步事件 shopId={}, marketplaceId={}",
                event.getShopId(), event.getMarketplaceId());

        // 同步订单
        try {
            orderSyncService.syncOrders(event.getShopId(), event.getMarketplaceId());
        } catch (Exception e) {
            log.error("[OrderEventListener] 订单同步失败 shopId={}", event.getShopId(), e);
        }

        // 同步 FBA 货件
        try {
            fbaShipmentService.syncShipments(event.getShopId());
        } catch (Exception e) {
            log.error("[OrderEventListener] FBA 货件同步失败 shopId={}", event.getShopId(), e);
        }
    }
}
