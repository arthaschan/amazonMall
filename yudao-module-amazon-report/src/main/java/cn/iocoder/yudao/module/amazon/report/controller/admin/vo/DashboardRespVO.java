package cn.iocoder.yudao.module.amazon.report.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 仪表盘 Response VO")
@Data
public class DashboardRespVO {

    @Schema(description = "总销售额")
    private BigDecimal totalSales;

    @Schema(description = "总订单数")
    private Integer totalOrders;

    @Schema(description = "广告花费")
    private BigDecimal adSpend;

    @Schema(description = "利润")
    private BigDecimal profit;

    @Schema(description = "库存健康评分")
    private BigDecimal inventoryHealthScore;

    @Schema(description = "平均评分")
    private BigDecimal avgRating;

    @Schema(description = "趋势数据")
    private List<TrendPoint> trends;

    @Data
    @Schema(description = "趋势数据点")
    public static class TrendPoint {
        @Schema(description = "日期")
        private String date;
        @Schema(description = "销售额")
        private BigDecimal sales;
        @Schema(description = "订单数")
        private Integer orders;
        @Schema(description = "利润")
        private BigDecimal profit;
    }
}
