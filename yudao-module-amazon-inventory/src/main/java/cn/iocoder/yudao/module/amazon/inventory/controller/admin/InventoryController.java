package cn.iocoder.yudao.module.amazon.inventory.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.InventoryPageReqVO;
import cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo.InventoryRespVO;
import cn.iocoder.yudao.module.amazon.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存管理")
@RestController
@RequestMapping("/amazon/inventory")
@Validated
public class InventoryController {

    @Resource
    private InventoryService inventoryService;

    @GetMapping("/page")
    @Operation(summary = "库存分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:inventory:query')")
    public CommonResult<PageResult<InventoryRespVO>> getInventoryPage(@Valid InventoryPageReqVO reqVO) {
        var page = inventoryService.getInventoryPage(reqVO);
        return success(BeanUtils.toBean(page, InventoryRespVO.class));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步库存")
    @io.swagger.v3.oas.annotations.Parameter(name = "shopId", description = "店铺 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:inventory:sync')")
    public CommonResult<Boolean> syncInventory(@RequestParam Long shopId) {
        inventoryService.syncInventory(shopId);
        return success(true);
    }
}
