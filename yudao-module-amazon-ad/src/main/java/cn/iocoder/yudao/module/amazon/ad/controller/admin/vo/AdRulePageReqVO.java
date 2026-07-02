package cn.iocoder.yudao.module.amazon.ad.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 广告规则分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AdRulePageReqVO extends PageParam {

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "作用范围: CAMPAIGN / ADGROUP / KEYWORD")
    private String scope;

    @Schema(description = "状态: 0=禁用 1=启用")
    private Integer status;
}
