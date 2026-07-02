package cn.iocoder.yudao.module.amazon.report.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 对话 Response VO")
@Data
public class AiChatRespVO {

    @Schema(description = "AI 回答")
    private String answer;

    @Schema(description = "数据依据 JSON")
    private String dataReference;
}
