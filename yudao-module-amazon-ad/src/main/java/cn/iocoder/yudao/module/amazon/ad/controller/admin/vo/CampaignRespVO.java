package cn.iocoder.yudao.module.amazon.ad.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 广告活动 Response VO")
@Data
public class CampaignRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "亚马逊活动 ID")
    private Long campaignId;

    @Schema(description = "活动名称")
    private String campaignName;

    @Schema(description = "广告类型")
    private String campaignType;

    @Schema(description = "投放类型")
    private String targetingType;

    @Schema(description = "日预算")
    private BigDecimal dailyBudget;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
