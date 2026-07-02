package cn.iocoder.yudao.module.amazon.listing.service;

/**
 * Listing 同步服务。
 * <p>从 SP-API 拉取商品 Listing 数据并持久化。</p>
 *
 * @author AmazonOps AI
 */
public interface ListingSyncService {

    /**
     * 同步指定店铺的 Listing 数据。
     *
     * @param shopId        店铺 ID
     * @param marketplaceId 站点 ID
     */
    void syncListings(Long shopId, String marketplaceId);
}
