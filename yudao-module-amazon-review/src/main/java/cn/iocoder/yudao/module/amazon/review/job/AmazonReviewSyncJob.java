package cn.iocoder.yudao.module.amazon.review.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.review.service.ReviewSyncService;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 评论同步 Job
 * <p>
 * 从 Amazon SP-API 拉取商品评论数据，包括星级、内容、验证购买标识等。
 * 同步后可触发 AI 情感分析流程。
 * <p>
 * 处理器名称: amazonReviewSyncJob
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("amazonReviewSyncJob")
public class AmazonReviewSyncJob extends AbstractAmazonSyncJob {

    @Resource
    private ReviewSyncService reviewSyncService;

    @Override
    protected String getJobName() {
        return "amazonReviewSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) throws Exception {
        log.info("[doSync] 开始评论同步 | shopId={}, marketplace={}",
                shop.getId(), shop.getMarketplaceId());

        reviewSyncService.syncReviews(shop.getId(), shop.getMarketplaceId());

        log.info("[doSync] 评论同步完成 | shopId={}", shop.getId());
    }

}
