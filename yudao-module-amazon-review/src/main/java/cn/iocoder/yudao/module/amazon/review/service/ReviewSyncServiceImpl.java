package cn.iocoder.yudao.module.amazon.review.service;

import cn.iocoder.yudao.module.amazon.review.dal.dataobject.AmazonReviewDO;
import cn.iocoder.yudao.module.amazon.review.dal.mysql.AmazonReviewMapper;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonShopDO;
import cn.iocoder.yudao.module.amazon.shop.enums.AmazonMarketplaceEnum;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.yudao.module.amazon.common.core.SpApiClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

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
    /** 批量入库大小 */
    private static final int BATCH_SIZE = 500;

    /**
     * 专用于下载预签名 S3 文档的 HTTP 客户端。
     * <p>预签名 URL 已包含鉴权信息，无需 AWS Sig V4 签名。</p>
     */
    private static final OkHttpClient DOWNLOAD_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .build();

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
        String compressionAlg = documentResponse.path("compressionAlgorithm").asText(null);
        log.info("[ReviewSync] 报告文档下载链接已获取 documentId={}, shopId={}, compression={}",
                reportDocumentId, shopId, compressionAlg);

        // ── Step 4: 下载并解析报告文档 ──────────────────────────────────────
        int totalSynced = downloadAndParseReportDocument(downloadUrl, compressionAlg, shop);
        log.info("[ReviewSync] 评论同步完成 shopId={}, marketplaceId={}, totalSynced={}",
                shopId, marketplaceId, totalSynced);
    }

    /**
     * 下载并解析报告文档。
     * <p>通过 OkHttp GET 预签名 S3 URL 下载报告文件，
     * 根据 compressionAlgorithm 决定是否需要 GZIP 解压，
     * 然后按 TSV 格式逐行解析并批量入库。</p>
     *
     * @param downloadUrl     预签名下载链接
     * @param compressionAlg  压缩算法（GZIP 或 null）
     * @param shop            店铺信息
     * @return 同步的评论总数
     */
    private int downloadAndParseReportDocument(String downloadUrl, String compressionAlg,
                                                AmazonShopDO shop) {
        Request request = new Request.Builder()
                .url(downloadUrl)
                .get()
                .build();

        Response response;
        try {
            response = DOWNLOAD_CLIENT.newCall(request).execute();
        } catch (IOException e) {
            log.error("[ReviewSync] 下载报告文档失败 url={}", downloadUrl, e);
            throw new RuntimeException("Failed to download report document: " + e.getMessage(), e);
        }

        try {
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null) {
                String errorMsg = String.format(
                        "Report document download failed: HTTP %d", response.code());
                log.error("[ReviewSync] {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 根据压缩算法包装输入流
            InputStream rawStream = responseBody.byteStream();
            InputStream dataStream;
            if ("GZIP".equalsIgnoreCase(compressionAlg)) {
                dataStream = new GZIPInputStream(rawStream);
            } else {
                dataStream = rawStream;
            }

            return parseTsvStream(dataStream, shop);

        } catch (IOException e) {
            log.error("[ReviewSync] 读取报告文档流失败", e);
            throw new RuntimeException("Failed to read report document stream: " + e.getMessage(), e);
        } finally {
            response.close();
        }
    }

    /**
     * 解析 TSV 格式的报告数据流，将每行映射为 AmazonReviewDO 并批量入库。
     * <p>采用表头驱动策略：读取第一行作为列名，构建列名到索引的映射，
     * 确保不依赖固定的列顺序。</p>
     *
     * @param dataStream 输入流（已解压）
     * @param shop       店铺信息
     * @return 同步的评论总数
     */
    private int parseTsvStream(InputStream dataStream, AmazonShopDO shop) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(dataStream, StandardCharsets.UTF_8));

        // 读取表头行，构建列名→索引映射
        String headerLine = reader.readLine();
        if (headerLine == null || headerLine.trim().isEmpty()) {
            log.warn("[ReviewSync] 报告文档为空，无数据行");
            return 0;
        }

        String[] headers = headerLine.split("\t", -1);
        Map<String, Integer> columnIndex = buildColumnIndex(headers);
        log.info("[ReviewSync] TSV 表头解析完成, columns={}, columnCount={}", Arrays.asList(headers), headers.length);

        int totalSynced = 0;
        int totalErrors = 0;
        List<AmazonReviewDO> batch = new ArrayList<AmazonReviewDO>(BATCH_SIZE);

        String line;
        int lineNum = 1;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] fields = line.split("\t", -1);

            try {
                AmazonReviewDO review = mapReviewFromTsv(fields, columnIndex, shop);
                if (review.getReviewId() == null || review.getReviewId().trim().isEmpty()) {
                    log.warn("[ReviewSync] 跳过缺少 review_id 的行 lineNum={}", lineNum);
                    totalErrors++;
                    continue;
                }
                batch.add(review);

                // 达到批量大小时执行入库
                if (batch.size() >= BATCH_SIZE) {
                    persistBatch(batch);
                    totalSynced += batch.size();
                    log.info("[ReviewSync] 批量入库完成, batchSize={}, totalSynced={}",
                            batch.size(), totalSynced);
                    batch.clear();
                }
            } catch (Exception e) {
                totalErrors++;
                log.warn("[ReviewSync] 解析行失败 lineNum={}, error={}", lineNum, e.getMessage());
            }
        }

        // 处理最后一批
        if (!batch.isEmpty()) {
            persistBatch(batch);
            totalSynced += batch.size();
            log.info("[ReviewSync] 最终批量入库 batchSize={}, totalSynced={}",
                    batch.size(), totalSynced);
            batch.clear();
        }

        if (totalErrors > 0) {
            log.warn("[ReviewSync] 解析完成, 共 {} 行出错 totalSynced={}, totalErrors={}",
                    lineNum - 1, totalSynced, totalErrors);
        }

        return totalSynced;
    }

    /**
     * 构建列名到索引的映射表。
     * <p>列名统一转为小写并去除前后空白，以便后续不区分大小写查找。</p>
     */
    private Map<String, Integer> buildColumnIndex(String[] headers) {
        Map<String, Integer> index = new HashMap<String, Integer>(headers.length);
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim().toLowerCase(), i);
        }
        return index;
    }

    /**
     * 持久化一批评论记录。
     * <p>使用 MyBatis Plus 的 insertOrUpdate 逐条写入，
     * 以 review_id 作为唯一键实现幂等同步。</p>
     */
    private void persistBatch(List<AmazonReviewDO> batch) {
        for (AmazonReviewDO review : batch) {
            try {
                amazonReviewMapper.insert(review);
            } catch (Exception e) {
                // 如果 insert 失败（可能是重复 review_id），尝试 update
                if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                    try {
                        AmazonReviewDO existing = amazonReviewMapper.selectOne(
                                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AmazonReviewDO>()
                                        .eq(AmazonReviewDO::getReviewId, review.getReviewId())
                                        .last("LIMIT 1"));
                        if (existing != null) {
                            review.setId(existing.getId());
                            amazonReviewMapper.updateById(review);
                        }
                    } catch (Exception updateEx) {
                        log.warn("[ReviewSync] 更新已有评论失败 reviewId={}", review.getReviewId(), updateEx);
                    }
                } else {
                    log.warn("[ReviewSync] 入库评论失败 reviewId={}", review.getReviewId(), e);
                }
            }
        }
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
     * 基于表头索引将 TSV 行映射为 AmazonReviewDO。
     * <p>通过列名（小写）查找列索引，兼容 Amazon 报告列顺序变化。</p>
     *
     * <p>GET_MERCHANT_REVIEWS_ALL_DATA 常见列名：
     * review-id, marketplace, product-asin, reviewer-name, rating,
     * review-title, review-body, review-date, verified-purchase,
     * helpful-votes</p>
     *
     * @param fields      TSV 行拆分后的字段数组
     * @param columnIndex 列名→索引映射（来自 buildColumnIndex）
     * @param shop        店铺信息
     * @return 映射后的评论 DO
     */
    private AmazonReviewDO mapReviewFromTsv(String[] fields, Map<String, Integer> columnIndex,
                                             AmazonShopDO shop) {
        AmazonReviewDO review = new AmazonReviewDO();
        review.setShopId(shop.getId());
        review.setTenantId(shop.getTenantId());

        review.setReviewId(getField(fields, columnIndex, "review-id"));
        review.setAsin(getField(fields, columnIndex, "product-asin"));
        review.setReviewerName(getField(fields, columnIndex, "reviewer-name"));
        review.setRating(parseIntSafe(getField(fields, columnIndex, "rating")));
        review.setTitle(getField(fields, columnIndex, "review-title"));
        review.setBody(getField(fields, columnIndex, "review-body"));
        review.setReviewDate(parseDateTimeSafe(getField(fields, columnIndex, "review-date")));
        review.setVerifiedPurchase(parseBooleanSafe(getField(fields, columnIndex, "verified-purchase")));
        review.setHelpfulVotes(parseIntSafe(getField(fields, columnIndex, "helpful-votes")));

        return review;
    }

    /**
     * 根据列名从字段数组中安全取值。
     *
     * @param fields      字段数组
     * @param columnIndex 列名→索引映射
     * @param columnName  列名（小写）
     * @return 字段值，不存在时返回 null
     */
    private static String getField(String[] fields, Map<String, Integer> columnIndex,
                                    String columnName) {
        Integer idx = columnIndex.get(columnName);
        if (idx == null || idx >= fields.length) {
            return null;
        }
        String value = fields[idx];
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    private static Integer parseIntSafe(String value) {
        try {
            return (value != null && !value.trim().isEmpty()) ? Integer.parseInt(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBooleanSafe(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String normalized = value.trim().toLowerCase();
        if ("yes".equals(normalized) || "true".equals(normalized) || "1".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("no".equals(normalized) || "false".equals(normalized) || "0".equals(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static LocalDateTime parseDateTimeSafe(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDate.parse(value.trim(),
                        DateTimeFormatter.ISO_DATE).atStartOfDay();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
