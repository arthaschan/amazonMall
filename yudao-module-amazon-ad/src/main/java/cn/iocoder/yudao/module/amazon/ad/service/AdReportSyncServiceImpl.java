package cn.iocoder.yudao.module.amazon.ad.service;

import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdCampaignDO;
import cn.iocoder.yudao.module.amazon.ad.dal.dataobject.AmazonAdReportDailyDO;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdCampaignMapper;
import cn.iocoder.yudao.module.amazon.ad.dal.mysql.AmazonAdReportDailyMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.yudao.module.amazon.common.core.SpApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.GZIPInputStream;

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

    /** Amazon Ads API Client ID placeholder - replace with actual LWA Client ID from app registration. */
    private static final String ADS_API_CLIENT_ID = "amzn1.application-oa2-client.xxx";

    /** 报告日期格式：yyyyMMdd。 */
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** OkHttpClient for Ads API HTTP calls. */
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    /** JSON media type for request bodies. */
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    /** Jackson ObjectMapper for JSON parsing. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void syncAdReports(Long shopId, String marketplaceId) {
        log.info("[AdReportSync] 开始同步广告报表 shopId={}, marketplaceId={}", shopId, marketplaceId);

        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            log.error("[AdReportSync] 店铺不存在 shopId={}", shopId);
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }

        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(marketplaceId);
        if (marketplace == null) {
            log.error("[AdReportSync] 不支持的 marketplaceId={}", marketplaceId);
            throw new IllegalArgumentException("Unsupported marketplaceId: " + marketplaceId);
        }

        // Get Ads API access token - check if available
        String accessToken = amazonShopService.getDecryptedAccessToken(shopId);
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("[AdReportSync] Amazon Ads API OAuth2 token not configured for shopId={}, skipping sync", shopId);
            return;
        }

        // Determine Ads API scope (profile ID) - try to fetch
        String profileId = fetchAdsProfileId(accessToken, marketplace.getAwsRegion(), shop.getCountryCode());
        if (profileId == null) {
            log.warn("[AdReportSync] Could not determine Ads profile ID for shopId={}", shopId);
            return;
        }

        // Request report for last 7 days
        LocalDate reportDate = LocalDate.now().minusDays(1);
        String reportId = requestKeywordReport(accessToken, profileId, reportDate);
        if (reportId == null) {
            log.warn("[AdReportSync] Failed to request report for shopId={}", shopId);
            return;
        }

        // Poll for report completion
        String downloadUrl = pollReportStatus(accessToken, profileId, reportId);
        if (downloadUrl == null) {
            log.warn("[AdReportSync] Report not ready or failed for shopId={}", shopId);
            return;
        }

        // Download and parse report
        int count = downloadAndParseReport(downloadUrl, reportDate, shop);
        log.info("[AdReportSync] 广告报表同步完成 shopId={}, 记录数={}", shopId, count);
    }

    /**
     * Fetch the Ads API profile ID matching the given country code.
     *
     * @param accessToken OAuth2 access token
     * @param awsRegion   AWS region (for logging)
     * @param countryCode country code to match (e.g., "US", "DE")
     * @return profile ID string, or null if not found
     */
    private String fetchAdsProfileId(String accessToken, String awsRegion, String countryCode) {
        Request request = new Request.Builder()
                .url(ADS_API_BASE_URL + "/v2/profiles")
                .addHeader("Amazon-Advertising-API-ClientId", ADS_API_CLIENT_ID)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();
        Response response = null;
        try {
            response = HTTP_CLIENT.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("[AdReportSync] Failed to fetch profiles, HTTP status={}, region={}",
                        response.code(), awsRegion);
                return null;
            }
            String body = response.body().string();
            JsonNode profilesArray = OBJECT_MAPPER.readTree(body);
            if (!profilesArray.isArray()) {
                log.warn("[AdReportSync] Profiles response is not an array");
                return null;
            }
            for (JsonNode profile : profilesArray) {
                String profileCountry = profile.has("countryCode") ? profile.get("countryCode").asText() : null;
                if (countryCode != null && countryCode.equalsIgnoreCase(profileCountry)) {
                    String profileId = profile.get("profileId").asText();
                    log.info("[AdReportSync] 获取到广告 profile profileId={}, countryCode={}", profileId, countryCode);
                    return profileId;
                }
            }
            // If no country match, try first profile as fallback
            if (profilesArray.size() > 0) {
                String profileId = profilesArray.get(0).get("profileId").asText();
                log.info("[AdReportSync] No exact country match, using first profile profileId={}", profileId);
                return profileId;
            }
            return null;
        } catch (Exception e) {
            log.error("[AdReportSync] Error fetching Ads profiles", e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Request a Sponsored Products keyword report from the Ads API.
     *
     * @param accessToken OAuth2 access token
     * @param profileId   Ads profile ID
     * @param reportDate  report date
     * @return reportId string, or null on failure
     */
    private String requestKeywordReport(String accessToken, String profileId, LocalDate reportDate) {
        String dateStr = reportDate.format(REPORT_DATE_FORMAT);
        String jsonBody = "{\"reportDate\": \"" + dateStr + "\", "
                + "\"metrics\": \"impressions,clicks,cost,sales14d,attributedConversions14d,acos14d,roas14d,cpc,ctr\"}";

        Request request = new Request.Builder()
                .url(ADS_API_BASE_URL + "/v2/sp/reportKeywords")
                .addHeader("Amazon-Advertising-API-ClientId", ADS_API_CLIENT_ID)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Amazon-Advertising-API-Scope", profileId)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON_MEDIA))
                .build();
        Response response = null;
        try {
            response = HTTP_CLIENT.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("[AdReportSync] Failed to request keyword report, HTTP status={}", response.code());
                return null;
            }
            String body = response.body().string();
            JsonNode jsonNode = OBJECT_MAPPER.readTree(body);
            String reportId = jsonNode.has("reportId") ? jsonNode.get("reportId").asText() : null;
            if (reportId != null) {
                log.info("[AdReportSync] 报表已提交 reportId={}, reportDate={}", reportId, dateStr);
            }
            return reportId;
        } catch (Exception e) {
            log.error("[AdReportSync] Error requesting keyword report", e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Poll for report completion. Retries every 10 seconds, up to 30 attempts.
     *
     * @param accessToken OAuth2 access token
     * @param profileId   Ads profile ID
     * @param reportId    report ID to check
     * @return download URL (location field) on success, or null on failure/timeout
     */
    private String pollReportStatus(String accessToken, String profileId, String reportId) {
        int maxAttempts = 30;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Request request = new Request.Builder()
                    .url(ADS_API_BASE_URL + "/v2/reports/" + reportId)
                    .addHeader("Amazon-Advertising-API-ClientId", ADS_API_CLIENT_ID)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Amazon-Advertising-API-Scope", profileId)
                    .get()
                    .build();
            Response response = null;
            try {
                response = HTTP_CLIENT.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("[AdReportSync] Poll report failed, HTTP status={}, attempt={}/{}",
                            response.code(), attempt, maxAttempts);
                    return null;
                }
                String body = response.body().string();
                JsonNode jsonNode = OBJECT_MAPPER.readTree(body);
                String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "UNKNOWN";

                if ("SUCCESS".equals(status)) {
                    String location = jsonNode.has("location") ? jsonNode.get("location").asText() : null;
                    log.info("[AdReportSync] 报表已生成 reportId={}, location={}", reportId, location);
                    return location;
                } else if ("FAILURE".equals(status)) {
                    String details = jsonNode.has("statusDetails") ? jsonNode.get("statusDetails").asText() : "";
                    log.warn("[AdReportSync] 报表生成失败 reportId={}, details={}", reportId, details);
                    return null;
                } else {
                    log.info("[AdReportSync] 报表生成中 reportId={}, status={}, attempt={}/{}",
                            reportId, status, attempt, maxAttempts);
                }
            } catch (Exception e) {
                log.error("[AdReportSync] Error polling report status, attempt={}/{}", attempt, maxAttempts, e);
                return null;
            } finally {
                if (response != null) {
                    response.close();
                }
            }

            // Sleep 10 seconds before next attempt
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[AdReportSync] Poll interrupted for reportId={}", reportId);
                    return null;
                }
            }
        }
        log.warn("[AdReportSync] Poll exhausted after {} attempts for reportId={}", maxAttempts, reportId);
        return null;
    }

    /**
     * Download the report from the given URL, parse gzip JSON, and persist records.
     *
     * @param downloadUrl pre-signed S3 download URL
     * @param reportDate  report date
     * @param shop        shop information
     * @return number of records processed
     */
    private int downloadAndParseReport(String downloadUrl, LocalDate reportDate, AmazonShopDO shop) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .get()
                .build();
        Response response = null;
        int count = 0;
        try {
            response = HTTP_CLIENT.newCall(request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("[AdReportSync] Failed to download report, HTTP status={}", response.code());
                return 0;
            }

            // Parse gzip-compressed JSON array
            GZIPInputStream gzipStream = new GZIPInputStream(response.body().byteStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JsonNode reportArray = OBJECT_MAPPER.readTree(sb.toString());
            if (reportArray == null || !reportArray.isArray()) {
                log.warn("[AdReportSync] Report response is not a JSON array");
                return 0;
            }

            for (JsonNode row : reportArray) {
                try {
                    Long campaignId = row.has("campaignId") ? row.get("campaignId").asLong() : null;
                    Long adGroupId = row.has("adGroupId") ? row.get("adGroupId").asLong() : null;
                    Long keywordId = row.has("keywordId") ? row.get("keywordId").asLong() : null;
                    String keywordText = row.has("keyword") ? row.get("keyword").asText() : null;
                    String matchType = row.has("matchType") ? row.get("matchType").asText() : null;
                    Long impressions = row.has("impressions") ? row.get("impressions").asLong() : 0L;
                    Long clicks = row.has("clicks") ? row.get("clicks").asLong() : 0L;
                    BigDecimal cost = row.has("cost") ? new BigDecimal(row.get("cost").asText()) : BigDecimal.ZERO;
                    BigDecimal sales = row.has("sales14d") ? new BigDecimal(row.get("sales14d").asText()) : BigDecimal.ZERO;
                    Integer orders = row.has("attributedConversions14d") ? row.get("attributedConversions14d").asInt() : 0;

                    AmazonAdReportDailyDO reportDO = buildReportDailyDO(
                            campaignId, adGroupId, keywordId,
                            keywordText, matchType,
                            reportDate,
                            impressions, clicks, cost, sales, orders,
                            shop
                    );

                    amazonAdReportDailyMapper.insertOrUpdate(reportDO);

                    // Sync campaign metadata to AmazonAdCampaignDO
                    if (campaignId != null) {
                        String campaignName = row.has("campaignName") ? row.get("campaignName").asText() : null;
                        AmazonAdCampaignDO campaign = new AmazonAdCampaignDO();
                        campaign.setShopId(shop.getId());
                        campaign.setTenantId(shop.getTenantId());
                        campaign.setCampaignId(campaignId);
                        campaign.setCampaignName(campaignName);
                        campaign.setCampaignType("SP");
                        amazonAdCampaignMapper.insertOrUpdate(campaign);
                    }

                    count++;
                } catch (Exception rowEx) {
                    log.warn("[AdReportSync] 跳过解析异常的行: {}", rowEx.getMessage());
                }
            }
            log.info("[AdReportSync] 报表解析完成，共 {} 条记录", count);
        } catch (Exception e) {
            log.error("[AdReportSync] Error downloading/parsing report", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return count;
    }

    /**
     * 将 Ads API 报告 JSON 行映射为 AmazonAdReportDailyDO。
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
