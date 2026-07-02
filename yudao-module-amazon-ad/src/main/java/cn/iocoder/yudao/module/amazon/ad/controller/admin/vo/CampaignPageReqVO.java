package cn.iocoder.yudao.module.amazon.ad.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 广告活动分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CampaignPageReqVO extends PageParam {

    @Schema(description = "活动名称")
    private String campaignName;

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "广告类型: SP / SB / SD")
    private String campaignType;

    @Schema(description = "状态")
    private String status;
}
