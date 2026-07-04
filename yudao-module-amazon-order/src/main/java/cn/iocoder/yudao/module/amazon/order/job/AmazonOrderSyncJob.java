package cn.iocoder.yudao.module.amazon.order.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.order.service.OrderSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 订单同步 Job
 * <p>
 * 从 Amazon SP-API 拉取最新订单数据并同步到本地数据库。
 * 自动遍历所有已启用店铺，逐个执行订单同步。
 * <p>
 * 在 Yudao 管理后台注册 Job 时：
 * - 处理器名称: amazonOrderSyncJob
 * - 参数示例（可选）: {"shopId": 1, "marketplaceId": "ATVPDKIKX0DER"}
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("amazonOrderSyncJob")
public class AmazonOrderSyncJob extends AbstractAmazonSyncJob {

    @Resource
    private OrderSyncService orderSyncService;

    @Override
    protected String getJobName() {
        return "amazonOrderSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) throws Exception {
        log.info("[doSync] 开始订单同步 | shopId={}, marketplace={}",
                shop.getId(), shop.getMarketplaceId());

        // 调用 OrderSyncService 执行 SP-API 订单同步
        orderSyncService.syncOrders(shop.getId(), shop.getMarketplaceId());

        log.info("[doSync] 订单同步完成 | shopId={}", shop.getId());
    }

}
