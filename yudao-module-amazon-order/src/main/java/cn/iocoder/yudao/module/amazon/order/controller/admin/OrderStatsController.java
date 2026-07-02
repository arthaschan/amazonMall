package cn.iocoder.yudao.module.amazon.order.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderStatsVO;
import cn.iocoder.yudao.module.amazon.order.service.OrderStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 订单统计")
@RestController
@RequestMapping("/amazon/order/stats")
@Validated
public class OrderStatsController {

    @Resource
    private OrderStatsService orderStatsService;

    @GetMapping("/summary")
    @Operation(summary = "订单统计概览")
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "shopId", description = "店铺 ID", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "start", description = "开始日期"),
            @io.swagger.v3.oas.annotations.Parameter(name = "end", description = "结束日期")
    })
    @PreAuthorize("@ss.hasPermission('amazon:order:stats:query')")
    public CommonResult<OrderStatsVO> getStats(
            @RequestParam Long shopId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return success(orderStatsService.getStats(shopId, start, end));
    }
}
