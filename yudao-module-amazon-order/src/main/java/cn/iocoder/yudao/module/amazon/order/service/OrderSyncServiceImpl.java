package cn.iocoder.yudao.module.amazon.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单同步实现。
 * <p>通过 SP-API 拉取订单数据并持久化到本地数据库。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class OrderSyncServiceImpl implements OrderSyncService {

    @Override
    public void syncOrders(Long shopId, String marketplaceId) {
        // TODO: 调用 SP-API OrdersApi.getOrders() 分页拉取并持久化
    }
}
