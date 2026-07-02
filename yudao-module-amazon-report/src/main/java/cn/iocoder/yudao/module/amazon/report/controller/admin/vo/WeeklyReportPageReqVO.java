package cn.iocoder.yudao.module.amazon.report.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 周报分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WeeklyReportPageReqVO extends PageParam {

    @Schema(description = "店铺 ID")
    private Long shopId;

    @Schema(description = "报告周 (如 2024-W03)")
    private String reportWeek;
}
