package cn.iocoder.yudao.module.amazon.ad.job;

import cn.iocoder.yudao.module.amazon.ad.service.AdReportSyncService;
import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 广告报告同步 Job
 * <p>
 * 从 Amazon Advertising API 拉取广告活动和关键词级别的每日报告数据，
 * 包括曝光、点击、花费、销售额、ACoS、ROAS、CPC、CTR 等指标。
 * <p>
 * 处理器名称: amazonAdReportSyncJob
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("amazonAdReportSyncJob")
public class AmazonAdReportSyncJob extends AbstractAmazonSyncJob {

    @Resource
    private AdReportSyncService adReportSyncService;

    @Override
    protected String getJobName() {
        return "amazonAdReportSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) throws Exception {
        log.info("[doSync] 开始广告报告同步 | shopId={}, marketplace={}",
                shop.getId(), shop.getMarketplaceId());

        adReportSyncService.syncAdReports(shop.getId(), shop.getMarketplaceId());

        log.info("[doSync] 广告报告同步完成 | shopId={}", shop.getId());
    }

}
