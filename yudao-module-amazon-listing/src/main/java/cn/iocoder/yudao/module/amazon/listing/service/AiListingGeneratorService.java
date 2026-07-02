package cn.iocoder.yudao.module.amazon.listing.service;

import cn.iocoder.yudao.module.amazon.listing.dal.dataobject.AmazonListingVersionDO;

/**
 * AI Listing 生成服务。
 * <p>基于 AI 自动生成 / 优化标题、五点、描述、后台关键词。</p>
 *
 * @author AmazonOps AI
 */
public interface AiListingGeneratorService {

    /**
     * 为指定产品生成 AI Listing。
     *
     * @param productId 产品 ID
     * @param prompt    用户补充的优化方向
     * @return 生成的 Listing 版本
     */
    AmazonListingVersionDO generate(Long productId, String prompt);

    /**
     * 对已有 Listing 进行 AI 优化。
     *
     * @param productId 产品 ID
     * @param prompt    优化指令
     * @return 优化后的 Listing 版本
     */
    AmazonListingVersionDO optimize(Long productId, String prompt);
}
