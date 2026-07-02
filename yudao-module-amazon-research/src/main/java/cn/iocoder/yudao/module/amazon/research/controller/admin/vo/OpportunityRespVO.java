package cn.iocoder.yudao.module.amazon.research.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 产品机会 Response VO")
@Data
public class OpportunityRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "品类 ID")
    private Long nicheId;

    @Schema(description = "ASIN")
    private String asin;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "价格")
    private BigDecimal price;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "评论数")
    private Integer reviewCount;

    @Schema(description = "BSR")
    private Integer bsr;

    @Schema(description = "月搜索量")
    private Long monthlySearchVolume;

    @Schema(description = "预估月销量")
    private Integer estimatedMonthlySales;

    @Schema(description = "利润率")
    private BigDecimal profitMargin;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
