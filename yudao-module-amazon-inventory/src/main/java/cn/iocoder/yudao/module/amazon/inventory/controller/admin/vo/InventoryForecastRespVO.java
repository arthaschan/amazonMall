package cn.iocoder.yudao.module.amazon.inventory.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 库存预测 Response VO")
@Data
public class InventoryForecastRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "预测日期")
    private LocalDate forecastDate;

    @Schema(description = "预测日均销量")
    private BigDecimal predictedDailySales;

    @Schema(description = "置信度")
    private BigDecimal confidence;

    @Schema(description = "再订购点")
    private Integer reorderPoint;

    @Schema(description = "安全库存")
    private Integer safetyStock;

    @Schema(description = "建议补货量")
    private Integer suggestedReorderQty;

    @Schema(description = "备货周期（天）")
    private Integer leadTimeDays;
}
