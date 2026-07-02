package cn.iocoder.yudao.module.amazon.order.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 订单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageReqVO extends PageParam {

    @Schema(description = "亚马逊订单号")
    private String amazonOrderId;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "站点 ID")
    private String marketplaceId;

    @Schema(description = "订单状态")
    private String orderStatus;

    @Schema(description = "下单时间-开始")
    private LocalDateTime purchaseDateStart;

    @Schema(description = "下单时间-结束")
    private LocalDateTime purchaseDateEnd;
}
