package cn.iocoder.yudao.module.amazon.ad.service;

import java.util.Map;

/**
 * AI 广告优化器 Service。
 * <p>基于广告数据和 AI 分析，自动优化竞价、预算、关键词。</p>
 *
 * @author AmazonOps AI
 */
public interface AiAdOptimizerService {

    /**
     * 分析广告数据并生成优化建议。
     *
     * @param shopId 店铺 ID
     * @return 优化建议，key=维度，value=建议内容
     */
    Map<String, Object> analyzeAndSuggest(Long shopId);

    /**
     * 自动应用优化建议（调整竞价、否定关键词等）。
     *
     * @param shopId 店铺 ID
     */
    void autoOptimize(Long shopId);
}
