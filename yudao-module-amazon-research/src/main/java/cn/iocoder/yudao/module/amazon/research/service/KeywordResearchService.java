package cn.iocoder.yudao.module.amazon.research.service;

import java.util.List;

/**
 * 关键词调研服务。
 * <p>提供关键词搜索量、竞争度、相关词等数据分析。</p>
 *
 * @author AmazonOps AI
 */
public interface KeywordResearchService {

    /**
     * 获取关键词的搜索量。
     *
     * @param marketplaceId 站点 ID
     * @param keyword       关键词
     * @return 月搜索量
     */
    Long getSearchVolume(String marketplaceId, String keyword);

    /**
     * 获取相关关键词列表。
     *
     * @param marketplaceId 站点 ID
     * @param seedKeyword   种子关键词
     * @return 相关关键词列表
     */
    List<String> getRelatedKeywords(String marketplaceId, String seedKeyword);
}
