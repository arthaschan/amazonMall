package cn.iocoder.yudao.module.amazon.shop.service.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonApiLogPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonApiStatsRespVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonApiLogDO;
import cn.iocoder.yudao.module.amazon.shop.dal.mysql.AmazonApiLogMapper;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonApiMonitorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Amazon API monitoring service implementation.
 *
 * <p>Provides audit logging, statistics aggregation, slow-request detection,
 * and error-rate analysis for SP-API calls.
 *
 * @author AmazonOps AI
 */
@Slf4j
@Service
@Validated
public class AmazonApiMonitorServiceImpl implements AmazonApiMonitorService {

    /** Default slow-request threshold: 2 seconds. */
    private static final int DEFAULT_SLOW_THRESHOLD_MS = 2000;

    @Resource
    private AmazonApiLogMapper apiLogMapper;

    @Override
    public void logApiCall(AmazonApiLogDO apiLog) {
        apiLogMapper.insert(apiLog);
    }

    @Override
    public AmazonApiStatsRespVO getApiStats(LocalDateTime startTime, LocalDateTime endTime) {
        // Use tenant ID 0 as default; in production, extract from security context
        Long tenantId = 0L;
        List<Map<String, Object>> rawStats = apiLogMapper.selectApiStats(tenantId, startTime, endTime);

        return buildStatsResponse(rawStats);
    }

    @Override
    public List<AmazonApiLogDO> getSlowRequests(Long shopId, Integer thresholdMs,
                                                  LocalDateTime startTime, LocalDateTime endTime) {
        int threshold = (thresholdMs != null && thresholdMs > 0) ? thresholdMs : DEFAULT_SLOW_THRESHOLD_MS;
        return apiLogMapper.selectSlowRequests(shopId, threshold, startTime, endTime);
    }

    @Override
    public AmazonApiStatsRespVO getErrorRate(LocalDateTime startTime, LocalDateTime endTime) {
        // Error rate uses the same stats query, which includes error counts
        return getApiStats(startTime, endTime);
    }

    @Override
    public PageResult<AmazonApiLogDO> getApiLogPage(AmazonApiLogPageReqVO pageReqVO) {
        return apiLogMapper.selectPage(pageReqVO);
    }

    // ── Internal Helpers ──────────────────────────────────────────────────

    /**
     * Builds the aggregated stats response from raw database results.
     */
    private AmazonApiStatsRespVO buildStatsResponse(List<Map<String, Object>> rawStats) {
        List<AmazonApiStatsRespVO.EndpointStats> endpointStatsList = new ArrayList<>();
        long totalCalls = 0;
        long totalErrors = 0;
        double totalAvgResponseTime = 0;

        for (Map<String, Object> row : rawStats) {
            String endpoint = (String) row.get("endpoint");
            long totalCount = toLong(row.get("totalCount"));
            long errorCount = toLong(row.get("errorCount"));
            double avgResponseTimeMs = toDouble(row.get("avgResponseTimeMs"));

            double errorRate = totalCount > 0 ? (double) errorCount / totalCount : 0.0;

            AmazonApiStatsRespVO.EndpointStats stats = AmazonApiStatsRespVO.EndpointStats.builder()
                    .endpoint(endpoint)
                    .totalCount(totalCount)
                    .errorCount(errorCount)
                    .errorRate(errorRate)
                    .avgResponseTimeMs(avgResponseTimeMs)
                    .build();
            endpointStatsList.add(stats);

            totalCalls += totalCount;
            totalErrors += errorCount;
            totalAvgResponseTime += avgResponseTimeMs * totalCount;
        }

        double overallErrorRate = totalCalls > 0 ? (double) totalErrors / totalCalls : 0.0;
        double overallAvgResponseTime = totalCalls > 0 ? totalAvgResponseTime / totalCalls : 0.0;

        return AmazonApiStatsRespVO.builder()
                .endpoints(endpointStatsList)
                .totalCalls(totalCalls)
                .totalErrors(totalErrors)
                .overallErrorRate(overallErrorRate)
                .overallAvgResponseTimeMs(overallAvgResponseTime)
                .build();
    }

    private static long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
