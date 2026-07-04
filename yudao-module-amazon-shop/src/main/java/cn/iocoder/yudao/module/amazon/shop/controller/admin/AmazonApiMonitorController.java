package cn.iocoder.yudao.module.amazon.shop.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonApiLogPageReqVO;
import cn.iocoder.yudao.module.amazon.shop.controller.admin.vo.AmazonApiStatsRespVO;
import cn.iocoder.yudao.module.amazon.shop.dal.dataobject.AmazonApiLogDO;
import cn.iocoder.yudao.module.amazon.shop.service.AmazonApiMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * Amazon API monitoring controller.
 *
 * <p>Provides REST endpoints for API call statistics, slow-request detection,
 * error-rate analysis, and audit log browsing.
 *
 * @author AmazonOps AI
 */
@Tag(name = "Management Console - Amazon API Monitor")
@RestController
@RequestMapping("/amazon/api-monitor")
@Validated
public class AmazonApiMonitorController {

    @Resource
    private AmazonApiMonitorService apiMonitorService;

    @GetMapping("/stats")
    @Operation(summary = "Get API call statistics",
            description = "Returns per-endpoint statistics including call count, error count, error rate, and average response time")
    @Parameters({
            @Parameter(name = "startTime", description = "Start time", required = true),
            @Parameter(name = "endTime", description = "End time", required = true)
    })
    @PreAuthorize("@ss.hasPermission('amazon:api-monitor:query')")
    public CommonResult<AmazonApiStatsRespVO> getApiStats(
            @RequestParam("startTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime endTime) {
        AmazonApiStatsRespVO stats = apiMonitorService.getApiStats(startTime, endTime);
        return success(stats);
    }

    @GetMapping("/slow-requests")
    @Operation(summary = "Get slow API requests",
            description = "Returns API requests that exceeded the response time threshold")
    @Parameters({
            @Parameter(name = "shopId", description = "Shop ID filter (optional)"),
            @Parameter(name = "thresholdMs", description = "Response time threshold in ms (default 2000)"),
            @Parameter(name = "startTime", description = "Start time", required = true),
            @Parameter(name = "endTime", description = "End time", required = true)
    })
    @PreAuthorize("@ss.hasPermission('amazon:api-monitor:query')")
    public CommonResult<List<AmazonApiLogDO>> getSlowRequests(
            @RequestParam(value = "shopId", required = false) Long shopId,
            @RequestParam(value = "thresholdMs", required = false, defaultValue = "2000") Integer thresholdMs,
            @RequestParam("startTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime endTime) {
        List<AmazonApiLogDO> slowRequests = apiMonitorService.getSlowRequests(
                shopId, thresholdMs, startTime, endTime);
        return success(slowRequests);
    }

    @GetMapping("/error-rate")
    @Operation(summary = "Get API error rate by endpoint",
            description = "Returns error rate statistics grouped by API endpoint")
    @Parameters({
            @Parameter(name = "startTime", description = "Start time", required = true),
            @Parameter(name = "endTime", description = "End time", required = true)
    })
    @PreAuthorize("@ss.hasPermission('amazon:api-monitor:query')")
    public CommonResult<AmazonApiStatsRespVO> getErrorRate(
            @RequestParam("startTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime endTime) {
        AmazonApiStatsRespVO errorRate = apiMonitorService.getErrorRate(startTime, endTime);
        return success(errorRate);
    }

    @GetMapping("/logs/page")
    @Operation(summary = "Get paginated API audit logs")
    @PreAuthorize("@ss.hasPermission('amazon:api-monitor:query')")
    public CommonResult<PageResult<AmazonApiLogDO>> getApiLogPage(@Valid AmazonApiLogPageReqVO pageReqVO) {
        PageResult<AmazonApiLogDO> pageResult = apiMonitorService.getApiLogPage(pageReqVO);
        return success(pageResult);
    }
}
