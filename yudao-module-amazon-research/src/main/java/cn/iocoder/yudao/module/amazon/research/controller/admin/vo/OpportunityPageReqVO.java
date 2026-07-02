package cn.iocoder.yudao.module.amazon.research.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 产品机会分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class OpportunityPageReqVO extends PageParam {

    @Schema(description = "品类 ID", example = "1")
    private Long nicheId;

    @Schema(description = "ASIN", example = "B08XXXXXXX")
    private String asin;
}
