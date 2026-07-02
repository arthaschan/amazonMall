package cn.iocoder.yudao.module.amazon.research.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 关键词调研服务实现。
 *
 * @author AmazonOps AI
 */
@Service
public class KeywordResearchServiceImpl implements KeywordResearchService {

    @Override
    public Long getSearchVolume(String marketplaceId, String keyword) {
        // TODO: 集成 SP-API 或第三方数据源
        return 0L;
    }

    @Override
    public List<String> getRelatedKeywords(String marketplaceId, String seedKeyword) {
        // TODO: 集成 Amazon Autocomplete / Brand Analytics
        return Collections.emptyList();
    }
}
