package cn.iocoder.yudao.module.amazon.review.service;

/**
 * 评论同步服务。
 * <p>从 SP-API 拉取商品评论数据并持久化。</p>
 *
 * @author AmazonOps AI
 */
public interface ReviewSyncService {

    /**
     * 同步指定店铺的评论数据。
     *
     * @param shopId        店铺 ID
     * @param marketplaceId 站点 ID
     */
    void syncReviews(Long shopId, String marketplaceId);
}
