package cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Schema(description = "管理后台 - 库存分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryPageReqVO extends PageParam {

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "快照日期")
    private LocalDate snapshotDate;
}
