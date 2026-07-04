package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 评论同步服务实现。
 * <p>通过 SP-API Reports API 请求 GET_MERCHANT_REVIEWS_ALL_DATA 报告，
 * 轮询等待报告生成完成后获取下载链接，最终持久化到本地数据库。</p>
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
public class ReviewSyncServiceImpl implements ReviewSyncService {

    @Resource
    private SpApiClient spApiClient;

    @Resource
    private AmazonReviewMapper amazonReviewMapper;

    @Resource
    private AmazonShopService amazonShopService;

    private static final String REPORTS_PATH = "/reports/2021-06-30/reports";
    private static final String DOCUMENTS_PATH = "/reports/2021-06-30/documents";
    private static final String REPORT_TYPE = "GET_MERCHANT_REVIEWS_ALL_DATA";

    /** 最大轮询次数（每次间隔 10 秒，总计约 5 分钟）。 */
    private static final int MAX_POLL_ATTEMPTS = 30;
    /** 轮询间隔（毫秒）。 */
    private static final long POLL_INTERVAL_MS = 10_000L;

    @Override
    public void syncReviews(Long shopId, String marketplaceId) {
        log.info("[ReviewSync] 开始同步评论 shopId={}, marketplaceId={}", shopId, marketplaceId);

        // 1. 加载店铺信息
        AmazonShopDO shop = amazonShopService.getShopById(shopId);
        if (shop == null) {
            log.error("[ReviewSync] 店铺不存在 shopId={}", shopId);
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }

        // 2. 解析 AWS Region
        AmazonMarketplaceEnum marketplace = AmazonMarketplaceEnum.ofMarketplaceId(marketplaceId);
        if (marketplace == null) {
            log.error("[ReviewSync] 不支持的 marketplaceId={}", marketplaceId);
            throw new IllegalArgumentException("Unsupported marketplaceId: " + marketplaceId);
        }
        String awsRegion = marketplace.getAwsRegion();
        String sellerId = shop.getSellerId();

        // ── Step 1: 请求评论报告 ───────────────────────────────────────────
        Map<String, Object> reportRequest = new HashMap<String, Object>();
        reportRequest.put("reportType", REPORT_TYPE);
        reportRequest.put("marketplaceIds", Collections.singletonList(marketplaceId));

        JsonNode createResponse;
        try {
            createResponse = spApiClient.post(sellerId, awsRegion, REPORTS_PATH, reportRequest);
        } catch (Exception e) {
            log.error("[ReviewSync] 请求报告失败 shopId={}", shopId, e);
            throw new RuntimeException("Failed to request review report: " + e.getMessage(), e);
        }

        if (createResponse == null || !createResponse.hasNonNull("reportId")) {
            log.error("[ReviewSync] 报告请求响应中缺少 reportId, response={}", createResponse);
            throw new RuntimeException("Report creation response missing reportId");
        }

        String reportId = createResponse.get("reportId").asText();
        log.info("[ReviewSync] 报告已提交 reportId={}, shopId={}", reportId, shopId);

        // ── Step 2: 轮询报告状态直到完成 ────────────────────────────────────
        String reportDocumentId = pollReportUntilDone(sellerId, awsRegion, reportId, shopId);

        // ── Step 3: 获取报告文档下载链接 ────────────────────────────────────
        String documentPath = DOCUMENTS_PATH + "/" + reportDocumentId;
        JsonNode documentResponse;
        try {
            documentResponse = spApiClient.get(sellerId, awsRegion, documentPath, null);
        } catch (Exception e) {
            log.error("[ReviewSync] 获取报告文档信息失败 documentId={}", reportDocumentId, e);
            throw new RuntimeException("Failed to get report document: " + e.getMessage(), e);
        }

        if (documentResponse == null || !documentResponse.hasNonNull("url")) {
            log.error("[ReviewSync] 报告文档响应中缺少下载 URL, response={}", documentResponse);
            throw new RuntimeException("Report document response missing download URL");
        }

        String downloadUrl = documentResponse.get("url").asText();
        log.info("[ReviewSync] 报告文档下载链接已获取 documentId={}, shopId={}", reportDocumentId, shopId);

        // ── Step 4: 下载并解析报告文档 ──────────────────────────────────────
        // TODO: Download and parse report document
        // 报告文档通常为 TSV/CSV 格式，需要额外的 HTTP 客户端下载文件并逐行解析。
        // 下载 URL 是预签名的 S3 链接，有效期约 5 分钟。
        // 解析流程：
        //   1. 通过 OkHttpClient 或 HttpClient GET downloadUrl 下载文件流
        //   2. 按 TSV 分隔符 (\t) 逐行读取
        //   3. 跳过表头行，将每行映射为 AmazonReviewDO：
        //      - review_id -> reviewId
        //      - asin -> asin
        //      - reviewer_name -> reviewerName
        //      - rating (1-5) -> rating
        //      - review_title -> title
        //      - review_body -> body
        //      - review_date -> reviewDate (LocalDateTime)
        //      - verified_purchase (true/false) -> verifiedPurchase
        //   4. 设置 shopId, tenantId
        //   5. 调用 amazonReviewMapper.insertOrUpdate(reviewDO) 持久化
        //
        // 示例代码结构：
        // int totalSynced = 0;
        // try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        //         httpClient.newCall(new Request.Builder().url(downloadUrl).build()).execute()
        //                 .body().byteStream(), StandardCharsets.UTF_8))) {
        //     String headerLine = reader.readLine(); // 跳过表头
        //     String line;
        //     while ((line = reader.readLine()) != null) {
        //         String[] fields = line.split("\t", -1);
        //         AmazonReviewDO review = mapReviewFromTsv(fields, shop);
        //         amazonReviewMapper.insertOrUpdate(review);
        //         totalSynced++;
        //     }
        // }
        log.warn("[ReviewSync] 报告文档下载和解析尚未实现 downloadUrl={}, shopId={}", downloadUrl, shopId);
    }

    /**
     * 轮询报告状态，直到状态变为 DONE / CANCELLED / FATAL。
     *
     * @return reportDocumentId（仅当状态为 DONE 时返回）
     */
    private String pollReportUntilDone(String sellerId, String awsRegion,
                                        String reportId, Long shopId) {
        String reportPath = REPORTS_PATH + "/" + reportId;

        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Report polling interrupted", e);
            }

            JsonNode statusResponse;
            try {
                statusResponse = spApiClient.get(sellerId, awsRegion, reportPath, null);
            } catch (Exception e) {
                log.warn("[ReviewSync] 轮询报告状态失败 (attempt {}/{}) reportId={}",
                        attempt, MAX_POLL_ATTEMPTS, reportId, e);
                continue;
            }

            if (statusResponse == null) {
                log.warn("[ReviewSync] 轮询报告状态返回空 (attempt {}/{})", attempt, MAX_POLL_ATTEMPTS);
                continue;
            }

            String processingStatus = statusResponse.path("processingStatus").asText("");
            log.info("[ReviewSync] 报告状态轮询 attempt={}/{}, reportId={}, status={}",
                    attempt, MAX_POLL_ATTEMPTS, reportId, processingStatus);

            switch (processingStatus) {
                case "DONE": {
                    String reportDocumentId = statusResponse.path("reportDocumentId").asText(null);
                    if (reportDocumentId == null || reportDocumentId.trim().isEmpty()) {
                        throw new RuntimeException("Report DONE but missing reportDocumentId: " + reportId);
                    }
                    log.info("[ReviewSync] 报告生成完成 reportId={}, documentId={}", reportId, reportDocumentId);
                    return reportDocumentId;
                }
                case "CANCELLED": {
                    log.warn("[ReviewSync] 报告已被取消 reportId={}, shopId={}", reportId, shopId);
                    throw new RuntimeException("Report was cancelled: " + reportId);
                }
                case "FATAL": {
                    String errorMsg = statusResponse.path("processingStatusMessage").asText("unknown error");
                    log.error("[ReviewSync] 报告生成失败 reportId={}, error={}", reportId, errorMsg);
                    throw new RuntimeException("Report generation failed: " + reportId + " - " + errorMsg);
                }
                // IN_QUEUE / IN_PROGRESS -> 继续轮询
            }
        }

        log.error("[ReviewSync] 报告轮询超时 reportId={}, shopId={}, maxAttempts={}",
                reportId, shopId, MAX_POLL_ATTEMPTS);
        throw new RuntimeException("Report polling timed out after " + MAX_POLL_ATTEMPTS
                + " attempts for reportId: " + reportId);
    }

    /**
     * 将 TSV 报告行映射为 AmazonReviewDO（供 Step 4 实现时调用）。
     *
     * @param fields TSV 行拆分后的字段数组
     * @param shop   店铺信息
     * @return 映射后的评论 DO
     */
    @SuppressWarnings("unused")
    private AmazonReviewDO mapReviewFromTsv(String[] fields, AmazonShopDO shop) {
        AmazonReviewDO review = new AmazonReviewDO();
        review.setShopId(shop.getId());
        review.setTenantId(shop.getTenantId());

        // TSV 列顺序取决于报告定义，以下为常见映射（需根据实际表头调整）
        if (fields.length > 0) review.setReviewId(fields[0]);
        if (fields.length > 1) review.setAsin(fields[1]);
        if (fields.length > 2) review.setReviewerName(fields[2]);
        if (fields.length > 3) review.setRating(parseIntSafe(fields[3]));
        if (fields.length > 4) review.setTitle(fields[4]);
        if (fields.length > 5) review.setBody(fields[5]);
        if (fields.length > 6) review.setReviewDate(parseDateTimeSafe(fields[6]));
        if (fields.length > 7) review.setVerifiedPurchase(Boolean.parseBoolean(fields[7]));

        return review;
    }

    private static Integer parseIntSafe(String value) {
        try {
            return (value != null && !value.trim().isEmpty()) ? Integer.parseInt(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static java.time.LocalDateTime parseDateTimeSafe(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return java.time.LocalDateTime.parse(value.trim(),
                    java.time.format.DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return java.time.LocalDate.parse(value.trim(),
                        java.time.format.DateTimeFormatter.ISO_DATE).atStartOfDay();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
