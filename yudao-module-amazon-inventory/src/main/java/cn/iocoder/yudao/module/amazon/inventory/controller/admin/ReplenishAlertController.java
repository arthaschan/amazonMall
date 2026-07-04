package cn.iocoder.yudao.module.amazon.inventory.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.ReplenishAlertPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.dal.dataobject.AmazonReplenishAlertDO;
import cn.iocoder.yudao.module.amazon.inventory.service.ReplenishAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 补货预警")
@RestController
@RequestMapping("/amazon/inventory/alert")
@Validated
public class ReplenishAlertController {

    @Resource
    private ReplenishAlertService alertService;

    @GetMapping("/page")
    @Operation(summary = "预警分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:inventory:alert:query')")
    public CommonResult<PageResult<AmazonReplenishAlertDO>> getAlertPage(@Valid ReplenishAlertPageReqVO reqVO) {
        return success(alertService.getAlertPage(reqVO));
    }

    @PostMapping("/acknowledge")
    @Operation(summary = "确认预警")
    @Parameter(name = "id", description = "预警 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:inventory:alert:update')")
    public CommonResult<Boolean> acknowledge(@RequestParam Long id) {
        alertService.acknowledgeAlert(id);
        return success(true);
    }

    @PostMapping("/scan")
    @Operation(summary = "扫描并生成预警")
    @Parameter(name = "shopId", description = "店铺 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:inventory:alert:scan')")
    public CommonResult<Boolean> scanAndAlert(@RequestParam Long shopId) {
        alertService.scanAndAlert(shopId);
        return success(true);
    }
}
