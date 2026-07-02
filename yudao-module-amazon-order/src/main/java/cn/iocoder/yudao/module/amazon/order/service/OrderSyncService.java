package cn.iocoder.yudao.module.amazon.order.service;

/**
 * 订单同步服务。
 * <p>从 SP-API 拉取订单数据并持久化。</p>
 *
 * @author AmazonOps AI
 */
public interface OrderSyncService {

    /**
     * 同步指定店铺的订单。
     *
     * @param shopId        店铺 ID
     * @param marketplaceId 站点 ID
     */
    void syncOrders(Long shopId, String marketplaceId);
}
