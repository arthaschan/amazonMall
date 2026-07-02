package cn.iocoder.yudao.module.amazon.shop.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonApiLogPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonApiStatsRespVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonApiLogDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Amazon API monitoring service interface.
 *
 * <p>Provides audit logging, statistics, slow-request detection,
 * and error-rate analysis for SP-API calls.
 *
 * @author AmazonOps AI
 */
public interface AmazonApiMonitorService {

    /**
     * Records an API call in the audit log.
     *
     * @param log the API log entry to persist
     */
    void logApiCall(AmazonApiLogDO log);

    /**
     * Gets API call statistics grouped by endpoint for the given time range.
     *
     * @param startTime the start of the time range
     * @param endTime   the end of the time range
     * @return aggregated statistics
     */
    AmazonApiStatsRespVO getApiStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Gets API requests that exceeded the given response time threshold.
     *
     * @param shopId      optional shop ID filter (null for all shops)
     * @param thresholdMs response time threshold in milliseconds (default 2000)
     * @param startTime   the start of the time range
     * @param endTime     the end of the time range
     * @return list of slow API log entries
     */
    List<AmazonApiLogDO> getSlowRequests(Long shopId, Integer thresholdMs,
                                          LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Calculates the error rate by endpoint for the given time range.
     *
     * @param startTime the start of the time range
     * @param endTime   the end of the time range
     * @return error rate statistics
     */
    AmazonApiStatsRespVO getErrorRate(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Paginated query of API logs.
     *
     * @param pageReqVO the page request with filters
     * @return paginated result
     */
    PageResult<AmazonApiLogDO> getApiLogPage(AmazonApiLogPageReqVO pageReqVO);
}
