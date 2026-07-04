package cn.iocoder.yudao.module.amazon.research.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "管理后台 - 品类保存 Request VO")
@Data
public class NicheSaveReqVO {

    @Schema(description = "品类 ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "品类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "瑜伽垫")
    @NotBlank(message = "品类名称不能为空")
    private String name;

    @Schema(description = "站点 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "ATVPDKIKX0DER")
    @NotBlank(message = "站点不能为空")
    private String marketplaceId;

    @Schema(description = "所属类目", example = "Sports & Outdoors")
    private String category;

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
}
