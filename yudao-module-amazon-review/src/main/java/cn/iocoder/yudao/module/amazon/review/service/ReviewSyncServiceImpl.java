package cn.iocoder.yudao.module.amazon.review.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 评论同步服务实现。
 * <p>通过 SP-API 拉取商品评论并持久化到本地数据库。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class ReviewSyncServiceImpl implements ReviewSyncService {

    @Override
    public void syncReviews(Long shopId, String marketplaceId) {
        // TODO: 调用 SP-API 拉取评论数据并持久化
        log.info("[ReviewSync] 开始同步评论 shopId={}, marketplaceId={}", shopId, marketplaceId);
    }
}
