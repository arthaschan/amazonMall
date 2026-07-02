package cn.iocoder.yudao.module.amazon.review.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 客服模板分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerTemplatePageReqVO extends PageParam {

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板类型: REFUND / EXCHANGE / GUIDE / THANKS / APOLOGY")
    private String templateType;

    @Schema(description = "语言")
    private String language;
}
