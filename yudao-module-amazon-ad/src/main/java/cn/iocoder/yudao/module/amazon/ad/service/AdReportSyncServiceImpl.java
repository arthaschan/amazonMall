package cn.iocoder.yudao.module.amazon.ad.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 广告报表同步服务实现。
 * <p>通过 Amazon Ads API 拉取广告报表并持久化到本地数据库。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AdReportSyncServiceImpl implements AdReportSyncService {

    @Override
    public void syncAdReports(Long shopId, String marketplaceId) {
        // TODO: 调用 Amazon Ads API 拉取广告报表并持久化
        log.info("[AdReportSync] 开始同步广告报表 shopId={}, marketplaceId={}", shopId, marketplaceId);
    }
}
