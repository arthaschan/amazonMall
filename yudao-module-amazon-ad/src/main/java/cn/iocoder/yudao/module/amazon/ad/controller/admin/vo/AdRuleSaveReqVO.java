package cn.iocoder.yudao.module.amazon.ad.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 广告规则保存 Request VO")
@Data
public class AdRuleSaveReqVO {

    @Schema(description = "规则 ID（更新时必填）")
    private Long id;

    @Schema(description = "店铺 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "店铺 ID 不能为空")
    private Long shopId;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @Schema(description = "作用范围: CAMPAIGN / ADGROUP / KEYWORD")
    @NotBlank(message = "作用范围不能为空")
    private String scope;

    @Schema(description = "条件 JSON")
    private String conditionJson;

    @Schema(description = "动作 JSON")
    private String actionJson;
}
