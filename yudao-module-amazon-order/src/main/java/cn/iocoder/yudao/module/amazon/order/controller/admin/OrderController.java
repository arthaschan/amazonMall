package cn.iocoder.yudao.module.amazon.order.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderPageReqVO;
import cn.iocoder.yudao.module.amazon.order.controller.admin.vo.OrderRespVO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderDO;
import cn.iocoder.yudao.module.amazon.order.dal.dataobject.AmazonOrderItemDO;
import cn.iocoder.yudao.module.amazon.order.dal.mysql.AmazonOrderItemMapper;
import cn.iocoder.yudao.module.amazon.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 订单管理")
@RestController
@RequestMapping("/amazon/order")
@Validated
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private AmazonOrderItemMapper orderItemMapper;

    @GetMapping("/get")
    @Operation(summary = "获取订单详情")
    @Parameter(name = "id", description = "订单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('amazon:order:query')")
    public CommonResult<OrderRespVO> getOrder(@RequestParam Long id) {
        AmazonOrderDO order = orderService.getOrder(id);
        OrderRespVO vo = BeanUtils.toBean(order, OrderRespVO.class);
        if (vo != null) {
            List<AmazonOrderItemDO> items = orderItemMapper.selectByOrderId(id);
            vo.setItems(BeanUtils.toBean(items, OrderRespVO.OrderItemVO.class));
        }
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "订单分页列表")
    @PreAuthorize("@ss.hasPermission('amazon:order:query')")
    public CommonResult<PageResult<OrderRespVO>> getOrderPage(@Valid OrderPageReqVO reqVO) {
        PageResult<AmazonOrderDO> page = orderService.getOrderPage(reqVO);
        return success(BeanUtils.toBean(page, OrderRespVO.class));
    }
}
