package cn.iocoder.yudao.module.amazon.review.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.review.service.ReviewService;
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
    private ReviewService reviewService;

    @Override
    protected String getJobName() {
        return "amazonReviewSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) throws Exception {
        log.info("[doSync] 开始评论同步 | shopId={}, marketplace={}",
                shop.getId(), shop.getMarketplaceId());

        // TODO: 接入 SP-API 评论接口
        // 1. 获取店铺下所有 ASIN 列表
        // 2. 逐 ASIN 拉取评论数据
        // 3. 持久化到 amazon_review 表
        // 4. 触发 AI 情感分析 (低星评论生成 ReviewAlert)
        log.info("[doSync] 评论同步完成 | shopId={}", shop.getId());
    }

}
