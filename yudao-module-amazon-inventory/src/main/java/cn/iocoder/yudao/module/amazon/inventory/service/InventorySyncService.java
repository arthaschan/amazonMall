package cn.iocoder.yudao.module.amazon.inventory.service;

/**
 * 库存同步服务。
 * <p>从 SP-API 拉取 FBA 库存数据并持久化。</p>
 *
 * @author AmazonOps AI
 */
public interface InventorySyncService {

    /**
     * 同步指定店铺的库存数据。
     *
     * @param shopId        店铺 ID
     * @param marketplaceId 站点 ID
     */
    void syncInventory(Long shopId, String marketplaceId);
}
