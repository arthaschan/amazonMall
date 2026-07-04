package cn.iocoder.yudao.module.amazon.report.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - 仪表盘查询 Request VO")
@Data
public class DashboardReqVO {

    @Schema(description = "店铺 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "店铺 ID 不能为空")
    private Long shopId;

    @Schema(description = "开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
