package cn.iocoder.yudao.module.amazon.report.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.WeeklyReportPageReqVO;
import cn.iocoder.yudao.module.amazon.report.dal.dataobject.AmazonWeeklyReportDO;
import cn.iocoder.yudao.module.amazon.report.service.AiWeeklyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI 周报")
@RestController
@RequestMapping("/amazon/report/weekly")
@Validated
public class AiWeeklyReportController {

    @Resource
    private AiWeeklyReportService weeklyReportService;

    @PostMapping("/generate")
    @Operation(summary = "生成周报")
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "shopId", description = "店铺 ID", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "reportWeek", description = "报告周 (如 2024-W03)", required = true)
    })
    @PreAuthorize("@ss.hasPermission('amazon:report:weekly:generate')")
    public CommonResult<AmazonWeeklyReportDO> generateReport(
            @RequestParam Long shopId, @RequestParam String reportWeek) {
        return success(weeklyReportService.generateReport(shopId, reportWeek));
    }

    @GetMapping("/get")
    @Operation(summary = "获取周报")
    @PreAuthorize("@ss.hasPermission('amazon:report:weekly:query')")
    public CommonResult<AmazonWeeklyReportDO> getReport(
            @RequestParam Long shopId, @RequestParam String reportWeek) {
        return success(weeklyReportService.getReport(shopId, reportWeek));
    }

    @GetMapping("/page")
    @Operation(summary = "周报分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:report:weekly:query')")
    public CommonResult<PageResult<AmazonWeeklyReportDO>> getReportPage(@Valid WeeklyReportPageReqVO reqVO) {
        return success(weeklyReportService.getReportPage(reqVO));
    }
}
