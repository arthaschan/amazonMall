package cn.iocoder.yudao.module.amazon.listing.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI Listing 生成 Request VO")
@Data
public class AiGenerateReqVO {

    @Schema(description = "产品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @Schema(description = "优化方向/提示词", example = "突出环保材质，面向美国市场")
    private String prompt;
}
