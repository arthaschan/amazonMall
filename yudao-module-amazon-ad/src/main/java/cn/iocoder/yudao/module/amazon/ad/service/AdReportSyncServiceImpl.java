package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdCampaignMapper;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdReportDailyMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 广告报表同步服务实现。
 *
 * <p>通过 Amazon Ads API（advertising-api.amazon.com）拉取广告活动及关键词报表数据，
 * 并持久化到本地数据库。
 *
 * <p><strong>注意：</strong>Amazon Ads API 与 SP-API 使用不同的 base URL 和鉴权流程：
 * <ul>
 *   <li>SP-API: sellingpartnerapi-{region}.amazon.com，AWS Signature V4</li>
 *   <li>Ads API: advertising-api.amazon.com，OAuth2 Bearer Token + Amazon-Advertising-API-ClientId 头</li>
 * </ul>
 * 当前 SpApiClient 仅支持 SP-API 端点，Ads API 需要独立的 OAuth2 流程和 HTTP 客户端配置。
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class AdReportSyncServiceImpl implements AdReportSyncService {

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonAdCampaignMapper amazonAdCampaignMapper;

    @Resource
    private AmazonAdReportDailyMapper amazonAdReportDailyMapper;

    @Resource
    private AmazonShopService amazonShopService;

    /** Amazon Ads API base URL（生产环境）。 */
    private static final String ADS_API_BASE_URL = "https://advertising-api.amazon.com";

    /** 报告日期格式：yyyyMMdd。 */
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void syncAdReports(Long shopId, String marketplaceId) {
        log.info("[AdReportSync] 开始同步广告报表 shopId={}, marketplaceId={}", shopId, marketplaceId);

        // ── 1. 加载店铺信息 ─────────────────────────────────────────────────
        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            log.error("[AdReportSync] 店铺不存在 shopId={}", shopId);
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }

        // ── 2. 解析 AWS Region（用于日志和后续扩展）──────────────────────────
        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(marketplaceId);
        if (marketplace == null) {
            log.error("[AdReportSync] 不支持的 marketplaceId={}", marketplaceId);
            throw new IllegalArgumentException("Unsupported marketplaceId: " + marketplaceId);
        }
        String awsRegion = marketplace.getAwsRegion();
        String sellerId = shop.getSellerId();

        log.info("[AdReportSync] 店铺已加载 sellerId={}, region={}, countryCode={}",
                sellerId, awsRegion, shop.getCountryCode());

        // ── 3. 警告：Ads API 需要独立 OAuth2 鉴权 ───────────────────────────
        log.warn("[AdReportSync] Amazon Ads API requires separate OAuth2 flow - implementation pending. "
                + "Ads API base URL: {}, SP-API client does not support this endpoint.", ADS_API_BASE_URL);

        // ── 以下为 Ads API 调用的完整步骤说明（待 Ads OAuth2 客户端实现后启用） ──

        // Step 1: Get advertising profile ID
        // GET https://advertising-api.amazon.com/v2/profiles
        // Required Headers:
        //   Amazon-Advertising-API-ClientId: {lwaClientId}
        //   Authorization: Bearer {accessToken}
        //
        // Response: array of profile objects, select the one matching countryCode/marketplaceId
        //   profile.id -> profileId (required for all subsequent API calls)
        //   profile.countryCode -> country validation
        //
        // Persist as needed for subsequent calls:
        //   String profileId = response[matchingIndex].id;
        //   log.info("[AdReportSync] 获取到广告 profile profileId={}", profileId);

        // Step 2: Request SP campaign keyword report
        // POST https://advertising-api.amazon.com/v2/sp/reportKeywords
        // Required Headers:
        //   Amazon-Advertising-API-ClientId: {lwaClientId}
        //   Authorization: Bearer {accessToken}
        //   Amazon-Advertising-API-Scope: {profileId}
        //   Content-Type: application/json
        //
        // Request Body:
        //   {
        //     "reportDate": "20260101",  // 报表日期 yyyyMMdd
        //     "metrics": "impressions,clicks,cost,sales14d,attributedConversions14d,acos14d,roas14d,cpc,ctr"
        //   }
        //
        // Response: { "reportId": "xxx", "recordType": "keywords", "status": "IN_PROGRESS", "statusDetails": "" }
        //   String reportId = response.reportId;
        //   log.info("[AdReportSync] 报表已提交 reportId={}", reportId);

        // Step 3: Poll for report completion
        // GET https://advertising-api.amazon.com/v2/reports/{reportId}
        // Required Headers:
        //   Amazon-Advertising-API-ClientId: {lwaClientId}
        //   Authorization: Bearer {accessToken}
        //   Amazon-Advertising-API-Scope: {profileId}
        //
        // Poll every 10s until status is SUCCESS or FAILURE (max 30 attempts)
        // Response (SUCCESS):
        //   { "reportId": "xxx", "status": "SUCCESS", "statusDetails": "",
        //     "location": "https://adsapi-eu.amazon.com/v1/reports/xxx/document" }
        //
        // String reportUrl = response.location;  // 预签名 S3 下载链接
        // log.info("[AdReportSync] 报表已生成 reportUrl={}", reportUrl);

        // Step 4: Download and parse report (gzip JSON)
        // GET {reportUrl}
        // Response: gzip-compressed JSON array of report rows
        //
        // 解析流程：
        //   1. 使用 OkHttpClient 下载并解压 gzip 流
        //   2. 解析 JSON 数组，每条记录包含：
        //      - campaignId -> campaignId (Long)
        //      - adGroupId -> adGroupId (Long)
        //      - keywordId -> keywordId (Long)
        //      - keyword -> keywordText (String)
        //      - matchType -> matchType (String: EXACT/PHRASE/BROAD)
        //      - impressions -> impressions (Long)
        //      - clicks -> clicks (Long)
        //      - cost -> cost (BigDecimal)
        //      - sales14d -> sales (BigDecimal)
        //      - attributedConversions14d -> orders (Integer)
        //      - acos14d -> acos (BigDecimal)
        //      - roas14d -> roas (BigDecimal)
        //      - cpc -> cpc (BigDecimal)
        //      - ctr -> ctr (BigDecimal)
        //   3. 映射到 AmazonAdReportDailyDO：
        //      AmazonAdReportDailyDO report = new AmazonAdReportDailyDO();
        //      report.setShopId(shop.getId());
        //      report.setTenantId(shop.getTenantId());
        //      report.setCampaignId(...);
        //      report.setReportDate(LocalDate.parse(reportDateStr, REPORT_DATE_FORMAT));
        //      ... (set all fields)
        //      amazonAdReportDailyMapper.insertOrUpdate(report);
        //   4. 同步广告活动元数据到 AmazonAdCampaignDO：
        //      AmazonAdCampaignDO campaign = new AmazonAdCampaignDO();
        //      campaign.setShopId(shop.getId());
        //      campaign.setTenantId(shop.getTenantId());
        //      campaign.setCampaignId(...);
        //      campaign.setCampaignName(...);
        //      campaign.setCampaignType("SP");
        //      amazonAdCampaignMapper.insertOrUpdate(campaign);
        //   5. log.info("[AdReportSync] 报表解析完成，共 {} 条记录", totalSynced);

        log.info("[AdReportSync] shopId={}, marketplaceId={} - 广告报表同步框架已就绪，"
                + "等待 Ads OAuth2 客户端实现后启用完整流程", shopId, marketplaceId);
    }

    /**
     * 将 Ads API 报告 JSON 行映射为 AmazonAdReportDailyDO（待 Step 4 实现时调用）。
     *
     * @param campaignId    广告活动 ID
     * @param adGroupId     广告组 ID
     * @param keywordId     关键词 ID
     * @param keywordText   关键词文本
     * @param matchType     匹配类型（EXACT/PHRASE/BROAD）
     * @param reportDate    报告日期
     * @param impressions   曝光量
     * @param clicks        点击量
     * @param cost          花费
     * @param sales         销售额
     * @param orders        订单数
     * @param shop          店铺信息
     * @return 映射后的广告日报 DO
     */
    @SuppressWarnings("unused")
    private AmazonAdReportDailyDO buildReportDailyDO(Long campaignId, Long adGroupId, Long keywordId,
                                                      String keywordText, String matchType,
                                                      LocalDate reportDate,
                                                      Long impressions, Long clicks,
                                                      java.math.BigDecimal cost,
                                                      java.math.BigDecimal sales,
                                                      Integer orders,
                                                      AmazonShopDO shop) {
        AmazonAdReportDailyDO report = new AmazonAdReportDailyDO();
        report.setShopId(shop.getId());
        report.setTenantId(shop.getTenantId());
        report.setCampaignId(campaignId);
        report.setAdGroupId(adGroupId);
        report.setKeywordId(keywordId);
        report.setKeywordText(keywordText);
        report.setMatchType(matchType);
        report.setReportDate(reportDate);
        report.setImpressions(impressions);
        report.setClicks(clicks);
        report.setCost(cost);
        report.setSales(sales);
        report.setOrders(orders);
        // ACoS = cost / sales (若 sales > 0)
        if (sales != null && sales.compareTo(java.math.BigDecimal.ZERO) > 0) {
            report.setAcos(cost.divide(sales, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal.valueOf(100)));
        }
        // ROAS = sales / cost (若 cost > 0)
        if (cost != null && cost.compareTo(java.math.BigDecimal.ZERO) > 0) {
            report.setRoas(sales.divide(cost, 4, java.math.RoundingMode.HALF_UP));
        }
        // CPC = cost / clicks (若 clicks > 0)
        if (clicks != null && clicks > 0) {
            report.setCpc(cost.divide(java.math.BigDecimal.valueOf(clicks), 4, java.math.RoundingMode.HALF_UP));
        }
        // CTR = clicks / impressions (若 impressions > 0)
        if (impressions != null && impressions > 0) {
            report.setCtr(java.math.BigDecimal.valueOf(clicks)
                    .divide(java.math.BigDecimal.valueOf(impressions), 6, java.math.RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal.valueOf(100)));
        }
        return report;
    }
}
