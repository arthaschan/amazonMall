package cn.iocoder.yudao.module.amazon.report.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.report.controller.admin.vo.DashboardRespVO;
import cn.iocoder.yudao.module.amazon.report.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 经营仪表盘")
@RestController
@RequestMapping("/amazon/report/dashboard")
@Validated
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "仪表盘概览")
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "shopId", description = "店铺 ID", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "startDate", description = "开始日期"),
            @io.swagger.v3.oas.annotations.Parameter(name = "endDate", description = "结束日期")
    })
    @PreAuthorize("@ss.hasPermission('amazon:report:dashboard:query')")
    public CommonResult<DashboardRespVO> getDashboard(
            @RequestParam Long shopId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return success(dashboardService.getDashboard(shopId, startDate, endDate));
    }
}
