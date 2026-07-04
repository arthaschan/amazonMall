package cn.iocoder.yudao.module.amazon.ad.job;

import cn.iocoder.yudao.module.amazon.ad.service.AdReportSyncService;
import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 广告报表同步定时任务。
 *
 * <p>遍历所有已启用店铺，从 Amazon Ads API 拉取最新广告报表数据并持久化。
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("adReportSyncJob")
public class AdReportSyncJob extends AbstractAmazonSyncJob {

    @Resource
    private AdReportSyncService adReportSyncService;

    @Override
    protected String getJobName() {
        return "adReportSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) {
        adReportSyncService.syncAdReports(shop.getId(), shop.getMarketplaceId());
    }
}
