package cn.iocoder.yudao.module.amazon.listing.job;

import cn.iocoder.yudao.module.amazon.common.sync.AbstractAmazonSyncJob;
import cn.iocoder.yudao.module.amazon.listing.service.ListingSyncService;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Listing 同步定时任务。
 *
 * <p>遍历所有已启用店铺，从 SP-API 拉取最新商品 Listing 数据并持久化。
 *
 * @author AmazonOps AI
 */
@Slf4j
@Component("listingSyncJob")
public class ListingSyncJob extends AbstractAmazonSyncJob {

    @Resource
    private ListingSyncService listingSyncService;

    @Override
    protected String getJobName() {
        return "listingSyncJob";
    }

    @Override
    protected void doSync(AmazonShopDO shop) {
        listingSyncService.syncListings(shop.getId(), shop.getMarketplaceId());
    }
}
