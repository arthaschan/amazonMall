package cn.iocoder.yudao.module.amazon.research.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 品类 Response VO")
@Data
public class NicheRespVO {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "品类名称")
    private String name;

    @Schema(description = "站点 ID")
    private String marketplaceId;

    @Schema(description = "所属类目")
    private String category;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "全知评分")
    private BigDecimal omniscientScore;

    @Schema(description = "需求评分")
    private BigDecimal demandScore;

    @Schema(description = "竞争评分")
    private BigDecimal competitionScore;

    @Schema(description = "盈利评分")
    private BigDecimal profitabilityScore;

    @Schema(description = "评论护城河评分")
    private BigDecimal reviewMoatScore;

    @Schema(description = "价格稳定性评分")
    private BigDecimal priceStabilityScore;

    @Schema(description = "季节性评分")
    private BigDecimal seasonalityScore;

    @Schema(description = "自然排名评分")
    private BigDecimal organicRankScore;

    @Schema(description = "广告依赖度评分")
    private BigDecimal adDependencyScore;

    @Schema(description = "供应商评分")
    private BigDecimal supplierScore;

    @Schema(description = "评分权重")
    private Map<String, BigDecimal> scoreWeights;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
