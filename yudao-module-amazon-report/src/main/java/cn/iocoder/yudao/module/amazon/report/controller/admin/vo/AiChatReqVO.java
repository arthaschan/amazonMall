package cn.iocoder.yudao.module.amazon.report.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI 对话 Request VO")
@Data
public class AiChatReqVO {

    @Schema(description = "店铺 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "店铺 ID 不能为空")
    private Long shopId;

    @Schema(description = "用户问题", requiredMode = Schema.RequiredMode.REQUIRED, example = "上周哪个 ASIN 销量最高？")
    @NotBlank(message = "问题不能为空")
    private String question;
}
