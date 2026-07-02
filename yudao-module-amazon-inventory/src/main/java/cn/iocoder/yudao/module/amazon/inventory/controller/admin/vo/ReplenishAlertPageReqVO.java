package cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 补货预警分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReplenishAlertPageReqVO extends PageParam {

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "预警类型: LOW_STOCK / OUT_OF_STOCK / OVERSTOCK")
    private String alertType;

    @Schema(description = "是否已确认")
    private Boolean acknowledged;
}
