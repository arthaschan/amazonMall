package cn.iocoder.yudao.module.amazon.ad.service;

/**
 * 广告报表同步服务。
 * <p>从 SP-API / Amazon Ads API 拉取广告报表数据并持久化。</p>
 *
 * @author AmazonOps AI
 */
public interface AdReportSyncService {

    /**
     * 同步指定店铺的广告报表数据。
     *
     * @param shopId        店铺 ID
     * @param marketplaceId 站点 ID
     */
    void syncAdReports(Long shopId, String marketplaceId);
}
