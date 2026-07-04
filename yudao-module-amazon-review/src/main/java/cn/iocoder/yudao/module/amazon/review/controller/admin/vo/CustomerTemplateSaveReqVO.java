package cn.iocoder.yudao.module.amazon.review.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 客服模板保存 Request VO")
@Data
public class CustomerTemplateSaveReqVO {

    @Schema(description = "模板 ID（更新时必填）")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "模板类型: REFUND / EXCHANGE / GUIDE / THANKS / APOLOGY")
    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "主题")
    private String subject;

    @Schema(description = "正文")
    private String body;

    @Schema(description = "变量列表")
    private List<String> variables;
}
