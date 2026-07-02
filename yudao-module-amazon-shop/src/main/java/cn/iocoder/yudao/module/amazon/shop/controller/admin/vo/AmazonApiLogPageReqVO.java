package cn.iocoder.yudao.module.amazon.shop.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Management Console - Amazon API Log Page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AmazonApiLogPageReqVO extends PageParam {

    @Schema(description = "Shop ID (exact match)", example = "1024")
    private Long shopId;

    @Schema(description = "API endpoint (fuzzy match)", example = "/orders/v0/orders")
    private String apiEndpoint;

    @Schema(description = "HTTP method (exact match)", example = "GET")
    private String requestMethod;

    @Schema(description = "HTTP response code (exact match)", example = "200")
    private Integer responseCode;

    @Schema(description = "Creation time range start")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeStart;

    @Schema(description = "Creation time range end")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeEnd;
}
