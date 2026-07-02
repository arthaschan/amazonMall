package cn.iocoder.yudao.module.amazon.order.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.order.service.OrderSyncService;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单同步定时任务。
 *
 * <p>遍历所有已启用店铺，从 SP-API 拉取最新订单数据并持久化。
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("orderSyncJob")
public class OrderSyncJob extends AbstractAmazonSyncJob {

    @Resource
    private OrderSyncService orderSyncService;

    @Override
    protected String getJobName() {
        return "orderSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) {
        orderSyncService.syncOrders(shop.getId(), shop.getMarketplaceId());
    }
}
