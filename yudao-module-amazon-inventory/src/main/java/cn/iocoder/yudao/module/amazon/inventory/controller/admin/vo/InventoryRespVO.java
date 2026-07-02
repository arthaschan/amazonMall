package cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 库存 Response VO")
@Data
public class InventoryRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "仓库代码")
    private String fulfillmentCenter;

    @Schema(description = "可售数量")
    private Integer availableQty;

    @Schema(description = "预留数量")
    private Integer reservedQty;

    @Schema(description = "入库中数量")
    private Integer inboundQty;

    @Schema(description = "不可售数量")
    private Integer unfulfillableQty;

    @Schema(description = "可售天数")
    private Integer daysOfSupply;

    @Schema(description = "快照日期")
    private LocalDate snapshotDate;
}
