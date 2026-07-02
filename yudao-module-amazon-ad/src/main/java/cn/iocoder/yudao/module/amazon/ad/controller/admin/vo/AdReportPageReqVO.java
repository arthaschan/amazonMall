package cn.iocoder.yudao.module.amazon.ad.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Schema(description = "管理后台 - 广告日报分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AdReportPageReqVO extends PageParam {

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "广告活动 ID")
    private Long campaignId;

    @Schema(description = "关键词")
    private String keywordText;

    @Schema(description = "报告日期-开始")
    private LocalDate reportDateStart;

    @Schema(description = "报告日期-结束")
    private LocalDate reportDateEnd;
}
