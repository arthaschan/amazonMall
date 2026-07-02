package cn.iocoder.yudao.module.amazon.inventory.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.InventoryForecastRespVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonInventoryForecastDO;
import cn.iocoder.yudao.module.amazon.inventory.service.InventoryForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存预测")
@RestController
@RequestMapping("/amazon/inventory/forecast")
@Validated
public class InventoryForecastController {

    @Resource
    private InventoryForecastService forecastService;

    @PostMapping("/generate")
    @Operation(summary = "生成库存预测")
    @Parameters({
            @io.swagger.v3.oas.annotations.Parameter(name = "shopId", description = "店铺 ID", required = true),
            @io.swagger.v3.oas.annotations.Parameter(name = "asin", description = "ASIN", required = true)
    })
    @PreAuthorize("@ss.hasPermission('amazon:inventory:forecast:generate')")
    public CommonResult<InventoryForecastRespVO> generateForecast(
            @RequestParam Long shopId, @RequestParam String asin) {
        var forecast = forecastService.generateForecast(shopId, asin);
        return success(BeanUtils.toBean(forecast, InventoryForecastRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "预测记录列表")
    @PreAuthorize("@ss.hasPermission('amazon:inventory:forecast:query')")
    public CommonResult<List<InventoryForecastRespVO>> getForecasts(
            @RequestParam Long shopId, @RequestParam String asin) {
        var list = forecastService.getForecasts(shopId, asin);
        return success(BeanUtils.toBean(list, InventoryForecastRespVO.class));
    }

    @GetMapping("/latest")
    @Operation(summary = "最新预测")
    @PreAuthorize("@ss.hasPermission('amazon:inventory:forecast:query')")
    public CommonResult<InventoryForecastRespVO> getLatest(
            @RequestParam Long shopId, @RequestParam String asin) {
        var forecast = forecastService.getLatestForecast(shopId, asin);
        return success(BeanUtils.toBean(forecast, InventoryForecastRespVO.class));
    }
}
