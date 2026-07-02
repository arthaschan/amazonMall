package cn.iocoder.yudao.module.amazon.order.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.FbaShipmentPageReqVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonFbaShipmentDO;
import cn.iocoder.yudao.module.amazon.order.service.FbaShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - FBA 货件管理")
@RestController
@RequestMapping("/amazon/order/shipment")
@Validated
public class FbaShipmentController {

    @Resource
    private FbaShipmentService fbaShipmentService;

    @GetMapping("/get")
    @Operation(summary = "获取货件详情")
    @Parameter(name = "id", description = "货件 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:order:shipment:query')")
    public CommonResult<AmazonFbaShipmentDO> getShipment(@RequestParam Long id) {
        return success(fbaShipmentService.getShipment(id));
    }

    @GetMapping("/page")
    @Operation(summary = "货件分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:order:shipment:query')")
    public CommonResult<PageResult<AmazonFbaShipmentDO>> getShipmentPage(@Valid FbaShipmentPageReqVO reqVO) {
        return success(fbaShipmentService.getShipmentPage(reqVO));
    }
}
