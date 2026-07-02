package cn.iocoder.yudao.module.amazon.research.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 品类分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class NichePageReqVO extends PageParam {

    @Schema(description = "品类名称", example = "瑜伽垫")
    private String name;

    @Schema(description = "站点 ID", example = "ATVPDKIKX0DER")
    private String marketplaceId;

    @Schema(description = "状态", example = "2")
    private Integer status;
}
