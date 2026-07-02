package cn.iocoder.yudao.module.amazon.review.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.review.service.ReviewSyncService;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 评论同步定时任务。
 *
 * <p>遍历所有已启用店铺，从 SP-API 拉取最新商品评论数据并持久化。
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("reviewSyncJob")
public class ReviewSyncJob extends AbstractAmazonSyncJob {

    @Resource
    private ReviewSyncService reviewSyncService;

    @Override
    protected String getJobName() {
        return "reviewSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) {
        reviewSyncService.syncReviews(shop.getId(), shop.getMarketplaceId());
    }
}
