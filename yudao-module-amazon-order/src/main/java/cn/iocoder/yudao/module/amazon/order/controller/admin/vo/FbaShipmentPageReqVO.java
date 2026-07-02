package cn.iocoder.yudao.module.amazon.order.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - FBA 货件分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class FbaShipmentPageReqVO extends PageParam {

    @Schema(description = "货件 ID")
    private String shipmentId;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "货件状态")
    private String status;
}
