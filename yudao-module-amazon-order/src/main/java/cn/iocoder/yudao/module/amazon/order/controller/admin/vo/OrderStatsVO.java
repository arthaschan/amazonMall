package cn.iocoder.yudao.module.amazon.order.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单统计数据 DTO (record-style)。
 */
@Data
@Schema(description = "管理后台 - 订单统计 VO")
public class OrderStatsVO {

    @Schema(description = "总订单数")
    private Integer totalOrders;

    @Schema(description = "总销售额")
    private BigDecimal totalSales;

    @Schema(description = "平均客单价")
    private BigDecimal avgOrderValue;

    @Schema(description = "FBA 订单占比")
    private BigDecimal fbaOrderRate;

    @Schema(description = "退款订单数")
    private Integer refundOrders;
}
